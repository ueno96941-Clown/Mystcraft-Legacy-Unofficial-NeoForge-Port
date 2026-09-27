package com.xcompwiz.mystcraft.world.worldgen;

public record AgeGradientPoint(AgeColor color, float interval) {
    public AgeGradientPoint {
        if (color == null) throw new NullPointerException("color");
        if (interval <= 0.0F) interval = 1.0F;
    }
}
