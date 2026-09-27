package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Random;

/** Exact Minecraft 1.12 MapGenBase + MapGenMineshaft candidate test. */
public final class AgeLegacyMineshaftPlacementMath {
    public static final double CHANCE = 0.004D;

    private AgeLegacyMineshaftPlacementMath() {}

    /**
     * Replays the Random state seen by MapGenMineshaft#canSpawnStructureAtCoords.
     *
     * <p>MapGenBase first derives two world-seed multipliers, reseeds per candidate chunk,
     * then MapGenStructure#recursiveGenerate consumes one nextInt() before calling the
     * mineshaft predicate. The predicate itself is the 0.004 chance followed by the old
     * distance-from-origin gate.</p>
     */
    public static boolean isPlacementChunk(long worldSeed, int chunkX, int chunkZ) {
        Random random = new Random(worldSeed);
        long xMul = random.nextLong();
        long zMul = random.nextLong();
        random.setSeed((long) chunkX * xMul ^ (long) chunkZ * zMul ^ worldSeed);

        // MapGenStructure#recursiveGenerate: this.rand.nextInt();
        random.nextInt();
        return random.nextDouble() < CHANCE
                && random.nextInt(80) < Math.max(Math.abs(chunkX), Math.abs(chunkZ));
    }
}
