package com.xcompwiz.mystcraft.world.worldgen;

/** Reproduces the y-range arithmetic from legacy SymbolTerrainGenFlat. */
public final class AgeFlatLayerPlanner {
    private AgeFlatLayerPlanner() {}

    public static AgeFlatLayerPlan plan(int averageGroundLevel, int seaLevel) {
        int terrainDepth = Math.max(0, averageGroundLevel - 1);
        int seaDepth = Math.max(0, seaLevel - averageGroundLevel + 1);
        return new AgeFlatLayerPlan(terrainDepth, seaDepth);
    }
}
