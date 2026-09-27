package com.xcompwiz.mystcraft.world.worldgen;

import java.util.List;
import java.util.Objects;

/** Modern biome-key resolution layered on top of the legacy BiomeController plan. */
public record AgeResolvedBiomePlan(
        AgeBiomeControllerMode mode,
        List<String> explicitModernBiomeIds,
        List<String> effectiveModernBiomeIds,
        List<Integer> explicitLegacyBiomeNumericIds,
        List<Integer> effectiveLegacyBiomeNumericIds,
        List<String> unresolvedLegacyBiomeSymbols,
        int randomFallbackBiomeCount,
        int zoomScale) {
    public AgeResolvedBiomePlan {
        Objects.requireNonNull(mode, "mode");
        explicitModernBiomeIds = List.copyOf(Objects.requireNonNull(explicitModernBiomeIds, "explicitModernBiomeIds"));
        effectiveModernBiomeIds = List.copyOf(Objects.requireNonNull(effectiveModernBiomeIds, "effectiveModernBiomeIds"));
        explicitLegacyBiomeNumericIds = List.copyOf(Objects.requireNonNull(explicitLegacyBiomeNumericIds, "explicitLegacyBiomeNumericIds"));
        effectiveLegacyBiomeNumericIds = List.copyOf(Objects.requireNonNull(effectiveLegacyBiomeNumericIds, "effectiveLegacyBiomeNumericIds"));
        unresolvedLegacyBiomeSymbols = List.copyOf(Objects.requireNonNull(unresolvedLegacyBiomeSymbols, "unresolvedLegacyBiomeSymbols"));
        if (explicitModernBiomeIds.size() != explicitLegacyBiomeNumericIds.size())
            throw new IllegalArgumentException("explicit modern/legacy biome lists must stay aligned");
        if (effectiveModernBiomeIds.size() != effectiveLegacyBiomeNumericIds.size())
            throw new IllegalArgumentException("effective modern/legacy biome lists must stay aligned");
        if (randomFallbackBiomeCount < 0) throw new IllegalArgumentException("randomFallbackBiomeCount must be >= 0");
    }

    public boolean fullyMapped() { return unresolvedLegacyBiomeSymbols.isEmpty(); }
}
