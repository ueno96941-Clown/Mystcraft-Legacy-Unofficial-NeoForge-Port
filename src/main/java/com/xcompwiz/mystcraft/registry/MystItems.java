package com.xcompwiz.mystcraft.registry;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.item.ItemAgebook;
import com.xcompwiz.mystcraft.item.ItemBoosterPack;
import com.xcompwiz.mystcraft.item.ItemFolder;
import com.xcompwiz.mystcraft.item.ItemInkVial;
import com.xcompwiz.mystcraft.item.ItemLinkbook;
import com.xcompwiz.mystcraft.item.ItemLinkbookUnlinked;
import com.xcompwiz.mystcraft.item.ItemPage;
import com.xcompwiz.mystcraft.item.ItemPortfolio;
import com.xcompwiz.mystcraft.item.ItemWritingDesk;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Legacy item identifiers, now incrementally replaced by behavior-bearing
 * 1.21.1 implementations.
 */
public final class MystItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Mystcraft.MOD_ID);

    // Stage 1: real Page / Linking Book data behavior.
    public static final DeferredItem<ItemPage> PAGE = ITEMS.registerItem("page", ItemPage::new);
    public static final DeferredItem<ItemAgebook> AGEBOOK = ITEMS.registerItem(
            "agebook", ItemAgebook::new, new Item.Properties().durability(15).rarity(Rarity.EPIC));
    public static final DeferredItem<ItemLinkbook> LINKBOOK = ITEMS.registerItem(
            "linkbook", ItemLinkbook::new, new Item.Properties().durability(15).rarity(Rarity.RARE));
    public static final DeferredItem<ItemLinkbookUnlinked> UNLINKED_BOOK = ITEMS.registerItem(
            "unlinkedbook", ItemLinkbookUnlinked::new, new Item.Properties().stacksTo(16));

    // Peripheral legacy identities. Booster, Folder and Portfolio now carry their
    // gameplay behavior; glasses remain a legacy developer-only identity and are not exposed in Creative.
    public static final DeferredItem<ItemBoosterPack> BOOSTER = ITEMS.registerItem("booster", ItemBoosterPack::new);
    public static final DeferredItem<ItemFolder> FOLDER = ITEMS.registerItem(
            "folder", ItemFolder::new,
            new Item.Properties().stacksTo(1).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
    public static final DeferredItem<ItemPortfolio> PORTFOLIO = ITEMS.registerItem(
            "portfolio", ItemPortfolio::new,
            new Item.Properties().stacksTo(1).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
    public static final DeferredItem<ItemWritingDesk> WRITING_DESK_ITEM = ITEMS.registerItem(
            "writingdesk", properties -> new ItemWritingDesk(MystBlocks.WRITING_DESK.get(), properties),
            new Item.Properties().stacksTo(1));
    public static final DeferredItem<ItemInkVial> INK_VIAL = ITEMS.registerItem(
            "vial", ItemInkVial::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> GLASSES = ITEMS.registerSimpleItem("glasses", new Item.Properties().stacksTo(1));

    // Legacy ItemBlock identities. The Writing Desk keeps its historical item ID (mystcraft:writingdesk)
    // while using a normal 1.21.1 BlockItem implementation. Black ink still has no public BlockItem.
    public static final DeferredItem<BlockItem> INK_MIXER_BLOCK = ITEMS.registerSimpleBlockItem("blockinkmixer", MystBlocks.INK_MIXER);
    public static final DeferredItem<BlockItem> BOOK_BINDER_BLOCK = ITEMS.registerSimpleBlockItem("blockbookbinder", MystBlocks.BOOK_BINDER);
    public static final DeferredItem<BlockItem> BOOK_RECEPTACLE_BLOCK = ITEMS.registerSimpleBlockItem("blockbookreceptacle", MystBlocks.BOOK_RECEPTACLE);
    public static final DeferredItem<BlockItem> BOOK_STAND_BLOCK = ITEMS.registerSimpleBlockItem("blockbookstand", MystBlocks.BOOK_STAND);
    public static final DeferredItem<BlockItem> LECTERN_BLOCK = ITEMS.registerSimpleBlockItem("blocklectern", MystBlocks.LECTERN);
    public static final DeferredItem<BlockItem> LINK_MODIFIER_BLOCK = ITEMS.registerSimpleBlockItem("blocklinkmodifier", MystBlocks.LINK_MODIFIER);
    public static final DeferredItem<BlockItem> CRYSTAL_BLOCK = ITEMS.registerSimpleBlockItem("blockcrystal", MystBlocks.CRYSTAL);
    public static final DeferredItem<BlockItem> LINK_PORTAL_BLOCK = ITEMS.registerSimpleBlockItem("linkportal", MystBlocks.LINK_PORTAL);
    public static final DeferredItem<BlockItem> STAR_FISSURE_BLOCK = ITEMS.registerSimpleBlockItem("blockstarfissure", MystBlocks.STAR_FISSURE);

    private MystItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
