package com.xcompwiz.mystcraft.world.worldgen;

import java.util.List;
import java.util.Objects;

/** Registry-independent modern biome-source selection with retained legacy biome identity. */
public record AgeBiomeSourceSelection(
        AgeBiomeControllerMode mode,
        List<String> modernBiomeIds,
        List<Integer> legacyBiomeNumericIds,
        boolean nativeProvider,
        boolean readyForWorldgen,
        long ageSeed,
        int zoomScale) {

    public AgeBiomeSourceSelection {
        Objects.requireNonNull(mode, "mode");
        modernBiomeIds = List.copyOf(Objects.requireNonNull(modernBiomeIds, "modernBiomeIds"));
        legacyBiomeNumericIds = List.copyOf(Objects.requireNonNull(legacyBiomeNumericIds, "legacyBiomeNumericIds"));
        if (modernBiomeIds.size() != legacyBiomeNumericIds.size())
            throw new IllegalArgumentException("modern/legacy biome lists must stay aligned");
    }

    public static AgeBiomeSourceSelection from(AgeResolvedBiomePlan plan, long ageSeed) {
        boolean nativeProvider = plan.mode() == AgeBiomeControllerMode.NATIVE;
        boolean ready = switch (plan.mode()) {
            case SINGLE -> !plan.effectiveModernBiomeIds().isEmpty();
            case GRID, TILED -> plan.effectiveModernBiomeIds().size() >= 2;
            case HUGE, LARGE, MEDIUM, SMALL, TINY -> plan.effectiveModernBiomeIds().size() >= 3;
            case NATIVE -> true;
            default -> false;
        };
        return new AgeBiomeSourceSelection(plan.mode(), plan.effectiveModernBiomeIds(),
                plan.effectiveLegacyBiomeNumericIds(), nativeProvider, ready, ageSeed, plan.zoomScale());
    }
}
