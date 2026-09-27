package com.xcompwiz.mystcraft.world.worldgen;

/**
 * Legacy floating-island terrain target with a per-column modification mask.
 * The mask is consumed by the later biome surface/finalization pass.
 */
public interface LegacyFloatingIslandBuffer extends LegacyTerrainBuffer {
    void markModifiedColumn(int localX, int localZ);
}
