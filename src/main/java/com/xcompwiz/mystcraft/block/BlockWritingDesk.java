package com.xcompwiz.mystcraft.block;

import com.mojang.serialization.MapCodec;
import com.xcompwiz.mystcraft.blockentity.WritingDeskBlockEntity;
import com.xcompwiz.mystcraft.item.ItemWritingDesk;
import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.page.WritingStationRuntime;
import com.xcompwiz.mystcraft.registry.MystItems;
import com.xcompwiz.mystcraft.registry.MystBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Legacy four-part Writing Desk furniture plus its current writing baseline.
 *
 * <p>The 0.13.7.06 desk consists of a two-block lower desk and an optional
 * two-block upper extension. Only the lower non-foot block owns the block entity;
 * all four parts resolve interactions back to that block entity.</p>
 */
public final class BlockWritingDesk extends BaseEntityBlock {
    public static final MapCodec<BlockWritingDesk> CODEC = simpleCodec(BlockWritingDesk::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty IS_TOP = BooleanProperty.create("istop");
    public static final BooleanProperty IS_FOOT = BooleanProperty.create("isfoot");

    private static final VoxelShape TOP_NORTH = Block.box(0, 0, 0, 8, 12, 16);
    private static final VoxelShape TOP_SOUTH = Block.box(8, 0, 0, 16, 12, 16);
    private static final VoxelShape TOP_WEST = Block.box(0, 0, 0, 16, 12, 8);
    private static final VoxelShape TOP_EAST = Block.box(0, 0, 8, 16, 12, 16);

    public BlockWritingDesk(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(IS_TOP, false)
                .setValue(IS_FOOT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, IS_TOP, IS_FOOT);
    }

    /**
     * StructureTemplate rotates block positions and delegates state rotation back to the block.
     * Without these hooks an Archivist house could rotate the two desk positions while leaving
     * their local SOUTH facing unchanged, breaking the main/foot multiblock relationship.
     */
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getClockWise();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        BlockPos foot = context.getClickedPos().relative(facing);
        if (!context.getLevel().getBlockState(foot).canBeReplaced(context)) {
            return null;
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (state.getValue(IS_TOP) || state.getValue(IS_FOOT)) return;
        BlockPos foot = pos.relative(state.getValue(FACING));
        if (level.getBlockState(foot).canBeReplaced()) {
            level.setBlock(foot, state.setValue(IS_FOOT, true), Block.UPDATE_ALL);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        BlockPos paired = state.getValue(IS_FOOT) ? pos.relative(facing.getOpposite()) : pos.relative(facing);
        if (neighborPos.equals(paired)) {
            BlockState pairState = level.getBlockState(paired);
            if (!isMatchingPair(state, pairState)) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        if (state.getValue(IS_TOP) && !state.getValue(IS_FOOT) && direction == Direction.DOWN) {
            BlockState below = level.getBlockState(pos.below());
            if (!below.is(this) || below.getValue(IS_TOP) || below.getValue(IS_FOOT)) {
                return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    private static boolean isMatchingPair(BlockState state, BlockState other) {
        return other.getBlock() == state.getBlock()
                && other.getValue(FACING) == state.getValue(FACING)
                && other.getValue(IS_TOP).equals(state.getValue(IS_TOP))
                && other.getValue(IS_FOOT) != state.getValue(IS_FOOT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(IS_TOP) || state.getValue(IS_FOOT)) return null;
        return new WritingDeskBlockEntity(pos, state);
    }

    /**
     * Do not expose the block entity through vanilla's spectator MenuProvider shortcut.
     * The Writing Desk menu needs its main BlockPos in the NeoForge extra payload.
     */
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || state.getValue(IS_TOP) || state.getValue(IS_FOOT)) return null;
        return createTickerHelper(type, MystBlockEntities.WRITING_DESK.get(), WritingDeskBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // 0.13.7.06 used ENTITYBLOCK_ANIMATED: the whole two-block desk and
        // optional backboard are rendered once from the lower-main block entity.
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(IS_TOP)) return Shapes.block();
        return switch (state.getValue(FACING)) {
            case NORTH -> TOP_NORTH;
            case SOUTH -> TOP_SOUTH;
            case WEST -> TOP_WEST;
            case EAST -> TOP_EAST;
            default -> Shapes.block();
        };
    }

    @Nullable
    public static BlockPos getMainPos(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockWritingDesk)) return null;
        BlockPos resolved = state.getValue(IS_TOP) ? pos.below() : pos;
        BlockState resolvedState = level.getBlockState(resolved);
        if (!(resolvedState.getBlock() instanceof BlockWritingDesk)) return null;
        if (resolvedState.getValue(IS_FOOT)) {
            resolved = resolved.relative(resolvedState.getValue(FACING).getOpposite());
            resolvedState = level.getBlockState(resolved);
        }
        if (!(resolvedState.getBlock() instanceof BlockWritingDesk)
                || resolvedState.getValue(IS_TOP) || resolvedState.getValue(IS_FOOT)) return null;
        return resolved;
    }

    @Nullable
    private static WritingDeskBlockEntity getDesk(Level level, BlockPos pos) {
        BlockPos main = getMainPos(level, pos);
        if (main == null) return null;
        return level.getBlockEntity(main) instanceof WritingDeskBlockEntity desk ? desk : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        // 0.13.7.06 always routed desk interaction through its GUI.  Returning
        // PASS_TO_DEFAULT_BLOCK_INTERACTION lets the normal block interaction
        // continue to useWithoutItem even when the player is holding a page or vial.
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (!com.xcompwiz.mystcraft.util.MystMenuOpenPolicy.canOpen(player)) return InteractionResult.PASS;
        WritingDeskBlockEntity desk = getDesk(level, pos);
        if (desk == null) return InteractionResult.PASS;
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockPos main = getMainPos(level, pos);
            if (main != null) {
                serverPlayer.openMenu(desk, buffer -> buffer.writeBlockPos(main));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !state.getValue(IS_TOP) && !state.getValue(IS_FOOT)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof WritingDeskBlockEntity desk) {
                Containers.dropContents(level, pos, desk);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !state.getValue(IS_TOP)) {
            BlockPos main = state.getValue(IS_FOOT)
                    ? pos.relative(state.getValue(FACING).getOpposite())
                    : pos;
            BlockPos topMain = main.above();
            BlockState topState = level.getBlockState(topMain);
            if (topState.is(this) && topState.getValue(IS_TOP) && !topState.getValue(IS_FOOT)) {
                BlockPos topFoot = topMain.relative(topState.getValue(FACING));
                level.removeBlock(topMain, false);
                if (level.getBlockState(topFoot).is(this)) level.removeBlock(topFoot, false);
                if (!player.getAbilities().instabuild) {
                    popResource(level, topMain, ItemWritingDesk.topStack());
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(state.getValue(IS_TOP) ? ItemWritingDesk.topStack() : ItemWritingDesk.baseStack());
    }
}
