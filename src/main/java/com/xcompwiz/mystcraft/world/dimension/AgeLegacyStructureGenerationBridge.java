package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.Mystcraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Compatibility generation hooks for legacy structures whose modern Structure definitions
 * carry assumptions that did not exist in Mystcraft 0.13.7.06.
 */
final class AgeLegacyStructureGenerationBridge {
    private static final ResourceKey<Structure> FORTRESS = ResourceKey.create(
            Registries.STRUCTURE, ResourceLocation.withDefaultNamespace("fortress"));

    private AgeLegacyStructureGenerationBridge() {}

    /**
     * Modern minecraft:fortress is biome-gated to Nether biomes.  Legacy MapGenNetherBridge
     * was not: when the Mystcraft Nether Fortress page was authored, it could generate in an
     * otherwise ordinary Age.  After vanilla createStructures() has had its normal chance,
     * retry only our Age-local legacy placement with an all-biomes predicate.
     */
    static void ensureNetherFortress(
            ChunkGenerator generator,
            RegistryAccess registryAccess,
            ChunkGeneratorStructureState structureState,
            StructureManager structureManager,
            ChunkAccess chunk,
            StructureTemplateManager structureTemplateManager) {
        ChunkPos chunkPos = chunk.getPos();
        SectionPos sectionPos = SectionPos.bottomOf(chunk);

        for (var holderSet : structureState.possibleStructureSets()) {
            StructureSet set = holderSet.value();
            if (!(set.placement() instanceof AgeLegacyNetherFortressPlacement placement)) continue;
            if (!placement.isStructureChunk(structureState, chunkPos.x, chunkPos.z)) continue;

            for (StructureSet.StructureSelectionEntry entry : set.structures()) {
                var holder = entry.structure();
                if (!holder.is(FORTRESS)) continue;

                Structure fortress = holder.value();
                StructureStart existing = structureManager.getStartForStructure(sectionPos, fortress, chunk);
                if (existing != null && existing.isValid()) return;
                int references = existing == null ? 0 : existing.getReferences();

                StructureStart generated = fortress.generate(
                        registryAccess,
                        generator,
                        generator.getBiomeSource(),
                        structureState.randomState(),
                        structureTemplateManager,
                        structureState.getLevelSeed(),
                        chunkPos,
                        references,
                        chunk,
                        biome -> true);
                if (generated.isValid()) {
                    structureManager.setStartForStructure(sectionPos, fortress, generated, chunk);
                    Mystcraft.LOGGER.debug(
                            "Legacy Nether Fortress start: chunk={}, seed={}",
                            chunkPos, structureState.getLevelSeed());
                }
                return;
            }
        }
    }
}
