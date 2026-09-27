package com.xcompwiz.mystcraft.world.worldgen;

/** Mutable per-Age weather-controller state, independent of vanilla's shared world data. */
public final class AgeWeatherRuntimeState {
    private int rainCounter;
    private int thunderCounter;
    private boolean raining;
    private boolean thundering;
    private float rainStrength;
    private float thunderStrength;
    private boolean initialized;

    public int rainCounter() { return rainCounter; }
    public void setRainCounter(int value) { rainCounter = value; }
    public int thunderCounter() { return thunderCounter; }
    public void setThunderCounter(int value) { thunderCounter = value; }
    public boolean raining() { return raining; }
    public void setRaining(boolean value) { raining = value; }
    public boolean thundering() { return thundering; }
    public void setThundering(boolean value) { thundering = value; }
    public float rainStrength() { return rainStrength; }
    public void setRainStrength(float value) { rainStrength = clamp01(value); }
    public float thunderStrength() { return thunderStrength; }
    public void setThunderStrength(float value) { thunderStrength = clamp01(value); }
    public boolean initialized() { return initialized; }
    public void setInitialized(boolean value) { initialized = value; }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
