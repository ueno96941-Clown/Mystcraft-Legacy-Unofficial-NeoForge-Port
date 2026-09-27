package com.xcompwiz.mystcraft.inventory;

import com.xcompwiz.mystcraft.blockentity.InkMixerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Automation view matching legacy IOInventory: slots 0/1 insert, slot 2 extract. */
public final class InkMixerItemHandler implements IItemHandler {
    private final InkMixerBlockEntity mixer;

    public InkMixerItemHandler(InkMixerBlockEntity mixer) { this.mixer = mixer; }

    @Override public int getSlots() { return InkMixerBlockEntity.SLOT_COUNT; }
    @Override public ItemStack getStackInSlot(int slot) { return valid(slot) ? mixer.getItem(slot) : ItemStack.EMPTY; }
    @Override public int getSlotLimit(int slot) { return valid(slot) ? 64 : 0; }
    @Override public boolean isItemValid(int slot, ItemStack stack) { return (slot == 0 || slot == 1) && mixer.canPlaceItem(slot, stack); }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!isItemValid(slot, stack) || stack.isEmpty()) return stack;
        ItemStack existing = mixer.getItem(slot);
        int max = Math.min(64, stack.getMaxStackSize());
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(existing, stack)) return stack;
            max = Math.min(max, existing.getMaxStackSize());
        }
        int room = max - existing.getCount();
        if (room <= 0) return stack;
        int move = Math.min(room, stack.getCount());
        if (!simulate) {
            if (existing.isEmpty()) mixer.setItem(slot, stack.copyWithCount(move));
            else {
                existing.grow(move);
                mixer.setChanged();
            }
        }
        if (move >= stack.getCount()) return ItemStack.EMPTY;
        ItemStack remainder = stack.copy();
        remainder.shrink(move);
        return remainder;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot != InkMixerBlockEntity.SLOT_CONTAINER_OUTPUT || amount <= 0) return ItemStack.EMPTY;
        ItemStack existing = mixer.getItem(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        int take = Math.min(amount, existing.getCount());
        ItemStack result = existing.copyWithCount(take);
        if (!simulate) mixer.removeItem(slot, take);
        return result;
    }

    private static boolean valid(int slot) { return slot >= 0 && slot < InkMixerBlockEntity.SLOT_COUNT; }
}
