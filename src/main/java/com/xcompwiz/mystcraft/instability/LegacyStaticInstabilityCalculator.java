package com.xcompwiz.mystcraft.instability;

import com.xcompwiz.mystcraft.world.agedata.AgeRecord;
import com.xcompwiz.mystcraft.world.worldgen.AgeSymbolResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Score-only static portion of the legacy Age Instability calculation. */
public final class LegacyStaticInstabilityCalculator {
    private LegacyStaticInstabilityCalculator() {}

    public static StaticBreakdown calculate(AgeRecord age) {
        List<String> effective = AgeSymbolResolver.resolve(age.seed(), age.symbols());
        Map<String,Integer> counts = new HashMap<>();
        int direct = 0;
        for (String id : effective) {
            int count = counts.merge(id, 1, Integer::sum);
            direct += LegacySymbolInstability.instabilityModifier(id, count);
        }
        int extraControllers = extraControllerCost(effective);
        int dangling = LegacyModifierInstabilityReplay.calculate(effective);
        return new StaticBreakdown(age.baseInstability(), direct, extraControllers, dangling, List.copyOf(effective));
    }

    private static int extraControllerCost(List<String> symbols) {
        int biome = 0, terrain = 0, lighting = 0, weather = 0;
        for (String id : symbols) {
            if (isBiomeController(id)) ++biome;
            else if (isTerrainController(id)) ++terrain;
            else if (isLightingController(id)) ++lighting;
            else if (isWeatherController(id)) ++weather;
        }
        return extras(biome) + extras(terrain) + extras(lighting) + extras(weather);
    }

    private static int extras(int count) { return Math.max(0, count - 1) * LegacyInstabilityData.EXTRA_CONTROLLER; }
    private static boolean isBiomeController(String id) { return id.startsWith("mystcraft:BioCon"); }
    private static boolean isTerrainController(String id) { return id.startsWith("mystcraft:Terrain"); }
    private static boolean isLightingController(String id) { return id.startsWith("mystcraft:Lighting"); }
    private static boolean isWeatherController(String id) { return id.startsWith("mystcraft:Weather"); }

    public record StaticBreakdown(
            int baseInstability,
            int directSymbolInstability,
            int extraControllerInstability,
            int danglingModifierInstability,
            List<String> effectiveSymbols) {
        public int rawStaticScore() {
            return baseInstability + directSymbolInstability + extraControllerInstability + danglingModifierInstability;
        }
    }
}
