package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Objects;

/** One Age-local placed-feature injection request. */
public record AgePlacedFeatureInjection(
        AgeFeatureKind featureKind,
        String placedFeatureId,
        int decorationStepOrdinal,
        String compatibilityNote) {

    public AgePlacedFeatureInjection {
        Objects.requireNonNull(featureKind, "featureKind");
        Objects.requireNonNull(placedFeatureId, "placedFeatureId");
        compatibilityNote = compatibilityNote == null ? "" : compatibilityNote;
        if (decorationStepOrdinal < 0) {
            throw new IllegalArgumentException("decorationStepOrdinal must be >= 0");
        }
    }
}
