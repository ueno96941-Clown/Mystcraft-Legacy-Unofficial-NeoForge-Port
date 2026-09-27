package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.world.worldgen.AgeLegacyBiomeTemperatureMath;
import com.xcompwiz.mystcraft.world.worldgen.AgePrecipitationBridge;
import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherMode;
import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherResolver;

public final class AgePrecipitationClimateHarness {
    public static void main(String[] args) {
        var rain = AgeWeatherResolver.resolveMode(AgeWeatherMode.RAIN);
        var snow = AgeWeatherResolver.resolveMode(AgeWeatherMode.SNOW);
        var cloudy = AgeWeatherResolver.resolveMode(AgeWeatherMode.CLOUDY);

        // Legacy WeatherRain clamps the base to >=0.20, but Biome#getFloatTemperature then
        // applies altitude cooling. With zero temperature-noise, y=100 crosses below 0.15.
        float rainBase = rain.temperature(0.0F);
        requireClose(rainBase, 0.20F, "rain base clamp");
        requireClose(AgeLegacyBiomeTemperatureMath.adjusted(rainBase, 64, 0.0), 0.20F, "sea-level rain temp");
        float highRain = AgeLegacyBiomeTemperatureMath.adjusted(rainBase, 100, 0.0);
        require(highRain < AgePrecipitationBridge.LEGACY_SNOW_TEMPERATURE, "high rain age cools into snow threshold");
        require(AgePrecipitationBridge.precipitationAtControlledTemperature(rain, true, false, highRain)
                == AgePrecipitationBridge.Type.SNOW, "cold forced-rain column renders snow");

        // WeatherRendererMyst selected texture from temperature after testing canRain||enableSnow.
        // Therefore an enabled-snow biome at a warm positional temperature renders rain.
        require(AgePrecipitationBridge.precipitationAtControlledTemperature(snow, false, false, 0.20F)
                == AgePrecipitationBridge.Type.RAIN, "warm snow-enabled column follows temperature texture split");
        require(AgePrecipitationBridge.precipitationAtControlledTemperature(snow, false, false, 0.10F)
                == AgePrecipitationBridge.Type.SNOW, "cold snow-enabled column renders snow");
        require(AgePrecipitationBridge.precipitationAtControlledTemperature(cloudy, true, true, 0.10F)
                == AgePrecipitationBridge.Type.NONE, "disabled precipitation remains none");

        System.out.println("AgePrecipitationClimateHarness: PASS");
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    private static void requireClose(float actual, float expected, String label) {
        if (Math.abs(actual - expected) > 0.00001F) {
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
        }
    }
}
