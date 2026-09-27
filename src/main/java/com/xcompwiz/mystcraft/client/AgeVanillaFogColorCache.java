package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.world.worldgen.AgeColor;

/**
 * Per-frame cache of the vanilla/NeoForge fog RGB before Mystcraft ColorFog overrides it.
 *
 * <p>CP237 uses this as the sky dome horizon colour when an Age specifies ColorSky but no
 * ColorFog. That matches 0.13.7.06 composition: the custom sky was still viewed through the
 * ordinary atmospheric fog near the horizon.</p>
 */
public final class AgeVanillaFogColorCache {
    private static String dimension;
    private static AgeColor color;

    private AgeVanillaFogColorCache() {}

    public static void capture(String dimensionId, float red, float green, float blue) {
        dimension = dimensionId;
        color = new AgeColor(clamp(red), clamp(green), clamp(blue));
    }

    public static AgeColor get(String dimensionId) {
        return dimensionId != null && dimensionId.equals(dimension) ? color : null;
    }

    public static void clear() {
        dimension = null;
        color = null;
    }

    private static float clamp(float value) {
        return Math.max(0F, Math.min(1F, value));
    }
}
