package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Objects;

public record AgeVanillaWorldgenTarget(
        AgeFeatureKind featureKind,
        AgeVanillaWorldgenTargetKind targetKind,
        String resourceId,
        boolean exactEquivalent,
        String compatibilityNote) {

    public AgeVanillaWorldgenTarget {
        Objects.requireNonNull(featureKind, "featureKind");
        Objects.requireNonNull(targetKind, "targetKind");
        Objects.requireNonNull(resourceId, "resourceId");
        compatibilityNote = compatibilityNote == null ? "" : compatibilityNote;
    }
}
