package com.xcompwiz.mystcraft.data;

import com.xcompwiz.mystcraft.linking.LinkProperties;
import com.xcompwiz.mystcraft.api.impl.runtime.ApiLinkPropertyRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 1.21.1 reconstruction of the 0.13.7.06 Ink Mixer catalyst table. */
public final class InkEffects {
    private record TagBinding(List<TagKey<Item>> tags, Map<String, Float> effects) {}

    private static final Map<Item, Map<String, Float>> ITEM_EFFECTS = new LinkedHashMap<>();
    private static final List<TagBinding> TAG_EFFECTS;
    private static final Map<String, Integer> PROPERTY_COLORS = Map.of(
            LinkProperties.INTRA_LINKING, 0x00FF00,
            LinkProperties.INTRA_LINKING_ONLY, 0xFFFFFF,
            LinkProperties.GENERATE_PLATFORM, 0x808080,
            LinkProperties.MAINTAIN_MOMENTUM, 0x0000FF,
            LinkProperties.DISARM, 0xFF0000,
            LinkProperties.RELATIVE, 0x990099
    );

    static {
        add(Items.GUNPOWDER, LinkProperties.DISARM, 0.20F);
        add(Items.MUSHROOM_STEW, LinkProperties.DISARM, 0.05F);
        add(Items.CLAY_BALL, LinkProperties.GENERATE_PLATFORM, 0.25F);
        add(Items.EXPERIENCE_BOTTLE, LinkProperties.INTRA_LINKING, 0.15F);
        add(Items.ENDER_PEARL, LinkProperties.INTRA_LINKING, 0.15F);
        add(Items.ENDER_PEARL, LinkProperties.DISARM, 0.15F);
        add(Items.FEATHER, LinkProperties.MAINTAIN_MOMENTUM, 0.15F);
        add(Items.FIRE_CHARGE, LinkProperties.DISARM, 0.25F);

        TAG_EFFECTS = List.of(
                tag("dyes/black", effects("", 0.50F)),
                tag("dusts/brass", effects(LinkProperties.DISARM, 0.15F)),
                tag("dusts/bronze", effects(LinkProperties.DISARM, 0.15F)),
                tag("dusts/tin", effects(LinkProperties.GENERATE_PLATFORM, 0.10F, LinkProperties.INTRA_LINKING, 0.10F)),
                tag("dusts/iron", effects(LinkProperties.GENERATE_PLATFORM, 0.15F, LinkProperties.INTRA_LINKING, 0.15F)),
                tag("dusts/lead", effects(LinkProperties.DISARM, 0.20F, LinkProperties.INTRA_LINKING, 0.20F)),
                tag("dusts/silver", effects(LinkProperties.GENERATE_PLATFORM, 0.20F, LinkProperties.INTRA_LINKING, 0.20F)),
                tag("dusts/diamond", effects(LinkProperties.INTRA_LINKING, 0.25F,
                        LinkProperties.MAINTAIN_MOMENTUM, 0.10F, LinkProperties.GENERATE_PLATFORM, 0.10F)),
                tag("dusts/gold", effects(LinkProperties.INTRA_LINKING, 0.25F,
                        LinkProperties.GENERATE_PLATFORM, 0.10F, LinkProperties.DISARM, 0.10F))
        );
    }

    private InkEffects() {}

    private static void add(Item item, String property, float probability) {
        Map<String, Float> current = new LinkedHashMap<>(ITEM_EFFECTS.getOrDefault(item, Map.of()));
        current.merge(property, probability, Float::sum);
        ITEM_EFFECTS.put(item, Collections.unmodifiableMap(current));
    }

    private static Map<String, Float> effects(Object... pairs) {
        Map<String, Float> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) map.put((String) pairs[i], (Float) pairs[i + 1]);
        return Collections.unmodifiableMap(map);
    }

    private static TagBinding tag(String path, Map<String, Float> effects) {
        TagKey<Item> common = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
        TagKey<Item> forge = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", path));
        return new TagBinding(List.of(common, forge), effects);
    }

    public static Map<String, Float> getItemEffects(ItemStack stack) {
        if (stack.isEmpty()) return null;
        LinkedHashMap<String, Float> result = new LinkedHashMap<>();
        Map<String, Float> exact = ITEM_EFFECTS.get(stack.getItem());
        if (exact != null) result.putAll(exact);
        if (exact == null) {
            outer: for (TagBinding binding : TAG_EFFECTS) {
                for (TagKey<Item> tag : binding.tags()) if (stack.is(tag)) {
                    result.putAll(binding.effects());
                    break outer;
                }
            }
        }
        Map<String,Float> custom = ApiLinkPropertyRegistry.effects(stack);
        if (custom != null) custom.forEach((key,value) -> result.merge(key,value,Float::sum));
        return result.isEmpty() ? null : Collections.unmodifiableMap(result);
    }


    /** All properties exposed by the legacy Link Modifier, including API registrations. */
    public static List<String> getProperties() {
        java.util.LinkedHashSet<String> properties = new java.util.LinkedHashSet<>(LinkProperties.LEGACY_INK_PROPERTIES);
        properties.addAll(ApiLinkPropertyRegistry.colors().keySet());
        return List.copyOf(properties);
    }

    public static int getPropertyColor(String property) {
        var custom = ApiLinkPropertyRegistry.color(property);
        return custom == null ? PROPERTY_COLORS.getOrDefault(property, 0xFFFFFF) : custom.asInt();
    }
}
