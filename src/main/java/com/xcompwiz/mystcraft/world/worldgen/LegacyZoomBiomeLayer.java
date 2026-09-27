package com.xcompwiz.mystcraft.world.worldgen;

/**
 * Point-sampling equivalent of Mystcraft's GenLayerZoomMyst.
 *
 * <p>For one output cell only the surrounding 2x2 parent cells are needed.</p>
 */
public final class LegacyZoomBiomeLayer implements LegacyBiomeLayer {
    private final LegacyBiomeLayer parent;
    private final LegacyLayerRandom random;

    public LegacyZoomBiomeLayer(long worldSeed, long layerSeed, LegacyBiomeLayer parent) {
        this.parent = parent;
        this.random = new LegacyLayerRandom(layerSeed);
        this.random.initWorldGenSeed(worldSeed);
    }

    @Override
    public int sample(int x, int z) {
        int parentX = x >> 1;
        int parentZ = z >> 1;
        int localX = x & 1;
        int localZ = z & 1;

        int nw = parent.sample(parentX, parentZ);
        if (localX == 0 && localZ == 0) return nw;

        int ne = parent.sample(parentX + 1, parentZ);
        int sw = parent.sample(parentX, parentZ + 1);
        int se = parent.sample(parentX + 1, parentZ + 1);

        synchronized (random) {
            random.initChunkSeed(((long) parentX) << 1, ((long) parentZ) << 1);
            int west = random.selectRandom(nw, sw);
            int north = random.selectRandom(nw, ne);
            int center = random.selectModeOrRandom(nw, ne, sw, se);

            if (localX == 0) return west;
            if (localZ == 0) return north;
            return center;
        }
    }
}
