package com.xcompwiz.mystcraft.world.worldgen;
/** Legacy WeatherRendererMyst precipitation render-radius contract. */
public final class AgeWeatherRenderDistanceContract {
    private AgeWeatherRenderDistanceContract() {}
    public static final int FAST_RADIUS = 5;
    public static final int FANCY_RADIUS = 10;
    public static int radius(boolean fancyGraphics) { return fancyGraphics ? FANCY_RADIUS : FAST_RADIUS; }
}
