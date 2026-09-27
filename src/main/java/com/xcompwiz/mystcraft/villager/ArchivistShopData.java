package com.xcompwiz.mystcraft.villager;

import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.registry.MystItems;
import com.xcompwiz.mystcraft.symbol.SymbolItemEconomy;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Persistent 0.13.7.06 custom-Archivist stock simulation.
 *
 * <p>Stock is stored under the same conceptual Mystcraft/Trade subtree used by
 * the 1.12 entity data.  Three page stacks correspond to minimum card ranks
 * 1/2/3 and start at three copies. Booster stock starts at five. Every 12000
 * game ticks one legacy restock roll is simulated; unloaded time is caught up
 * when the shop is next opened.</p>
 */
public final class ArchivistShopData {
    private static final String ROOT = "Mystcraft";
    private static final String TRADE = "Trade";
    private static final String INVENTORY = "Inventory";
    private static final String BOOSTER_COUNT = "boostercount";
    private static final String LAST_RESTOCK = "lastrestock";
    private static final long STEP_SIZE = 12000L;

    private final Villager villager;
    private final ItemStack[] pageItems = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private int boosterCount = 5;
    private long lastRestock;
    private boolean dirty;

    private ArchivistShopData(Villager villager) {
        this.villager = villager;
        load();
        for (int i = 0; i < pageItems.length; ++i) getShopItem(i);
        simulate();
        saveIfDirty();
    }

    public static ArchivistShopData open(Villager villager) {
        return new ArchivistShopData(villager);
    }

    public ItemStack getShopItem(int index) {
        if (index < 0 || index >= pageItems.length) return ItemStack.EMPTY;
        if (pageItems[index].isEmpty() && !villager.level().isClientSide()) {
            SymbolItemEconomy.RankedSymbol selected = SymbolItemEconomy.chooseAtLeastRank(villager.getRandom(), index + 1);
            if (selected != null) {
                ItemStack page = Page.createSymbolPage(selected.legacyId());
                page.setCount(3);
                pageItems[index] = page;
                dirty = true;
            }
        }
        return pageItems[index];
    }

    public int getShopItemPrice(int index) {
        String symbol = Page.getSymbolId(getShopItem(index));
        return symbol == null ? 100 : SymbolItemEconomy.archivistPrice(symbol);
    }

    public int getBoosterCount() { return boosterCount; }
    public int getBoosterCost() { return 20; }

    public void simulate() {
        long now = villager.level().getGameTime();
        long delta = Math.max(0L, now - lastRestock);
        // Preserve legacy elapsed-time simulation but put a defensive ceiling on
        // pathological imported timestamps. Stock saturates long before this cap.
        int guard = 0;
        while (delta >= STEP_SIZE && guard++ < 100_000) {
            delta -= STEP_SIZE;
            lastRestock += STEP_SIZE;
            restock(villager.getRandom());
        }
        if (lastRestock == 0L && now < STEP_SIZE) {
            // Fresh early-world villagers retain the old zero epoch behavior.
        }
    }

    private void restock(RandomSource random) {
        // nonechance=1, boosterchance=3, pageitems.length=3 => seven outcomes.
        int roll = random.nextInt(7) - 1;
        if (roll < 0) return;
        roll -= 3;
        if (roll < 0) {
            if (boosterCount < 8) {
                ++boosterCount;
                dirty = true;
            }
            return;
        }
        if (roll >= 0 && roll < pageItems.length) {
            ItemStack stack = pageItems[roll];
            if (!stack.isEmpty() && stack.getCount() < 5) stack.grow(1);
            dirty = true; // Legacy markUpdated even when the page was empty/full.
        }
    }

    public boolean purchasePage(ServerPlayer player, int index) {
        ItemStack stock = getShopItem(index);
        if (stock.isEmpty()) return false;
        int price = getShopItemPrice(index);
        ItemStack result = stock.copyWithCount(1);
        if (!purchase(player, result, price)) return false;
        stock.shrink(1);
        if (stock.isEmpty()) pageItems[index] = ItemStack.EMPTY;
        dirty = true;
        saveIfDirty();
        return true;
    }

    public boolean purchaseBooster(ServerPlayer player) {
        if (boosterCount <= 0) return false;
        if (!purchase(player, new ItemStack(MystItems.BOOSTER.get()), getBoosterCost())) return false;
        --boosterCount;
        dirty = true;
        saveIfDirty();
        return true;
    }

