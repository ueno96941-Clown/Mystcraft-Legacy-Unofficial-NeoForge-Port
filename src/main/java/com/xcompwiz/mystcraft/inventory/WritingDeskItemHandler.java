package com.xcompwiz.mystcraft.inventory;

import com.xcompwiz.mystcraft.blockentity.WritingDeskBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Automation view matching TileEntityDesk#buildWorkInventory in 0.13.7.06.
 *
 * <p>The old IOInventory exposed all four work slots for inspection, but only the
 * paper slot was an automation input and it declared no automation output slots.
 * Target/container/output remained GUI/misc slots.</p>
 */
public final class WritingDeskItemHandler implements IItemHandler {
    private final WritingDeskBlockEntity desk;

    public WritingDeskItemHandler(WritingDeskBlockEntity desk) {
        this.desk = desk;
    }

    @Override
    public int getSlots() {
        return WritingDeskBlockEntity.WORK_SLOT_COUNT;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return valid(slot) ? desk.getItem(slot) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        if (!valid(slot)) return 0;
        return slot == WritingDeskBlockEntity.TARGET_SLOT ? 1 : 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == WritingDeskBlockEntity.PAPER_SLOT && desk.canPlaceItem(slot, stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!isItemValid(slot, stack) || stack.isEmpty()) return stack;
        ItemStack existing = desk.getItem(slot);
        int max = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(existing, stack)) return stack;
            max = Math.min(max, existing.getMaxStackSize());
        }
        int room = max - existing.getCount();
        if (room <= 0) return stack;
        int move = Math.min(room, stack.getCount());
        if (!simulate) {
            if (existing.isEmpty()) desk.setItem(slot, stack.copyWithCount(move));
            else {
                existing.grow(move);
                desk.setChanged();
            }
        }
        if (move >= stack.getCount()) return ItemStack.EMPTY;
        ItemStack remainder = stack.copy();
        remainder.shrink(move);
        return remainder;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        // Legacy buildWorkInventory supplied an empty outSlots array.
        return ItemStack.EMPTY;
    }

    private static boolean valid(int slot) {
        return slot >= 0 && slot < WritingDeskBlockEntity.WORK_SLOT_COUNT;
    }
}
