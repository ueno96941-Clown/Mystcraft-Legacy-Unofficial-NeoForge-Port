package com.xcompwiz.mystcraft.world.worldgen;

/** Point-sampling form of Mystcraft's legacy GenLayerBiomeMyst. */
public final class LegacyRandomBiomeLayer implements LegacyBiomeLayer {
    private final LegacyLayerRandom random;
    private final int biomeCount;

    public LegacyRandomBiomeLayer(long worldSeed, int biomeCount) {
        if (biomeCount <= 0) throw new IllegalArgumentException("biomeCount must be > 0");
        this.biomeCount = biomeCount;
        this.random = new LegacyLayerRandom(200L);
        this.random.initWorldGenSeed(worldSeed);
    }

    @Override
    public int sample(int x, int z) {
        synchronized (random) {
            random.initChunkSeed(x, z);
            return random.nextInt(biomeCount);
        }
    }
}
