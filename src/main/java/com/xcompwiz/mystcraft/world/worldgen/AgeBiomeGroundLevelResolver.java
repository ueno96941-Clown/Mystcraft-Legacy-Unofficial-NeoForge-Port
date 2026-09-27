package com.xcompwiz.mystcraft.world.worldgen;

import com.xcompwiz.mystcraft.symbol.LegacyBiomeSymbolRegistry;

import java.util.List;

/**
 * Replays legacy SymbolBiome.registerLogic ground-level side effect:
 * (int)(biome.getBaseHeight() * 64 + 64).
 *
 * <p>Because modern Biome no longer exposes the old baseHeight field, this calculation
 * deliberately uses the legacy numeric-biome descriptor rather than modern biome data.</p>
 */
public final class AgeBiomeGroundLevelResolver {
    private AgeBiomeGroundLevelResolver() {}

    public static int resolve(List<String> effectiveSymbols, int defaultGroundLevel) {
        // AgeController.setAverageGroundLevel() did not overwrite this value. The
        // first SymbolBiome supplied the initial level and every later biome
        // averaged its level with the current value using integer division.
        // This matters for Grid/Tiled/etc. Ages containing multiple biome pages.
        Integer ground = null;

        for (String symbol : effectiveSymbols) {
            var def = LegacyBiomeSymbolRegistry.resolveLegacy(symbol).orElse(null);
            if (def != null) {
                int height = (int) (def.legacyBaseHeight() * 64.0D + 64.0D);
                ground = ground == null ? height : (ground + height) / 2;
            }
        }
        return ground == null ? defaultGroundLevel : ground;
    }
}
