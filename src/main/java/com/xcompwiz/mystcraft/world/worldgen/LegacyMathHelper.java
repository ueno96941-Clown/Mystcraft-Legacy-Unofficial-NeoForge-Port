package com.xcompwiz.mystcraft.world.worldgen;

/** Minecraft 1.12-compatible sine-table helpers used by legacy MapGen classes. */
public final class LegacyMathHelper {
    private static final float[] SIN_TABLE = new float[65536];

    static {
        for (int i = 0; i < SIN_TABLE.length; i++) {
            SIN_TABLE[i] = (float) Math.sin(i * Math.PI * 2.0D / 65536.0D);
        }
    }

    private LegacyMathHelper() {}

    public static float sin(float value) {
        return SIN_TABLE[(int) (value * 10430.378F) & 65535];
    }

    public static float cos(float value) {
        return SIN_TABLE[(int) (value * 10430.378F + 16384.0F) & 65535];
    }

    public static int floor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }
}
