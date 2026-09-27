package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.AgeFeatureKind;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.List;

/**
 * Executes the direct Mystcraft IPopulate ports as one post-biome-decoration feature.
 *
 * <p>Legacy AgeController passed one Random instance through its IPopulate list in Symbol
 * registration order. The expression was {@code flag = flag || mod.populate(...)}, so a
 * successful Village populator did more than pass {@code flag=true}: Java short-circuiting
 * stopped every later IPopulate entirely, including its RNG consumption. Modern StructureStarts
 * are prepared earlier, but the four MapGenStructure Symbols place their pieces here in the
 * authored IPopulate order so Village can preserve that exact control-flow role.</p>
 *
 * <p>Minecraft 1.21.1 resets a decorator seed for each PlacedFeature, while Legacy had already
 * advanced its population Random through biome decoration and animal spawning before entering
 * AgeController. That pre-controller RNG state cannot be reconstructed without replaying those
 * world-dependent operations. We intentionally keep Minecraft's supplied batch RandomSource
 * rather than pretending the legacy boundary seed is the exact post-decoration state.</p>
 */
public final class AgeLegacyPopulationBatchFeature extends Feature<NoneFeatureConfiguration> {
    public record PopulationStep(AgeFeatureKind kind, AgeLegacyPopulationFeature feature) {
        public static PopulationStep starFissureSentinel() {
            return new PopulationStep(AgeFeatureKind.STAR_FISSURE, null);
        }

        /** StructureStart exists already; piece placement is deferred into Legacy population. */
        public static PopulationStep legacyStructure(AgeFeatureKind kind) {
            if (kind != AgeFeatureKind.MINESHAFTS
                    && kind != AgeFeatureKind.STRONGHOLDS
                    && kind != AgeFeatureKind.NETHER_FORTRESS
                    && kind != AgeFeatureKind.VILLAGES) {
                throw new IllegalArgumentException("Not a Legacy structure population kind: " + kind);
            }
            return new PopulationStep(kind, null);
        }

        public static PopulationStep direct(AgeLegacyPopulationFeature feature) {
            return new PopulationStep(feature.kind(), feature);
        }
    }

    private final List<PopulationStep> orderedSteps;
    private final AgeLegacySmallLibraryFeature smallLibrary = new AgeLegacySmallLibraryFeature();

    public AgeLegacyPopulationBatchFeature(List<PopulationStep> orderedSteps) {
        super(NoneFeatureConfiguration.CODEC);
        this.orderedSteps = List.copyOf(orderedSteps);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        // MapGenScatteredFeatureMyst was unconditional in every Legacy Age. Run the
        // Small Library before the Symbol-authored Mystcraft population stream.
        int chunkX = Math.floorDiv(context.origin().getX(), 16) * 16;
        int chunkZ = Math.floorDiv(context.origin().getZ(), 16) * 16;

        // Legacy population from neighboring chunks may have staged exact-position writes
        // for this chunk because 1.21.1 forbids writing beyond the active FEATURES radius.
        // Apply them now, while this chunk is the WorldGenRegion center and the writes are legal.
        AgeLegacyDeferredBlockWrites.drainForCurrentChunk(context.level(), chunkX, chunkZ);

        boolean any = smallLibrary.place(context);

        for (int index = 0; index < orderedSteps.size(); ++index) {
            PopulationStep step = orderedSteps.get(index);
            if (isLegacyStructure(step.kind())) {
                boolean placed = AgeLegacyStructurePopulationBridge.place(
                        context.level(),
                        context.chunkGenerator(),
                        context.random(),
                        step.kind(),
                        chunkX,
                        chunkZ);
                any |= placed;

                // Only SymbolVillages returned MapGenStructure.generateStructure's boolean.
                // Mineshaft/Stronghold/NetherFort performed the same placement call but then
                // returned false, so only a successful Village activates AgeController's
                // flag = flag || mod.populate(...) short-circuit.
                if (step.kind() == AgeFeatureKind.VILLAGES && placed) {
                    if (containsStarFissureAfter(index)) {
                        AgeStarFissurePopulationBridge.suppressIfSpawnChunk(
                                context.level(), chunkX, chunkZ);
                    }
                    return any;
                }
                continue;
            }
            if (step.kind() == AgeFeatureKind.STAR_FISSURE) {
                // WorldGenMystStarFissure#generate always returned true. Reaching this Symbol
                // in the spawn chunk therefore consumes its RNG exactly once and terminates the
                // remaining Legacy IPopulate list. The cross-chunk block writes are deferred to
                // the server thread by AgeStarFissurePopulationBridge.
                if (AgeStarFissurePopulationBridge.stageIfSpawnChunk(
                        context.level(), context.random(), chunkX, chunkZ)) {
                    return any;
                }
                continue;
            }
            if (step.feature() != null) {
                any |= step.feature().place(context);
            }
        }
        return any;
    }

    private static boolean isLegacyStructure(AgeFeatureKind kind) {
        return kind == AgeFeatureKind.MINESHAFTS
                || kind == AgeFeatureKind.STRONGHOLDS
                || kind == AgeFeatureKind.NETHER_FORTRESS
                || kind == AgeFeatureKind.VILLAGES;
    }

    private boolean containsStarFissureAfter(int index) {
        for (int i = index + 1; i < orderedSteps.size(); ++i) {
            if (orderedSteps.get(i).kind() == AgeFeatureKind.STAR_FISSURE) return true;
        }
        return false;
    }
}
