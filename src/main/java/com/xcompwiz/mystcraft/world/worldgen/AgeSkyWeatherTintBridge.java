package com.xcompwiz.mystcraft.world.worldgen;

/**
 * Legacy WorldProviderMyst sky-color weather attenuation, kept client/API independent.
 * Rain is applied first, then thunder, exactly as 0.13.7.06 did after resolving ColorSky.
 */
public final class AgeSkyWeatherTintBridge {
    private AgeSkyWeatherTintBridge() {}

    public static AgeColor apply(AgeColor color, float rainStrength, float thunderStrength) {
        float red = color.r(), green = color.g(), blue = color.b();
        float rain = clamp01(rainStrength);
        if (rain > 0.0F) {
            float gray = (red * 0.3F + green * 0.59F + blue * 0.11F) * 0.6F;
            float keep = 1.0F - rain * 0.75F;
            red = red * keep + gray * (1.0F - keep);
            green = green * keep + gray * (1.0F - keep);
            blue = blue * keep + gray * (1.0F - keep);
        }
        float thunder = clamp01(thunderStrength);
        if (thunder > 0.0F) {
            float gray = (red * 0.3F + green * 0.59F + blue * 0.11F) * 0.2F;
            float keep = 1.0F - thunder * 0.75F;
            red = red * keep + gray * (1.0F - keep);
            green = green * keep + gray * (1.0F - keep);
            blue = blue * keep + gray * (1.0F - keep);
        }
        return new AgeColor(red, green, blue);
    }

    private static float clamp01(float value) { return Math.max(0.0F, Math.min(1.0F, value)); }
}
