package com.xcompwiz.mystcraft.item;

import com.xcompwiz.mystcraft.api.item.IItemOrderablePageProvider;
import com.xcompwiz.mystcraft.api.item.IItemRenameable;
import com.xcompwiz.mystcraft.api.item.IItemWritable;
import com.xcompwiz.mystcraft.page.Page;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Ordered portable page storage. */
public final class ItemFolder extends ItemPageContainer implements IItemRenameable, IItemOrderablePageProvider, IItemWritable {
    public static final int PAGE_CAPACITY = 64;

    public ItemFolder(Properties properties) { super(properties, PAGE_CAPACITY); }

    @Override protected boolean preserveSparseSlots() { return true; }

    @Override public String getDisplayName(Player player, ItemStack stack) { String name=getLegacyName(stack); return name.isEmpty()?null:name; }
    @Override public void setDisplayName(Player player, ItemStack stack, String name) { setLegacyName(stack,name); }

    @Override
    public boolean writeSymbol(Player player, ItemStack stack, ResourceLocation symbol) {
        if (symbol == null) return false;
        List<ItemStack> pages = new ArrayList<>(getPages(stack));
        for (ItemStack page : pages) {
            if (Page.isBlank(page)) {
                Page.setSymbolId(page, symbol.toString());
                setPages(stack, pages);
                return true;
            }
        }
        return false;
    }

    @Override public ItemStack removePage(Player player, ItemStack folder, int index) { return removeAt(folder,index); }
    @Override public List<ItemStack> getPageList(Player player, ItemStack folder) { return getPages(folder); }
    @Override public ItemStack setPage(Player player, ItemStack folder, ItemStack page, int index) { return setAtLegacy(folder,page,index); }
    @Override public ItemStack addPage(Player player, ItemStack folder, ItemStack page) { insert(folder,page); return page.isEmpty()?ItemStack.EMPTY:page; }
}
