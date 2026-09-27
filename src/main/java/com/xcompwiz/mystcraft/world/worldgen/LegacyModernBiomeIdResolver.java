package com.xcompwiz.mystcraft.world.worldgen;

import com.xcompwiz.mystcraft.symbol.LegacyBiomeSymbolRegistry;

import java.util.Map;

public final class LegacyModernBiomeIdResolver {
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("minecraft:mountains", "minecraft:windswept_hills"),
            Map.entry("minecraft:snowy_mountains", "minecraft:snowy_slopes"),
            Map.entry("minecraft:mushroom_field_shore", "minecraft:mushroom_fields"),
            Map.entry("minecraft:desert_hills", "minecraft:desert"),
            Map.entry("minecraft:wooded_hills", "minecraft:forest"),
            Map.entry("minecraft:taiga_hills", "minecraft:taiga"),
            Map.entry("minecraft:mountain_edge", "minecraft:windswept_hills"),
            Map.entry("minecraft:jungle_hills", "minecraft:jungle"),
            Map.entry("minecraft:birch_forest_hills", "minecraft:birch_forest"),
            Map.entry("minecraft:snowy_taiga_hills", "minecraft:snowy_taiga"),
            Map.entry("minecraft:wooded_mountains", "minecraft:windswept_forest"),
            Map.entry("minecraft:desert_lakes", "minecraft:desert"),
            Map.entry("minecraft:gravelly_mountains", "minecraft:windswept_gravelly_hills"),
            Map.entry("minecraft:taiga_mountains", "minecraft:taiga"),
            Map.entry("minecraft:swamp_hills", "minecraft:swamp"),
            Map.entry("minecraft:modified_jungle", "minecraft:jungle"),
            Map.entry("minecraft:modified_jungle_edge", "minecraft:sparse_jungle"),
            Map.entry("minecraft:tall_birch_forest", "minecraft:old_growth_birch_forest"),
            Map.entry("minecraft:tall_birch_hills", "minecraft:old_growth_birch_forest"),
            Map.entry("minecraft:dark_forest_hills", "minecraft:dark_forest"),
            Map.entry("minecraft:snowy_taiga_mountains", "minecraft:snowy_taiga"),
            Map.entry("minecraft:giant_spruce_taiga", "minecraft:old_growth_spruce_taiga"),
            Map.entry("minecraft:giant_spruce_taiga_hills", "minecraft:old_growth_spruce_taiga"),
            Map.entry("minecraft:modified_gravelly_mountains", "minecraft:windswept_gravelly_hills"),
            Map.entry("minecraft:shattered_savanna", "minecraft:windswept_savanna"),
            Map.entry("minecraft:shattered_savanna_plateau", "minecraft:windswept_savanna"),
            Map.entry("minecraft:modified_wooded_badlands_plateau", "minecraft:wooded_badlands"),
            Map.entry("minecraft:modified_badlands_plateau", "minecraft:badlands")
    );

    private LegacyModernBiomeIdResolver() {}

    public static String resolve(String legacyBiomeSymbol) {
        String modern = LegacyBiomeSymbolRegistry.resolveLegacy(legacyBiomeSymbol)
                .map(def -> def.modernBiomeId())
                .orElse("minecraft:plains");
        return resolveModernId(modern);
    }

    /**
     * Canonicalizes a persisted/previously-resolved 1.12 biome resource id to a biome
     * that still exists in Minecraft 1.21.1. This runtime boundary is intentionally
     * idempotent so old Age records containing values such as minecraft:swamp_hills
     * can be restored safely after the alias table is introduced.
     */
    public static String resolveModernId(String biomeId) {
        return ALIASES.getOrDefault(biomeId, biomeId);
    }
}
