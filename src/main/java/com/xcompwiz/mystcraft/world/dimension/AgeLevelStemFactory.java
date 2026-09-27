package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.AgeWorldgenPlan;
import com.xcompwiz.mystcraft.world.worldgen.LegacyModernBiomeIdResolver;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Builds the modern LevelStem representation for a reserved Mystcraft Age. */
public final class AgeLevelStemFactory {
    private AgeLevelStemFactory() {}

    /**
     * Creates a complete LevelStem from the resolved worldgen plan for the reserved Age.
     *
     * <p>All restored biome-controller families are connected: Native, Single, Grid, Tiled,
     * and the Huge/Large/Medium/Small/Tiny legacy zoom family.</p>
     */
    public static LevelStem create(RegistryAccess registries, AgeDimensionDefinition definition, AgeWorldgenPlan plan) {
        Objects.requireNonNull(registries, "registries");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(plan, "plan");

        Holder.Reference<DimensionType> dimensionType = registries
                .registryOrThrow(Registries.DIMENSION_TYPE)
                .getHolderOrThrow(definition.dimensionTypeKey());

        BiomeSource biomeSource = resolveBiomeSource(registries, plan);

        var noiseRegistry = registries.registryOrThrow(Registries.NOISE_SETTINGS);
        Holder.Reference<NoiseGeneratorSettings> overworldNoise =
                noiseRegistry.getHolderOrThrow(plan.noiseSettings());

        BlockState terrainBlock = resolveBlockState(registries, plan.terrainBlockId());
        BlockState seaBlock = resolveBlockState(registries, plan.seaBlockId());

        ChunkGenerator generator = switch (plan.terrainMode()) {
            case NORMAL -> createNoiseTerrain(biomeSource,
                    AgeNoiseRouterIsolation.withoutImplicitOverworldCaves(
                            registries,
                            withMaterials(overworldNoise.value(), terrainBlock, seaBlock, plan.seaLevel()),
                            false));
            case AMPLIFIED -> createNoiseTerrain(biomeSource,
                    AgeNoiseRouterIsolation.withoutImplicitOverworldCaves(
                            registries,
                            withMaterials(
                                    noiseRegistry.getHolderOrThrow(NoiseGeneratorSettings.AMPLIFIED).value(),
                                    terrainBlock, seaBlock, plan.seaLevel()),
                            true));
            case NETHER -> createNoiseTerrain(biomeSource,
                    withMaterials(
                            noiseRegistry.getHolderOrThrow(NoiseGeneratorSettings.NETHER).value(),
                            terrainBlock, seaBlock, plan.seaLevel()));
            case END -> createNoiseTerrain(biomeSource,
                    withMaterials(
                            noiseRegistry.getHolderOrThrow(NoiseGeneratorSettings.END).value(),
                            terrainBlock, seaBlock, plan.seaLevel()));
            case FLAT -> createFlatTerrain(
                    biomeSource,
                    withMaterials(overworldNoise.value(), terrainBlock, seaBlock, plan.seaLevel()),
                    terrainBlock, seaBlock, plan.averageGroundLevel(), plan.seaLevel());
            case VOID -> createVoidTerrain(biomeSource);
            case UNKNOWN -> createNoiseTerrain(biomeSource,
                    AgeNoiseRouterIsolation.withoutImplicitOverworldCaves(
                            registries,
                            withMaterials(overworldNoise.value(), terrainBlock, seaBlock, plan.seaLevel()),
                            false));
        };

        AgeChunkGeneratorGenerationSettingsInstaller.install(
                registries,
                generator,
                plan.localGenerationPlan(),
                plan.featurePlan(),
                definition.seed(),
                plan.terrainMode(),
                plan.terrainBlockId());

        return new LevelStem(dimensionType, generator);
    }


