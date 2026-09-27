package com.xcompwiz.mystcraft.world.worldgen;

/** Pure legacy weather-controller parameters resolved from effective Symbols. */
public record AgeWeatherPlan(
        AgeWeatherMode mode,
        boolean toggleable,
        float rainingStrength,
        float thunderingStrength,
        Boolean rainEnabled,
        Boolean snowEnabled,
        Float minimumTemperature,
        Float maximumTemperature,
        int rainDuration,
        int rainDurationBase,
        int rainCooldown,
        int rainCooldownBase,
        int thunderDuration,
        int thunderDurationBase,
        int thunderCooldown,
        int thunderCooldownBase) {

    public boolean fixedWeather() {
        return toggleable;
    }

    /** Replays legacy IWeatherController#getTemperature without mutating biome registries. */
    public float temperature(float current) {
        float value = current;
        if (minimumTemperature != null && value < minimumTemperature) value = minimumTemperature;
        if (maximumTemperature != null && value > maximumTemperature) value = maximumTemperature;
        return value;
    }

    /** Replays legacy precipitation enable override; null means preserve biome default. */
    public boolean rainEnabled(boolean biomeDefault) {
        return rainEnabled == null ? biomeDefault : rainEnabled;
    }

    /** Replays legacy snow enable override; null means preserve biome default. */
    public boolean snowEnabled(boolean biomeDefault) {
        return snowEnabled == null ? biomeDefault : snowEnabled;
    }
}
