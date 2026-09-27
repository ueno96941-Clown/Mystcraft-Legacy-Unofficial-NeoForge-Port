package com.xcompwiz.mystcraft.api.impl.runtime;

import com.xcompwiz.mystcraft.api.util.Color;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/** Extensible part of the old InkEffects public API. */
public final class ApiLinkPropertyRegistry {
    private static final Map<String, Color> COLORS = new LinkedHashMap<>();
    private static final Map<Item, Map<String, Float>> ITEMS = new LinkedHashMap<>();
    private static final Map<String, Map<String, Float>> ITEM_NAMES = new LinkedHashMap<>();
    private ApiLinkPropertyRegistry() {}

    public static synchronized void registerProperty(String id, Color color) {
        if (id == null || id.isBlank()) return;
        COLORS.put(id, color == null ? new Color(1F,1F,1F) : color);
    }
    public static synchronized Map<String, Color> colors() { return Map.copyOf(COLORS); }
    public static synchronized Color color(String id) { return COLORS.get(id); }

    public static synchronized void add(Item item, String property, float probability) {
        if (item == null || property == null) return;
        LinkedHashMap<String,Float> map = new LinkedHashMap<>(ITEMS.getOrDefault(item, Map.of()));
        map.merge(property, probability, Float::sum);
        ITEMS.put(item, Map.copyOf(map));
    }
    public static synchronized void add(ItemStack stack, String property, float probability) {
        if (stack == null || stack.isEmpty()) return;
        add(stack.getItem(), property, probability);
    }
    public static synchronized void add(String name, String property, float probability) {
        if (name == null || name.isBlank() || property == null) return;
        ResourceLocation id = ResourceLocation.tryParse(name);
        if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
            add(BuiltInRegistries.ITEM.get(id), property, probability);
            return;
        }
        LinkedHashMap<String,Float> map = new LinkedHashMap<>(ITEM_NAMES.getOrDefault(name, Map.of()));
        map.merge(property, probability, Float::sum);
        ITEM_NAMES.put(name, Map.copyOf(map));
    }
    public static synchronized Map<String,Float> effects(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        LinkedHashMap<String,Float> out = new LinkedHashMap<>();
        var direct = ITEMS.get(stack.getItem());
        if (direct != null) out.putAll(direct);
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        var named = ITEM_NAMES.get(id.toString());
        if (named != null) named.forEach((k,v)->out.merge(k,v,Float::sum));
        return out.isEmpty() ? null : Map.copyOf(out);
    }
}
