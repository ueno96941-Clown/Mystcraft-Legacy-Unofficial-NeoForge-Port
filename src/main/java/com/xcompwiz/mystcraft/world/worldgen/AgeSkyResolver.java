package com.xcompwiz.mystcraft.world.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Replays legacy modifier slots and celestial Symbol construction in authored order. */
public final class AgeSkyResolver {
    private AgeSkyResolver() {}

    public static AgeSkyPlan resolve(long ageSeed, List<String> symbols) {
        List<AgeCelestialPlan> celestials = new ArrayList<>();
        Random symbolSeeds = new Random(ageSeed);
        AgeLegacyColorModifierState colorMods = new AgeLegacyColorModifierState();
        Float angle = null;
        Float phase = null;
        boolean drawHorizon = true;
        boolean drawVoid = true;
        Float horizonOverride = null;
        Float cloudHeightOverride = null;
        boolean endSky = false;
        AgeColorGradient endSkyGradient = new AgeColorGradient(List.of());

        for (String symbol : symbols) {
            // Legacy AgeController handed every registered Symbol its own nextLong(), even when
            // that Symbol did not use randomness. Keep this stream advance unconditional.
            long symbolSeed = symbolSeeds.nextLong();

            Float length = lengthValue(symbol);
            if (length != null) {
                colorMods.pushFactor(length);
                continue;
            }
            Float angleValue = angleValue(symbol);
            if (angleValue != null) {
                angle = angle == null ? angleValue : averageAngles(angle, angleValue);
                continue;
            }
            Float phaseValue = phaseValue(symbol);
            if (phaseValue != null) {
                phase = phase == null ? phaseValue : averageAngles(phase, phaseValue);
                continue;
            }
            AgeColor color = colorFor(symbol);
            if (color != null) {
                colorMods.pushColor(color);
                continue;
            }

            switch (symbol) {
                case "mystcraft:ModGradient" -> colorMods.pushGradientPoint();
                case "mystcraft:ColorHorizon" -> colorMods.pushHorizonColor();

                // All legacy Symbols shared the same modifier slots. These Symbols are
                // resolved by other modern planners, but they still destructively consume
                // COLOR/GRADIENT here so modifiers cannot leak into later celestial Symbols.
                case "mystcraft:ColorSky", "mystcraft:ColorSkyNight" ->
                        colorMods.popGradient(new AgeColor(1F, 1F, 1F));
                case "mystcraft:ColorFog" ->
                        colorMods.popGradient(new AgeColor(0.7529412F, 0.8470588F, 1.0F));
                case "mystcraft:ColorCloud" ->
                        colorMods.popGradient(new AgeColor(1F, 1F, 1F));
                case "mystcraft:ColorWater", "mystcraft:ColorGrass", "mystcraft:ColorFoliage" ->
                        colorMods.popColor();
                case "mystcraft:EnvLightning" ->
                        // EffectLightning is intentionally disabled by project policy, but
                        // SymbolEnvLightning still called ModifierUtils.popGradient().
                        colorMods.popGradient(null);

                case "mystcraft:ModClear" -> {
                    colorMods.clear();
                    angle = null;
                    phase = null;
                }
                case "mystcraft:SunNormal" -> {
                    Float factor = colorMods.popFactor();
                    Float useAngle = angle; angle = null;
                    Float usePhase = phase; phase = null;
                    AgeColorGradient sunset = colorMods.popSunset();
                    celestials.add(createSun(symbolSeed, factor, useAngle, usePhase, sunset));
                }
                case "mystcraft:MoonNormal" -> {
                    Float factor = colorMods.popFactor();
                    Float useAngle = angle; angle = null;
                    Float usePhase = phase; phase = null;
                    AgeColorGradient sunset = colorMods.popSunset();
                    celestials.add(createMoon(symbolSeed, factor, useAngle, usePhase, sunset));
                }
                case "mystcraft:StarsNormal" -> {
                    Float factor = colorMods.popFactor();
                    Float useAngle = angle; angle = null;
                    celestials.add(createStars(symbolSeed, AgeCelestialKind.STARS_NORMAL,
                            factor, useAngle, colorMods.popGradient(new AgeColor(1,1,1))));
                }
                case "mystcraft:StarsTwinkle" -> {
                    Float factor = colorMods.popFactor();
                    Float useAngle = angle; angle = null;
                    celestials.add(createStars(symbolSeed, AgeCelestialKind.STARS_TWINKLE,
                            factor, useAngle, colorMods.popGradient(new AgeColor(1,1,1))));
                }
                case "mystcraft:StarsEndSky" -> {
                    endSkyGradient = colorMods.popGradient(new AgeColor(.156F,.156F,.156F));
                    celestials.add(new AgeCelestialPlan(AgeCelestialKind.STARS_END_SKY,0L,0F,0F,
                            false,null,null,symbolSeed,false,false));
                    endSky = true;
                    horizonOverride = average(horizonOverride, 0.0F);
                    drawHorizon = false;
                    drawVoid = false;
                }
                case "mystcraft:Rainbow" -> {
                    Float useAngle = angle; angle = null;
                    celestials.add(createRainbow(symbolSeed, useAngle));
                }
                case "mystcraft:NoHorizon" -> {
                    horizonOverride = average(horizonOverride, 0.0F);
                    drawHorizon = false;
                    drawVoid = false;
                }
                case "mystcraft:TerrainNether" -> {
                    cloudHeightOverride = average(cloudHeightOverride, 200.0F);
                    horizonOverride = average(horizonOverride, 128.0F);
                }
                case "mystcraft:TerrainEnd" -> {
                    horizonOverride = average(horizonOverride, 0.0F);
                    drawHorizon = false;
                    drawVoid = false;
                }
                case "mystcraft:TerrainVoid" -> {
                    cloudHeightOverride = average(cloudHeightOverride, 0.0F);
                    horizonOverride = average(horizonOverride, 0.0F);
                    drawHorizon = false;
                    drawVoid = false;
                }
                case "mystcraft:Skylands" -> {
                    cloudHeightOverride = average(cloudHeightOverride, 42.5F);
                    horizonOverride = average(horizonOverride, 0.0F);
                }
                default -> { }
            }
        }

        int horizonHeight = Math.round(horizonOverride == null ? 63.0F : horizonOverride);
        float cloudHeight = cloudHeightOverride == null
                ? com.xcompwiz.mystcraft.world.dimension.ModernAgeHeight.NORMAL_CLOUD_HEIGHT
                : cloudHeightOverride;
        return new AgeSkyPlan(celestials, drawHorizon, drawVoid, horizonHeight, cloudHeight, endSky, endSkyGradient);
    }

