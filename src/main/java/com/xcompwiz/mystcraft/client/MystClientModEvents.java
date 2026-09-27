package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.registry.MystMenus;
import com.xcompwiz.mystcraft.registry.MystBlockEntities;
import com.xcompwiz.mystcraft.registry.MystItems;
import com.xcompwiz.mystcraft.registry.MystEntities;
import com.xcompwiz.mystcraft.world.dimension.AgeDimensionKeys;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;


/** Client MOD-bus registration events; runtime viewport/network events stay in MystClientEvents. */
@EventBusSubscriber(modid = Mystcraft.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class MystClientModEvents {
    private MystClientModEvents() {}

    @SubscribeEvent public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent e) {
        e.register(AgeDimensionKeys.AGE_DIMENSION_TYPE_ID, new AgeDimensionSpecialEffects());
    }
    @SubscribeEvent public static void registerClientExtensions(RegisterClientExtensionsEvent e) {
        e.registerItem(new IClientItemExtensions() {
            private final PageItemRenderer renderer = new PageItemRenderer();
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() { return renderer; }
        }, MystItems.PAGE.get());
        e.registerItem(new IClientItemExtensions() {
            private final DisplayFurnitureItemRenderer renderer = new DisplayFurnitureItemRenderer();
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() { return renderer; }
        }, MystItems.BOOK_STAND_BLOCK.get(), MystItems.LECTERN_BLOCK.get());
    }

    @SubscribeEvent public static void registerScreens(RegisterMenuScreensEvent e) {
        e.register(MystMenus.BOOK.get(), LinkBookScreen::new);
        e.register(MystMenus.BOOK_BINDER.get(), BookBinderScreen::new);
        e.register(MystMenus.WRITING_DESK.get(), WritingDeskScreen::new);
        e.register(MystMenus.INK_MIXER.get(), InkMixerScreen::new);
        e.register(MystMenus.LINK_MODIFIER.get(), LinkModifierScreen::new);
        e.register(MystMenus.ARCHIVIST.get(), ArchivistShopScreen::new);
    }


    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(MystEntities.LINKBOOK.get(), LinkBookEntityRenderer::new);
        e.registerEntityRenderer(MystEntities.METEOR.get(), MeteorEntityRenderer::new);
        e.registerBlockEntityRenderer(MystBlockEntities.BOOK_DISPLAY.get(), BookDisplayRenderer::new);
        e.registerBlockEntityRenderer(MystBlockEntities.BOOK_RECEPTACLE.get(), BookReceptacleRenderer::new);
        e.registerBlockEntityRenderer(MystBlockEntities.WRITING_DESK.get(), WritingDeskRenderer::new);
        e.registerBlockEntityRenderer(MystBlockEntities.STAR_FISSURE.get(), StarFissureRenderer::new);
    }

    @SubscribeEvent public static void registerBlockColors(RegisterColorHandlersEvent.Block e) {
        e.register(AgeBlockColorHandlers::grass,
                Blocks.GRASS_BLOCK,Blocks.SHORT_GRASS,Blocks.TALL_GRASS,Blocks.FERN,Blocks.LARGE_FERN,
                Blocks.POTTED_FERN,Blocks.SUGAR_CANE);
        e.register(AgeBlockColorHandlers::foliage,
                Blocks.OAK_LEAVES,Blocks.SPRUCE_LEAVES,Blocks.BIRCH_LEAVES,Blocks.JUNGLE_LEAVES,Blocks.ACACIA_LEAVES,Blocks.DARK_OAK_LEAVES,Blocks.MANGROVE_LEAVES,Blocks.CHERRY_LEAVES,
                Blocks.VINE,Blocks.LILY_PAD,Blocks.MELON_STEM);
        e.register(AgeBlockColorHandlers::water,Blocks.WATER,Blocks.BUBBLE_COLUMN);
        e.register(CrystalPortalColorHandler::color, com.xcompwiz.mystcraft.registry.MystBlocks.LINK_PORTAL.get());
    }
}
