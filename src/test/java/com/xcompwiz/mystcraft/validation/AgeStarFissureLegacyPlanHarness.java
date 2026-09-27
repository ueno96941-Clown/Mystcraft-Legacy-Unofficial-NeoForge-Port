package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.world.worldgen.AgeStarFissureLegacyPlan;

import java.util.Arrays;
import java.util.Random;

/** Verifies SymbolStarFissure + WorldGenMystStarFissure coordinate/noise draw parity. */
public final class AgeStarFissureLegacyPlanHarness {
    public static void main(String[] args) {
        long[] seeds = {0L, 1L, -1L, 0x1234ABCD5678EF90L, Long.MIN_VALUE, Long.MAX_VALUE};
        int[][] chunks = {{0, 0}, {16, 16}, {-32, 48}, {1600, -3200}};
        for (long seed : seeds) {
            for (int[] chunk : chunks) {
                Random actualRandom = new Random(seed);
                AgeStarFissureLegacyPlan.Plan actual = AgeStarFissureLegacyPlan.create(
                        chunk[0], chunk[1], actualRandom::nextInt);

                Random reference = new Random(seed);
                int expectedX = chunk[0] + reference.nextInt(16) + 8;
                int expectedZ = chunk[1] + reference.nextInt(16) + 8;
                int[][] expectedNoise = referenceNoise(reference);

                require(actual.originX() == expectedX, "origin X mismatch");
                require(actual.originZ() == expectedZ, "origin Z mismatch");
                require(Arrays.deepEquals(actual.noise(), expectedNoise), "noise rows mismatch");
                require(actual.originX() >= chunk[0] + 8 && actual.originX() <= chunk[0] + 23,
                        "Legacy +8..+23 X range");
                require(actual.originZ() >= chunk[1] + 8 && actual.originZ() <= chunk[1] + 23,
                        "Legacy +8..+23 Z range");
                require(actual.noise().length >= 10 && actual.noise().length <= 17,
                        "Legacy length 10..17");
            }
        }
        System.out.println("AgeStarFissureLegacyPlanHarness: PASS");
    }

    private static int[][] referenceNoise(Random random) {
        int length = random.nextInt(8) + 10;
        int[][] noise = new int[length][2];
        noise[0][0] = noise[0][1] = 0;
        for (int row = 1; row < length; ++row) {
            int scale = length + Math.min(row, length - row) + 1;
            noise[row][1] = random.nextInt(scale) - (scale >> 1);
            noise[row][1] = (noise[row][1] + noise[row - 1][1]) / 2;
            noise[row][0] = random.nextInt(scale) - (scale >> 1);
            noise[row][0] = (noise[row][0] + noise[row - 1][0]) / 2;
            if (noise[row][0] > noise[row][1]) {
                int swap = noise[row][0];
                noise[row][0] = noise[row][1];
                noise[row][1] = swap;
            }
            if (noise[row][0] > noise[row - 1][1]) noise[row][0] = noise[row - 1][1];
            if (noise[row][1] < noise[row - 1][0]) noise[row][1] = noise[row - 1][0];
        }
        return noise;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
