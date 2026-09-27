package com.xcompwiz.mystcraft.item;

import com.xcompwiz.mystcraft.block.BlockWritingDesk;
import com.xcompwiz.mystcraft.util.LegacyItemData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Legacy Writing Desk item.
 *
 * <p>0.13.7.06 used one registry ID with metadata 0 for the two-block desk base
 * and metadata 1 for its two-block upper extension. Modern item damage metadata
 * no longer exists, so the port keeps the single historical ID and stores the
 * subtype in CUSTOM_DATA.</p>
 */
public final class ItemWritingDesk extends BlockItem {
    private static final String KEY_PART = "MystWritingDeskPart";
    private static final String PART_TOP = "top";

    public ItemWritingDesk(Block block, Properties properties) {
        super(block, properties);
    }

    public static boolean isTopExtension(ItemStack stack) {
        return PART_TOP.equals(LegacyItemData.copy(stack).getString(KEY_PART));
    }

    public static ItemStack baseStack() {
        return new ItemStack(com.xcompwiz.mystcraft.registry.MystItems.WRITING_DESK_ITEM.get());
    }

    public static ItemStack topStack() {
        ItemStack stack = baseStack();
        LegacyItemData.update(stack, tag -> tag.putString(KEY_PART, PART_TOP));
        // 1.12 used metadata 1 for the distinct desk extension icon. Modern item
        // metadata is gone, so mirror that visible subtype with CustomModelData.
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(isTopExtension(stack)
                ? "item.mystcraft.writingdesk.top"
                : "item.mystcraft.writingdesk");
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack held = context.getItemInHand();
        if (!isTopExtension(held)) {
            return super.useOn(context);
        }

        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clicked);
        if (!(clickedState.getBlock() instanceof BlockWritingDesk)) {
            return InteractionResult.PASS;
        }

        BlockPos mainPos = BlockWritingDesk.getMainPos(level, clicked);
        if (mainPos == null) {
            return InteractionResult.PASS;
        }
        BlockState mainState = level.getBlockState(mainPos);
        if (!mainState.is(getBlock()) || mainState.getValue(BlockWritingDesk.IS_TOP)) {
            return InteractionResult.PASS;
        }

        BlockPos footPos = mainPos.relative(mainState.getValue(BlockWritingDesk.FACING));
        BlockPos topMain = mainPos.above();
        BlockPos topFoot = footPos.above();
        if (!level.getBlockState(topMain).canBeReplaced() || !level.getBlockState(topFoot).canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        var player = context.getPlayer();
        if (player != null && (!player.mayUseItemAt(topMain, context.getClickedFace(), held)
                || !player.mayUseItemAt(topFoot, context.getClickedFace(), held))) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            BlockState top = getBlock().defaultBlockState()
                    .setValue(BlockWritingDesk.FACING, mainState.getValue(BlockWritingDesk.FACING))
                    .setValue(BlockWritingDesk.IS_TOP, true)
                    .setValue(BlockWritingDesk.IS_FOOT, false);
            level.setBlock(topMain, top, Block.UPDATE_ALL);
            level.setBlock(topFoot, top.setValue(BlockWritingDesk.IS_FOOT, true), Block.UPDATE_ALL);
            if (player == null || !player.getAbilities().instabuild) {
                held.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
