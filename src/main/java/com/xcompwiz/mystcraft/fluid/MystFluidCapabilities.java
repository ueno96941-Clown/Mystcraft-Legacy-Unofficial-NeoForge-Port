package com.xcompwiz.mystcraft.fluid;

import com.xcompwiz.mystcraft.registry.MystItems;
import com.xcompwiz.mystcraft.registry.MystBlockEntities;
import com.xcompwiz.mystcraft.inventory.InkMixerItemHandler;
import com.xcompwiz.mystcraft.inventory.WritingDeskItemHandler;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Registers modern NeoForge capability views for legacy Mystcraft fluid containers. */
public final class MystFluidCapabilities {
    private MystFluidCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> new InkVialFluidHandler(stack),
                MystItems.INK_VIAL.get());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                MystBlockEntities.WRITING_DESK.get(),
                (desk, side) -> new WritingDeskInkHandler(desk));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                MystBlockEntities.WRITING_DESK.get(),
                (desk, side) -> new WritingDeskItemHandler(desk));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                MystBlockEntities.INK_MIXER.get(),
                (mixer, side) -> new InkMixerItemHandler(mixer));
    }
}
