package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Objects;

public record AgeFeaturePlanEntry(
        AgeFeatureKind kind,
        AgeFeatureStage stage,
        String materialBlockId,
        String biomeLegacySymbol,
        int chanceDenominator,
        String notes) {

    public AgeFeaturePlanEntry {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(stage, "stage");
        notes = notes == null ? "" : notes;
    }
}
