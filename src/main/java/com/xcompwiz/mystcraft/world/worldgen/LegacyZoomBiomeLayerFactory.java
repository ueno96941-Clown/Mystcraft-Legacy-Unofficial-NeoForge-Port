package com.xcompwiz.mystcraft.world.worldgen;

/**
 * Builds the generation-biome layer used by legacy Huge/Large/Medium/Small/Tiny.
 *
 * <p>Matches SymbolBiomeControllerLarge.computeGenLayers() for genBiomes:
 * random biome layer(seed 200) -> 2 zooms(seed 1000,1001) -> zoomScale extra zooms
 * (seed 1000+i) -> smooth(seed 1000).</p>
 *
 * <p>Each stage is wrapped in a bounded memoization layer. Legacy 1.12 GenLayer
 * evaluated areas and reused parent cells; the 1.21 point-sampling port otherwise
 * recursively recomputed those cells, becoming pathological at Huge scale.</p>
 */
public final class LegacyZoomBiomeLayerFactory {
    private LegacyZoomBiomeLayerFactory() {}

    public static LegacyBiomeLayer create(long worldSeed, int zoomScale, int biomeCount) {
        if (zoomScale < 0 || zoomScale > 4) {
            throw new IllegalArgumentException("zoomScale must be in [0,4]");
        }

        LegacyBiomeLayer layer = cached(new LegacyRandomBiomeLayer(worldSeed, biomeCount));

        layer = cached(new LegacyZoomBiomeLayer(worldSeed, 1000L, layer));
        layer = cached(new LegacyZoomBiomeLayer(worldSeed, 1001L, layer));

        for (int i = 0; i < zoomScale; i++) {
            layer = cached(new LegacyZoomBiomeLayer(worldSeed, 1000L + i, layer));
        }

        return cached(new LegacySmoothBiomeLayer(worldSeed, 1000L, layer));
    }

    private static LegacyBiomeLayer cached(LegacyBiomeLayer layer) {
        return new LegacyCachedBiomeLayer(layer);
    }
}
