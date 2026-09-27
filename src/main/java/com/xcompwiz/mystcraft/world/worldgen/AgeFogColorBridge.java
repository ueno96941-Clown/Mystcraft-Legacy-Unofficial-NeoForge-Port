package com.xcompwiz.mystcraft.world.worldgen;

import java.util.List;

/** Exact 0.13.7.06 fog provider-list folding. */
public final class AgeFogColorBridge {
    private AgeFogColorBridge() {}

    public static AgeColor resolve(List<AgeColorChannelPlan> providers, float worldTimeOver12000, float celestialAngle) {
        AgeColor natural = natural(celestialAngle);
        AgeColor folded = AgeColorProviderFold.fold(providers, channel -> {
            if (channel.mode() == AgeColorProviderMode.UNSET) return null;
            if (channel.mode() == AgeColorProviderMode.NATURAL) return natural;
            if (channel.mode() == AgeColorProviderMode.CUSTOM_STATIC && channel.staticColor() != null) return channel.staticColor();
            if (channel.mode() == AgeColorProviderMode.CUSTOM_DYNAMIC && channel.gradient() != null && !channel.gradient().isEmpty()) {
                AgeColor color = channel.gradient().sample(worldTimeOver12000);
                if (color.r()==0F && color.g()==0F && color.b()==0F) return new AgeColor(.0001F,.0001F,.0001F);
                return color;
            }
            return null;
        });
        return folded == null ? natural : folded;
    }

    private static AgeColor natural(float celestialAngle) {
        float daylight=clamp((float)Math.cos(celestialAngle*Math.PI*2F)*2F+.5F);
        return new AgeColor(.7529412F*(daylight*.94F+.06F), .8470588F*(daylight*.94F+.06F), daylight*.91F+.09F);
    }
    private static float clamp(float v){return Math.max(0F,Math.min(1F,v));}
}
