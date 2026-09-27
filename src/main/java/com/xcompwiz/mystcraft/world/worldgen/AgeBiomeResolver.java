package com.xcompwiz.mystcraft.world.worldgen;

import com.xcompwiz.mystcraft.symbol.LegacyBiomeSymbolRegistry;

import java.util.ArrayList;
import java.util.List;

/** Resolves consumed legacy numeric biome Pages while retaining their original numeric identities. */
public final class AgeBiomeResolver {
    private AgeBiomeResolver() {}

    public static AgeResolvedBiomePlan resolve(long ageSeed, AgeBiomeControllerPlan plan) {
        List<String> modern = new ArrayList<>();
        List<Integer> legacyNumeric = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();

        for (String legacy : plan.explicitLegacyBiomeSymbols()) {
            LegacyBiomeSymbolRegistry.resolveLegacy(legacy).ifPresentOrElse(def -> {
                modern.add(LegacyModernBiomeIdResolver.resolve(legacy));
                legacyNumeric.add(def.legacyNumericId());
            }, () -> unresolved.add(legacy));
        }

        AgeBiomeFallbackResolver.FillResult effective = AgeBiomeFallbackResolver.fill(
                ageSeed, modern, legacyNumeric, plan.missingFallbackBiomeCount());

        return new AgeResolvedBiomePlan(
                plan.mode(), modern, effective.modernBiomeIds(), legacyNumeric, effective.legacyNumericIds(),
                unresolved, plan.missingFallbackBiomeCount(), plan.zoomScale());
    }
}
