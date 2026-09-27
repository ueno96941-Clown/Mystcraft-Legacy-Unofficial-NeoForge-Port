package com.xcompwiz.mystcraft.page;

import com.xcompwiz.mystcraft.linking.LinkOptions;
import com.xcompwiz.mystcraft.registry.MystItems;
import com.xcompwiz.mystcraft.symbol.LegacySymbolId;
import com.xcompwiz.mystcraft.util.LegacyItemData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 1.21.1 implementation of the legacy Mystcraft page payload.
 *
 * <p>The keys deliberately match 0.13.7.06: {@code Quality},
 * {@code linkpanel}, {@code properties}, and {@code symbol}.</p>
 */
public final class Page {
    public static final String KEY_QUALITY = "Quality";
    public static final String KEY_LINK_PANEL = "linkpanel";
    public static final String KEY_PROPERTIES = "properties";
    public static final String KEY_SYMBOL = "symbol";

    private Page() {}

    public static void setQuality(ItemStack page, String trait, int quality) {
        if (page.isEmpty()) return;
        LegacyItemData.update(page, data -> {
            CompoundTag qualities = data.contains(KEY_QUALITY, Tag.TAG_COMPOUND)
                    ? data.getCompound(KEY_QUALITY)
                    : new CompoundTag();
            qualities.putInt(trait, quality);
            data.put(KEY_QUALITY, qualities);
        });
    }

    public static int getTotalQuality(ItemStack page) {
        CompoundTag data = LegacyItemData.copy(page);
        if (!data.contains(KEY_QUALITY, Tag.TAG_COMPOUND)) return 0;
        CompoundTag qualities = data.getCompound(KEY_QUALITY);
        int total = 0;
        for (String key : qualities.getAllKeys()) {
            total += qualities.getInt(key);
        }
        return total;
    }

    public static Integer getQuality(ItemStack page, String trait) {
        CompoundTag data = LegacyItemData.copy(page);
        if (!data.contains(KEY_QUALITY, Tag.TAG_COMPOUND)) return null;
        CompoundTag qualities = data.getCompound(KEY_QUALITY);
        return qualities.contains(trait, Tag.TAG_INT) ? qualities.getInt(trait) : null;
    }

    public static boolean isBlank(ItemStack page) {
        if (page.isEmpty()) return true;
        CompoundTag data = LegacyItemData.copy(page);
        return !data.contains(KEY_LINK_PANEL, Tag.TAG_COMPOUND) && !data.contains(KEY_SYMBOL, Tag.TAG_STRING);
    }

    public static boolean isLinkPanel(ItemStack page) {
        if (page.isEmpty()) return false;
        return LegacyItemData.copy(page).contains(KEY_LINK_PANEL, Tag.TAG_COMPOUND);
    }

    public static void makeLinkPanel(ItemStack page) {
        if (page.isEmpty()) return;
        LegacyItemData.update(page, data -> {
            if (!data.contains(KEY_LINK_PANEL, Tag.TAG_COMPOUND)) {
                data.put(KEY_LINK_PANEL, new CompoundTag());
            }
        });
    }

    public static void addLinkProperty(ItemStack page, String linkProperty) {
        if (page.isEmpty()) return;
        LegacyItemData.update(page, data -> {
            CompoundTag panel = data.contains(KEY_LINK_PANEL, Tag.TAG_COMPOUND)
                    ? data.getCompound(KEY_LINK_PANEL)
                    : new CompoundTag();
            ListTag properties = panel.getList(KEY_PROPERTIES, Tag.TAG_STRING);
            properties.add(StringTag.valueOf(linkProperty));
            panel.put(KEY_PROPERTIES, properties);
            data.put(KEY_LINK_PANEL, panel);
        });
    }

    public static Collection<String> getLinkProperties(ItemStack page) {
        if (page.isEmpty()) return null;
        CompoundTag data = LegacyItemData.copy(page);
        if (!data.contains(KEY_LINK_PANEL, Tag.TAG_COMPOUND)) return null;

        CompoundTag panel = data.getCompound(KEY_LINK_PANEL);
        ListTag properties = panel.getList(KEY_PROPERTIES, Tag.TAG_STRING);
        List<String> result = new ArrayList<>(properties.size());
        for (int i = 0; i < properties.size(); i++) {
            result.add(properties.getString(i));
        }
        return result;
    }

    public static void applyLinkPanel(ItemStack linkPanel, ItemStack linkingItem) {
        Collection<String> properties = getLinkProperties(linkPanel);
        if (properties == null) return;
        for (String property : properties) {
            LinkOptions.setFlag(linkingItem, property, true);
        }
    }

    public static void setSymbol(ItemStack page, ResourceLocation symbol) {
        setSymbolId(page, symbol == null ? null : symbol.toString());
    }

    /** Stores the exact legacy symbol string, including mixed-case 1.12 paths. */
    public static void setSymbolId(ItemStack page, String symbolId) {
        if (page.isEmpty()) return;
        LegacyItemData.update(page, data -> {
            if (symbolId == null || symbolId.isBlank()) {
                data.remove(KEY_SYMBOL);
            } else {
                data.putString(KEY_SYMBOL, LegacySymbolId.qualify(symbolId));
            }
        });
    }

    /** Returns the exact persisted legacy symbol identity. */
    public static String getSymbolId(ItemStack page) {
        if (page.isEmpty()) return null;
        CompoundTag data = LegacyItemData.copy(page);
        if (!data.contains(KEY_SYMBOL, Tag.TAG_STRING)) return null;
        String id = data.getString(KEY_SYMBOL);
        return id.isBlank() ? null : LegacySymbolId.qualify(id);
    }

    /**
     * Returns the port-side canonical key for code that requires a modern
     * ResourceLocation. Do not write this key back into legacy NBT.
     */
    public static ResourceLocation getSymbol(ItemStack page) {
        return LegacySymbolId.canonicalKey(getSymbolId(page));
    }

    public static ItemStack createPage() {
        ItemStack page = new ItemStack(MystItems.PAGE.get());
        LegacyItemData.set(page, new CompoundTag());
        return page;
    }

    public static ItemStack createLinkPage() {
        ItemStack page = createPage();
        makeLinkPanel(page);
        return page;
    }

    public static ItemStack createLinkPage(String property) {
        ItemStack page = createPage();
        addLinkProperty(page, property);
        return page;
    }

    public static ItemStack createSymbolPage(ResourceLocation symbol) {
        ItemStack page = createPage();
        setSymbol(page, symbol);
        return page;
    }

    public static ItemStack createSymbolPage(String legacySymbolId) {
        ItemStack page = createPage();
        setSymbolId(page, legacySymbolId);
        return page;
    }

    public static List<String> copyLinkProperties(ItemStack page) {
        Collection<String> properties = getLinkProperties(page);
        return properties == null ? List.of() : List.copyOf(properties);
    }
}
