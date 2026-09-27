package com.xcompwiz.mystcraft.world.worldgen;

import java.util.List;

/** Legacy AgeController color-provider list folding (sequential pairwise average). */
public final class AgeColorProviderFold {
    private AgeColorProviderFold() {}

    public interface Sampler { AgeColor sample(AgeColorChannelPlan plan); }

    public static AgeColor fold(List<AgeColorChannelPlan> providers, Sampler sampler) {
        if (providers == null || providers.isEmpty()) return null;
        AgeColor color = null;
        for (AgeColorChannelPlan provider : providers) {
            AgeColor next = sampler.sample(provider);
            if (next == null) continue;
            color = color == null ? next : average(color, next);
        }
        return color;
    }

    public static AgeColor average(AgeColor a, AgeColor b) {
        return new AgeColor((a.r()+b.r())*0.5F, (a.g()+b.g())*0.5F, (a.b()+b.b())*0.5F);
    }
}
