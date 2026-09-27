package com.xcompwiz.mystcraft.api.impl.runtime;

import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Mutable API-era economy overrides that do not touch persisted page identities. */
public final class ApiSymbolOverrides {
    private static final Map<String, Integer> CARD_RANK = new LinkedHashMap<>();
    private static final Map<String, Boolean> TRADABLE = new LinkedHashMap<>();
    private static final Map<String, List<ItemStack>> TRADE_ITEMS = new LinkedHashMap<>();
    private ApiSymbolOverrides() {}

    public static synchronized void setCardRank(String legacyId, int rank) { CARD_RANK.put(legacyId, rank); }
    public static synchronized Integer cardRank(String legacyId) { return CARD_RANK.get(legacyId); }
    public static synchronized void setTradable(String legacyId, boolean value) { TRADABLE.put(legacyId, value); }
    public static synchronized boolean tradable(String legacyId, boolean defaultValue) { return TRADABLE.getOrDefault(legacyId, defaultValue); }
    public static synchronized void setTradeItems(String legacyId, ItemStack primary, ItemStack secondary) {
        java.util.ArrayList<ItemStack> list = new java.util.ArrayList<>(2);
        if (primary != null && !primary.isEmpty()) list.add(primary.copy());
        if (secondary != null && !secondary.isEmpty()) list.add(secondary.copy());
        TRADE_ITEMS.put(legacyId, List.copyOf(list));
    }
    public static synchronized List<ItemStack> tradeItems(String legacyId) {
        return TRADE_ITEMS.getOrDefault(legacyId, List.of()).stream().map(ItemStack::copy).toList();
    }
}
