package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Objects;
import java.util.function.IntUnaryOperator;

/**
 * Runtime weather state machine derived from legacy WeatherControllerBase and
 * WeatherControllerToggleable.
 */
public final class AgeWeatherRuntimeController {
    private AgeWeatherRuntimeController() {}

    /**
     * Applies fixed/toggleable Legacy weather immediately at an Age-entry boundary.
     *
     * <p>Legacy WorldProviderMyst#calculateInitialWeather() established visual weather
     * before the player saw the world. Runtime-created 1.21.1 dimensions can fire the
     * dimension-change event before their first LevelTickEvent.Post, so waiting for
     * {@link #tick(AgeWeatherPlan, AgeWeatherRuntimeState, IntUnaryOperator)} can leave
     * the joining client showing clear weather until a later sync/re-entry.</p>
     *
     * <p>Only fixed/toggleable modes are touched here. Cyclic Normal/Fast/Slow weather
     * must keep its persisted counters and must not consume a random/tick step merely
     * because a player entered the Age.</p>
     */
    public static void prepareForEntry(AgeWeatherPlan plan, AgeWeatherRuntimeState state) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(state, "state");
        if (plan.mode() == AgeWeatherMode.UNKNOWN || !plan.toggleable()) return;
        applyToggleable(plan, state);
        state.setInitialized(true);
    }

    public static void tick(
            AgeWeatherPlan plan,
            AgeWeatherRuntimeState state,
            IntUnaryOperator nextInt) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(nextInt, "nextInt");

        if (!state.initialized()) {
            initialize(plan, state);
        }

        if (plan.mode() == AgeWeatherMode.UNKNOWN) {
            return;
        }

        if (plan.toggleable()) {
            applyToggleable(plan, state);
            return;
        }

        tickCycle(plan, state, nextInt);
    }

    private static void initialize(AgeWeatherPlan plan, AgeWeatherRuntimeState state) {
        state.setRainCounter(0);
        state.setThunderCounter(0);
        state.setRaining(false);
        state.setThundering(false);
        state.setRainStrength(0.0F);
        state.setThunderStrength(0.0F);
        state.setInitialized(true);

        if (plan.toggleable()) {
            applyToggleable(plan, state);
        }
    }

    private static void applyToggleable(AgeWeatherPlan plan, AgeWeatherRuntimeState state) {
        state.setRainStrength(plan.rainingStrength());
        state.setThunderStrength(plan.thunderingStrength());

        switch (plan.mode()) {
            case ALWAYS, RAIN, SNOW -> {
                state.setRaining(true);
                state.setThundering(false);
            }
            case STORM -> {
                state.setRaining(true);
                state.setThundering(true);
            }
            case CLOUDY -> {
                // Legacy Cloudy drove visual rain strength to 1 while explicitly disabling
                // rain/snow precipitation. The biome precipitation suppression is applied
                // by a later weather-biome hook; global weather state remains non-raining here.
                state.setRaining(false);
                state.setThundering(false);
            }
            case OFF -> {
                state.setRaining(false);
                state.setThundering(false);
            }
            default -> {
                // no-op
            }
        }
    }

    private static void tickCycle(
            AgeWeatherPlan plan,
            AgeWeatherRuntimeState state,
            IntUnaryOperator nextInt) {

        int thunder = state.thunderCounter();
        if (thunder <= 0) {
            if (state.thundering()) {
                thunder = plan.thunderDurationBase() + randomPart(nextInt, plan.thunderDuration());
            } else {
                thunder = plan.thunderCooldownBase() + randomPart(nextInt, plan.thunderCooldown());
            }
        } else {
            thunder--;
            if (thunder <= 0) {
                state.setThundering(!state.thundering());
            }
        }
        state.setThunderCounter(thunder);

        int rain = state.rainCounter();
        if (rain <= 0) {
            if (state.raining()) {
                rain = plan.rainDurationBase() + randomPart(nextInt, plan.rainDuration());
            } else {
                rain = plan.rainCooldownBase() + randomPart(nextInt, plan.rainCooldown());
            }
        } else {
            rain--;
            if (rain <= 0) {
                state.setRaining(!state.raining());
            }
        }
        state.setRainCounter(rain);

        float rainStrength = state.rainStrength() + (state.raining() ? 0.01F : -0.01F);
        float thunderStrength = state.thunderStrength() + (state.thundering() ? 0.01F : -0.01F);
        state.setRainStrength(rainStrength);
        state.setThunderStrength(thunderStrength);
    }

    private static int randomPart(IntUnaryOperator nextInt, int bound) {
        return bound > 0 ? nextInt.applyAsInt(bound) : 0;
    }
}
