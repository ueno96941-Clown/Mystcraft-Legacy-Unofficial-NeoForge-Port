package com.xcompwiz.mystcraft.world.worldgen;

import java.util.List;

/** Replays legacy AgeDirector ground/sea-level side effects independently of which terrain controller wins. */
public final class AgeTerrainLevelResolver {
    public static final int DEFAULT_AVERAGE_GROUND_LEVEL = 64;
    public static final int DEFAULT_SEA_LEVEL = 63;

    private AgeTerrainLevelResolver() {}

    public static AgeTerrainLevels resolve(List<String> effectiveSymbols) {
        Integer sea = null;
        for (String symbol : effectiveSymbols) {
            Integer next = switch (symbol) {
                case "mystcraft:TerrainNether" -> 32;
                case "mystcraft:TerrainEnd" -> 49;
                default -> null;
            };
            if (next != null) sea = sea == null ? next : (sea + next) / 2;
        }
        return new AgeTerrainLevels(DEFAULT_AVERAGE_GROUND_LEVEL, sea == null ? DEFAULT_SEA_LEVEL : sea);
    }
}
