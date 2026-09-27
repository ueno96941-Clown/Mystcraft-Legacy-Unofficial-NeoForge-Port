package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.world.worldgen.AgeLegacyPopulationSeedMath;
import java.util.Random;

/** Verifies the exact 0.13.7.06 ChunkProviderMyst population reseed boundary. */
public final class AgeLegacyPopulationSeedMathHarness {
    public static void main(String[] args) {
        long[] seeds = {0L, 1L, -1L, 1234567890123456789L, Long.MIN_VALUE, Long.MAX_VALUE};
        int[][] chunks = {{0,0},{1,0},{0,1},{17,-9},{-12345,6789},{Integer.MAX_VALUE,Integer.MIN_VALUE}};
        for (long seed : seeds) {
            for (int[] chunk : chunks) {
                long actual = AgeLegacyPopulationSeedMath.populationBoundarySeed(seed, chunk[0], chunk[1]);
                Random reference = new Random(seed);
                long xMul = reference.nextLong() / 2L * 2L + 1L;
                long zMul = reference.nextLong() / 2L * 2L + 1L;
                long expected = (long) chunk[0] * xMul + (long) chunk[1] * zMul ^ seed;
                if (actual != expected) {
                    throw new AssertionError("population seed mismatch seed=" + seed
                            + " chunk=" + chunk[0] + "," + chunk[1]
                            + " expected=" + expected + " actual=" + actual);
                }
            }
        }
        System.out.println("AgeLegacyPopulationSeedMathHarness: PASS");
    }
}
