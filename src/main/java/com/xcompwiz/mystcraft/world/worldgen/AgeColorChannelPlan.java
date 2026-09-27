package com.xcompwiz.mystcraft.world.worldgen;

public record AgeColorChannelPlan(
        AgeColorProviderMode mode,
        AgeColorGradient gradient,
        AgeColor staticColor,
        boolean nightInverted) {

    public static AgeColorChannelPlan unset() {
        return new AgeColorChannelPlan(AgeColorProviderMode.UNSET, null, null, false);
    }
}
