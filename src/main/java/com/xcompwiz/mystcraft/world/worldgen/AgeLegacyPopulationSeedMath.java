package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Random;

/** Exact ChunkProviderMyst 0.13.7.06 chunk-population reseed boundary. */
public final class AgeLegacyPopulationSeedMath {
    private AgeLegacyPopulationSeedMath() {}

    /**
     * Returns the value passed to Random#setSeed immediately before Legacy chunk population.
     * chunkX/chunkZ are chunk coordinates, not block coordinates.
     */
    public static long populationBoundarySeed(long ageSeed, int chunkX, int chunkZ) {
        Random random = new Random(ageSeed);
        long xMul = random.nextLong() / 2L * 2L + 1L;
        long zMul = random.nextLong() / 2L * 2L + 1L;
        return (long) chunkX * xMul + (long) chunkZ * zMul ^ ageSeed;
    }
}
