package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.world.worldgen.AgeBiomeControllerMode;
import com.xcompwiz.mystcraft.world.worldgen.AgeBiomeControllerResolver;
import com.xcompwiz.mystcraft.world.worldgen.AgeLightingMode;
import com.xcompwiz.mystcraft.world.worldgen.AgeLightingResolver;
import com.xcompwiz.mystcraft.world.worldgen.AgeSymbolResolver;
import com.xcompwiz.mystcraft.world.worldgen.AgeTerrainMaterialResolver;
import com.xcompwiz.mystcraft.world.worldgen.AgeTerrainMode;
import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherMode;
import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherResolver;

import java.util.List;

/** Runtime-independent grammar/resolver check for CP221's default authored Age sequence. */
public final class BookBinderDefaultAgeResolutionHarness {
    private static final List<String> AUTHORED = List.of(
            "mystcraft:ModMat_stone_0",
            "mystcraft:ModMat_water_0",
            "mystcraft:TerrainFlat",
            "mystcraft:Biome1",
            "mystcraft:BioConSingle",
            "mystcraft:WeatherNorm",
            "mystcraft:LightingNormal"
    );

    public static void main(String[] args) {
        for (long seed : new long[] {0L, 1L, 2L, 123456789L, -1L}) {
            List<String> effective = AgeSymbolResolver.resolve(seed, AUTHORED);
            if (effective.size() < AUTHORED.size() || !effective.subList(0, AUTHORED.size()).equals(AUTHORED)) {
                throw new AssertionError("Legacy grammar moved/rewrote authored default pages for seed " + seed + ": " + effective);
            }

            long terrainControllers = effective.stream().filter(BookBinderDefaultAgeResolutionHarness::isTerrainController).count();
            if (terrainControllers != 1 || !effective.contains("mystcraft:TerrainFlat")) {
                throw new AssertionError("Default sequence gained a second terrain controller for seed " + seed);
            }

            var material = AgeTerrainMaterialResolver.resolve(AgeTerrainMode.FLAT, effective);
            if (!"minecraft:stone".equals(material.terrainBlockId()) || !"minecraft:water".equals(material.seaBlockId())) {
                throw new AssertionError("Default material resolution drifted for seed " + seed + ": " + material);
            }

            var biome = AgeBiomeControllerResolver.resolve(effective);
            if (biome.mode() != AgeBiomeControllerMode.SINGLE
                    || !biome.explicitLegacyBiomeSymbols().equals(List.of("mystcraft:Biome1"))) {
                throw new AssertionError("Default biome resolution drifted for seed " + seed + ": " + biome);
            }
            if (AgeWeatherResolver.resolve(effective).mode() != AgeWeatherMode.NORMAL) {
                throw new AssertionError("Default weather resolution drifted for seed " + seed);
            }
            if (AgeLightingResolver.resolve(effective).mode() != AgeLightingMode.NORMAL) {
                throw new AssertionError("Default lighting resolution drifted for seed " + seed);
            }
        }
        System.out.println("BookBinderDefaultAgeResolutionHarness: PASS");
    }

    private static boolean isTerrainController(String symbol) {
        return switch (symbol) {
            case "mystcraft:TerrainNormal", "mystcraft:TerrainAmplified", "mystcraft:TerrainNether",
                    "mystcraft:TerrainEnd", "mystcraft:TerrainFlat", "mystcraft:TerrainVoid" -> true;
            default -> false;
        };
    }
}
