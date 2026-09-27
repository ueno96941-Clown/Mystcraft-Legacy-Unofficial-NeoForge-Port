package com.xcompwiz.mystcraft.world.worldgen;

import java.util.ArrayList;
import java.util.List;

/** Resolves non-worldgen environment Symbols while enforcing the custom stability policy. */
public final class AgeEnvironmentResolver {
    private AgeEnvironmentResolver() {}

    public static AgeEnvironmentPlan resolve(List<String> effectiveSymbols) {
        // Legacy default: PvP is enabled unless the explicit Anti-PvP/PvPOff Symbol disables it.
        boolean pvpEnabled = true;
        boolean acceleratedRandomTicks = false;
        ArrayList<String> disabledHazards = new ArrayList<>();
        for (String symbol : effectiveSymbols) {
            if ("mystcraft:PvPOff".equals(symbol)) {
                pvpEnabled = false;
                continue;
            }
            if ("mystcraft:EnvAccel".equals(symbol)) {
                acceleratedRandomTicks = true;
                continue;
            }
            if (isIntentionallyDisabledHazard(symbol)) {
                disabledHazards.add(symbol);
            }
        }
        return new AgeEnvironmentPlan(pvpEnabled, acceleratedRandomTicks, disabledHazards);
    }

    public static boolean isIntentionallyDisabledHazard(String symbol) {
        return "mystcraft:EnvExplosions".equals(symbol)
                || "mystcraft:EnvLightning".equals(symbol)
                || "mystcraft:EnvMeteor".equals(symbol)
                || "mystcraft:EnvScorch".equals(symbol);
    }
}
