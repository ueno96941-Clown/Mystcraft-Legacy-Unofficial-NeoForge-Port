package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.LegacyFloatingIslandSurfaceBuffer;
import com.xcompwiz.mystcraft.world.worldgen.LegacyFloatingIslandMaskView;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.Objects;

/**
 * Modern-height adapter for the original floating-island geometry kernel.
 * Legacy vertical cells are expanded across the actual 1.21.1 Age height.
 */
public final class ChunkAccessFloatingIslandBuffer implements LegacyFloatingIslandSurfaceBuffer, LegacyFloatingIslandMaskView {
    private final ChunkAccess chunk;
    private final BlockState replacement;
    private final BlockState top;
    private final BlockState filler;
    private final BlockState sandstone;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private final boolean[] modifiedColumns = new boolean[256];
    private final int minBlockX;
    private final int minBlockZ;

    public ChunkAccessFloatingIslandBuffer(
            ChunkAccess chunk,
            BlockState replacement,
            BlockState top,
            BlockState filler,
            BlockState sandstone) {
        this.chunk = Objects.requireNonNull(chunk, "chunk");
        this.replacement = Objects.requireNonNull(replacement, "replacement");
        this.top = Objects.requireNonNull(top, "top");
        this.filler = Objects.requireNonNull(filler, "filler");
        this.sandstone = Objects.requireNonNull(sandstone, "sandstone");
        this.minBlockX = chunk.getPos().getMinBlockX();
        this.minBlockZ = chunk.getPos().getMinBlockZ();
    }

    private int startY(int legacyY) { return ModernAgeHeight.legacyCellStart(chunk, legacyY); }
    private int endY(int legacyY) { return ModernAgeHeight.legacyCellEndInclusive(chunk, legacyY); }

    private BlockState stateAtWorldY(int x, int worldY, int z) {
        return chunk.getBlockState(pos.set(minBlockX + x, worldY, minBlockZ + z));
    }

    private boolean anyState(int x, int y, int z, java.util.function.Predicate<BlockState> predicate) {
        for (int worldY = startY(y); worldY <= endY(y); ++worldY) {
            if (predicate.test(stateAtWorldY(x, worldY, z))) return true;
        }
        return false;
    }

    private void fillCell(int x, int y, int z, BlockState state) {
        for (int worldY = startY(y); worldY <= endY(y); ++worldY) {
            chunk.setBlockState(pos.set(minBlockX + x, worldY, minBlockZ + z), state, false);
        }
    }

    @Override public boolean isBedrock(int x, int y, int z) {
        return anyState(x, y, z, state -> state.is(Blocks.BEDROCK));
    }

    @Override public boolean isLiquid(int x, int y, int z) {
        return anyState(x, y, z, state -> !state.getFluidState().isEmpty());
    }

    @Override public boolean isWater(int x, int y, int z) {
        return anyState(x, y, z, state -> state.getFluidState().is(FluidTags.WATER));
    }

    @Override public void setReplacement(int x, int y, int z) { fillCell(x, y, z, replacement); }

    @Override public void markModifiedColumn(int localX, int localZ) {
        modifiedColumns[localX + localZ * 16] = true;
    }

    @Override public boolean isStone(int x, int y, int z) {
        return anyState(x, y, z, state -> state.is(Blocks.STONE));
    }

    @Override public void setTop(int x, int y, int z) {
        // Surface semantics apply to the highest modern block represented by this legacy cell.
        int worldY = endY(y);
        chunk.setBlockState(pos.set(minBlockX + x, worldY, minBlockZ + z), top, false);
    }

    @Override public void setFiller(int x, int y, int z) { fillCell(x, y, z, filler); }
    @Override public void setSandstone(int x, int y, int z) { fillCell(x, y, z, sandstone); }
    @Override public boolean fillerIsSand() { return filler.is(Blocks.SAND); }
    public boolean[] copyModifiedColumns() { return modifiedColumns.clone(); }
}
