package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.LegacyBigTreeBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.Objects;

/**
 * Modern-height adapter for the legacy giant-tree geometry.
 * The historical 0..255 Y coordinates are mapped across the live Age build range.
 */
public final class ChunkAccessLegacyBigTreeBuffer implements LegacyBigTreeBuffer {
    private final ChunkAccess chunk;
    private final BlockState log;
    private final BlockState leaves;
    private final int minBlockX;
    private final int minBlockZ;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    public ChunkAccessLegacyBigTreeBuffer(ChunkAccess chunk) {
        this.chunk = Objects.requireNonNull(chunk, "chunk");
        this.log = Blocks.OAK_LOG.defaultBlockState();
        this.leaves = Blocks.OAK_LEAVES.defaultBlockState();
        this.minBlockX = chunk.getPos().getMinBlockX();
        this.minBlockZ = chunk.getPos().getMinBlockZ();
    }

    private int startY(int legacyY) { return ModernAgeHeight.legacyCellStart(chunk, legacyY); }
    private int endY(int legacyY) { return ModernAgeHeight.legacyCellEndInclusive(chunk, legacyY); }

    @Override
    public boolean isBedrock(int x, int y, int z) {
        for (int worldY = startY(y); worldY <= endY(y); ++worldY) {
            if (chunk.getBlockState(pos.set(minBlockX + x, worldY, minBlockZ + z)).is(Blocks.BEDROCK)) return true;
        }
        return false;
    }

    private void fill(int x, int y, int z, BlockState state) {
        for (int worldY = startY(y); worldY <= endY(y); ++worldY) {
            chunk.setBlockState(pos.set(minBlockX + x, worldY, minBlockZ + z), state, false);
        }
    }

    @Override public void setLog(int x, int y, int z) { fill(x, y, z, log); }
    @Override public void setLeaves(int x, int y, int z) { fill(x, y, z, leaves); }
}
