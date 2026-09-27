package com.xcompwiz.mystcraft.world.worldgen;

import com.xcompwiz.mystcraft.symbol.LegacyBiomeSymbolDefinition;
import com.xcompwiz.mystcraft.symbol.LegacyBiomeSymbolRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Recreates legacy SymbolBiome.getRandomBiome() fallback while preserving old numeric identity. */
public final class AgeBiomeFallbackResolver {
    private AgeBiomeFallbackResolver() {}

    public record FillResult(List<String> modernBiomeIds, List<Integer> legacyNumericIds) {
        public FillResult {
            modernBiomeIds = List.copyOf(modernBiomeIds);
            legacyNumericIds = List.copyOf(legacyNumericIds);
            if (modernBiomeIds.size() != legacyNumericIds.size())
                throw new IllegalArgumentException("modern/legacy biome lists must stay aligned");
        }
    }

    public static String pickLegacyId(long ageSeed) {
        List<LegacyBiomeSymbolDefinition> selectable = selectable();
        return selectable.get(new Random(ageSeed).nextInt(selectable.size())).legacyId();
    }

    public static FillResult fill(long ageSeed, List<String> explicitModernBiomeIds,
                                  List<Integer> explicitLegacyNumericIds, int missingCount) {
        if (explicitModernBiomeIds.size() != explicitLegacyNumericIds.size())
            throw new IllegalArgumentException("explicit modern/legacy biome lists must stay aligned");
        ArrayList<String> modern = new ArrayList<>(explicitModernBiomeIds);
        ArrayList<Integer> legacy = new ArrayList<>(explicitLegacyNumericIds);
        if (missingCount > 0) {
            List<LegacyBiomeSymbolDefinition> selectable = selectable();
            Random random = new Random(ageSeed);
            for (int i = 0; i < missingCount; i++) {
                LegacyBiomeSymbolDefinition chosen = selectable.get(random.nextInt(selectable.size()));
                modern.add(LegacyModernBiomeIdResolver.resolve(chosen.legacyId()));
                legacy.add(chosen.legacyNumericId());
            }
        }
        return new FillResult(modern, legacy);
    }

    /** Compatibility helper retained for callers which only need modern ids. */
    public static List<String> fill(long ageSeed, List<String> explicitModernBiomeIds, int missingCount) {
        ArrayList<Integer> unknown = new ArrayList<>();
        for (int i = 0; i < explicitModernBiomeIds.size(); i++) unknown.add(Integer.MIN_VALUE);
        return fill(ageSeed, explicitModernBiomeIds, unknown, missingCount).modernBiomeIds();
    }

    private static List<LegacyBiomeSymbolDefinition> selectable() {
        List<LegacyBiomeSymbolDefinition> selectable = new ArrayList<>(LegacyBiomeSymbolRegistry.values());
        selectable.sort(Comparator.comparingInt(LegacyBiomeSymbolDefinition::legacyNumericId));
        if (selectable.isEmpty()) throw new IllegalStateException("No restored legacy biome symbols are available for fallback");
        return selectable;
    }
}
