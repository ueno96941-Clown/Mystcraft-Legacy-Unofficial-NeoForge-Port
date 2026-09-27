package com.xcompwiz.mystcraft.world.worldgen;

/** Legacy lighting controller behavior retained independently of rendering backend. */
public record AgeLightingPlan(AgeLightingMode mode) {
    public int scaleBlockLight(int blockLightValue) {
        int clamped = Math.max(0, Math.min(15, blockLightValue));
        return switch (mode) {
            case BRIGHT -> clamped + (15 - clamped) / 2;
            case DARK -> clamped / 2;
            case NORMAL, UNKNOWN -> clamped;
        };
    }

    public float brightnessTableValue(int lightLevel) {
        int i = Math.max(0, Math.min(15, lightLevel));
        float f1 = 1.0F - i / 15.0F;
        float normal = (1.0F - f1) / (f1 * 3.0F + 1.0F);
        return switch (mode) {
            case BRIGHT -> normal * 0.75F + 0.25F;
            case DARK -> normal / 2.0F;
            case NORMAL, UNKNOWN -> normal;
        };
    }
}
