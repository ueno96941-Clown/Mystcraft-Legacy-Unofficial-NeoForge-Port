package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.world.worldgen.AgeLocalGenerationPlan;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;

import java.util.List;
import java.util.Objects;

/**
 * Replaces the runtime Age's structure-placement state with one built only from
 * the StructureSets requested by that Age.
 *
 * <p>This is done after ServerLevel construction, because ServerChunkCache/ChunkMap
 * create the ordinary structure state during their constructors. Replacing the
 * per-level state avoids mutating the global StructureSet registry.</p>
 */
public final class AgeStructureStateInstaller {
    private AgeStructureStateInstaller() {}

    public static void install(
            ServerLevel level,
            AgeLocalGenerationPlan plan,
            long ageSeed) {

        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(plan, "plan");

        var selected = AgeStructureSetSelection.resolve(level.registryAccess(), plan);
        var chunkSource = level.getChunkSource();
        var generator = chunkSource.getGenerator();

        // createForFlat() hard-codes concentricRingsSeed=0. That is unsuitable for
        // Mystcraft because the Strongholds symbol must remain Age-seed deterministic.
        // Construct the same state explicitly with both the ordinary placement seed and
        // concentric-ring seed bound to the durable Age seed. The constructor is exposed
        // narrowly by our access transformer; the selected holder list stays Age-local.
        ChunkGeneratorStructureState state = new ChunkGeneratorStructureState(
                chunkSource.randomState(),
                generator.getBiomeSource(),
                ageSeed,
                ageSeed,
                List.copyOf(selected));

        // CP329: ModernFix's cache_strongholds mixin attaches a per-dimension cache path
        // and server reference to the structure state created by ServerLevel's constructor.
        // Mystcraft replaces that state here, after the constructor hook has already run.
        // Re-attach the same optional runtime contract before ensureStructuresGenerated(),
        // otherwise ModernFix's redirected stronghold worker executor is left null.
        AgeModernFixStrongholdCacheBridge.attachIfPresent(level, state);

        // Vanilla ServerLevel eagerly initializes the structure state it creates during
        // construction. We replace that object, so replay the same initialization on the
        // replacement before publishing it to ChunkMap. This also starts concentric-ring
        // biome-search futures early rather than deferring them to the first structure query.
        state.ensureStructuresGenerated();

        // CP246 deliberately keeps Minecraft 1.21.1's native structure-placement RNG and
        // concentric-ring implementation. Mystcraft still decides which StructureSets exist;
        // Minecraft decides how those requested structures are placed in a modern world.

        logStrongholdDiagnostics(level, state, plan, ageSeed);
        chunkSource.chunkMap.chunkGeneratorState = state;
    }

    private static void logStrongholdDiagnostics(
            ServerLevel level,
            ChunkGeneratorStructureState state,
            AgeLocalGenerationPlan plan,
            long ageSeed) {
        if (!plan.allowedStructureSetIds().contains("minecraft:strongholds")) return;

        for (var holder : state.possibleStructureSets()) {
            if (!(holder.value().placement() instanceof ConcentricRingsStructurePlacement placement)) continue;
            List<ChunkPos> positions = state.getRingPositionsFor(placement);
            if (positions == null || positions.isEmpty()) {
                Mystcraft.LOGGER.warn(
                        "Stronghold ring generation produced no positions for Age {} (seed={})",
                        level.dimension().location(), ageSeed);
                continue;
            }

            String first = positions.stream().limit(3)
                    .map(pos -> "chunk(" + pos.x + "," + pos.z + ") / block("
                            + (pos.getMinBlockX() + 8) + "," + (pos.getMinBlockZ() + 8) + ")")
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("<none>");
            Mystcraft.LOGGER.debug(
                    "Stronghold ring state for Age {}: seed={}, ringCount={}, first={}",
                    level.dimension().location(), ageSeed, positions.size(), first);
        }
    }
}
