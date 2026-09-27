package com.xcompwiz.mystcraft.world.worldgen;

/**
 * Exact 1.12 GenLayer seed arithmetic used by Mystcraft's biome layers.
 *
 * <p>Overflow is intentional and matches Java long arithmetic in legacy Minecraft.</p>
 */
public final class LegacyLayerRandom {
    private static final long MULTIPLIER = 6364136223846793005L;
    private static final long ADDEND = 1442695040888963407L;

    private final long baseSeed;
    private long worldGenSeed;
    private long chunkSeed;

    public LegacyLayerRandom(long layerSeed) {
        long seed = layerSeed;
        seed = seed * MULTIPLIER + ADDEND;
        seed += layerSeed;
        seed = seed * MULTIPLIER + ADDEND;
        seed += layerSeed;
        seed = seed * MULTIPLIER + ADDEND;
        seed += layerSeed;
        this.baseSeed = seed;
    }

    public void initWorldGenSeed(long worldSeed) {
        worldGenSeed = worldSeed;
        worldGenSeed = worldGenSeed * MULTIPLIER + ADDEND + baseSeed;
        worldGenSeed = worldGenSeed * MULTIPLIER + ADDEND + baseSeed;
        worldGenSeed = worldGenSeed * MULTIPLIER + ADDEND + baseSeed;
    }

    public void initChunkSeed(long x, long z) {
        chunkSeed = worldGenSeed;
        chunkSeed = chunkSeed * MULTIPLIER + ADDEND + x;
        chunkSeed = chunkSeed * MULTIPLIER + ADDEND + z;
        chunkSeed = chunkSeed * MULTIPLIER + ADDEND + x;
        chunkSeed = chunkSeed * MULTIPLIER + ADDEND + z;
    }

    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be > 0");
        int value = (int) ((chunkSeed >> 24) % bound);
        if (value < 0) value += bound;
        chunkSeed = chunkSeed * MULTIPLIER + ADDEND + worldGenSeed;
        return value;
    }

    public int selectRandom(int a, int b) {
        return nextInt(2) == 0 ? a : b;
    }

    public int selectModeOrRandom(int a, int b, int c, int d) {
        if (b == c && c == d) return b;
        if (a == b && a == c) return a;
        if (a == b && a == d) return a;
        if (a == c && a == d) return a;
        if (a == b && c != d) return a;
        if (a == c && b != d) return a;
        if (a == d && b != c) return a;
        if (b == c && a != d) return b;
        if (b == d && a != c) return b;
        if (c == d && a != b) return c;
        return switch (nextInt(4)) {
            case 0 -> a;
            case 1 -> b;
            case 2 -> c;
            default -> d;
        };
    }
}
