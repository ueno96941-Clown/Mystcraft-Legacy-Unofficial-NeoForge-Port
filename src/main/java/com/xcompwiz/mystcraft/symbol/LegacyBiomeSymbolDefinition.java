package com.xcompwiz.mystcraft.symbol;

import java.util.Objects;

/** Dynamic legacy biome Page identity plus its best modern 1.21.1 biome key. */
public record LegacyBiomeSymbolDefinition(
        String legacyId,
        int legacyNumericId,
        String modernBiomeId,
        boolean exactModernEquivalent,
        double legacyBaseHeight) {

    public LegacyBiomeSymbolDefinition {
        legacyId = LegacySymbolId.qualify(Objects.requireNonNull(legacyId, "legacyId"));
        modernBiomeId = Objects.requireNonNull(modernBiomeId, "modernBiomeId");
        if (legacyNumericId < 0) throw new IllegalArgumentException("legacyNumericId must be >= 0");
    }
}
