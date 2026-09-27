package com.xcompwiz.mystcraft.world.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Small exact replay of the legacy COLOR / GRADIENT / FACTOR / SUNSET modifier slots.
 *
 * <p>The crucial property is destructive consumption: one modifier value cannot be reused by
 * two later Symbols. This mirrors AgeController.popModifier() and SymbolGradient/
 * SymbolHorizonColor from 0.13.7.06.</p>
 */
final class AgeLegacyColorModifierState {
    private Float factor;
    private AgeColor color;
    private final ArrayList<AgeGradientPoint> gradient = new ArrayList<>();
    private AgeColorGradient sunset;

    void pushFactor(float value) {
        factor = factor == null ? value : (factor + value) / 2.0F;
    }

    Float popFactor() {
        Float out = factor;
        factor = null;
        return out;
    }

    void pushColor(AgeColor next) {
        color = color == null ? next : color.average(next);
    }

    AgeColor popColor() {
        AgeColor out = color;
        color = null;
        return out;
    }

    /** Replays SymbolGradient: GRADIENT + FACTOR(default 1) + COLOR -> GRADIENT. */
    void pushGradientPoint() {
        float interval = factor == null ? 1.0F : factor;
        // Legacy ColorGradient.pushColor() normalized null/zero/negative intervals to 1.0F.
        // ModZero therefore freezes celestial periods, but does NOT create a zero-length color segment.
        if (interval <= 0.0F) interval = 1.0F;
        factor = null;
        if (color != null) {
            gradient.add(new AgeGradientPoint(color, interval));
            color = null;
        }
    }

    /** Replays ModifierUtils.popGradient(), with an optional default colour. */
    AgeColorGradient popGradient(AgeColor fallback) {
        if (!gradient.isEmpty()) {
            AgeColorGradient out = new AgeColorGradient(gradient);
            gradient.clear();
            // Legacy intentionally leaves COLOR untouched when a non-empty GRADIENT wins.
            return out;
        }
        gradient.clear();
        ArrayList<AgeGradientPoint> out = new ArrayList<>();
        if (color != null) {
            out.add(new AgeGradientPoint(color, 1.0F));
            color = null;
        } else if (fallback != null) {
            out.add(new AgeGradientPoint(fallback, 1.0F));
        }
        return new AgeColorGradient(out);
    }

    /** Replays SymbolHorizonColor: append the current gradient to the SUNSET slot. */
    void pushHorizonColor() {
        AgeColorGradient add = popGradient(null);
        if (sunset == null || sunset.isEmpty()) {
            sunset = add;
        } else if (!add.isEmpty()) {
            sunset = sunset.append(add);
        }
    }

    AgeColorGradient popSunset() {
        AgeColorGradient out = sunset;
        sunset = null;
        return out;
    }

    void clear() {
        factor = null;
        color = null;
        gradient.clear();
        sunset = null;
    }
}
