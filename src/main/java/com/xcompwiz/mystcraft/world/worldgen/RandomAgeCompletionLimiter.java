package com.xcompwiz.mystcraft.world.worldgen;

import com.xcompwiz.mystcraft.symbol.LegacyBiomeSymbolRegistry;
import com.xcompwiz.mystcraft.symbol.LegacyMaterialSymbolRegistry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Safety boundary for grammar-generated (not explicitly authored) Age completion.
 *
 * <p>The legacy grammar intentionally allows recursive plural expansion.  That is useful for
 * hand-authored books, but modern runtime biomes can carry mutually incompatible placed-feature
 * orderings.  Random completion therefore uses a bounded semantic budget: singleton decisions
 * get one slot, inherently plural decisions get two. Explicitly authored Pages are never removed.</p>
 */
public final class RandomAgeCompletionLimiter {
    private RandomAgeCompletionLimiter() {}

    private enum Bucket {
        TERRAIN(1), TERRAIN_MATERIAL(1), SEA_MATERIAL(1), BIOME_CONTROLLER(1), BIOME(2), WEATHER(1), LIGHTING(1),
        SUN(2), MOON(2), STARFIELD(2),
        FOG_COLOR(1), DAY_SKY_COLOR(1), NIGHT_SKY_COLOR(1), CLOUD_COLOR(1),
        GRASS_COLOR(1), FOLIAGE_COLOR(1), WATER_COLOR(1),
        HORIZON_COLOR(1), HORIZON(1), RAINBOW(1),
        SMALL_FEATURE(2), MEDIUM_FEATURE(2), LARGE_FEATURE(2), ENV_EFFECT(2);

        final int max;
        Bucket(int max) { this.max = max; }
    }

    public static List<String> limit(List<String> expanded, List<String> explicit) {
        Map<String, Integer> explicitRemaining = new HashMap<>();
        for (String id : explicit) explicitRemaining.merge(id, 1, Integer::sum);

        EnumMap<Bucket, Integer> used = new EnumMap<>(Bucket.class);
        ArrayList<String> result = new ArrayList<>(expanded.size());

        int terrainMaterialSlotsSeen = 0;
        boolean terrainDecisionSeen = false;

        for (String id : expanded) {
            boolean authored = consume(explicitRemaining, id);
            Bucket bucket = bucket(id);

            // TerrainGen always emits BlockTerrain, BlockSea, then the terrain terminal.
            // While still inside that leading TerrainGen segment, classify the first two
            // material terminals by role. Material symbols used later by Lakes/Tendrils/etc.
            // are deliberately left alone.
            if (!terrainDecisionSeen && LegacyMaterialSymbolRegistry.resolveLegacy(id).isPresent()) {
                bucket = terrainMaterialSlotsSeen++ == 0 ? Bucket.TERRAIN_MATERIAL : Bucket.SEA_MATERIAL;
            }
            if (bucket == Bucket.TERRAIN) terrainDecisionSeen = true;
            if (authored) {
                result.add(id);
                if (bucket != null) used.merge(bucket, 1, Integer::sum);
                continue;
            }
            if (bucket == null) {
                result.add(id);
                continue;
            }
            int count = used.getOrDefault(bucket, 0);
            if (count < bucket.max) {
                result.add(id);
                used.put(bucket, count + 1);
            }
        }
        return List.copyOf(result);
    }

    private static boolean consume(Map<String, Integer> remaining, String id) {
        int count = remaining.getOrDefault(id, 0);
        if (count <= 0) return false;
        if (count == 1) remaining.remove(id); else remaining.put(id, count - 1);
        return true;
    }

    private static Bucket bucket(String id) {
        if (id == null) return null;
        if (LegacyBiomeSymbolRegistry.containsLegacy(id)) return Bucket.BIOME;
        return switch (id) {
            case "mystcraft:TerrainNormal", "mystcraft:TerrainAmplified", "mystcraft:TerrainNether",
                 "mystcraft:TerrainFlat", "mystcraft:TerrainEnd", "mystcraft:TerrainVoid" -> Bucket.TERRAIN;
            case "mystcraft:BioConNative", "mystcraft:BioConSingle", "mystcraft:BioConTiled",
                 "mystcraft:BioConGrid", "mystcraft:BioConHuge", "mystcraft:BioConLarge",
                 "mystcraft:BioConMedium", "mystcraft:BioConSmall", "mystcraft:BioConTiny" -> Bucket.BIOME_CONTROLLER;
            case "mystcraft:WeatherOn", "mystcraft:WeatherCloudy", "mystcraft:WeatherFast",
                 "mystcraft:WeatherNorm", "mystcraft:WeatherOff", "mystcraft:WeatherRain",
                 "mystcraft:WeatherSlow", "mystcraft:WeatherSnow", "mystcraft:WeatherStorm" -> Bucket.WEATHER;
            case "mystcraft:LightingBright", "mystcraft:LightingDark", "mystcraft:LightingNormal" -> Bucket.LIGHTING;
            case "mystcraft:SunNormal", "mystcraft:SunDark" -> Bucket.SUN;
            case "mystcraft:MoonNormal", "mystcraft:MoonDark" -> Bucket.MOON;
            case "mystcraft:StarsNormal", "mystcraft:StarsTwinkle", "mystcraft:StarsEndSky", "mystcraft:StarsDark" -> Bucket.STARFIELD;
            case "mystcraft:ColorFog", "mystcraft:ColorFogNat" -> Bucket.FOG_COLOR;
            case "mystcraft:ColorSky", "mystcraft:ColorSkyNat" -> Bucket.DAY_SKY_COLOR;
            case "mystcraft:ColorSkyNight" -> Bucket.NIGHT_SKY_COLOR;
            case "mystcraft:ColorCloud", "mystcraft:ColorCloudNat" -> Bucket.CLOUD_COLOR;
            case "mystcraft:ColorGrass", "mystcraft:ColorGrassNat" -> Bucket.GRASS_COLOR;
            case "mystcraft:ColorFoliage", "mystcraft:ColorFoliageNat" -> Bucket.FOLIAGE_COLOR;
            case "mystcraft:ColorWater", "mystcraft:ColorWaterNat" -> Bucket.WATER_COLOR;
            case "mystcraft:ColorHorizon" -> Bucket.HORIZON_COLOR;
            case "mystcraft:NoHorizon" -> Bucket.HORIZON;
            case "mystcraft:Rainbow" -> Bucket.RAINBOW;
            case "mystcraft:StarFissure", "mystcraft:Obelisks", "mystcraft:LakesSurface",
                 "mystcraft:LakesDeep", "mystcraft:CryForm" -> Bucket.SMALL_FEATURE;
            case "mystcraft:NetherFort", "mystcraft:Villages", "mystcraft:Strongholds",
                 "mystcraft:Mineshafts", "mystcraft:Ravines", "mystcraft:TerModSpheres",
                 "mystcraft:Dungeons", "mystcraft:GenSpikes" -> Bucket.MEDIUM_FEATURE;
            case "mystcraft:FloatIslands", "mystcraft:Tendrils", "mystcraft:Skylands",
                 "mystcraft:Caves", "mystcraft:DenseOres", "mystcraft:HugeTrees" -> Bucket.LARGE_FEATURE;
            case "mystcraft:EnvAccel", "mystcraft:EnvExplosions", "mystcraft:EnvLightning",
                 "mystcraft:EnvMeteor", "mystcraft:EnvScorch" -> Bucket.ENV_EFFECT;
            default -> null;
        };
    }
}
