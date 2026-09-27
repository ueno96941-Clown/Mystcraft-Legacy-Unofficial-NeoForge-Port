package com.xcompwiz.mystcraft.inventory;

import com.xcompwiz.mystcraft.registry.MystItems;
import com.xcompwiz.mystcraft.registry.MystMenus;
import com.xcompwiz.mystcraft.villager.ArchivistShopData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 1.21.1 server-authoritative bridge for the legacy custom Archivist shop. */
public final class ArchivistShopMenu extends AbstractContainerMenu {
    public static final int PAGE_COUNT = 3;
    public static final int BOOSTER_INDEX = 3;
    private static final int SHOP_SLOTS = 4;
    private static final int PLAYER_START = SHOP_SLOTS;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final Villager villager;
    private final ArchivistShopData shop;
    private final SimpleContainer display = new SimpleContainer(SHOP_SLOTS);
    private int emeraldValue;
    private final int[] prices = new int[] {100, 100, 100, 20};

    public ArchivistShopMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, resolveVillager(inventory.player, buffer.readVarInt()));
    }

    public ArchivistShopMenu(int containerId, Inventory inventory, Villager villager) {
        super(MystMenus.ARCHIVIST.get(), containerId);
        this.villager = villager;
        this.shop = villager != null && !inventory.player.level().isClientSide()
                ? ArchivistShopData.open(villager)
                : null;

        // The legacy shop presented stock as display items and used separate Buy
        // buttons. The player cannot insert/remove these menu slots directly.
        for (int i = 0; i < SHOP_SLOTS; ++i) {
            final int displayIndex = i;
            // CP245 modern shop layout: four equal offer cards (booster + three pages).
            // Slots remain server-synchronized display-only inventory entries; the screen
            // draws the surrounding cards, names, prices and purchase buttons.
            int visualColumn = i == BOOSTER_INDEX ? 0 : i + 1;
            int x = 25 + visualColumn * 56;
            int y = 36;
            this.addSlot(new Slot(display, displayIndex, x, y) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) { return false; }
            });
        }

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 37 + col * 18, 128 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) addSlot(new Slot(inventory, col, 37 + col * 18, 188));

        addDataSlot(new DataSlot() {
            @Override public int get() {
                if (inventory.player instanceof ServerPlayer serverPlayer) emeraldValue = ArchivistShopData.countEmeraldValue(serverPlayer);
                return emeraldValue;
            }
            @Override public void set(int value) { emeraldValue = Math.max(0, value); }
        });
        for (int i = 0; i < SHOP_SLOTS; ++i) {
            final int index = i;
            addDataSlot(new DataSlot() {
                @Override public int get() {
                    if (shop != null) prices[index] = index == BOOSTER_INDEX ? shop.getBoosterCost() : shop.getShopItemPrice(index);
                    return prices[index];
                }
                @Override public void set(int value) { prices[index] = Math.max(0, value); }
            });
        }
        refreshDisplay();
    }

    private static Villager resolveVillager(Player player, int entityId) {
        Entity entity = player.level().getEntity(entityId);
        return entity instanceof Villager villager ? villager : null;
    }

    public int getEmeraldValue() { return emeraldValue; }
    public int getPrice(int index) { return index >= 0 && index < prices.length ? prices[index] : 0; }
    public Villager getVillager() { return villager; }
    /** Client-visible synchronized stock used by the modern Archivist offer cards. */
    public ItemStack getDisplayItem(int index) {
        return index >= 0 && index < SHOP_SLOTS ? getSlot(index).getItem() : ItemStack.EMPTY;
    }

    private void refreshDisplay() {
        if (shop == null) return;
        for (int i = 0; i < PAGE_COUNT; ++i) display.setItem(i, shop.getShopItem(i).copy());
        ItemStack boosters = new ItemStack(MystItems.BOOSTER.get(), Math.max(0, shop.getBoosterCount()));
        display.setItem(BOOSTER_INDEX, boosters);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= SHOP_SLOTS) return super.clickMenuButton(player, id);
        if (shop == null || !(player instanceof ServerPlayer serverPlayer)) return true;
        boolean purchased = id == BOOSTER_INDEX ? shop.purchaseBooster(serverPlayer) : shop.purchasePage(serverPlayer, id);
        if (purchased) {
            refreshDisplay();
            broadcastChanges();
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return villager != null && villager.isAlive() && player.distanceToSqr(villager) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < PLAYER_START || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        int inventoryIndex = index - PLAYER_START;
        if (inventoryIndex < 27) {
            if (!moveItemStackTo(source, PLAYER_START + 27, PLAYER_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(source, PLAYER_START, PLAYER_START + 27, false)) {
            return ItemStack.EMPTY;
        }
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return source.getCount() == original.getCount() ? ItemStack.EMPTY : original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (shop != null) shop.saveIfDirty();
    }
}
