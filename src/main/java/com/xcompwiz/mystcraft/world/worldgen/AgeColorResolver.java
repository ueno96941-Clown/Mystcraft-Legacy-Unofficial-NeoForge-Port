package com.xcompwiz.mystcraft.world.worldgen;

import java.util.List;

/** Replays legacy environmental COLOR / GRADIENT modifier semantics. */
public final class AgeColorResolver {
    private AgeColorResolver() {}

    /**
     * Authored pages must keep their modifier relationship intact. The completed grammar list may
     * insert random fallback branches between a ModColor and the visual Symbol that authored it;
     * replaying only that flattened list can therefore let unrelated generated stars/gradients
     * destructively consume the player's colour. Resolve the complete list for fallback channels,
     * then let any explicitly authored visual channel replace its generated counterpart.
     */
    public static AgeColorPlan resolve(List<String> requestedSymbols, List<String> effectiveSymbols) {
        AgeColorPlan generated = resolve(effectiveSymbols);
        AgeColorPlan authored = resolve(requestedSymbols);
        return new AgeColorPlan(
                choose(authored.sky(), generated.sky()),
                choose(authored.fog(), generated.fog()),
                choose(authored.cloud(), generated.cloud()),
                choose(authored.water(), generated.water()),
                choose(authored.grass(), generated.grass()),
                choose(authored.foliage(), generated.foliage()),
                authored.horizonGradient() != null && !authored.horizonGradient().isEmpty()
                        ? authored.horizonGradient() : generated.horizonGradient());
    }

    private static List<AgeColorChannelPlan> choose(List<AgeColorChannelPlan> authored, List<AgeColorChannelPlan> generated) {
        return authored != null && !authored.isEmpty() ? authored : generated;
    }

    public static AgeColorPlan resolve(List<String> effectiveSymbols) {
        AgeLegacyColorModifierState state = new AgeLegacyColorModifierState();

        java.util.ArrayList<AgeColorChannelPlan> sky = new java.util.ArrayList<>();
        java.util.ArrayList<AgeColorChannelPlan> fog = new java.util.ArrayList<>();
        java.util.ArrayList<AgeColorChannelPlan> cloud = new java.util.ArrayList<>();
        java.util.ArrayList<AgeColorChannelPlan> water = new java.util.ArrayList<>();
        java.util.ArrayList<AgeColorChannelPlan> grass = new java.util.ArrayList<>();
        java.util.ArrayList<AgeColorChannelPlan> foliage = new java.util.ArrayList<>();

        for (String symbol : effectiveSymbols) {
            AgeColor base = colorFor(symbol);
            if (base != null) {
                state.pushColor(base);
                continue;
            }

            Float length = lengthValue(symbol);
            if (length != null) {
                state.pushFactor(length);
                continue;
            }

            switch (symbol) {
                case "mystcraft:ModGradient" -> state.pushGradientPoint();
                case "mystcraft:ModClear" -> state.clear();

                // ColorHorizon does not colour the lower sky plane. In 0.13.7.06 it converts
                // COLOR/GRADIENT into the SUNSET slot, which the next Sun/Moon consumes.
                case "mystcraft:ColorHorizon" -> state.pushHorizonColor();

                // Project policy disables the runtime lightning hazard, not the legacy
                // Symbol's modifier side effect. Preserve popGradient() so later color
                // Symbols see the same remaining modifier state as 0.13.7.06.
                case "mystcraft:EnvLightning" -> state.popGradient(null);

                case "mystcraft:SunNormal", "mystcraft:MoonNormal" -> {
                    state.popFactor();
                    state.popSunset();
                }
                case "mystcraft:StarsNormal", "mystcraft:StarsTwinkle" -> {
                    state.popFactor();
                    state.popGradient(new AgeColor(1F,1F,1F));
                }
                case "mystcraft:StarsEndSky" ->
                        state.popGradient(new AgeColor(.156F,.156F,.156F));

                case "mystcraft:ColorSky" -> sky.add(
                        dynamic(state.popGradient(new AgeColor(1F,1F,1F)), false));
                case "mystcraft:ColorSkyNight" -> sky.add(
                        dynamic(state.popGradient(new AgeColor(1F,1F,1F)), true));
                case "mystcraft:ColorSkyNat" -> sky.add(natural());

                case "mystcraft:ColorFog" -> fog.add(
                        dynamic(state.popGradient(new AgeColor(0.7529412F,0.8470588F,1.0F)), false));
                case "mystcraft:ColorFogNat" -> fog.add(natural());

                case "mystcraft:ColorCloud" -> cloud.add(
                        dynamic(state.popGradient(new AgeColor(1F,1F,1F)), false));
                case "mystcraft:ColorCloudNat" -> cloud.add(natural());

                case "mystcraft:ColorWater" -> water.add(staticColor(state.popColor()));
                case "mystcraft:ColorWaterNat" -> water.add(natural());

                case "mystcraft:ColorGrass" -> grass.add(staticColor(state.popColor()));
                case "mystcraft:ColorGrassNat" -> grass.add(natural());

                case "mystcraft:ColorFoliage" -> foliage.add(staticColor(state.popColor()));
                case "mystcraft:ColorFoliageNat" -> foliage.add(natural());

                default -> { }
            }
        }

        // SUNSET is consumed by celestial objects. A dangling ColorHorizon modifier had no
        // standalone lower-sky rendering effect in the old mod.
        return new AgeColorPlan(sky, fog, cloud, water, grass, foliage, new AgeColorGradient(List.of()));
    }

