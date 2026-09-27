package com.xcompwiz.mystcraft.world.worldgen;

/** Pure legacy ColorHorizon sampling contract for the final sky renderer. */
public final class AgeHorizonRenderBridge {
    private AgeHorizonRenderBridge() {}

    public static Integer color(AgeColorPlan colors, float celestialAngle) {
        AgeColorGradient g = colors.horizonGradient();
        if (g == null || g.isEmpty()) return null;
        return AgeClientVisualBridge.rgb(g.sample(celestialAngle));
    }
}
