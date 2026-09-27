package com.xcompwiz.mystcraft.registry;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.block.BlockBookBinder;
import com.xcompwiz.mystcraft.block.BlockBookDisplay;
import com.xcompwiz.mystcraft.block.BlockLinkModifier;
import com.xcompwiz.mystcraft.block.BlockInkMixer;
import com.xcompwiz.mystcraft.block.BlockWritingDesk;
import com.xcompwiz.mystcraft.block.BlockBookReceptacle;
import com.xcompwiz.mystcraft.block.BlockCrystal;
import com.xcompwiz.mystcraft.block.BlockDecay;
import com.xcompwiz.mystcraft.block.BlockLinkPortal;
import com.xcompwiz.mystcraft.block.BlockStarFissure;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Legacy block IDs from Mystcraft 0.13.7.06.
 * Blocks are replaced with functional ports incrementally while retaining IDs.
 */
public final class MystBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Mystcraft.MOD_ID);

    public static final DeferredBlock<BlockInkMixer> INK_MIXER = BLOCKS.registerBlock(
            "blockinkmixer",
            BlockInkMixer::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<BlockBookBinder> BOOK_BINDER = BLOCKS.registerBlock(
            "blockbookbinder",
            BlockBookBinder::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .noOcclusion());
    public static final DeferredBlock<BlockBookReceptacle> BOOK_RECEPTACLE = BLOCKS.registerBlock(
            "blockbookreceptacle",
            BlockBookReceptacle::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.0F)
                    .noOcclusion());
    public static final DeferredBlock<BlockBookDisplay> BOOK_STAND = BLOCKS.registerBlock(
            "blockbookstand", BlockBookDisplay::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F).noOcclusion());
    public static final DeferredBlock<BlockBookDisplay> LECTERN = BLOCKS.registerBlock(
            "blocklectern", BlockBookDisplay::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).noOcclusion());
    public static final DeferredBlock<BlockLinkModifier> LINK_MODIFIER = BLOCKS.registerBlock(
            "blocklinkmodifier", BlockLinkModifier::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F).noOcclusion());
    public static final DeferredBlock<BlockDecay> DECAY = BLOCKS.register("blockdecay", () -> new BlockDecay());
    public static final DeferredBlock<BlockCrystal> CRYSTAL = BLOCKS.registerBlock(
            "blockcrystal",
            BlockCrystal::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(1.0F)
                    // Legacy Crystal uses the same brittle/glassy break character as Glowstone.
                    // In modern Minecraft Glowstone is backed by the glass sound set.
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .lightLevel(state -> 8));
    public static final DeferredBlock<BlockLinkPortal> LINK_PORTAL = BLOCKS.registerBlock(
            "linkportal",
            BlockLinkPortal::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .noCollission()
                    .noOcclusion()
                    .strength(-1.0F, 3600000.0F)
                    .lightLevel(state -> 12));
    public static final DeferredBlock<BlockWritingDesk> WRITING_DESK = BLOCKS.registerBlock(
            "writingdesk",
            BlockWritingDesk::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<BlockStarFissure> STAR_FISSURE = BLOCKS.registerBlock(
            "blockstarfissure",
            BlockStarFissure::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .noCollission()
                    .noOcclusion()
                    .strength(-1.0F, 3600000.0F)
                    .lightLevel(state -> 6)
                    .noLootTable());
    public static final DeferredBlock<LiquidBlock> BLACK_INK = BLOCKS.register(
            "fluidblockblackink",
            () -> new LiquidBlock(MystFluids.BLACK_INK.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .noCollission()
                    .strength(100.0F)
                    .noLootTable()));

    private MystBlocks() {}

    private static DeferredBlock<Block> simple(String id, MapColor color) {
        return BLOCKS.registerSimpleBlock(id, BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(2.0F));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
