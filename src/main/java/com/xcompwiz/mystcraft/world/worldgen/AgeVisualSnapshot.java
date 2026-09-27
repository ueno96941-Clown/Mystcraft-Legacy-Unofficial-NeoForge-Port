package com.xcompwiz.mystcraft.world.worldgen;

import java.util.List;
import java.util.Map;

/** Typed, registry-independent client view decoded from AgeVisualPayload. */
public record AgeVisualSnapshot(
        int version, AgeLightingMode lighting, boolean drawHorizon, boolean drawVoid,
        int horizonHeight, float cloudHeight, boolean endSkyBackground, AgeWeatherMode weather,
        List<AgeCelestialPlan> celestials, Map<String, List<AgeColorChannelPlan>> colors,
        AgeColorGradient horizonGradient, AgeColorGradient endSkyGradient) {
    public AgeVisualSnapshot {
        celestials = List.copyOf(celestials);
        java.util.HashMap<String,List<AgeColorChannelPlan>> copy = new java.util.HashMap<>();
        colors.forEach((k,v) -> copy.put(k, List.copyOf(v)));
        colors = Map.copyOf(copy);
    }
}
