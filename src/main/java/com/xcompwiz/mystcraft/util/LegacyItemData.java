package com.xcompwiz.mystcraft.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * Compatibility bridge for the old 1.12.2 per-stack NBT payloads.
 *
 * <p>Minecraft 1.21.1 stores arbitrary stack NBT in the CUSTOM_DATA data
 * component. Mystcraft's legacy key names are intentionally retained inside
 * that component so later migration code has a stable target.</p>
 */
public final class LegacyItemData {
    private LegacyItemData() {}

    public static boolean hasData(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && !data.isEmpty();
    }

    public static CompoundTag copy(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    public static void set(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
    }

    public static void update(ItemStack stack, Consumer<CompoundTag> mutator) {
        CompoundTag tag = copy(stack);
        mutator.accept(tag);
        set(stack, tag);
    }
}