    private static AgeCelestialPlan createSun(long seed, Float factor, Float angle, Float phase, AgeColorGradient sunset) {
        Random rand = new Random(seed);
        double periodFactor = factor != null ? factor : 0.4D * rand.nextDouble() + 0.8D;
        long period = (long) (periodFactor * 24000L);
        float actualAngle = -(angle != null ? angle : (float) (rand.nextDouble() * 360.0D));
        float offset = phaseOffset(rand, period, phase);
        return new AgeCelestialPlan(AgeCelestialKind.SUN, period, actualAngle, offset, true,
                sunset, null, seed, factor == null, angle == null);
    }

    private static AgeCelestialPlan createMoon(long seed, Float factor, Float angle, Float phase, AgeColorGradient sunset) {
        Random rand = new Random(seed);
        double periodFactor = factor != null ? factor : 1.8D * rand.nextDouble() + 0.2D;
        long period = (long) (periodFactor * 24000L);
        float actualAngle = -(angle != null ? angle : (float) (rand.nextDouble() * 360.0D));
        float offset = phaseOffset(rand, period, phase);
        return new AgeCelestialPlan(AgeCelestialKind.MOON, period, actualAngle, offset, false,
                sunset, null, seed, factor == null, angle == null);
    }

    private static AgeCelestialPlan createStars(long seed, AgeCelestialKind kind, Float factor, Float angle,
                                                AgeColorGradient starGradient) {
        Random rand = new Random(seed);
        double periodFactor = factor != null ? factor : 1.8D * rand.nextDouble() + 0.2D;
        long period = (long) (periodFactor * 240000L);
        float actualAngle = -(angle != null ? angle : (float) (rand.nextDouble() * 360.0D));
        return new AgeCelestialPlan(kind, period, actualAngle, 0F, false,
                null, starGradient, seed, factor == null, angle == null);
    }

