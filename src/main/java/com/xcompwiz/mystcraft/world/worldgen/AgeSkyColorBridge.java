package com.xcompwiz.mystcraft.world.worldgen;

import java.util.List;

/** Pure client sampling for legacy ColorSky/ColorSkyNight provider lists. */
public final class AgeSkyColorBridge {
    private AgeSkyColorBridge() {}

    public static AgeColor sky(AgeVisualSnapshot snapshot, int naturalRgb, float legacyTemperature, long worldTime, float celestialAngle) {
        AgeColor natural = fromRgb(naturalRgb);
        List<AgeColorChannelPlan> providers = snapshot.colors().get("sky");
        AgeColor folded = AgeColorProviderFold.fold(providers, channel -> {
            if (channel.mode() == AgeColorProviderMode.UNSET) return null;
            if (channel.mode() == AgeColorProviderMode.NATURAL) return legacyNaturalSky(legacyTemperature, celestialAngle);
            AgeColor color;
            if (channel.mode() == AgeColorProviderMode.CUSTOM_STATIC && channel.staticColor() != null) {
                color = channel.staticColor();
            } else if (channel.gradient() != null && !channel.gradient().isEmpty()) {
                color = channel.gradient().sample(worldTime / 12000.0F);
            } else return null;
            float alpha = clamp((float)Math.cos(celestialAngle * Math.PI * 2.0F) * 2.0F + 0.5F);
            if (channel.nightInverted()) alpha = 1.0F - alpha;
            return new AgeColor(color.r()*alpha, color.g()*alpha, color.b()*alpha);
        });
        return folded == null ? natural : folded;
    }


    /** Exact SymbolColorSkyNatural 0.13.7.06 colorizer (non-anaglyph path). */
    public static AgeColor legacyNaturalSky(float temperature, float celestialAngle) {
        float alpha = clamp((float)Math.cos(celestialAngle * Math.PI * 2.0F) * 2.0F + 0.5F);
        float t = Math.max(-1.0F, Math.min(1.0F, temperature / 3.0F));
        int rgb = hsvToRgb(0.62222224F - t * 0.05F, 0.5F + t * 0.1F, 1.0F);
        AgeColor base = fromRgb(rgb);
        return new AgeColor(base.r() * alpha, base.g() * alpha, base.b() * alpha);
    }

    // java.awt.Color.HSBtoRGB-equivalent conversion without a java.desktop runtime dependency.
    private static int hsvToRgb(float hue, float saturation, float value) {
        int r, g, b;
        if (saturation == 0.0F) {
            r = g = b = Math.round(value * 255.0F);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0F;
            int sector = (int)h;
            float f = h - sector;
            float p = value * (1.0F - saturation);
            float q = value * (1.0F - saturation * f);
            float t = value * (1.0F - saturation * (1.0F - f));
            float rf, gf, bf;
            switch (sector) {
                case 0 -> { rf=value; gf=t; bf=p; }
                case 1 -> { rf=q; gf=value; bf=p; }
                case 2 -> { rf=p; gf=value; bf=t; }
                case 3 -> { rf=p; gf=q; bf=value; }
                case 4 -> { rf=t; gf=p; bf=value; }
                default -> { rf=value; gf=p; bf=q; }
            }
            r=Math.round(rf*255.0F); g=Math.round(gf*255.0F); b=Math.round(bf*255.0F);
        }
        return (r << 16) | (g << 8) | b;
    }

    public static AgeColor horizon(AgeVisualSnapshot snapshot, float sample) {
        AgeColorGradient g = snapshot.horizonGradient();
        return g == null || g.isEmpty() ? null : g.sample(sample);
    }

    public static boolean requiresLegacyWeatherPass(AgeVisualSnapshot snapshot) {
        List<AgeColorChannelPlan> providers = snapshot.colors().get("sky");
        if (providers == null) return false;
        for (AgeColorChannelPlan channel : providers) {
            if (channel.mode() == AgeColorProviderMode.CUSTOM_DYNAMIC || channel.mode() == AgeColorProviderMode.CUSTOM_STATIC) return true;
        }
        return false;
    }

    private static AgeColor fromRgb(int rgb) {
        return new AgeColor(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F);
    }
    private static float clamp(float value) { return Math.max(0F, Math.min(1F, value)); }
}
