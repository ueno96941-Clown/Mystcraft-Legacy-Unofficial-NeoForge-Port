package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Random;

/**
 * Literal seed/range skeleton of Mystcraft 0.13.7.06 MapGenAdvanced.
 */
public abstract class LegacyMapGenAdvancedKernel {
    protected int range = 8;
    protected final Random rand = new Random();
    private final long seed;
    private final boolean replacementSolid;

    protected LegacyMapGenAdvancedKernel(long seed, boolean replacementSolid) {
        this.seed = seed;
        this.replacementSolid = replacementSolid;
    }

    public final void generate(int chunkX, int chunkZ, LegacyTerrainBuffer buffer) {
        rand.setSeed(seed);
        long xseed = rand.nextLong();
        long zseed = rand.nextLong();

        for (int x = chunkX - range; x <= chunkX + range; ++x) {
            for (int z = chunkZ - range; z <= chunkZ + range; ++z) {
                generateFromSource(x, z, chunkX, chunkZ, buffer, xseed, zseed);
            }
        }
    }

    /**
     * Executes exactly one source-chunk contribution. This matches the unit of work
     * that modern ChunkGenerator.applyCarvers invokes while it scans its own 17x17 area.
     */
    public final void generateFromSource(
            int sourceChunkX,
            int sourceChunkZ,
            int targetChunkX,
            int targetChunkZ,
            LegacyTerrainBuffer buffer) {

        rand.setSeed(seed);
        long xseed = rand.nextLong();
        long zseed = rand.nextLong();
        generateFromSource(
                sourceChunkX, sourceChunkZ, targetChunkX, targetChunkZ, buffer, xseed, zseed);
    }

    private void generateFromSource(
            int sourceChunkX,
            int sourceChunkZ,
            int targetChunkX,
            int targetChunkZ,
            LegacyTerrainBuffer buffer,
            long xseed,
            long zseed) {

        if (Math.abs(sourceChunkX - targetChunkX) > range
                || Math.abs(sourceChunkZ - targetChunkZ) > range) {
            return;
        }

        long xseed2 = sourceChunkX * xseed;
        long zseed2 = sourceChunkZ * zseed;
        rand.setSeed(xseed2 ^ zseed2 ^ seed);
        recursiveGenerate(
                sourceChunkX, sourceChunkZ, targetChunkX, targetChunkZ, buffer);
        afterRecursiveGenerate(
                sourceChunkX, sourceChunkZ, targetChunkX, targetChunkZ, buffer);
    }

    protected void afterRecursiveGenerate(
            int sourceChunkX,
            int sourceChunkZ,
            int targetChunkX,
            int targetChunkZ,
            LegacyTerrainBuffer buffer) {
    }

    protected abstract void recursiveGenerate(
            int sourceChunkX,
            int sourceChunkZ,
            int targetChunkX,
            int targetChunkZ,
            LegacyTerrainBuffer buffer);

    protected final boolean placeBlock(
            LegacyTerrainBuffer buffer, int x, int y, int z) {
        if (buffer.isBedrock(x, y, z)) {
            return false;
        }
        if (!replacementSolid && buffer.isLiquid(x, y, z)) {
            return false;
        }
        buffer.setReplacement(x, y, z);
        return true;
    }
}
