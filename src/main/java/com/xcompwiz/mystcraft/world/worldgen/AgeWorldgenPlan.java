package com.xcompwiz.mystcraft.world.worldgen;

import com.xcompwiz.mystcraft.symbol.SymbolAnalysis;
import com.xcompwiz.mystcraft.symbol.SymbolCompletionPlan;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import java.util.List;
import java.util.Objects;

/**
 * Immutable, registry-safe description of the world-generation choices for one Age.
 *
 * <p>{@code requestedSymbols} is the persisted/authored legacy sequence. The
 * Grammar-completed {@code effectiveSymbols} sequence is kept separately so later
 * world-effect stages never need to mutate or canonicalize the compatibility data.</p>
 */
public record AgeWorldgenPlan(
        ResourceKey<Biome> fixedBiome,
        ResourceKey<NoiseGeneratorSettings> noiseSettings,
        List<String> requestedSymbols,
        List<ResourceLocation> resolvedSymbolKeys,
        List<String> unresolvedSymbols,
        SymbolAnalysis symbolAnalysis,
        SymbolCompletionPlan symbolCompletionPlan,
        List<String> effectiveSymbols,
        SymbolAnalysis effectiveSymbolAnalysis,
        AgeTerrainMode terrainMode,
        String terrainBlockId,
        String seaBlockId,
        int averageGroundLevel,
        int seaLevel,
        AgeBiomeControllerPlan biomeControllerPlan,
        AgeResolvedBiomePlan resolvedBiomePlan,
        AgeBiomeSourceSelection biomeSourceSelection,
        AgeWeatherPlan weatherPlan,
        AgeEnvironmentPlan environmentPlan,
        AgeLightingPlan lightingPlan,
        AgeSkyPlan skyPlan,
        AgeColorPlan colorPlan,
        AgeFeaturePlan featurePlan,
        AgeVanillaWorldgenBridgePlan vanillaWorldgenBridgePlan,
        AgeLocalGenerationPlan localGenerationPlan,
        Resolution resolution) {

    public enum Resolution {
        /** No Symbol-driven terrain decision has been made yet. */
        BASELINE,
        /** Every requested legacy ID is known to the restored Symbol registry. */
        SYMBOL_REGISTRY_RESOLVED,
        /** At least one requested symbol is not registered. */
        PARTIALLY_RESOLVED,
        /** Required legacy Age root branches are still waiting for Grammar expansion. */
        GRAMMAR_COMPLETION_REQUIRED,
        /** Grammar has produced the completed terminal sequence for later world effects. */
        SYMBOL_DRIVEN
    }

    public AgeWorldgenPlan {
        Objects.requireNonNull(fixedBiome, "fixedBiome");
        Objects.requireNonNull(noiseSettings, "noiseSettings");
        Objects.requireNonNull(requestedSymbols, "requestedSymbols");
        Objects.requireNonNull(resolvedSymbolKeys, "resolvedSymbolKeys");
        Objects.requireNonNull(unresolvedSymbols, "unresolvedSymbols");
        Objects.requireNonNull(symbolAnalysis, "symbolAnalysis");
        Objects.requireNonNull(symbolCompletionPlan, "symbolCompletionPlan");
        Objects.requireNonNull(effectiveSymbols, "effectiveSymbols");
        Objects.requireNonNull(effectiveSymbolAnalysis, "effectiveSymbolAnalysis");
        Objects.requireNonNull(terrainMode, "terrainMode");
        Objects.requireNonNull(terrainBlockId, "terrainBlockId");
        Objects.requireNonNull(seaBlockId, "seaBlockId");
        Objects.requireNonNull(biomeControllerPlan, "biomeControllerPlan");
        Objects.requireNonNull(resolvedBiomePlan, "resolvedBiomePlan");
        Objects.requireNonNull(biomeSourceSelection, "biomeSourceSelection");
        Objects.requireNonNull(weatherPlan, "weatherPlan");
        Objects.requireNonNull(environmentPlan, "environmentPlan");
        Objects.requireNonNull(lightingPlan, "lightingPlan");
        Objects.requireNonNull(skyPlan, "skyPlan");
        Objects.requireNonNull(colorPlan, "colorPlan");
        Objects.requireNonNull(featurePlan, "featurePlan");
        Objects.requireNonNull(vanillaWorldgenBridgePlan, "vanillaWorldgenBridgePlan");
        Objects.requireNonNull(localGenerationPlan, "localGenerationPlan");
        Objects.requireNonNull(resolution, "resolution");
        requestedSymbols = List.copyOf(requestedSymbols);
        resolvedSymbolKeys = List.copyOf(resolvedSymbolKeys);
        unresolvedSymbols = List.copyOf(unresolvedSymbols);
        effectiveSymbols = List.copyOf(effectiveSymbols);
    }
}