    private static AgeCelestialPlan createRainbow(long seed, Float angle) {
        Random rand = new Random(seed);
        float actualAngle = -(angle != null ? angle : (float) (rand.nextDouble() * 360.0D));
        return new AgeCelestialPlan(AgeCelestialKind.RAINBOW, 0L, actualAngle, 0F, false,
                null, null, seed, false, angle == null);
    }

    private static float phaseOffset(Random rand, long period, Float phase) {
        float offset;
        if (phase != null) {
            offset = phase / 360.0F;
        } else {
            offset = rand.nextFloat();
            if (period == 0L) offset = offset / 2.0F + 0.25F;
        }
        return offset - 0.5F;
    }

    private static Float lengthValue(String symbol) {
        return switch (symbol) {
            case "mystcraft:ModZero" -> 0.0F;
            case "mystcraft:ModHalf" -> 0.5F;
            case "mystcraft:ModFull" -> 1.0F;
            case "mystcraft:ModDouble" -> 2.0F;
            default -> null;
        };
    }

    private static Float angleValue(String symbol) {
        return switch (symbol) {
            case "mystcraft:ModNorth" -> 0.0F;
            case "mystcraft:ModEast" -> 90.0F;
            case "mystcraft:ModSouth" -> 180.0F;
            case "mystcraft:ModWest" -> 270.0F;
            default -> null;
        };
    }

    private static Float phaseValue(String symbol) {
        return switch (symbol) {
            case "mystcraft:ModEnd" -> 0.0F;
            case "mystcraft:ModRising" -> 90.0F;
            case "mystcraft:ModNoon" -> 180.0F;
            case "mystcraft:ModSetting" -> 270.0F;
            default -> null;
        };
    }

    private static AgeColor colorFor(String symbol) {
        return switch (symbol) {
            case "mystcraft:ModColorMaroon" -> new AgeColor(.5f,0,0);
            case "mystcraft:ModColorRed" -> new AgeColor(1,0,0);
            case "mystcraft:ModColorOlive" -> new AgeColor(.5f,.5f,0);
            case "mystcraft:ModColorYellow" -> new AgeColor(1,1,0);
            case "mystcraft:ModColorDarkGreen" -> new AgeColor(0,.5f,0);
            case "mystcraft:ModColorGreen" -> new AgeColor(0,1,0);
            case "mystcraft:ModColorTeal" -> new AgeColor(0,.5f,.5f);
            case "mystcraft:ModColorCyan" -> new AgeColor(0,1,1);
            case "mystcraft:ModColorNavy" -> new AgeColor(0,0,.5f);
            case "mystcraft:ModColorBlue" -> new AgeColor(0,0,1);
            case "mystcraft:ModColorPurple" -> new AgeColor(.5f,0,.5f);
            case "mystcraft:ModColorMagenta" -> new AgeColor(1,0,1);
            case "mystcraft:ModColorBlack" -> new AgeColor(0,0,0);
            case "mystcraft:ModColorGrey" -> new AgeColor(.5f,.5f,.5f);
            case "mystcraft:ModColorSilver" -> new AgeColor(.75f,.75f,.75f);
            case "mystcraft:ModColorWhite" -> new AgeColor(1,1,1);
            default -> null;
        };
    }

    private static Float average(Float current, float next) {
        return current == null ? next : (current + next) / 2.0F;
    }

    static float averageAngles(float first, float second) {
        float third = second;
        if (Math.abs(first - second) > 180.0F) third += 360.0F;
        if (Math.abs(first - third) == 180.0F) third = first + 180.0F;
        float average = (first + third) / 2.0F;
        if (average >= 360.0F) average -= 360.0F;
        return average;
    }
}
