package com.xcompwiz.mystcraft.world.worldgen;

import java.util.List;

/** Runtime environment choices resolved from legacy fixed Symbols. */
public record AgeEnvironmentPlan(
        boolean pvpEnabled,
        boolean acceleratedRandomTicks,
        List<String> intentionallyDisabledHazardSymbols) {

    public AgeEnvironmentPlan {
        intentionallyDisabledHazardSymbols = List.copyOf(intentionallyDisabledHazardSymbols);
    }

    public boolean hasDisabledLegacyHazards() {
        return !intentionallyDisabledHazardSymbols.isEmpty();
    }
}
