package com.xcompwiz.mystcraft.world.worldgen;

public record AgeColor(float r, float g, float b) {
    public AgeColor average(AgeColor other) {
        return new AgeColor((r + other.r) / 2.0F, (g + other.g) / 2.0F, (b + other.b) / 2.0F);
    }
}
