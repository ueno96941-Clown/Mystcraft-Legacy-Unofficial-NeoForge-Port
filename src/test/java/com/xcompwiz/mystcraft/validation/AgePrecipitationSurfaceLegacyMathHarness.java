package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.world.worldgen.AgePrecipitationSurfaceLegacyMath;

public final class AgePrecipitationSurfaceLegacyMathHarness {
    private AgePrecipitationSurfaceLegacyMathHarness() {}

    public static void main(String[] args) {
        close(AgePrecipitationSurfaceLegacyMath.rainParticleY(63, 1.0D), 64.1D, "full particle");
        close(AgePrecipitationSurfaceLegacyMath.rainParticleY(63, 0.5D), 63.6D, "slab particle");
        close(AgePrecipitationSurfaceLegacyMath.rainSoundY(63, 1.0D), 63.1D, "full sound");
        close(AgePrecipitationSurfaceLegacyMath.hotSurfaceSmokeY(64, 0.0D), 64.1D, "lava smoke");
        close(AgePrecipitationSurfaceLegacyMath.hotSurfaceSmokeY(64, 0.25D), 63.85D, "raised min smoke");
        System.out.println("AgePrecipitationSurfaceLegacyMathHarness: PASS");
    }

    private static void close(double actual, double expected, String label) {
        if (Math.abs(actual - expected) > 1.0E-9D) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }
}
