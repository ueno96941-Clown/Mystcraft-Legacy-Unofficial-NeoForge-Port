package com.xcompwiz.mystcraft.world.worldgen;

/**
 * Backend-independent copy of Legacy WeatherRendererMyst packed-light math.
 *
 * <p>1.12 rain writes getCombinedLight(...) unchanged. Snow deliberately
 * brightens that packed value with {@code (light * 3 + 15728880) / 4} before
 * splitting it into the two lightmap components. Keeping this arithmetic here
 * lets the Legacy rule be regression-tested without pretending that the 1.21
 * shader/vertex API is identical to PARTICLE_POSITION_TEX_COLOR_LMAP.</p>
 */
public final class AgeWeatherPackedLightBridge {
    private static final int LEGACY_FULL_BRIGHT = 15728880;

    private AgeWeatherPackedLightBridge() {}

    public static int rainPacked(int combinedLight) {
        return combinedLight;
    }

    public static int snowPacked(int combinedLight) {
        return (combinedLight * 3 + LEGACY_FULL_BRIGHT) / 4;
    }

    public static int skyComponent(int packed) {
        return packed >>> 16 & 0xFFFF;
    }

    public static int blockComponent(int packed) {
        return packed & 0xFFFF;
    }
}
