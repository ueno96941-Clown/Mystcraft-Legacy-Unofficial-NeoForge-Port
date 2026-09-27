package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Random;

/**
 * Literal 2-D cutoff field from Mystcraft 0.13.7.06 SymbolSkylands.
 *
 * <p>The legacy Symbol constructs NoiseGeneratorOctaves(new Random(ageSeed), 7) and samples
 * a 16x16 field at unit scale. Non-air blocks at or below {@code 76 + (int)noise} are removed;
 * liquids above that cutoff are removed as well.</p>
 */
public final class LegacySkylandsKernel {
    private final LegacyNoiseGeneratorOctaves2D noise;
    private double[] skyNoise;

    public LegacySkylandsKernel(long ageSeed) {
        this.noise = new LegacyNoiseGeneratorOctaves2D(new Random(ageSeed), 7);
    }

    public synchronized int[] cutoffHeights(int chunkX, int chunkZ) {
        skyNoise = noise.generate(
                skyNoise,
                chunkX * 16,
                chunkZ * 16,
                16,
                16,
                1.0D,
                1.0D);

        int[] heights = new int[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // Legacy indexing: skyNoise[(x << 4) | z].
                heights[(x << 4) | z] = 76 + (int) skyNoise[(x << 4) | z];
            }
        }
        return heights;
    }
}
