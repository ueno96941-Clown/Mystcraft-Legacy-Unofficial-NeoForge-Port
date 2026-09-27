package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherPackedLightBridge;

/** Pure-Java regression checks for WeatherRendererMyst packed-light arithmetic. */
public final class AgeWeatherPackedLightHarness {
    public static void main(String[] args) {
        int sample = 0x00A00070;
        require(AgeWeatherPackedLightBridge.rainPacked(sample) == sample, "rain must preserve combined light");
        int expected = (sample * 3 + 15728880) / 4;
        int snow = AgeWeatherPackedLightBridge.snowPacked(sample);
        require(snow == expected, "snow brightening formula changed");
        require(AgeWeatherPackedLightBridge.skyComponent(snow) == (snow >>> 16 & 0xFFFF), "sky split");
        require(AgeWeatherPackedLightBridge.blockComponent(snow) == (snow & 0xFFFF), "block split");
        require(AgeWeatherPackedLightBridge.snowPacked(15728880) == 15728880, "full bright fixed point");
        System.out.println("AgeWeatherPackedLightHarness: PASS");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
