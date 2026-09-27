package com.xcompwiz.mystcraft.world.worldgen;

import java.util.List;

public final class AgeLightingResolver {
    private AgeLightingResolver() {}

    public static AgeLightingPlan resolve(List<String> effectiveSymbols) {
        AgeLightingMode selected = AgeLightingMode.UNKNOWN;
        for (String symbol : effectiveSymbols) {
            if ("mystcraft:LightingBright".equals(symbol)) selected = AgeLightingMode.BRIGHT;
            else if ("mystcraft:LightingDark".equals(symbol)) selected = AgeLightingMode.DARK;
            else if ("mystcraft:LightingNormal".equals(symbol)) selected = AgeLightingMode.NORMAL;
        }
        // Legacy AgeController.registerInterface(ILightingController) overwrote the previous
        // controller; extra-controller instability is intentionally ignored by project policy.
        return new AgeLightingPlan(selected);
    }
}
