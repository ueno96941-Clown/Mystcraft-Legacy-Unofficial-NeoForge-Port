package com.xcompwiz.mystcraft.world.dimension;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** NoiseBasedChunkGenerator with Mystcraft's Age-local structure compatibility bridge. */
final class AgeNoiseBasedChunkGenerator extends NoiseBasedChunkGenerator {
    AgeNoiseBasedChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource, settings);
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
