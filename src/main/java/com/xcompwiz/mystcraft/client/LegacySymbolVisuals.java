package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.symbol.LegacyBiomeSymbolRegistry;
import com.xcompwiz.mystcraft.symbol.LegacyMaterialSymbolRegistry;
import com.xcompwiz.mystcraft.symbol.LegacySymbolId;

import java.util.LinkedHashMap;
import java.util.Map;

/** Exact 0.13.7.06 fixed-symbol poems plus the legacy dynamic poem rules used for page rendering. */
public final class LegacySymbolVisuals {
    private static final Map<String, String[]> FIXED = new LinkedHashMap<>();
    static {
        FIXED.put("mystcraft:ColorCloud", new String[]{"Image", "Entropy", "Believe", "Weave"});
        FIXED.put("mystcraft:ColorCloudNat", new String[]{"Image", "Entropy", "Believe", "Nature"});
        FIXED.put("mystcraft:ColorFog", new String[]{"Image", "Entropy", "Explore", "Weave"});
        FIXED.put("mystcraft:ColorFogNat", new String[]{"Image", "Entropy", "Explore", "Nature"});
        FIXED.put("mystcraft:ColorFoliage", new String[]{"Image", "Growth", "Elevate", "Weave"});
        FIXED.put("mystcraft:ColorFoliageNat", new String[]{"Image", "Growth", "Elevate", "Nature"});
        FIXED.put("mystcraft:ColorGrass", new String[]{"Image", "Growth", "Resilience", "Weave"});
        FIXED.put("mystcraft:ColorGrassNat", new String[]{"Image", "Growth", "Resilience", "Nature"});
        FIXED.put("mystcraft:ColorSky", new String[]{"Image", "Celestial", "Harmony", "Weave"});
        FIXED.put("mystcraft:ColorSkyNat", new String[]{"Image", "Celestial", "Harmony", "Nature"});
        FIXED.put("mystcraft:ColorSkyNight", new String[]{"Image", "Celestial", "Contradict", "Weave"});
        FIXED.put("mystcraft:ColorWater", new String[]{"Image", "Flow", "Constraint", "Weave"});
        FIXED.put("mystcraft:ColorWaterNat", new String[]{"Image", "Flow", "Constraint", "Nature"});
        FIXED.put("mystcraft:Rainbow", new String[]{"Celestial", "Image", "Harmony", "Balance"});
        FIXED.put("mystcraft:NoHorizon", new String[]{"Celestial", "Inhibit", "Image", "Void"});
        FIXED.put("mystcraft:MoonDark", new String[]{"Celestial", "Void", "Inhibit", "Wisdom"});
        FIXED.put("mystcraft:MoonNormal", new String[]{"Celestial", "Image", "Cycle", "Wisdom"});
        FIXED.put("mystcraft:StarsDark", new String[]{"Celestial", "Void", "Inhibit", "Order"});
        FIXED.put("mystcraft:StarsEndSky", new String[]{"Celestial", "Image", "Chaos", "Weave"});
        FIXED.put("mystcraft:StarsNormal", new String[]{"Celestial", "Harmony", "Ethereal", "Order"});
        FIXED.put("mystcraft:StarsTwinkle", new String[]{"Celestial", "Harmony", "Ethereal", "Entropy"});
        FIXED.put("mystcraft:SunDark", new String[]{"Celestial", "Void", "Inhibit", "Energy"});
        FIXED.put("mystcraft:SunNormal", new String[]{"Celestial", "Image", "Stimulate", "Energy"});
        FIXED.put("mystcraft:BioConGrid", new String[]{"Constraint", "Nature", "Chain", "Mutual"});
        FIXED.put("mystcraft:BioConNative", new String[]{"Constraint", "Nature", "Tradition", "Sustain"});
        FIXED.put("mystcraft:BioConSingle", new String[]{"Constraint", "Nature", "Infinite", "Static"});
        FIXED.put("mystcraft:BioConTiled", new String[]{"Constraint", "Nature", "Chain", "Contradict"});
        FIXED.put("mystcraft:BioConHuge", new String[]{"Constraint", "Nature", "Weave", "Huge"});
        FIXED.put("mystcraft:BioConLarge", new String[]{"Constraint", "Nature", "Weave", "Large"});
        FIXED.put("mystcraft:BioConMedium", new String[]{"Constraint", "Nature", "Weave", "Medium"});
        FIXED.put("mystcraft:BioConSmall", new String[]{"Constraint", "Nature", "Weave", "Small"});
        FIXED.put("mystcraft:BioConTiny", new String[]{"Constraint", "Nature", "Weave", "Tiny"});
        FIXED.put("mystcraft:NoSea", new String[]{"Transform", "Constraint", "Flow", "Inhibit"});
        FIXED.put("mystcraft:PvPOff", new String[]{"Chain", "Chaos", "Encourage", "Harmony"});
        FIXED.put("mystcraft:EnvAccel", new String[]{"Survival", "Dynamic", "Change", "Spur"});
        FIXED.put("mystcraft:EnvExplosions", new String[]{"Survival", "Sacrifice", "Power", "Force"});
        FIXED.put("mystcraft:EnvLightning", new String[]{"Survival", "Sacrifice", "Power", "Energy"});
        FIXED.put("mystcraft:EnvMeteor", new String[]{"Survival", "Sacrifice", "Power", "Momentum"});
        FIXED.put("mystcraft:EnvScorch", new String[]{"Survival", "Sacrifice", "Power", "Chaos"});
        FIXED.put("mystcraft:LightingBright", new String[]{"Ethereal", "Power", "Infinite", "Spur"});
        FIXED.put("mystcraft:LightingDark", new String[]{"Ethereal", "Void", "Constraint", "Inhibit"});
        FIXED.put("mystcraft:LightingNormal", new String[]{"Ethereal", "Dynamic", "Cycle", "Balance"});
        FIXED.put("mystcraft:ModNorth", new String[]{"Transform", "Flow", "Motion", "Control"});
        FIXED.put("mystcraft:ModEast", new String[]{"Transform", "Flow", "Motion", "Tradition"});
        FIXED.put("mystcraft:ModSouth", new String[]{"Transform", "Flow", "Motion", "Chaos"});
        FIXED.put("mystcraft:ModWest", new String[]{"Transform", "Flow", "Motion", "Change"});
        FIXED.put("mystcraft:ModClear", new String[]{"Contradict", "Transform", "Change", "Void"});
        FIXED.put("mystcraft:ModGradient", new String[]{"Transform", "Image", "Merge", "Weave"});
        FIXED.put("mystcraft:ColorHorizon", new String[]{"Transform", "Image", "Celestial", "Change"});
        FIXED.put("mystcraft:ModZero", new String[]{"Transform", "Time", "System", "Inhibit"});
        FIXED.put("mystcraft:ModHalf", new String[]{"Transform", "Time", "System", "Stimulate"});
        FIXED.put("mystcraft:ModFull", new String[]{"Transform", "Time", "System", "Balance"});
        FIXED.put("mystcraft:ModDouble", new String[]{"Transform", "Time", "System", "Sacrifice"});
        FIXED.put("mystcraft:ModEnd", new String[]{"Transform", "Cycle", "System", "Rebirth"});
        FIXED.put("mystcraft:ModRising", new String[]{"Transform", "Cycle", "System", "Growth"});
        FIXED.put("mystcraft:ModNoon", new String[]{"Transform", "Cycle", "System", "Harmony"});
        FIXED.put("mystcraft:ModSetting", new String[]{"Transform", "Cycle", "System", "Future"});
        FIXED.put("mystcraft:Caves", new String[]{"Terrain", "Transform", "Void", "Flow"});
        FIXED.put("mystcraft:Dungeons", new String[]{"Civilization", "Constraint", "Chain", "Resurrect"});
        FIXED.put("mystcraft:FloatIslands", new String[]{"Terrain", "Transform", "Form", "Celestial"});
        FIXED.put("mystcraft:FeatureLargeDummy", new String[]{"Contradict", "Chaos", "Exist", "Terrain"});
        FIXED.put("mystcraft:FeatureMediumDummy", new String[]{"Contradict", "Chaos", "Exist", "Balance"});
        FIXED.put("mystcraft:FeatureSmallDummy", new String[]{"Contradict", "Chaos", "Exist", "Form"});
        FIXED.put("mystcraft:HugeTrees", new String[]{"Nature", "Stimulate", "Spur", "Elevate"});
        FIXED.put("mystcraft:LakesDeep", new String[]{"Nature", "Flow", "Static", "Explore"});
        FIXED.put("mystcraft:LakesSurface", new String[]{"Nature", "Flow", "Static", "Elevate"});
        FIXED.put("mystcraft:Mineshafts", new String[]{"Civilization", "Machine", "Motion", "Tradition"});
        FIXED.put("mystcraft:NetherFort", new String[]{"Civilization", "Machine", "Power", "Entropy"});
        FIXED.put("mystcraft:Obelisks", new String[]{"Civilization", "Resilience", "Static", "Form"});
        FIXED.put("mystcraft:Ravines", new String[]{"Terrain", "Transform", "Void", "Weave"});
        FIXED.put("mystcraft:TerModSpheres", new String[]{"Terrain", "Transform", "Form", "Cycle"});
        FIXED.put("mystcraft:GenSpikes", new String[]{"Nature", "Encourage", "Entropy", "Static"});
        FIXED.put("mystcraft:Strongholds", new String[]{"Civilization", "Wisdom", "Future", "Honor"});
        FIXED.put("mystcraft:Tendrils", new String[]{"Terrain", "Transform", "Growth", "Flow"});
        FIXED.put("mystcraft:Villages", new String[]{"Civilization", "Society", "Harmony", "Nurture"});
        FIXED.put("mystcraft:CryForm", new String[]{"Nature", "Encourage", "Growth", "Static"});
        FIXED.put("mystcraft:Skylands", new String[]{"Terrain", "Transform", "Void", "Elevate"});
        FIXED.put("mystcraft:StarFissure", new String[]{"Nature", "Harmony", "Mutual", "Void"});
        FIXED.put("mystcraft:DenseOres", new String[]{"Survival", "Stimulate", "Machine", "Chaos"});
        FIXED.put("mystcraft:WeatherOn", new String[]{"Sustain", "Static", "Tradition", "Stimulate"});
        FIXED.put("mystcraft:WeatherCloudy", new String[]{"Sustain", "Static", "Believe", "Motion"});
        FIXED.put("mystcraft:WeatherFast", new String[]{"Sustain", "Dynamic", "Tradition", "Spur"});
        FIXED.put("mystcraft:WeatherNorm", new String[]{"Sustain", "Dynamic", "Tradition", "Balance"});
        FIXED.put("mystcraft:WeatherOff", new String[]{"Sustain", "Static", "Stimulate", "Energy"});
        FIXED.put("mystcraft:WeatherRain", new String[]{"Sustain", "Static", "Rebirth", "Growth"});
        FIXED.put("mystcraft:WeatherSlow", new String[]{"Sustain", "Dynamic", "Tradition", "Inhibit"});
        FIXED.put("mystcraft:WeatherSnow", new String[]{"Sustain", "Static", "Inhibit", "Energy"});
        FIXED.put("mystcraft:WeatherStorm", new String[]{"Sustain", "Static", "Nature", "Power"});
        FIXED.put("mystcraft:TerrainAmplified", new String[]{"Terrain", "Form", "Tradition", "Spur"});
        FIXED.put("mystcraft:TerrainEnd", new String[]{"Terrain", "Form", "Ethereal", "Flow"});
        FIXED.put("mystcraft:TerrainFlat", new String[]{"Terrain", "Form", "Inhibit", "Motion"});
        FIXED.put("mystcraft:TerrainNether", new String[]{"Terrain", "Form", "Constraint", "Entropy"});
        FIXED.put("mystcraft:TerrainNormal", new String[]{"Terrain", "Form", "Tradition", "Flow"});
        FIXED.put("mystcraft:TerrainVoid", new String[]{"Terrain", "Form", "Infinite", "Void"});
    }

    private LegacySymbolVisuals() {}

    public static String[] poem(String legacyId) {
        String id = LegacySymbolId.qualify(legacyId);
        String[] fixed = FIXED.get(id);
        if (fixed != null) return fixed.clone();
        String path = path(id);
        if (path.startsWith("ModColor")) return new String[]{"Transform", "Image", "Weave", path};
        var material = LegacyMaterialSymbolRegistry.resolveLegacy(id);
        if (material.isPresent()) {
            return new String[]{"Transform", "Constraint", material.get().legacyWord(), path};
        }
        var biome = LegacyBiomeSymbolRegistry.resolveLegacy(id);
        if (biome.isPresent()) {
            var def = biome.get();
            String biomeWord = LegacyBiomeSymbolRegistry.legacyVisualWord(def);
            return new String[]{"Nature", "Nurture", "Encourage", biomeWord};
        }
        return new String[]{"Question", "Question", "Question", path};
    }

    private static String path(String id) {
        int split = id.indexOf(':');
        return split >= 0 && split + 1 < id.length() ? id.substring(split + 1) : id;
    }
}
