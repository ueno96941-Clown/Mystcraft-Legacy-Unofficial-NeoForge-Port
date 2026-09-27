package com.xcompwiz.mystcraft.block;

import com.mojang.serialization.MapCodec;
import com.xcompwiz.mystcraft.blockentity.BookBinderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;

public final class BlockBookBinder extends BaseEntityBlock {
    public static final MapCodec<BlockBookBinder> CODEC = simpleCodec(BlockBookBinder::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public BlockBookBinder(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BookBinderBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Spectator interaction in vanilla bypasses useWithoutItem() and opens a block's
     * MenuProvider directly, without NeoForge's extra menu payload.  Mystcraft's binder
     * menu requires that payload (BlockPos/title/page descriptors), so exposing the block
     * entity here produces a null RegistryFriendlyByteBuf on the client.
     *
     * <p>Normal players are opened explicitly from useWithoutItem() below with the complete
     * payload.  Returning null here is therefore deliberate and spectator-safe.</p>
     */
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return null;
    }


    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BookBinderBlockEntity binder) {
                Containers.dropContents(level, pos, binder);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!com.xcompwiz.mystcraft.util.MystMenuOpenPolicy.canOpen(player)) return InteractionResult.PASS;
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BookBinderBlockEntity binder) {
                serverPlayer.openMenu(binder, buffer -> {
                    buffer.writeBlockPos(pos);
                    buffer.writeUtf(binder.getPendingTitle(), BookBinderBlockEntity.MAX_TITLE_LENGTH);
                    com.xcompwiz.mystcraft.inventory.BookBinderMenu.writeDescriptors(buffer, binder.getPages());
                });
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
