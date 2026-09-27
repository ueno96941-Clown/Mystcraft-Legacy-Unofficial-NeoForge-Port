package com.xcompwiz.mystcraft.world.dimension;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * TerrainFlat adapter using Minecraft 1.21.1's vertical build contract.
 *
 * <p>Mystcraft's flat symbol still controls the base terrain, but the world is
 * no longer forced into the historical 0..255 box. Bedrock begins at the actual
 * minimum build Y, terrain reaches the authored absolute ground level, and the
 * ordinary modern carver/biome/structure pipeline can continue afterward.</p>
 */
public final class AgeFlatNoiseBasedChunkGenerator extends NoiseBasedChunkGenerator {
    private final List<BlockState> layers;
    private final int seaLevel;

    public AgeFlatNoiseBasedChunkGenerator(
            BiomeSource biomeSource,
            Holder<NoiseGeneratorSettings> noiseSettings,
            BlockState terrainBlock,
            BlockState seaBlock,
            int averageGroundLevel,
            int seaLevel) {
        super(biomeSource, noiseSettings);
        this.seaLevel = seaLevel;
        this.layers = buildLayers(terrainBlock, seaBlock, averageGroundLevel, seaLevel);
    }

    private static List<BlockState> buildLayers(
            BlockState terrainBlock, BlockState seaBlock, int averageGroundLevel, int seaLevel) {
        ArrayList<BlockState> out = new ArrayList<>(ModernAgeHeight.HEIGHT);
        for (int y = ModernAgeHeight.MIN_Y; y < ModernAgeHeight.MAX_Y_EXCLUSIVE; ++y) {
            BlockState state;
            if (y == ModernAgeHeight.MIN_Y) {
                state = Blocks.BEDROCK.defaultBlockState();
            } else if (y < averageGroundLevel) {
                state = terrainBlock;
            } else if (y <= seaLevel) {
                state = seaBlock;
            } else {
                state = Blocks.AIR.defaultBlockState();
            }
            out.add(state);
        }
        return List.copyOf(out);
    }

    @Override
    public void createStructures(
            RegistryAccess registryAccess,
            ChunkGeneratorStructureState structureState,
            StructureManager structureManager,
            ChunkAccess chunk,
            StructureTemplateManager structureTemplateManager) {
        super.createStructures(registryAccess, structureState, structureManager, chunk, structureTemplateManager);
        AgeLegacyStructureGenerationBridge.ensureNetherFortress(
                this, registryAccess, structureState, structureManager, chunk, structureTemplateManager);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int count = Math.min(chunk.getHeight(), layers.size());

        for (int index = 0; index < count; ++index) {
            BlockState state = layers.get(index);
            if (state.isAir()) continue;
            int y = chunk.getMinBuildHeight() + index;
            for (int x = 0; x < 16; ++x) {
                for (int z = 0; z < 16; ++z) {
                    chunk.setBlockState(pos.set(x, y, z), state, false);
                    oceanFloor.update(x, y, z, state);
                    worldSurface.update(x, y, z, state);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor level) {
        int topSolid = Math.max(level.getMinBuildHeight(), Math.min(level.getMaxBuildHeight(), seaLevel + 1));
        return topSolid;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        int max = Math.min(layers.size(), level.getHeight());
        for (int i = max - 1; i >= 0; --i) {
            BlockState state = layers.get(i);
            if (type.isOpaque().test(state)) return level.getMinBuildHeight() + i + 1;
        }
        return level.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        BlockState[] column = new BlockState[height.getHeight()];
        for (int i = 0; i < column.length; ++i) {
            column[i] = i < layers.size() ? layers.get(i) : Blocks.AIR.defaultBlockState();
        }
        return new NoiseColumn(height.getMinBuildHeight(), column);
    }

    @Override public int getMinY() { return ModernAgeHeight.MIN_Y; }
    @Override public int getGenDepth() { return ModernAgeHeight.HEIGHT; }
    @Override public int getSeaLevel() { return seaLevel; }
    @Override
    public Pair<BlockPos, Holder<Structure>> findNearestMapStructure(
            ServerLevel level,
            HolderSet<Structure> structure,
            BlockPos pos,
            int searchRadius,
            boolean skipKnownStructures) {
        Pair<BlockPos, Holder<Structure>> vanilla =
                super.findNearestMapStructure(level, structure, pos, searchRadius, skipKnownStructures);
        if (vanilla != null) return vanilla;
        return AgeLegacyStructureLocateBridge.findNearestFortress(
                level, structure, pos, searchRadius, skipKnownStructures);
    }

}
