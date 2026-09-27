package com.xcompwiz.mystcraft.world.dimension;

import com.mojang.datafixers.util.Pair;
import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.world.worldgen.AgeLegacyNetherFortressPlacementMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.HashSet;
import java.util.Set;

/** Locate compatibility for the 0.13.7.06 Nether Fortress Symbol. */
final class AgeLegacyStructureLocateBridge {
    private static final ResourceKey<Structure> FORTRESS = ResourceKey.create(
            Registries.STRUCTURE, ResourceLocation.withDefaultNamespace("fortress"));
    private static final int LEGACY_REGION_SIZE = 16;

    private AgeLegacyStructureLocateBridge() {}

    /**
     * Minecraft 1.21.1's StructureCheck refuses an ungenerated minecraft:fortress when the
     * active biome is not a Nether biome.  Mystcraft's legacy Symbol intentionally ignores that
     * biome restriction, so /locate must force only the old fortress candidate chunks through
     * STRUCTURE_STARTS and inspect the registered vanilla StructureStart directly.
     */
    static Pair<BlockPos, Holder<Structure>> findNearestFortress(
            ServerLevel level,
            HolderSet<Structure> requested,
            BlockPos origin,
            int searchRadius,
            boolean skipKnownStructures) {

        Holder<Structure> fortressHolder = null;
        for (Holder<Structure> holder : requested) {
            if (holder.is(FORTRESS)) {
                fortressHolder = holder;
                break;
            }
        }
        if (fortressHolder == null) return null;

        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        AgeLegacyNetherFortressPlacement placement = findLegacyPlacement(state);
        if (placement == null) return null;

        int centerChunkX = SectionPos.blockToSectionCoord(origin.getX());
        int centerChunkZ = SectionPos.blockToSectionCoord(origin.getZ());
        long seed = state.getLevelSeed();
        var structureManager = level.structureManager();
        Set<Long> visitedCandidates = new HashSet<>();

        for (int ring = 0; ring <= searchRadius; ring++) {
            Pair<BlockPos, Holder<Structure>> best = null;
            double bestDistance = Double.MAX_VALUE;

            for (int dz = -ring; dz <= ring; dz++) {
                boolean edgeZ = dz == -ring || dz == ring;
                for (int dx = -ring; dx <= ring; dx++) {
                    if (!edgeZ && dx != -ring && dx != ring) continue;

                    int queryChunkX = centerChunkX + LEGACY_REGION_SIZE * dx;
                    int queryChunkZ = centerChunkZ + LEGACY_REGION_SIZE * dz;
                    AgeLegacyNetherFortressPlacementMath.Candidate candidate =
                            AgeLegacyNetherFortressPlacementMath.candidate(seed, queryChunkX, queryChunkZ);
                    if (!candidate.enabled()) continue;

                    ChunkPos candidatePos = new ChunkPos(candidate.chunkX(), candidate.chunkZ());
                    if (!visitedCandidates.add(candidatePos.toLong())) continue;

                    ChunkAccess chunk = level.getChunk(
                            candidatePos.x,
                            candidatePos.z,
                            ChunkStatus.STRUCTURE_STARTS);
                    StructureStart start = structureManager.getStartForStructure(
                            SectionPos.bottomOf(chunk), fortressHolder.value(), chunk);
                    if (start == null || !start.isValid()) continue;

                    if (skipKnownStructures) {
                        if (!start.canBeReferenced()) continue;
                        structureManager.addReference(start);
                    }

                    BlockPos locatePos = placement.getLocatePos(start.getChunkPos());
                    double distance = origin.distSqr(locatePos);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = Pair.of(locatePos, fortressHolder);
                    }
                }
            }

            if (best != null) {
                Mystcraft.LOGGER.debug(
                        "Legacy Nether Fortress locate: dimension={}, origin={}, result={}, regionRing={}",
                        level.dimension().location(), origin, best.getFirst(), ring);
                return best;
            }
        }

        Mystcraft.LOGGER.warn(
                "Legacy Nether Fortress locate exhausted: dimension={}, origin={}, searchRadius={}",
                level.dimension().location(), origin, searchRadius);
        return null;
    }

    private static AgeLegacyNetherFortressPlacement findLegacyPlacement(ChunkGeneratorStructureState state) {
        for (Holder<StructureSet> holderSet : state.possibleStructureSets()) {
            StructureSet set = holderSet.value();
            if (!(set.placement() instanceof AgeLegacyNetherFortressPlacement placement)) continue;
            boolean containsFortress = set.structures().stream()
                    .anyMatch(entry -> entry.structure().is(FORTRESS));
            if (containsFortress) return placement;
        }
        return null;
    }
}
