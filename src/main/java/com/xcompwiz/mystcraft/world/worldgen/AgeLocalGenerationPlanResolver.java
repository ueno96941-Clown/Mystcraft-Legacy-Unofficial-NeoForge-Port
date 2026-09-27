package com.xcompwiz.mystcraft.world.worldgen;

import java.util.ArrayList;

/** Converts 13H-2 registry targets into Age-local generation overlay instructions. */
public final class AgeLocalGenerationPlanResolver {
    private AgeLocalGenerationPlanResolver() {}

    // GenerationStep.Decoration ordinals in Minecraft 1.21.1:
    // RAW_GENERATION=0, LAKES=1, LOCAL_MODIFICATIONS=2, UNDERGROUND_STRUCTURES=3,
    // SURFACE_STRUCTURES=4, STRONGHOLDS=5, UNDERGROUND_ORES=6,
    // UNDERGROUND_DECORATION=7, FLUID_SPRINGS=8, VEGETAL_DECORATION=9,
    // TOP_LAYER_MODIFICATION=10.
    public static final int STEP_LAKES = 1;
    public static final int STEP_UNDERGROUND_STRUCTURES = 3;

    public static AgeLocalGenerationPlan resolve(AgeVanillaWorldgenBridgePlan bridge) {
        ArrayList<AgePlacedFeatureInjection> features = new ArrayList<>();
        ArrayList<String> structures = new ArrayList<>();

        for (AgeVanillaWorldgenTarget target : bridge.targets()) {
            switch (target.targetKind()) {
                case STRUCTURE_SET -> structures.add(target.resourceId());
                case PLACED_FEATURE -> {
                    int step = switch (target.featureKind()) {
                        case DUNGEONS -> STEP_UNDERGROUND_STRUCTURES;
                        case SURFACE_LAKES, DEEP_LAKES -> STEP_LAKES;
                        default -> throw new IllegalStateException(
                                "No Age-local decoration step for " + target.featureKind());
                    };
                    features.add(new AgePlacedFeatureInjection(
                            target.featureKind(),
                            target.resourceId(),
                            step,
                            target.compatibilityNote()));
                }
            }
        }

        return new AgeLocalGenerationPlan(features, structures);
    }
}
