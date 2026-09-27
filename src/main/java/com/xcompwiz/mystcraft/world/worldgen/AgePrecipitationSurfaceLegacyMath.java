package com.xcompwiz.mystcraft.world.worldgen;

/** Pure coordinate formulas from Minecraft 1.12 EntityRenderer#addRainParticles. */
public final class AgePrecipitationSurfaceLegacyMath {
    private AgePrecipitationSurfaceLegacyMath() {}

    /** WATER_DROP Y: belowBlockY + 0.1 + boundingBox.maxY. */
    public static double rainParticleY(int belowBlockY, double maxY) {
        return (double) belowBlockY + 0.1D + maxY;
    }

    /** Legacy selected rain-sound Y is one block below the WATER_DROP particle Y. */
    public static double rainSoundY(int belowBlockY, double maxY) {
        return rainParticleY(belowBlockY, maxY) - 1.0D;
    }

    /** SMOKE_NORMAL Y: precipitationY + 0.1 - boundingBox.minY. */
    public static double hotSurfaceSmokeY(int precipitationY, double minY) {
        return (double) precipitationY + 0.1D - minY;
    }
}
