package com.xcompwiz.mystcraft.world.worldgen;

/**
 * Age-local replacement for the legacy BiomeWrapperMyst weather queries.
 *
 * <p>Modern biome registries are immutable/global, so callers query this bridge instead of
 * mutating Biome climate objects. The result preserves legacy rain/snow enable overrides and
 * the 0.15 temperature split used by the old Mystcraft weather renderer.</p>
 */
public final class AgePrecipitationBridge {
    public static final float LEGACY_SNOW_TEMPERATURE = 0.15F;

    private AgePrecipitationBridge() {}

    public enum Type { NONE, RAIN, SNOW }

    public static Type precipitation(
            AgeWeatherPlan plan,
            boolean biomeCanRain,
            boolean biomeCanSnow,
            float biomeTemperature) {
        return precipitationAtControlledTemperature(
                plan, biomeCanRain, biomeCanSnow, plan.temperature(biomeTemperature));
    }

    /**
     * Exact BiomeWrapperMyst + WeatherRendererMyst ordering once positional temperature has
     * already been calculated. Snow enable disables canRain, but the renderer then chooses the
     * visual precipitation from the 0.15 temperature split rather than from the snow flag itself.
     */
    public static Type precipitationAtControlledTemperature(
            AgeWeatherPlan plan,
            boolean biomeCanRain,
            boolean biomeCanSnow,
            float controlledTemperatureAtPosition) {
        boolean snowEnabled = plan.snowEnabled(biomeCanSnow);
        boolean rainEnabled = !snowEnabled && plan.rainEnabled(biomeCanRain);
        if (!snowEnabled && !rainEnabled) return Type.NONE;
        return controlledTemperatureAtPosition >= LEGACY_SNOW_TEMPERATURE
                ? Type.RAIN
                : Type.SNOW;
    }

    public static boolean canRain(AgeWeatherPlan plan, boolean biomeCanRain, boolean biomeCanSnow) {
        return !plan.snowEnabled(biomeCanSnow) && plan.rainEnabled(biomeCanRain);
    }

    public static boolean canSnow(AgeWeatherPlan plan, boolean biomeCanSnow) {
        return plan.snowEnabled(biomeCanSnow);
    }
}
