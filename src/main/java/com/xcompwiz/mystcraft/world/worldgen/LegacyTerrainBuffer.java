package com.xcompwiz.mystcraft.world.worldgen;

/**
 * Minimal block-access contract used by the literal 1.12 MapGen compatibility kernels.
 * Coordinates are chunk-local X/Z and legacy kernel Y=0..255; runtime adapters map Y onto the current Age height.
 */
public interface LegacyTerrainBuffer {
    boolean isBedrock(int localX, int y, int localZ);
    boolean isLiquid(int localX, int y, int localZ);
    boolean isWater(int localX, int y, int localZ);
    void setReplacement(int localX, int y, int localZ);
}
