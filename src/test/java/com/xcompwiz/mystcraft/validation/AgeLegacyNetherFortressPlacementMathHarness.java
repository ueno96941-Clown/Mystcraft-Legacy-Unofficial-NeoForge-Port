package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.world.worldgen.AgeLegacyNetherFortressPlacementMath;

/** Pure-Java regression vectors for Minecraft 1.12 MapGenNetherBridge placement math. */
public final class AgeLegacyNetherFortressPlacementMathHarness {
    private AgeLegacyNetherFortressPlacementMathHarness() {}

    public static void main(String[] args) {
        assertCandidate(0L, 0, 0, false, 5, 8);
        assertCandidate(0L, 16, 16, true, 25, 24);
        assertCandidate(1L, -1, -1, true, -5, -8);
        assertCandidate(123456789L, 0, 0, true, 7, 6);
        assertCandidate(123456789L, -16, -16, true, -7, -12);
        assertCandidate(-42L, 0, 0, true, 4, 9);

        // Queries inside the same 16x16 region must resolve the same regional candidate.
        var a = AgeLegacyNetherFortressPlacementMath.candidate(0L, 0, 0);
        var b = AgeLegacyNetherFortressPlacementMath.candidate(0L, 15, 15);
        if (!a.equals(b)) throw new AssertionError("regional candidate drift: " + a + " vs " + b);

        if (!AgeLegacyNetherFortressPlacementMath.isPlacementChunk(0L, 25, 24)) {
            throw new AssertionError("known enabled fortress candidate not recognized");
        }
        if (AgeLegacyNetherFortressPlacementMath.isPlacementChunk(0L, 5, 8)) {
            throw new AssertionError("disabled one-in-three region must not place fortress");
        }
        System.out.println("AgeLegacyNetherFortressPlacementMathHarness: PASS");
    }

    private static void assertCandidate(
            long seed, int qx, int qz, boolean enabled, int x, int z) {
        var actual = AgeLegacyNetherFortressPlacementMath.candidate(seed, qx, qz);
        var expected = new AgeLegacyNetherFortressPlacementMath.Candidate(enabled, x, z);
        if (!actual.equals(expected)) {
            throw new AssertionError("candidate mismatch seed=" + seed + " query=" + qx + "," + qz
                    + " expected=" + expected + " actual=" + actual);
        }
    }
}
