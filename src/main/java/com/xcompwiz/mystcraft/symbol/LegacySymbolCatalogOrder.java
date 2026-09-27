package com.xcompwiz.mystcraft.symbol;

import java.util.Set;

/**
 * Exclusive Writing Desk catalogue groups ordered for assembling a complete,
 * grammar-stable Age.  This is a browse order only: it never changes persisted
 * page order or Grammar resolution.
 *
 * <p>Every Symbol resolves to exactly one group.  In particular, the three
 * Feature Dummy pages live inside their real Small/Medium/Large Feature groups;
 * they are not a separate test-only category.</p>
 */
public final class LegacySymbolCatalogOrder {
    private static final Set<String> SUN = Set.of("mystcraft:SunNormal", "mystcraft:SunDark");
    private static final Set<String> MOON = Set.of("mystcraft:MoonNormal", "mystcraft:MoonDark");
    private static final Set<String> STARS = Set.of("mystcraft:StarsNormal", "mystcraft:StarsTwinkle", "mystcraft:StarsEndSky", "mystcraft:StarsDark");
    private static final Set<String> VISUAL = Set.of(
            "mystcraft:ColorCloud", "mystcraft:ColorCloudNat", "mystcraft:ColorFog", "mystcraft:ColorFogNat",
            "mystcraft:ColorFoliage", "mystcraft:ColorFoliageNat", "mystcraft:ColorGrass", "mystcraft:ColorGrassNat",
            "mystcraft:ColorSky", "mystcraft:ColorSkyNat", "mystcraft:ColorSkyNight", "mystcraft:ColorWater", "mystcraft:ColorWaterNat",
            "mystcraft:Rainbow", "mystcraft:NoHorizon");
    private static final Set<String> FEATURE_SMALL = Set.of(
            "mystcraft:StarFissure", "mystcraft:Obelisks", "mystcraft:LakesSurface", "mystcraft:LakesDeep",
            "mystcraft:CryForm", "mystcraft:FeatureSmallDummy");
    private static final Set<String> FEATURE_MEDIUM = Set.of(
            "mystcraft:NetherFort", "mystcraft:Villages", "mystcraft:Strongholds", "mystcraft:Mineshafts",
            "mystcraft:Ravines", "mystcraft:TerModSpheres", "mystcraft:Dungeons", "mystcraft:GenSpikes",
            "mystcraft:FeatureMediumDummy");
    private static final Set<String> FEATURE_LARGE = Set.of(
            "mystcraft:FloatIslands", "mystcraft:Tendrils", "mystcraft:Skylands", "mystcraft:Caves",
            "mystcraft:DenseOres", "mystcraft:HugeTrees", "mystcraft:FeatureLargeDummy");

    // The numeric order follows the complete Age grammar at a human-useful level:
    // materials -> terrain -> biome -> controllers -> weather/lighting -> modifiers
    // -> sky branches -> visual -> Small/Medium/Large features -> effects.
    public static final int TERRAIN_MATERIAL = 0;
    public static final int SEA_MATERIAL = 1;
    public static final int AUX_MATERIAL = 2;
    public static final int TERRAIN = 3;
    public static final int BIOME = 4;
    public static final int BIOME_CONTROLLER = 5;
    public static final int WEATHER = 6;
    public static final int LIGHTING = 7;
    public static final int MODIFIER = 8;
    public static final int SUN_GROUP = 9;
    public static final int MOON_GROUP = 10;
    public static final int STARS_GROUP = 11;
    public static final int VISUAL_GROUP = 12;
    public static final int FEATURE_SMALL_GROUP = 13;
    public static final int FEATURE_MEDIUM_GROUP = 14;
    public static final int FEATURE_LARGE_GROUP = 15;
    public static final int EFFECT = 16;
    public static final int OTHER = 17;

    private LegacySymbolCatalogOrder() {}

    public static int group(String legacyId) {
        String id = LegacySymbolId.qualify(legacyId);

        var material = LegacyMaterialSymbolRegistry.resolveLegacy(id);
        if (material.isPresent()) {
            LegacyMaterialSymbolDefinition definition = material.get();
            if (definition.usableAs("mystcraft:BlockTerrain")) return TERRAIN_MATERIAL;
            if (definition.usableAs("mystcraft:BlockSea")) return SEA_MATERIAL;
            return AUX_MATERIAL;
        }
        if ("mystcraft:NoSea".equals(id)) return SEA_MATERIAL;
        if (LegacyBiomeSymbolRegistry.containsLegacy(id)) return BIOME;

        var staticSymbol = SymbolRegistry.resolveLegacy(id);
        if (staticSymbol.isEmpty()) return OTHER;

        if (SUN.contains(id)) return SUN_GROUP;
        if (MOON.contains(id)) return MOON_GROUP;
        if (STARS.contains(id)) return STARS_GROUP;
        if (VISUAL.contains(id)) return VISUAL_GROUP;
        if (FEATURE_SMALL.contains(id)) return FEATURE_SMALL_GROUP;
        if (FEATURE_MEDIUM.contains(id)) return FEATURE_MEDIUM_GROUP;
        if (FEATURE_LARGE.contains(id)) return FEATURE_LARGE_GROUP;

        return switch (staticSymbol.get().category()) {
            case TERRAIN -> TERRAIN;
            case BIOME -> BIOME;
            case BIOME_CONTROLLER -> BIOME_CONTROLLER;
            case WEATHER -> WEATHER;
            case LIGHTING -> LIGHTING;
            case MODIFIER -> MODIFIER;
            case CELESTIAL, COLOR -> VISUAL_GROUP;
            case ENVIRONMENT -> EFFECT;
            case FEATURE, STRUCTURE -> FEATURE_MEDIUM_GROUP; // fallback only; fixed legacy features are caught above.
            default -> OTHER;
        };
    }
}
