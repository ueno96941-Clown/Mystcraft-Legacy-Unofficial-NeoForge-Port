package com.xcompwiz.mystcraft.world.worldgen;

import java.util.List;

public record AgeVanillaWorldgenBridgePlan(
        List<AgeVanillaWorldgenTarget> targets,
        List<AgeFeatureKind> customRequired) {

    public AgeVanillaWorldgenBridgePlan {
        targets = List.copyOf(targets);
        customRequired = List.copyOf(customRequired);
    }
}
