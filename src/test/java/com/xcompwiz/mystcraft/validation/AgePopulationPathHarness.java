package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.world.worldgen.*;
import java.util.List;

public final class AgePopulationPathHarness {
    public static void main(String[] args) {
        AgeFeaturePlan plan = new AgeFeaturePlan(List.of(
                new AgeFeaturePlanEntry(AgeFeatureKind.SURFACE_LAKES, AgeFeatureStage.POPULATION,
                        "minecraft:water", null, 4, "legacy surface lake"),
                new AgeFeaturePlanEntry(AgeFeatureKind.DEEP_LAKES, AgeFeatureStage.POPULATION,
                        "minecraft:lava", null, 8, "legacy deep lake"),
                new AgeFeaturePlanEntry(AgeFeatureKind.DUNGEONS, AgeFeatureStage.POPULATION,
                        null, null, 0, "legacy dungeon")));

        AgeVanillaWorldgenBridgePlan bridge = AgeVanillaWorldgenBridgeResolver.resolve(plan);
        if (!bridge.customRequired().contains(AgeFeatureKind.SURFACE_LAKES)
                || !bridge.customRequired().contains(AgeFeatureKind.DEEP_LAKES)) {
            throw new AssertionError("Lakes must use direct Legacy population path");
        }
        if (bridge.targets().stream().anyMatch(t -> t.featureKind() == AgeFeatureKind.SURFACE_LAKES
                || t.featureKind() == AgeFeatureKind.DEEP_LAKES)) {
            throw new AssertionError("Lakes must not also map to modern PlacedFeatures");
        }
        if (!bridge.customRequired().contains(AgeFeatureKind.DUNGEONS)) {
            throw new AssertionError("Dungeons must use the direct Legacy 8-attempt population path");
        }
        long dungeonTargets = bridge.targets().stream()
                .filter(t -> t.featureKind() == AgeFeatureKind.DUNGEONS)
                .count();
        if (dungeonTargets != 0) {
            throw new AssertionError("Dungeons must not also inject modern monster-room PlacedFeatures: " + dungeonTargets);
        }
        System.out.println("AgePopulationPathHarness: PASS");
    }
}
