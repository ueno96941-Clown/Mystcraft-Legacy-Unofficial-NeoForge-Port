package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.world.worldgen.AgeLegacyMineshaftPlacementMath;

/** Fixed vectors for the 1.12 MapGenBase + MapGenMineshaft placement stream. */
public final class AgeLegacyMineshaftPlacementMathHarness {
    private AgeLegacyMineshaftPlacementMathHarness() {}

    public static void main(String[] args) {
        check(!AgeLegacyMineshaftPlacementMath.isPlacementChunk(0L, 0, 0), "origin gate");
        check(AgeLegacyMineshaftPlacementMath.isPlacementChunk(0L, 6, 12), "seed0 6,12");
        check(AgeLegacyMineshaftPlacementMath.isPlacementChunk(0L, -19, 18), "seed0 -19,18");
        check(AgeLegacyMineshaftPlacementMath.isPlacementChunk(1L, 10, -2), "seed1 10,-2");
        check(AgeLegacyMineshaftPlacementMath.isPlacementChunk(123456789L, 0, 20), "seed123 0,20");
        check(AgeLegacyMineshaftPlacementMath.isPlacementChunk(-42L, -23, -19), "seed-42 -23,-19");
        check(!AgeLegacyMineshaftPlacementMath.isPlacementChunk(0L, 5, 12), "neighbor differs");
        System.out.println("AgeLegacyMineshaftPlacementMathHarness: PASS");
    }

    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
