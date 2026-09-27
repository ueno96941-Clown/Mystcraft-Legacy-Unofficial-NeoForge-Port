package com.xcompwiz.mystcraft.world.worldgen;

import com.xcompwiz.mystcraft.block.BlockWritingDesk;
import com.xcompwiz.mystcraft.registry.MystBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;


/**
 * Preserves Legacy Writing Desk's state-dependent tile-entity semantics during
 * modern structure worldgen.
 *
 * <p>In 1.12 {@code BlockWritingDesk.hasTileEntity(state)} returned true only
 * for the lower non-foot state.  In 1.21.1 {@code BlockState.hasBlockEntity()}
 * is block-type based: every state of an {@code EntityBlock} reports true.
 * {@link WorldGenRegion#setBlock} therefore writes a DUMMY block-entity tag for
 * a Writing Desk foot even though {@link BlockWritingDesk#newBlockEntity}
 * correctly returns null for that state.  The DUMMY later produces a noisy
 * "Tried to load a block entity ... but failed" warning when the protochunk is
 * promoted.</p>
 *
 * <p>This processor is attached only to the Archivist house pool element.  For
 * non-owning desk parts generated through a {@link WorldGenRegion}, it writes
 * the already-transformed state directly into the target {@link ChunkAccess}
 * and suppresses normal StructureTemplate placement for that one block.  This
 * exactly preserves the old invariant: only the lower non-foot state owns a
 * block entity, while keeping the single legacy {@code mystcraft:writingdesk}
 * block ID and state flags.</p>
 */
public final class WritingDeskStructureProcessor extends StructureProcessor {
    public static final WritingDeskStructureProcessor INSTANCE = new WritingDeskStructureProcessor();

    private WritingDeskStructureProcessor() {}

    @Override
    public StructureTemplate.StructureBlockInfo process(
            LevelReader level,
            BlockPos offset,
            BlockPos pos,
            StructureTemplate.StructureBlockInfo original,
            StructureTemplate.StructureBlockInfo current,
            StructurePlaceSettings settings,
            StructureTemplate template) {
        BlockState processedState = current.state();
        if (!processedState.is(MystBlocks.WRITING_DESK.get())
                || (!processedState.getValue(BlockWritingDesk.IS_FOOT)
                    && !processedState.getValue(BlockWritingDesk.IS_TOP))) {
            return current;
        }

        // Only WorldGenRegion's ProtoChunk path manufactures the DUMMY tag.
        // Normal LevelChunk placement is safe: newBlockEntity(null) simply
        // leaves these legacy non-owning parts without a block entity.
        if (!(level instanceof WorldGenRegion region)) {
            return current;
        }

        int chunkX = Math.floorDiv(current.pos().getX(), 16);
        int chunkZ = Math.floorDiv(current.pos().getZ(), 16);
        if (!region.hasChunk(chunkX, chunkZ)) {
            // CP270: Structure processors run during FEATURES and must never force a chunk
            // outside the finite WorldGenRegion cache. Falling back to normal template
            // placement is noisy at worst (the old DUMMY-BE warning) but cannot kill worldgen.
            return current;
        }

        // StructureTemplate has already applied the element mirror/rotation before processors are
        // invoked; `current` is the processed placement state.  Applying settings a second time
        // rotates non-owning Writing Desk parts twice and can disagree with the owning lower part.
        ChunkAccess chunk = level.getChunk(current.pos());
        chunk.setBlockState(current.pos(), processedState, false);
        // Remove any stale pending/entity data if another processor or an old
        // generated version touched this position before us.
        chunk.removeBlockEntity(current.pos());
        return null;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        // This processor exists only inside the runtime-injected direct processor
        // list and is never data-driven/serialized.  Returning NOP keeps the
        // vanilla codec contract without inventing a persistent registry entry.
        return StructureProcessorType.NOP;
    }
}
