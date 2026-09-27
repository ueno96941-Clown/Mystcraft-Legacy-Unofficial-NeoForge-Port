package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherMode;
import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherResolver;
import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherRuntimeController;
import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherRuntimeState;

public final class AgeWeatherEntryInitializationHarness {
    public static void main(String[] args) {
        var rain = new AgeWeatherRuntimeState();
        rain.setInitialized(true); // mirrors a new CP223 Age restored from default saved flags
        AgeWeatherRuntimeController.prepareForEntry(AgeWeatherResolver.resolveMode(AgeWeatherMode.RAIN), rain);
        require(rain.raining(), "Rain is active before first level tick");
        require(!rain.thundering(), "Rain is not thunder");
        requireClose(rain.rainStrength(), 1.0F, "Rain strength");

        var storm = new AgeWeatherRuntimeState();
        storm.setInitialized(true);
        AgeWeatherRuntimeController.prepareForEntry(AgeWeatherResolver.resolveMode(AgeWeatherMode.STORM), storm);
        require(storm.raining(), "Storm rain is active before first level tick");
        require(storm.thundering(), "Storm thunder is active before first level tick");
        requireClose(storm.rainStrength(), 1.0F, "Storm rain strength");
        requireClose(storm.thunderStrength(), 1.0F, "Storm thunder strength");

        var normal = new AgeWeatherRuntimeState();
        normal.setInitialized(true);
        normal.setRainCounter(1234);
        normal.setRaining(true);
        normal.setRainStrength(0.42F);
        AgeWeatherRuntimeController.prepareForEntry(AgeWeatherResolver.resolveMode(AgeWeatherMode.NORMAL), normal);
        require(normal.rainCounter() == 1234, "Normal weather counter is not consumed on entry");
        require(normal.raining(), "Normal persisted raining flag is preserved");
        requireClose(normal.rainStrength(), 0.42F, "Normal weather strength is preserved");

        System.out.println("AgeWeatherEntryInitializationHarness: PASS");
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
