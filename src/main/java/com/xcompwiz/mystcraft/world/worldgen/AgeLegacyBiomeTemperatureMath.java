package com.xcompwiz.mystcraft.world.worldgen;

/** Pure Minecraft 1.12 biome temperature-at-position math used by Mystcraft weather. */
public final class AgeLegacyBiomeTemperatureMath {
    public static final int LEGACY_HEIGHT_THRESHOLD = 64;

    private AgeLegacyBiomeTemperatureMath() {}

    /**
     * Replays 1.12 Biome#getFloatTemperature after Mystcraft's weather controller has changed
     * the biome's default/base temperature. {@code temperatureNoise} is the 1-octave 1234-seed
     * Perlin sample at x/8,z/8 before the old *4 scale.
     */
    public static float adjusted(float controlledBaseTemperature, int y, double temperatureNoise) {
        if (y <= LEGACY_HEIGHT_THRESHOLD) return controlledBaseTemperature;
        float noiseTerm = (float) (temperatureNoise * 4.0D);
        return controlledBaseTemperature
                - (noiseTerm + (float) y - LEGACY_HEIGHT_THRESHOLD) * 0.05F / 30.0F;
    }
}
