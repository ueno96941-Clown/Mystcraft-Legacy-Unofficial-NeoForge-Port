package com.xcompwiz.mystcraft.world.worldgen;

/**
 * Pure layer counts for legacy TerrainFlat.
 *
 * <p>Bedrock is always one layer at y=0. terrainDepth covers y=1..ground-1 and
 * seaDepth covers y=ground..seaLevel when seaLevel >= ground.</p>
 */
public record AgeFlatLayerPlan(int terrainDepth, int seaDepth) {
    public AgeFlatLayerPlan {
        if (terrainDepth < 0) throw new IllegalArgumentException("terrainDepth must be >= 0");
        if (seaDepth < 0) throw new IllegalArgumentException("seaDepth must be >= 0");
    }
}
