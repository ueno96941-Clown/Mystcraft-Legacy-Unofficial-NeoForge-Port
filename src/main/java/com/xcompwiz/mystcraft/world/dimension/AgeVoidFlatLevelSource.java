package com.xcompwiz.mystcraft.world.dimension;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** Void terrain source that keeps Mystcraft's Age-local structure compatibility bridge. */
final class AgeVoidFlatLevelSource extends FlatLevelSource {
    AgeVoidFlatLevelSource(FlatLevelGeneratorSettings settings) {
        super(settings);
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
