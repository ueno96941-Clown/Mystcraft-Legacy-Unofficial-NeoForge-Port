package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.AgeLocalGenerationPlan;
import com.xcompwiz.mystcraft.world.worldgen.AgeTerrainMode;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import net.neoforged.neoforge.common.util.Lazy;

/**
 * Installs an Age-local BiomeGenerationSettings getter into an already-created
 * vanilla ChunkGenerator.
 *
 * <p>Minecraft 1.21.1's {@link ChunkGenerator} stores the generation-settings getter
 * independently of the terrain generator implementation. NeoForge's access transformer
 * widens that field for this one runtime replacement. After replacement, the feature-sort
 * cache is rebuilt through {@link ChunkGenerator#refreshFeaturesPerStep()}.</p>
 *
 * <p>No Biome object or global registry entry is mutated.</p>
 */
public final class AgeChunkGeneratorGenerationSettingsInstaller {
    private AgeChunkGeneratorGenerationSettingsInstaller() {}

    public static void install(
            RegistryAccess registries,
            ChunkGenerator generator,
            AgeLocalGenerationPlan plan,
            com.xcompwiz.mystcraft.world.worldgen.AgeFeaturePlan featurePlan,
            long ageSeed,
            AgeTerrainMode terrainMode,
            String terrainBlockId) {

        Objects.requireNonNull(registries, "registries");
        Objects.requireNonNull(generator, "generator");
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(featurePlan, "featurePlan");

        // Always install the overlay. Even an Age with no Caves/Ravines/Tendrils
        // must not inherit source-biome carvers that were absent from the authored Age.

        // Direct holders must be shared across every biome overlay in this Age.
        // ChunkGenerator merges the feature lists of all biomes touching a generation region;
        // constructing fresh direct PlacedFeature objects per biome would make one logical
        // Mystcraft populator appear multiple times and execute once for each neighboring biome.
        List<Holder<PlacedFeature>> legacyPopulationFeatures =
                AgeLegacyPopulationPlacedFeatures.create(featurePlan);

        final Holder<PlacedFeature> legacyFlatSurfaceFeature;
        if (terrainMode == AgeTerrainMode.FLAT && "minecraft:stone".equals(terrainBlockId)) {
            var configured = new net.minecraft.world.level.levelgen.feature.ConfiguredFeature<>(
                    new AgeLegacyFlatBiomeSurfaceFeature(),
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE);
            legacyFlatSurfaceFeature = Holder.direct(new PlacedFeature(Holder.direct(configured), List.of()));
        } else {
            legacyFlatSurfaceFeature = null;
        }

        Map<Holder<Biome>, BiomeGenerationSettings> cache = new IdentityHashMap<>();

        Function<Holder<Biome>, BiomeGenerationSettings> getter = biome ->
                cache.computeIfAbsent(
                        biome,
                        holder -> AgeBiomeGenerationSettingsOverlay.apply(
                                registries,
                                holder.value().getGenerationSettings(),
                                plan,
                                featurePlan,
                                ageSeed,
                                terrainMode,
                                legacyPopulationFeatures,
                                legacyFlatSurfaceFeature));

        // ChunkGenerator's vanilla featuresPerStep Lazy captures the generation-settings
        // function passed to the constructor. Merely replacing generationSettingsGetter and
        // invalidating that Lazy therefore rebuilds the sorter from the *old* biome settings.
        // applyBiomeDecoration(), however, reads the new getter. That split-brain state makes
        // newly injected direct features resolve to index -1 and crashes biome decoration.
        // Rebuild the Lazy itself from the same Age-local getter so the sorter and runtime
        // settings are guaranteed to describe the identical feature graph.
        generator.generationSettingsGetter = getter;
        generator.featuresPerStep = Lazy.of(() -> FeatureSorter.buildFeaturesPerStep(
                new ArrayList<>(generator.getBiomeSource().possibleBiomes()),
                holder -> getter.apply(holder).features(),
                true));
        generator.validate();
    }
}
