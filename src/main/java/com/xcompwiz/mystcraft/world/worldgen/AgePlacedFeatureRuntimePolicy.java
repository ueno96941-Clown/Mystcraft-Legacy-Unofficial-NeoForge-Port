package com.xcompwiz.mystcraft.world.worldgen;

/** Pure activation rules for the runtime placed-feature overlay. */
public final class AgePlacedFeatureRuntimePolicy {
    private AgePlacedFeatureRuntimePolicy() {}

    public static boolean requiresGenerationSettingsReplacement(AgeLocalGenerationPlan plan) {
        return !plan.placedFeatureInjections().isEmpty();
    }

    public static int injectedFeatureCount(AgeLocalGenerationPlan plan) {
        return plan.placedFeatureInjections().size();
    }

    public static boolean hasStructureOnlyChanges(AgeLocalGenerationPlan plan) {
        return plan.placedFeatureInjections().isEmpty()
                && !plan.allowedStructureSetIds().isEmpty();
    }
}
