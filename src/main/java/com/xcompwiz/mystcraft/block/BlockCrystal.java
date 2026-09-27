package com.xcompwiz.mystcraft.block;

import com.mojang.serialization.MapCodec;
import com.xcompwiz.mystcraft.portal.CrystalPortalRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockCrystal extends Block {
    public static final MapCodec<BlockCrystal> CODEC = simpleCodec(BlockCrystal::new);

    public BlockCrystal(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<BlockCrystal> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel && !oldState.is(state.getBlock())) {
            CrystalPortalRuntime.refreshCrystalNetwork(serverLevel, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level instanceof ServerLevel serverLevel) {
            CrystalPortalRuntime.refreshCrystalNetwork(serverLevel, pos);
        }
    }
}
