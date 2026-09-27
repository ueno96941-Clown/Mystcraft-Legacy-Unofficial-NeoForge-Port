package com.xcompwiz.mystcraft.world.worldgen;

/** Point-sampling equivalent of vanilla 1.12 GenLayerSmooth. */
public final class LegacySmoothBiomeLayer implements LegacyBiomeLayer {
    private final LegacyBiomeLayer parent;
    private final LegacyLayerRandom random;

    public LegacySmoothBiomeLayer(long worldSeed, long layerSeed, LegacyBiomeLayer parent) {
        this.parent = parent;
        this.random = new LegacyLayerRandom(layerSeed);
        this.random.initWorldGenSeed(worldSeed);
    }

    @Override
    public int sample(int x, int z) {
        int west = parent.sample(x - 1, z);
        int east = parent.sample(x + 1, z);
        int north = parent.sample(x, z - 1);
        int south = parent.sample(x, z + 1);
        int center = parent.sample(x, z);

        if (west == east && north == south) {
            synchronized (random) {
                random.initChunkSeed(x, z);
                return random.nextInt(2) == 0 ? west : north;
            }
        }
        if (west == east) return west;
        if (north == south) return north;
        return center;
    }
}
