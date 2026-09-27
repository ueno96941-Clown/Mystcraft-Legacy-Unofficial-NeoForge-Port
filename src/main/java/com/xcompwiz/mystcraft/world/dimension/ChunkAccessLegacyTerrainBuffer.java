package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.LegacyTerrainBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.Objects;

/**
 * Compatibility adapter for legacy Mystcraft-only geometry kernels.
 *
 * <p>The kernels still describe their vertical cells as 0..255, but the adapter
 * expands those cells across the actual modern chunk height. Ordinary caves and
 * ravines no longer use this bridge; they are delegated to Minecraft 1.21.1's
 * configured carvers. It remains for shapes that have no vanilla equivalent
 * (tendrils/spheres/floating-island geometry).</p>
 */
public class ChunkAccessLegacyTerrainBuffer implements LegacyTerrainBuffer {
    protected final ChunkAccess chunk;
    protected final BlockState replacement;
    protected final int minBlockX;
    protected final int minBlockZ;
    protected final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    public ChunkAccessLegacyTerrainBuffer(ChunkAccess chunk, BlockState replacement) {
        this.chunk = Objects.requireNonNull(chunk, "chunk");
        this.replacement = Objects.requireNonNull(replacement, "replacement");
        this.minBlockX = chunk.getPos().getMinBlockX();
        this.minBlockZ = chunk.getPos().getMinBlockZ();
    }

    protected int startY(int legacyY) {
        return ModernAgeHeight.legacyCellStart(chunk, legacyY);
    }

    protected int endY(int legacyY) {
        return ModernAgeHeight.legacyCellEndInclusive(chunk, legacyY);
    }

    protected BlockState stateAtWorldY(int x, int worldY, int z) {
        return chunk.getBlockState(pos.set(minBlockX + x, worldY, minBlockZ + z));
    }

    @Override
    public boolean isBedrock(int x, int y, int z) {
        for (int worldY = startY(y); worldY <= endY(y); ++worldY) {
            if (stateAtWorldY(x, worldY, z).is(Blocks.BEDROCK)) return true;
        }
        return false;
    }

    @Override
    public boolean isLiquid(int x, int y, int z) {
        for (int worldY = startY(y); worldY <= endY(y); ++worldY) {
            if (!stateAtWorldY(x, worldY, z).getFluidState().isEmpty()) return true;
        }
        return false;
    }

    @Override
    public boolean isWater(int x, int y, int z) {
        for (int worldY = startY(y); worldY <= endY(y); ++worldY) {
            if (stateAtWorldY(x, worldY, z).getFluidState().is(FluidTags.WATER)) return true;
        }
        return false;
    }

    @Override
    public void setReplacement(int x, int y, int z) {
        for (int worldY = startY(y); worldY <= endY(y); ++worldY) {
            chunk.setBlockState(pos.set(minBlockX + x, worldY, minBlockZ + z), replacement, false);
        }
    }
}