    private static AgeColorChannelPlan dynamic(AgeColorGradient gradient, boolean nightInverted) {
        return new AgeColorChannelPlan(AgeColorProviderMode.CUSTOM_DYNAMIC, gradient, null, nightInverted);
    }

    private static AgeColorChannelPlan staticColor(AgeColor color) {
        return new AgeColorChannelPlan(AgeColorProviderMode.CUSTOM_STATIC, null, color, false);
    }

    private static AgeColorChannelPlan natural() {
        return new AgeColorChannelPlan(AgeColorProviderMode.NATURAL, null, null, false);
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

    private static AgeColor colorFor(String symbol) {
        return switch (symbol) {
            case "mystcraft:ModColorMaroon" -> new AgeColor(0.50F,0.00F,0.00F);
            case "mystcraft:ModColorRed" -> new AgeColor(1.00F,0.00F,0.00F);
            case "mystcraft:ModColorOlive" -> new AgeColor(0.50F,0.50F,0.00F);
            case "mystcraft:ModColorYellow" -> new AgeColor(1.00F,1.00F,0.00F);
            case "mystcraft:ModColorDarkGreen" -> new AgeColor(0.00F,0.50F,0.00F);
            case "mystcraft:ModColorGreen" -> new AgeColor(0.00F,1.00F,0.00F);
            case "mystcraft:ModColorTeal" -> new AgeColor(0.00F,0.50F,0.50F);
            case "mystcraft:ModColorCyan" -> new AgeColor(0.00F,1.00F,1.00F);
            case "mystcraft:ModColorNavy" -> new AgeColor(0.00F,0.00F,0.50F);
            case "mystcraft:ModColorBlue" -> new AgeColor(0.00F,0.00F,1.00F);
            case "mystcraft:ModColorPurple" -> new AgeColor(0.50F,0.00F,0.50F);
            case "mystcraft:ModColorMagenta" -> new AgeColor(1.00F,0.00F,1.00F);
            case "mystcraft:ModColorBlack" -> new AgeColor(0.00F,0.00F,0.00F);
            case "mystcraft:ModColorGrey" -> new AgeColor(0.50F,0.50F,0.50F);
            case "mystcraft:ModColorSilver" -> new AgeColor(0.75F,0.75F,0.75F);
            case "mystcraft:ModColorWhite" -> new AgeColor(1.00F,1.00F,1.00F);
            default -> null;
        };
    }
}
