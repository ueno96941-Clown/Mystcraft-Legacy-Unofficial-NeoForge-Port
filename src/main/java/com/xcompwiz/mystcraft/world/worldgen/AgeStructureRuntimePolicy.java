package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Set;

public final class AgeStructureRuntimePolicy {
    private AgeStructureRuntimePolicy() {}

    public static Set<String> allowedStructureSets(AgeLocalGenerationPlan plan) {
        return Set.copyOf(plan.allowedStructureSetIds());
    }

    public static boolean vanillaStructuresDisabled(AgeLocalGenerationPlan plan) {
        return plan.allowedStructureSetIds().isEmpty();
    }

    public static int allowedCount(AgeLocalGenerationPlan plan) {
        return plan.allowedStructureSetIds().size();
    }
}