    private static BiomeSource resolveBiomeSource(RegistryAccess registries, AgeWorldgenPlan plan) {
        var biomeRegistry = registries.registryOrThrow(Registries.BIOME);
        var selection = plan.biomeSourceSelection();

        if (selection.readyForWorldgen()) {
            java.util.ArrayList<Holder<Biome>> resolved = new java.util.ArrayList<>();
            for (String modernId : selection.modernBiomeIds()) {
                String resolvedModernId = LegacyModernBiomeIdResolver.resolveModernId(modernId);
                ResourceLocation id = ResourceLocation.parse(resolvedModernId);
                var key = net.minecraft.resources.ResourceKey.create(Registries.BIOME, id);
                resolved.add(biomeRegistry.getHolderOrThrow(key));
            }

            return switch (selection.mode()) {
                case NATIVE -> createNativeBiomeSource(registries);
                case SINGLE -> new FixedBiomeSource(resolved.get(0));
                case GRID -> new MystcraftGridBiomeSource(resolved);
                case TILED -> new MystcraftTiledBiomeSource(resolved);
                case HUGE, LARGE, MEDIUM, SMALL, TINY -> new MystcraftZoomBiomeSource(
                        selection.ageSeed(), selection.zoomScale(), resolved);
                default -> new FixedBiomeSource(
                        biomeRegistry.getHolderOrThrow(plan.fixedBiome()));
            };
        }

        return new FixedBiomeSource(biomeRegistry.getHolderOrThrow(plan.fixedBiome()));
    }

    private static BiomeSource createNativeBiomeSource(RegistryAccess registries) {
        var parameters = registries
                .registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getHolderOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
        return MultiNoiseBiomeSource.createFromPreset(parameters);
    }

    /**
     * Shared modern adapter for legacy noise-based terrain families.
     *
     * <p>The legacy generators had bespoke 1.12 octave-noise implementations. On 1.21.1
     * the closest stable worldgen contract is the matching vanilla NoiseGeneratorSettings:
     * OVERWORLD for Normal, AMPLIFIED for Amplified, NETHER for Nether, and END for End.
     * Biome selection is supplied by the resolved controller source.</p>
     */
    private static NoiseBasedChunkGenerator createNoiseTerrain(
            BiomeSource biomeSource,
            NoiseGeneratorSettings noiseSettings) {
        return new AgeNoiseBasedChunkGenerator(
                biomeSource,
                Holder.direct(noiseSettings));
    }

    /**
     * Copies a vanilla terrain-shape definition while replacing only the two material slots
     * consumed by legacy Mystcraft terrain generators.
     */
    private static NoiseGeneratorSettings withMaterials(
            NoiseGeneratorSettings base,
            BlockState terrainBlock,
            BlockState seaBlock,
            int seaLevel) {
        return new NoiseGeneratorSettings(
                base.noiseSettings(),
                terrainBlock,
                seaBlock,
                base.noiseRouter(),
                base.surfaceRule(),
                base.spawnTarget(),
                seaLevel,
                base.disableMobGeneration(),
                base.aquifersEnabled(),
                base.oreVeinsEnabled(),
                base.useLegacyRandomSource());
    }

    private static BlockState resolveBlockState(RegistryAccess registries, String blockId) {
        // Blocks are a built-in/NeoForge registry; Mystcraft's DeferredRegister entries are
        // present here once server world creation reaches this path.
        ResourceLocation id = ResourceLocation.parse(blockId);
        Block block = BuiltInRegistries.BLOCK.get(id);
        if (block == Blocks.AIR && !"minecraft:air".equals(blockId)
                && !BuiltInRegistries.BLOCK.containsKey(id)) {
            throw new IllegalStateException("Unknown Mystcraft terrain material block: " + blockId);
        }
        return block.defaultBlockState();
    }

    /**
     * Legacy TerrainFlat base terrain running through the same post-terrain pipeline as
     * Standard terrain. Vanilla FlatLevelSource cannot be used here because 1.21.1 makes
     * its carver callback a no-op and replaces the authored BiomeSource with a fixed biome.
     */
    private static ChunkGenerator createFlatTerrain(
            BiomeSource biomeSource,
            NoiseGeneratorSettings noiseSettings,
            BlockState terrainBlock,
            BlockState seaBlock,
            int averageGroundLevel,
            int seaLevel) {
        return new AgeFlatNoiseBasedChunkGenerator(
                biomeSource,
                Holder.direct(noiseSettings),
                terrainBlock,
                seaBlock,
                averageGroundLevel,
                seaLevel);
    }

    /**
     * Modern implementation of legacy mystcraft:TerrainVoid. The old generator's terrain
     * callback was empty, so a FlatLevelSource with no layers gives the same block result.
     */
    private static FlatLevelSource createVoidTerrain(BiomeSource biomeSource) {
        Holder<Biome> voidBiome = biomeSource.possibleBiomes().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Mystcraft biome source has no biomes"));
        FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(
                Optional.empty(), voidBiome, List.of());
        settings.updateLayers();
        return new AgeVoidFlatLevelSource(settings);
    }
}