    private boolean purchase(ServerPlayer player, ItemStack result, int price) {
        if (countEmeraldValue(player) < price) return false;
        if (!player.getInventory().add(result.copy())) return false;
        if (!deductEmeraldValue(player, price)) {
            removeOneMatching(player, result);
            return false;
        }
        return true;
    }

    public static int countEmeraldValue(ServerPlayer player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(Items.EMERALD)) total += stack.getCount();
            else if (stack.is(Items.EMERALD_BLOCK)) total += 9 * stack.getCount();
        }
        return total;
    }

    /**
     * Equivalent monetary behavior to the old emerald/block change system:
     * loose emeralds are used first, then blocks are broken and any remainder is
     * returned as emerald change.  Failure to store change aborts safely.
     */
    private static boolean deductEmeraldValue(ServerPlayer player, int price) {
        if (price <= 0) return true;
        int loose = countItem(player, Items.EMERALD);
        int useLoose = Math.min(loose, price);
        removeItem(player, Items.EMERALD, useLoose);
        int remaining = price - useLoose;
        if (remaining <= 0) return true;

        int blocks = (remaining + 8) / 9;
        if (countItem(player, Items.EMERALD_BLOCK) < blocks) {
            // Restore the loose emeralds already taken.
            if (useLoose > 0) player.getInventory().placeItemBackInInventory(new ItemStack(Items.EMERALD, useLoose));
            return false;
        }
        removeItem(player, Items.EMERALD_BLOCK, blocks);
        int change = blocks * 9 - remaining;
        if (change > 0) player.getInventory().placeItemBackInInventory(new ItemStack(Items.EMERALD, change));
        return true;
    }

    private static int countItem(ServerPlayer player, net.minecraft.world.item.Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) if (stack.is(item)) total += stack.getCount();
        return total;
    }

    private static void removeItem(ServerPlayer player, net.minecraft.world.item.Item item, int amount) {
        for (int i = 0; i < player.getInventory().getContainerSize() && amount > 0; ++i) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(item)) continue;
            int take = Math.min(amount, stack.getCount());
            stack.shrink(take);
            amount -= take;
            if (stack.isEmpty()) player.getInventory().setItem(i, ItemStack.EMPTY);
        }
    }

    private static void removeOneMatching(ServerPlayer player, ItemStack match) {
        for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, match)) {
                stack.shrink(1);
                if (stack.isEmpty()) player.getInventory().setItem(i, ItemStack.EMPTY);
                return;
            }
        }
    }

    private void load() {
        CompoundTag persistent = villager.getPersistentData();
        CompoundTag root = persistent.contains(ROOT, Tag.TAG_COMPOUND) ? persistent.getCompound(ROOT) : new CompoundTag();
        CompoundTag trade = root.contains(TRADE, Tag.TAG_COMPOUND) ? root.getCompound(TRADE) : new CompoundTag();
        HolderLookup.Provider registries = villager.registryAccess();

        ListTag items = trade.getList(INVENTORY, Tag.TAG_COMPOUND);
        for (int i = 0; i < pageItems.length && i < items.size(); ++i) {
            pageItems[i] = ItemStack.parseOptional(registries, items.getCompound(i));
        }
        if (trade.contains(BOOSTER_COUNT, Tag.TAG_INT)) boosterCount = trade.getInt(BOOSTER_COUNT);
        if (trade.contains(LAST_RESTOCK, Tag.TAG_LONG)) lastRestock = trade.getLong(LAST_RESTOCK);
    }

    public void saveIfDirty() {
        if (!dirty || villager.level().isClientSide()) return;
        CompoundTag persistent = villager.getPersistentData();
        CompoundTag root = persistent.contains(ROOT, Tag.TAG_COMPOUND) ? persistent.getCompound(ROOT) : new CompoundTag();
        CompoundTag trade = new CompoundTag();
        ListTag items = new ListTag();
        HolderLookup.Provider registries = villager.registryAccess();
        for (ItemStack page : pageItems) items.add(page.saveOptional(registries));
        trade.put(INVENTORY, items);
        trade.putInt(BOOSTER_COUNT, boosterCount);
        trade.putLong(LAST_RESTOCK, lastRestock);
        root.put(TRADE, trade);
        persistent.put(ROOT, root);
        dirty = false;
    }
}
