package com.xcompwiz.mystcraft.world.worldgen;

import java.util.List;
import java.util.Objects;

/**
 * Registry-independent result of replaying legacy biome modifier/controller semantics.
 *
 * <p>Biome symbols remain as persisted legacy IDs here. 13E-2 maps the dynamic
 * `mystcraft:Biome<1.12 numeric id>` symbols onto 1.21.1 biome keys.</p>
 */
public record AgeBiomeControllerPlan(
        AgeBiomeControllerMode mode,
        List<String> explicitLegacyBiomeSymbols,
        int minimumBiomeCount,
        int missingFallbackBiomeCount,
        int zoomScale) {

    public AgeBiomeControllerPlan {
        Objects.requireNonNull(mode, "mode");
        explicitLegacyBiomeSymbols = List.copyOf(
                Objects.requireNonNull(explicitLegacyBiomeSymbols, "explicitLegacyBiomeSymbols"));
        if (minimumBiomeCount < 0) throw new IllegalArgumentException("minimumBiomeCount must be >= 0");
        if (missingFallbackBiomeCount < 0) throw new IllegalArgumentException("missingFallbackBiomeCount must be >= 0");
    }

    public boolean needsRandomFallbackBiomes() {
        return missingFallbackBiomeCount > 0;
    }

    public boolean usesNativeProvider() {
        return mode.usesNativeProvider();
    }
}
