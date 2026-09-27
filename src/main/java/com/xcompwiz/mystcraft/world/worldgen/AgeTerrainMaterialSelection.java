package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Objects;

/** Legacy terrain-generator material choices resolved from the effective Symbol execution order. */
public record AgeTerrainMaterialSelection(String terrainBlockId, String seaBlockId) {
    public AgeTerrainMaterialSelection {
        terrainBlockId = Objects.requireNonNull(terrainBlockId, "terrainBlockId");
        seaBlockId = Objects.requireNonNull(seaBlockId, "seaBlockId");
    }
}
