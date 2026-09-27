package com.xcompwiz.mystcraft.block;

import com.mojang.serialization.MapCodec;
import com.xcompwiz.mystcraft.blockentity.BookDisplayBlockEntity;
import com.xcompwiz.mystcraft.item.ItemLinking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Functional common implementation for mystcraft:blockbookstand and
 * mystcraft:blocklectern.
 *
 * <p>Insert one Mystcraft linking/descriptive book, use the display with an
 * empty hand to follow the stored link without consuming the book, and
 * sneak-use with an empty hand to retrieve it.</p>
 */
public final class BlockBookDisplay extends BaseEntityBlock {
    public static final MapCodec<BlockBookDisplay> CODEC = simpleCodec(BlockBookDisplay::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape STAND_SHAPE = Shapes.or(
            box(5, 0, 5, 11, 3, 11),
            box(7, 3, 7, 9, 11, 9),
            box(2, 10, 5, 14, 14, 11));
    private static final VoxelShape LECTERN_SHAPE = Shapes.or(
            box(0, 0, 3, 16, 2, 13),
            box(0, 2, 4, 16, 5, 12));

    public BlockBookDisplay(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BookDisplayBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 0.13.7.06 stored the placer horizontal facing directly for Lectern.
        // Book Stand used an 8-way yaw index; its cardinal projection is the same
        // horizontal direction.  Renderer-specific corrections belong in the BER,
        // not in placement state (the previous opposite() caused reversed furniture).
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.is(com.xcompwiz.mystcraft.registry.MystBlocks.LECTERN.get()) ? LECTERN_SHAPE : STAND_SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof BookDisplayBlockEntity display)
                || !display.canAccept(stack)
                || display.hasBook()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && display.insertBook(stack)) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.mystcraft.book_display_inserted"), true);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof BookDisplayBlockEntity display) || !display.hasBook()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                ItemStack book = display.removeBook();
                if (player.getMainHandItem().isEmpty()) player.setItemInHand(InteractionHand.MAIN_HAND, book);
                else player.getInventory().placeItemBackInInventory(book);
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.mystcraft.book_display_removed"), true);
            } else if (player instanceof ServerPlayer serverPlayer
                    && BookDisplayBlockEntity.isLinkingBook(display.getBook())) {
                ItemLinking.openStoredBookMenu(serverPlayer, pos, display.getBook());
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof BookDisplayBlockEntity display
                && !level.isClientSide) {
            ItemStack book = display.removeBook();
            if (!book.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, book);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BookDisplayBlockEntity display && display.hasBook() ? 15 : 0;
    }
}
