package com.xcompwiz.mystcraft.world.worldgen;

import java.util.List;

public record AgeColorPlan(
        List<AgeColorChannelPlan> sky,
        List<AgeColorChannelPlan> fog,
        List<AgeColorChannelPlan> cloud,
        List<AgeColorChannelPlan> water,
        List<AgeColorChannelPlan> grass,
        List<AgeColorChannelPlan> foliage,
        AgeColorGradient horizonGradient) {
    public AgeColorPlan {
        sky = List.copyOf(sky);
        fog = List.copyOf(fog);
        cloud = List.copyOf(cloud);
        water = List.copyOf(water);
        grass = List.copyOf(grass);
        foliage = List.copyOf(foliage);
    }
}
