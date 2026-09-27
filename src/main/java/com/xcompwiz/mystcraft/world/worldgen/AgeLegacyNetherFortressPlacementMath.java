package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Random;

/** Exact chunk-candidate math from Minecraft 1.12 MapGenNetherBridge. */
public final class AgeLegacyNetherFortressPlacementMath {
    public record Candidate(boolean enabled, int chunkX, int chunkZ) {}

    private AgeLegacyNetherFortressPlacementMath() {}

    public static Candidate candidate(long worldSeed, int queryChunkX, int queryChunkZ) {
        int regionX = queryChunkX >> 4;
        int regionZ = queryChunkZ >> 4;
        Random random = new Random((long) (regionX ^ regionZ << 4) ^ worldSeed);
        random.nextInt();
        boolean enabled = random.nextInt(3) == 0;
        int chunkX = (regionX << 4) + 4 + random.nextInt(8);
        int chunkZ = (regionZ << 4) + 4 + random.nextInt(8);
        return new Candidate(enabled, chunkX, chunkZ);
    }

    public static boolean isPlacementChunk(long worldSeed, int chunkX, int chunkZ) {
        Candidate candidate = candidate(worldSeed, chunkX, chunkZ);
        return candidate.enabled() && candidate.chunkX() == chunkX && candidate.chunkZ() == chunkZ;
    }
}
