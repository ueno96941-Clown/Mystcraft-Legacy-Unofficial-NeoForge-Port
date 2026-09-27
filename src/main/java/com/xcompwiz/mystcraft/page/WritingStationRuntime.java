package com.xcompwiz.mystcraft.page;

import com.xcompwiz.mystcraft.registry.MystItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Small server-side helpers shared by the modern Writing Desk and Ink Mixer.
 *
 * <p>Shared inventory helpers for the modern interaction layer.  The Writing Desk
 * itself now owns the legacy 1000 mB inkwell; {@link #consumeInk(Player)} remains
 * for the Ink Mixer, whose old basin consumed one full vial per Link Panel.</p>
 */
public final class WritingStationRuntime {
    private WritingStationRuntime() {}

    public static boolean hasInk(Player player) {
        return player.getAbilities().instabuild || findSlot(player.getInventory(), MystItems.INK_VIAL.get()) >= 0;
    }

    public static boolean hasPaper(Player player) {
        return player.getAbilities().instabuild || findSlot(player.getInventory(), Items.PAPER) >= 0;
    }

    public static boolean consumeInk(Player player) {
        if (player.getAbilities().instabuild) return true;
        Inventory inventory = player.getInventory();
        int slot = findSlot(inventory, MystItems.INK_VIAL.get());
        if (slot < 0) return false;
        ItemStack vial = inventory.getItem(slot);
        vial.shrink(1);
        if (vial.isEmpty()) inventory.setItem(slot, ItemStack.EMPTY);
        inventory.placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE));
        return true;
    }

    public static boolean consumePaper(Player player) {
        if (player.getAbilities().instabuild) return true;
        Inventory inventory = player.getInventory();
        int slot = findSlot(inventory, Items.PAPER);
        if (slot < 0) return false;
        ItemStack paper = inventory.getItem(slot);
        paper.shrink(1);
        if (paper.isEmpty()) inventory.setItem(slot, ItemStack.EMPTY);
        return true;
    }

    public static void give(Player player, ItemStack result) {
        player.getInventory().placeItemBackInInventory(result);
    }

    public static void message(Player player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
    }

    private static int findSlot(Inventory inventory, Item item) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) return slot;
        }
        return -1;
    }
}
