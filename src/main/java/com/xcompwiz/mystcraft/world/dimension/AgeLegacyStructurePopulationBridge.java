package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.world.worldgen.AgeFeatureKind;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Places the modern StructureStarts backing the four Legacy MapGenStructure Symbols at the
 * point where Mystcraft 0.13.7.06 called {@code MapGenStructure.generateStructure(...)}.
 *
 * <p>Legacy terrain alteration prepared StructureStarts before population. Population then used
 * the shifted 16x16 box {@code [chunk*16+8, chunk*16+23]} and the same population Random shared
 * with the rest of AgeController. Minecraft 1.21.1 normally places structure pieces earlier from
 * {@link ChunkGenerator#applyBiomeDecoration}; runtime Ages suppress that automatic placement and
 * route only these Symbol-selected starts through this bridge instead.</p>
 *
 * <p>Mineshaft, Stronghold and Nether Fortress populators ignored generateStructure's return value
 * and returned {@code false}; Village returned the value directly. Callers therefore use the
 * boolean result only for the Village short-circuit contract.</p>
 */
final class AgeLegacyStructurePopulationBridge {
    /**
     * WorldGenRegion only exposes the chunks participating in the active generation step.
     * Legacy's shifted +8..23 population box can ask StructureManager about the positive
     * neighbour even when that neighbour is not present in this particular FEATURES region.
     * 1.21.1 throws instead of returning an empty list in that case. Record the first instance
     * of each center/target pair so a bad Age cannot kill the integrated server or roll back a
     * save merely because a neighbouring StructureStart is not yet visible.
     */
    private static final Set<String> LOGGED_UNAVAILABLE_WORLDGEN_CHUNKS = ConcurrentHashMap.newKeySet();

    private AgeLegacyStructurePopulationBridge() {}

    static boolean place(
            WorldGenLevel level,
            ChunkGenerator generator,
            RandomSource random,
            AgeFeatureKind kind,
            int chunkBlockX,
            int chunkBlockZ) {

        if (!(level instanceof WorldGenRegion region)) return false;
        Predicate<Structure> predicate = predicate(level, kind);

        int minX = chunkBlockX + 8;
        int minZ = chunkBlockZ + 8;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        BoundingBox populationBox = new BoundingBox(
                minX,
                level.getMinBuildHeight() + 1,
                minZ,
                maxX,
                level.getMaxBuildHeight() - 1,
                maxZ);

        StructureManager structures = region.getLevel().structureManager().forWorldGenRegion(region);
        int minChunkX = Math.floorDiv(minX, 16);
        int maxChunkX = Math.floorDiv(maxX, 16);
        int minChunkZ = Math.floorDiv(minZ, 16);
        int maxChunkZ = Math.floorDiv(maxZ, 16);
        ChunkPos populationChunk = new ChunkPos(Math.floorDiv(chunkBlockX, 16), Math.floorDiv(chunkBlockZ, 16));
        Set<StructureStart> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean placed = false;

        /*
         * CP270: Never call StructureManager.startsForStructure() from FEATURES worldgen.
         * That convenience method follows every structure-reference long back to the start
         * chunk with LevelReader#getChunk(). A perfectly valid reference can point beyond the
         * finite WorldGenRegion cache; 1.21.1 then throws
         * "Requested chunk unavailable during world generation" and kills the integrated server.
         *
         * Instead, inspect only ChunkAccess objects which WorldGenRegion says are already
         * available. This is the same data startsForStructure() consumes, but without allowing
         * the helper to force/read a chunk outside the active generation region.
         */
        for (int cx = minChunkX; cx <= maxChunkX; ++cx) {
            for (int cz = minChunkZ; cz <= maxChunkZ; ++cz) {
                ChunkPos queryChunk = new ChunkPos(cx, cz);
                // CP272: WorldGenRegion#getChunk(..., false) still throws when the requested
                // chunk lies outside the active generation cache. Probe hasChunk() first; do
                // not rely on the create=false flag as a non-throwing availability check.
                if (!region.hasChunk(cx, cz)) {
                    logUnavailableWorldgenChunk(region, kind, populationChunk, queryChunk, "reference chunk");
                    continue;
                }
                ChunkAccess referenceChunk = region.getChunk(cx, cz, ChunkStatus.STRUCTURE_REFERENCES, false);
                if (referenceChunk == null) {
                    logUnavailableWorldgenChunk(region, kind, populationChunk, queryChunk, "reference chunk");
                    continue;
                }

                for (Map.Entry<Structure, it.unimi.dsi.fastutil.longs.LongSet> entry
                        : referenceChunk.getAllReferences().entrySet()) {
                    Structure structure = entry.getKey();
                    if (!predicate.test(structure)) continue;

                    for (long reference : entry.getValue()) {
                        int startChunkX = ChunkPos.getX(reference);
                        int startChunkZ = ChunkPos.getZ(reference);
                        if (!region.hasChunk(startChunkX, startChunkZ)) {
                            logUnavailableStructureStart(
                                    region, kind, populationChunk, queryChunk,
                                    new ChunkPos(startChunkX, startChunkZ));
                            continue;
                        }
                        ChunkAccess startChunk = region.getChunk(
                                startChunkX, startChunkZ, ChunkStatus.STRUCTURE_STARTS, false);
                        if (startChunk == null) {
                            logUnavailableStructureStart(
                                    region, kind, populationChunk, queryChunk,
                                    new ChunkPos(startChunkX, startChunkZ));
                            continue;
                        }

                        StructureStart start = startChunk.getStartForStructure(structure);
                        if (start == null || !seen.add(start) || !start.isValid()) continue;
                        BoundingBox structureBox = start.getBoundingBox();
                        if (!structureBox.intersects(populationBox)) continue;

                        start.placeInChunk(
                                level,
                                structures,
                                generator,
                                random,
                                populationBox,
                                populationChunk);
                        placed = true;
                        if (kind == AgeFeatureKind.STRONGHOLDS) {
                            Mystcraft.LOGGER.debug(
                                    "Stronghold start placed: startChunk={}, populationChunk={}, box={}",
                                    start.getChunkPos(), populationChunk, structureBox);
                        }
                    }
                }
            }
        }

        return placed;
    }


    private static void logUnavailableWorldgenChunk(
            WorldGenRegion region,
            AgeFeatureKind kind,
            ChunkPos populationChunk,
            ChunkPos queryChunk,
            String unavailablePart) {
        ChunkPos center = region.getCenter();
        String key = region.getLevel().dimension().location()
                + "|" + kind
                + "|" + center.x + "," + center.z
                + "|" + queryChunk.x + "," + queryChunk.z;
        if (LOGGED_UNAVAILABLE_WORLDGEN_CHUNKS.add(key)) {
            Mystcraft.LOGGER.warn(
                    "Skipped unavailable worldgen structure-reference chunk: dimension={}, kind={}, "
                            + "regionCenter={}, populationChunk={}, queryChunk={}, unavailable={}. "
                            + "No out-of-region chunk lookup was attempted.",
                    region.getLevel().dimension().location(),
                    kind,
                    center,
                    populationChunk,
                    queryChunk,
                    unavailablePart);
        }
    }

    private static void logUnavailableStructureStart(
            WorldGenRegion region,
            AgeFeatureKind kind,
            ChunkPos populationChunk,
            ChunkPos queryChunk,
            ChunkPos startChunk) {
        ChunkPos center = region.getCenter();
        String key = region.getLevel().dimension().location()
                + "|start|" + kind
                + "|" + center.x + "," + center.z
                + "|" + queryChunk.x + "," + queryChunk.z
                + "|" + startChunk.x + "," + startChunk.z;
        if (LOGGED_UNAVAILABLE_WORLDGEN_CHUNKS.add(key)) {
            Mystcraft.LOGGER.warn(
                    "Skipped out-of-region structure start during FEATURES: dimension={}, kind={}, "
                            + "regionCenter={}, populationChunk={}, queryChunk={}, startChunk={}. "
                            + "The reference is valid, but its start chunk is not resident in this WorldGenRegion; "
                            + "skipping is safer than recursively requesting worldgen and crashing the server.",
                    region.getLevel().dimension().location(),
                    kind,
                    center,
                    populationChunk,
                    queryChunk,
                    startChunk);
        }
    }

    private static Predicate<Structure> predicate(WorldGenLevel level, AgeFeatureKind kind) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        return structure -> matches(registry.getKey(structure), kind);
    }

    static boolean matches(ResourceLocation id, AgeFeatureKind kind) {
        if (id == null || !"minecraft".equals(id.getNamespace())) return false;
        String path = id.getPath();
        return switch (kind) {
            case MINESHAFTS -> "mineshaft".equals(path) || "mineshaft_mesa".equals(path);
            case STRONGHOLDS -> "stronghold".equals(path);
            case VILLAGES -> path.startsWith("village_");
            case NETHER_FORTRESS -> "fortress".equals(path);
            default -> false;
        };
    }
    static void clearDiagnostics() {
        LOGGED_UNAVAILABLE_WORLDGEN_CHUNKS.clear();
    }

}
