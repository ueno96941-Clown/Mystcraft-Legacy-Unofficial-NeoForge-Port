package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.world.worldgen.LegacyBiomeLayer;
import com.xcompwiz.mystcraft.world.worldgen.LegacyCachedBiomeLayer;
import com.xcompwiz.mystcraft.world.worldgen.LegacyRandomBiomeLayer;
import com.xcompwiz.mystcraft.world.worldgen.LegacySmoothBiomeLayer;
import com.xcompwiz.mystcraft.world.worldgen.LegacyZoomBiomeLayer;
import com.xcompwiz.mystcraft.world.worldgen.LegacyZoomBiomeLayerFactory;

/** Source/runtime invariant: cache optimization must not alter legacy biome indices. */
public final class LegacyZoomBiomeLayerCacheHarness {
    private static final long[] SEEDS = {0L, 1L, -1L, -3844323481697322708L, 0x5EED1234CAFEL};

    private LegacyZoomBiomeLayerCacheHarness() {}

    public static void main(String[] args) {
        for (long seed : SEEDS) {
            for (int zoomScale = 0; zoomScale <= 4; zoomScale++) {
                LegacyBiomeLayer plain = createPlain(seed, zoomScale, 4);
                LegacyBiomeLayer cached = LegacyZoomBiomeLayerFactory.create(seed, zoomScale, 4);
                for (int z = -24; z <= 24; z++) {
                    for (int x = -24; x <= 24; x++) {
                        int expected = plain.sample(x, z);
                        int actual = cached.sample(x, z);
                        if (expected != actual) {
                            throw new AssertionError("Biome cache changed output seed=" + seed
                                    + " zoomScale=" + zoomScale + " x=" + x + " z=" + z
                                    + " expected=" + expected + " actual=" + actual);
                        }
                    }
                }
            }
        }
        System.out.println("LegacyZoomBiomeLayerCacheHarness PASS");
    }

    private static LegacyBiomeLayer createPlain(long worldSeed, int zoomScale, int biomeCount) {
        LegacyBiomeLayer layer = new LegacyRandomBiomeLayer(worldSeed, biomeCount);
        layer = new LegacyZoomBiomeLayer(worldSeed, 1000L, layer);
        layer = new LegacyZoomBiomeLayer(worldSeed, 1001L, layer);
        for (int i = 0; i < zoomScale; i++) {
            layer = new LegacyZoomBiomeLayer(worldSeed, 1000L + i, layer);
        }
        return new LegacySmoothBiomeLayer(worldSeed, 1000L, layer);
    }
}
