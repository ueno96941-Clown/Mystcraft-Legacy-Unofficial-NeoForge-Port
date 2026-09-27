package com.xcompwiz.mystcraft.world.worldgen;

/** Pure coordinate math for legacy BioConGrid/BioConTiled. */
public final class AgeGridBiomeIndex {
    private AgeGridBiomeIndex() {}

    public static int indexForBlock(int x, int z, int biomeCount) {
        if (biomeCount <= 0) throw new IllegalArgumentException("biomeCount must be > 0");
        int index = (x >> 4) + (z >> 4);
        index %= biomeCount;
        if (index < 0) index += biomeCount;
        return index;
    }

    public static int indexForGenerationCell(int x, int z, int biomeCount) {
        // Legacy Grid's getBiomesForGeneration multiplied generation-cell coordinates by 4
        // before passing them into the same getBiomeAtCoords() method.
        return indexForBlock(x * 4, z * 4, biomeCount);
    }
}
