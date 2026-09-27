package com.xcompwiz.mystcraft.instability;

import com.xcompwiz.mystcraft.symbol.LegacyBiomeSymbolRegistry;
import com.xcompwiz.mystcraft.symbol.LegacyMaterialSymbolDefinition;
import com.xcompwiz.mystcraft.symbol.LegacyMaterialSymbolRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Score-only replay of the legacy modifier stack. It never changes generation or rendering.
 * The purpose is only to reproduce dangling-modifier Instability while the modern resolvers
 * remain the authoritative world-generation implementation.
 */
public final class LegacyModifierInstabilityReplay {
    private LegacyModifierInstabilityReplay() {}

    public static int calculate(List<String> symbols) {
        Map<String, Integer> modifiers = new LinkedHashMap<>();
        List<LegacyMaterialSymbolDefinition> blocks = new ArrayList<>();
        int biomes = 0;
        int instability = 0;

        for (String id : symbols) {
            var material = LegacyMaterialSymbolRegistry.resolveLegacy(id);
            if (material.isPresent()) {
                // Legacy pushBlock adds to the top of the queue.
                blocks.add(0, material.get());
                continue;
            }
            if (LegacyBiomeSymbolRegistry.resolveLegacy(id).isPresent()) {
                ++biomes;
                continue;
            }

            if (isAngle(id)) { consume(modifiers, "angle"); set(modifiers, "angle", 100); continue; }
            if (isPhase(id)) { consume(modifiers, "phase"); set(modifiers, "phase", 100); continue; }
            if (isLength(id)) { consume(modifiers, "factor"); set(modifiers, "factor", 100); continue; }
            if (isColor(id)) { consume(modifiers, "color"); set(modifiers, "color", 100); continue; }
            if ("mystcraft:ColorHorizon".equals(id)) { consume(modifiers, "sunset"); set(modifiers, "sunset", 0); continue; }
            if ("mystcraft:ModGradient".equals(id)) {
                consume(modifiers, "gradient");
                consume(modifiers, "factor");
                consume(modifiers, "color");
                set(modifiers, "gradient", 100);
                continue;
            }
            if ("mystcraft:ModClear".equals(id)) {
                for (int value : modifiers.values()) instability += (int) (value * LegacyInstabilityData.CLEAR_PERCENTAGE);
                instability += (int) (blocks.size() * LegacyInstabilityData.DANGLING_BLOCK * LegacyInstabilityData.CLEAR_PERCENTAGE);
                instability += (int) (biomes * LegacyInstabilityData.DANGLING_BIOME * LegacyInstabilityData.CLEAR_PERCENTAGE);
                modifiers.clear(); blocks.clear(); biomes = 0;
                continue;
            }

            // Consumers of ordinary modifiers.
            switch (id) {
                case "mystcraft:SunNormal", "mystcraft:MoonNormal" -> {
                    consume(modifiers, "factor"); consume(modifiers, "angle"); consume(modifiers, "phase"); consume(modifiers, "sunset");
                }
                case "mystcraft:StarsNormal", "mystcraft:StarsTwinkle" -> {
                    consume(modifiers, "factor"); consume(modifiers, "angle");
                }
                case "mystcraft:Rainbow" -> consume(modifiers, "angle");
                case "mystcraft:ColorCloud", "mystcraft:ColorFog", "mystcraft:ColorFoliage", "mystcraft:ColorGrass", "mystcraft:ColorSky", "mystcraft:ColorSkyNight", "mystcraft:ColorWater" -> consume(modifiers, "color");
                default -> { }
            }

            // Legacy material/biome queues. Only dangling cost matters here.
            switch (id) {
                case "mystcraft:TerrainNormal", "mystcraft:TerrainAmplified", "mystcraft:TerrainFlat", "mystcraft:TerrainNether", "mystcraft:TerrainEnd" -> {
                    popBlock(blocks, "mystcraft:BlockSea");
                    popBlock(blocks, "mystcraft:BlockTerrain");
                }
                case "mystcraft:LakesDeep" -> popBlock(blocks, "mystcraft:BlockFluid", "mystcraft:BlockGas");
                case "mystcraft:LakesSurface" -> popBlock(blocks, "mystcraft:BlockFluid");
                case "mystcraft:Tendrils", "mystcraft:GenSpikes", "mystcraft:Obelisks", "mystcraft:TerModSpheres" -> popBlock(blocks, "mystcraft:BlockStructure");
                case "mystcraft:CryForm" -> popBlock(blocks, "mystcraft:BlockCrystal");
                case "mystcraft:FloatIslands" -> { popBlock(blocks, "mystcraft:BlockStructure"); if (biomes > 0) --biomes; }
                case "mystcraft:BioConSingle" -> { if (biomes > 0) --biomes; }
                case "mystcraft:BioConGrid", "mystcraft:BioConTiled", "mystcraft:BioConHuge", "mystcraft:BioConLarge", "mystcraft:BioConMedium", "mystcraft:BioConSmall", "mystcraft:BioConTiny" -> biomes = 0;
                default -> { }
            }
        }

        for (int value : modifiers.values()) instability += value;
        instability += blocks.size() * LegacyInstabilityData.DANGLING_BLOCK;
        instability += biomes * LegacyInstabilityData.DANGLING_BIOME;
        return instability;
    }

    private static void set(Map<String,Integer> map, String key, int dangling) {
        Integer previous = map.put(key, dangling);
        // setModifier adds the overwritten modifier's dangling cost. The combining modifier
        // symbols pop their previous value first, so this mainly protects future/custom symbols.
        if (previous != null) throw new IllegalStateException("internal replay overwrite without consume: " + key);
    }

    private static void consume(Map<String,Integer> map, String key) { map.remove(key); }

    private static void popBlock(List<LegacyMaterialSymbolDefinition> blocks, String... categories) {
        for (int i = 0; i < blocks.size(); ++i) {
            var block = blocks.get(i);
            for (String category : categories) {
                if (block.usableAs(category)) { blocks.remove(i); return; }
            }
        }
    }

    private static boolean isAngle(String id) { return id.equals("mystcraft:ModNorth") || id.equals("mystcraft:ModEast") || id.equals("mystcraft:ModSouth") || id.equals("mystcraft:ModWest"); }
    private static boolean isPhase(String id) { return id.equals("mystcraft:ModEnd") || id.equals("mystcraft:ModRising") || id.equals("mystcraft:ModNoon") || id.equals("mystcraft:ModSetting"); }
    private static boolean isLength(String id) { return id.equals("mystcraft:ModZero") || id.equals("mystcraft:ModHalf") || id.equals("mystcraft:ModFull") || id.equals("mystcraft:ModDouble"); }
    private static boolean isColor(String id) { return id.startsWith("mystcraft:ModColor"); }
}
