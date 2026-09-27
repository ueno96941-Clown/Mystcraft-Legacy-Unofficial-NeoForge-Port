package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Set;

/**
 * Legacy 1.12 `Biome.topBlock/fillerBlock` compatibility for the restored vanilla biome IDs.
 * Unlisted vanilla biomes use the 1.12 Biome defaults: grass / dirt.
 */
public final class LegacyBiomeSurfaceRegistry {
    private static final Set<Integer> DESERT_OR_BEACH =
            Set.of(2, 16, 17, 26, 130);
    private static final Set<Integer> MUSHROOM =
            Set.of(14, 15);
    private static final Set<Integer> BADLANDS =
            Set.of(37, 38, 39, 165, 166, 167);

    private LegacyBiomeSurfaceRegistry() {}

    public static LegacyBiomeSurfaceDefinition resolve(String legacyBiomeSymbol) {
        int id = parseNumericId(legacyBiomeSymbol);

        if (id == 8) {
            return new LegacyBiomeSurfaceDefinition("minecraft:netherrack", "minecraft:netherrack");
        }
        if (id == 9) {
            return new LegacyBiomeSurfaceDefinition("minecraft:end_stone", "minecraft:end_stone");
        }
        if (id == 25) {
            return new LegacyBiomeSurfaceDefinition("minecraft:stone", "minecraft:stone");
        }
        if (MUSHROOM.contains(id)) {
            return new LegacyBiomeSurfaceDefinition("minecraft:mycelium", "minecraft:dirt");
        }
        if (DESERT_OR_BEACH.contains(id)) {
            return new LegacyBiomeSurfaceDefinition("minecraft:sand", "minecraft:sand");
        }
        if (BADLANDS.contains(id)) {
            // 1.12 Mesa topBlock was sand(red-sand state), filler was stained hardened clay.
            return new LegacyBiomeSurfaceDefinition("minecraft:red_sand", "minecraft:terracotta");
        }

        return new LegacyBiomeSurfaceDefinition("minecraft:grass_block", "minecraft:dirt");
    }

    static int parseNumericId(String symbol) {
        if (symbol == null || !symbol.startsWith("mystcraft:Biome")) {
            return 1; // plains-compatible fallback
        }
        try {
            return Integer.parseInt(symbol.substring("mystcraft:Biome".length()));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }
}
