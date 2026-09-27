package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Random;

/** Exact 1.12 MapGenScatteredFeatureMyst 32/8/salt candidate math. */
public final class AgeLegacySmallLibraryPlacementMath {
    public static final int MAX_DISTANCE = 32;
    public static final int MIN_DISTANCE = 8;
    public static final long SALT = 14357617L;

    private AgeLegacySmallLibraryPlacementMath() {}

    public static boolean isLibraryChunk(long worldSeed, int chunkX, int chunkZ) {
        int adjustedX = chunkX;
        int adjustedZ = chunkZ;
        if (adjustedX < 0) adjustedX -= MAX_DISTANCE - 1;
        if (adjustedZ < 0) adjustedZ -= MAX_DISTANCE - 1;
        int regionX = adjustedX / MAX_DISTANCE;
        int regionZ = adjustedZ / MAX_DISTANCE;
        Random random = new Random(regionX * 341873128712L + regionZ * 132897987541L + worldSeed + SALT);
        int candidateX = regionX * MAX_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE);
        int candidateZ = regionZ * MAX_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE);
        return chunkX == candidateX && chunkZ == candidateZ;
    }
}
