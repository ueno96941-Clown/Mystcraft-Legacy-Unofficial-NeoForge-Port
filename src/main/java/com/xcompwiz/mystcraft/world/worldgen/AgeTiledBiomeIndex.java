package com.xcompwiz.mystcraft.world.worldgen;

/** Pure coordinate math for legacy BioConTiled. */
public final class AgeTiledBiomeIndex {
    private AgeTiledBiomeIndex() {}

    public static int indexForBlock(int x, int z, int biomeCount) {
        if (biomeCount <= 0) throw new IllegalArgumentException("biomeCount must be > 0");
        int index = (x >> 4) + (z >> 4);
        index %= biomeCount;
        if (index < 0) index += biomeCount;
        return index;
    }

    public static int indexForGenerationCell(int x, int z, int biomeCount) {
        // Legacy Tiled did NOT multiply generation coordinates by 4.
        return indexForBlock(x, z, biomeCount);
    }
}
