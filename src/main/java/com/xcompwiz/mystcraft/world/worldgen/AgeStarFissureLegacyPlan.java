package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Objects;

/** Pure-Java port of the coordinate/noise draws performed by SymbolStarFissure + WorldGenMystStarFissure. */
public final class AgeStarFissureLegacyPlan {
    private AgeStarFissureLegacyPlan() {}

    @FunctionalInterface
    public interface BoundedIntRandom {
        int nextInt(int bound);
    }

    /**
     * One Legacy Star Fissure population attempt always succeeds. The old while(!flag) loop
     * therefore performs exactly one +8..+23 X/Z draw followed by the fissure noise draws.
     */
    public static Plan create(int chunkBlockX, int chunkBlockZ, BoundedIntRandom random) {
        Objects.requireNonNull(random, "random");
        int originX = chunkBlockX + random.nextInt(16) + 8;
        int originZ = chunkBlockZ + random.nextInt(16) + 8;

        int length = random.nextInt(8) + 10;
        int[][] noise = new int[length][2];
        noise[0][0] = 0;
        noise[0][1] = 0;
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
        return new Plan(originX, originZ, noise);
    }

    public record Plan(int originX, int originZ, int[][] noise) {
        public Plan {
            Objects.requireNonNull(noise, "noise");
            if (noise.length < 10 || noise.length > 17) {
                throw new IllegalArgumentException("Legacy Star Fissure noise length out of range: " + noise.length);
            }
        }
    }
}
