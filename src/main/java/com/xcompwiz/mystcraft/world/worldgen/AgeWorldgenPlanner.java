package com.xcompwiz.mystcraft.world.worldgen;

import com.xcompwiz.mystcraft.symbol.SymbolAnalysis;
import com.xcompwiz.mystcraft.symbol.SymbolAnalyzer;
import com.xcompwiz.mystcraft.symbol.SymbolCompletionPlan;
import com.xcompwiz.mystcraft.symbol.SymbolCompletionPlanner;
import com.xcompwiz.mystcraft.symbol.RequiredSymbolSlot;
import com.xcompwiz.mystcraft.world.agedata.AgeRecord;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import java.util.List;
import java.util.Objects;

/** Converts a persisted Age description into the modern worldgen contract. */
public final class AgeWorldgenPlanner {
    private AgeWorldgenPlanner() {}

    public static AgeWorldgenPlan plan(AgeRecord age) {
        Objects.requireNonNull(age, "age");

        List<String> requestedSymbols = age.symbols();
        SymbolAnalysis requestedAnalysis = SymbolAnalyzer.analyze(requestedSymbols);
        SymbolCompletionPlan completionPlan = SymbolCompletionPlanner.plan(age.seed(), requestedAnalysis);

        // Legacy AgeData#getSymbols(false): authored symbols are parsed and then the
        // grammar fills every remaining Age branch from the deterministic Age seed.
        List<String> effectiveSymbols = AgeSymbolResolver.resolve(age.seed(), requestedSymbols);
        SymbolAnalysis effectiveAnalysis = SymbolAnalyzer.analyze(effectiveSymbols);
        validateMandatoryControllers(effectiveAnalysis);

        AgeTerrainMode terrainMode = resolveTerrainMode(effectiveSymbols);
        AgeTerrainMaterialSelection terrainMaterials =
                AgeTerrainMaterialResolver.resolve(terrainMode, effectiveSymbols);
        AgeTerrainLevels terrainLevels = AgeTerrainLevelResolver.resolve(effectiveSymbols);
        int biomeAdjustedGroundLevel = AgeBiomeGroundLevelResolver.resolve(
                effectiveSymbols, terrainLevels.averageGroundLevel());
        AgeBiomeControllerPlan biomeControllerPlan =
                AgeBiomeControllerResolver.resolve(effectiveSymbols);
        AgeResolvedBiomePlan resolvedBiomePlan = AgeBiomeResolver.resolve(age.seed(), biomeControllerPlan);
        AgeBiomeSourceSelection biomeSourceSelection =
                AgeBiomeSourceSelection.from(resolvedBiomePlan, age.seed());
        AgeWeatherPlan weatherPlan = AgeWeatherResolver.resolve(effectiveSymbols);
        AgeEnvironmentPlan environmentPlan = AgeEnvironmentResolver.resolve(effectiveSymbols);
        AgeLightingPlan lightingPlan = AgeLightingResolver.resolve(effectiveSymbols);
        AgeSkyPlan skyPlan = AgeSkyResolver.resolve(age.seed(), effectiveSymbols);
        AgeColorPlan colorPlan = AgeColorResolver.resolve(requestedSymbols, effectiveSymbols);
        AgeFeaturePlan featurePlan = AgeFeatureResolver.resolve(age.seed(), effectiveSymbols);
        AgeVanillaWorldgenBridgePlan vanillaWorldgenBridgePlan =
                AgeVanillaWorldgenBridgeResolver.resolve(featurePlan);
        AgeLocalGenerationPlan localGenerationPlan =
                AgeLocalGenerationPlanResolver.resolve(vanillaWorldgenBridgePlan);

        AgeWorldgenPlan.Resolution resolution = effectiveAnalysis.fullyKnown()
                ? AgeWorldgenPlan.Resolution.SYMBOL_DRIVEN
                : AgeWorldgenPlan.Resolution.PARTIALLY_RESOLVED;

        return new AgeWorldgenPlan(
                Biomes.PLAINS,
                NoiseGeneratorSettings.OVERWORLD,
                requestedSymbols,
                effectiveAnalysis.resolvedKeysInOrder(),
                effectiveAnalysis.unknownLegacyIdsInOrder(),
                requestedAnalysis,
                completionPlan,
                effectiveSymbols,
                effectiveAnalysis,
                terrainMode,
                terrainMaterials.terrainBlockId(),
                terrainMaterials.seaBlockId(),
                biomeAdjustedGroundLevel,
                terrainLevels.seaLevel(),
                biomeControllerPlan,
                resolvedBiomePlan,
                biomeSourceSelection,
                weatherPlan,
                environmentPlan,
                lightingPlan,
                skyPlan,
                colorPlan,
                featurePlan,
                vanillaWorldgenBridgePlan,
                localGenerationPlan,
                resolution);
    }

    /**
     * Legacy GrammarRules.ROOT always supplies Terrain, BiomeController, Weather and Lighting.
     *
     * <p>Legacy AgeController had a second defensive fallback which independently selected a
     * provider for each missing interface using a fresh {@code new Random(ageSeed)} in the order
     * Biome -> Terrain -> Lighting -> Weather. With the restored 0.13.7.06 root grammar, a normal
     * completed Age cannot reach that fallback. Treat a missing mandatory controller here as a
     * grammar/registry invariant failure rather than silently inventing a second modern fallback
     * stream and shifting compatibility behavior.</p>
     */
    static void validateMandatoryControllers(SymbolAnalysis analysis) {
        for (RequiredSymbolSlot slot : RequiredSymbolSlot.values()) {
            if (!analysis.hasCategory(slot.category())) {
                throw new IllegalStateException(
                        "Legacy grammar failed to provide mandatory Age controller: "
                                + slot.grammarToken());
            }
        }
    }

    /**
     * Resolves legacy terrain-generator Symbols in authored/effective order; the last controller wins.
     *
     * <p>13D-3 recognizes all fixed legacy terrain-generator Symbols restored by the
     * static Symbol registry. Legacy AgeController.registerInterface(ITerrainGenerator) replaced earlier controllers, so the last terrain Symbol wins.</p>
     */
    static AgeTerrainMode resolveTerrainMode(List<String> effectiveSymbols) {
        AgeTerrainMode selected = AgeTerrainMode.UNKNOWN;
        for (String symbol : effectiveSymbols) {
            if ("mystcraft:TerrainNormal".equals(symbol)) selected = AgeTerrainMode.NORMAL;
            else if ("mystcraft:TerrainAmplified".equals(symbol)) selected = AgeTerrainMode.AMPLIFIED;
            else if ("mystcraft:TerrainNether".equals(symbol)) selected = AgeTerrainMode.NETHER;
            else if ("mystcraft:TerrainEnd".equals(symbol)) selected = AgeTerrainMode.END;
            else if ("mystcraft:TerrainFlat".equals(symbol)) selected = AgeTerrainMode.FLAT;
            else if ("mystcraft:TerrainVoid".equals(symbol)) selected = AgeTerrainMode.VOID;
        }
        return selected;
    }
}
