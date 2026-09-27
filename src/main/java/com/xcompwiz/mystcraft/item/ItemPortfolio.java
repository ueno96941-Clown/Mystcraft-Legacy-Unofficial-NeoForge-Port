package com.xcompwiz.mystcraft.item;

import com.xcompwiz.mystcraft.api.item.IItemPageCollection;
import com.xcompwiz.mystcraft.api.item.IItemOrderablePageProvider;
import com.xcompwiz.mystcraft.page.Page;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** High-capacity portable page collection. */
public final class ItemPortfolio extends ItemPageContainer implements IItemPageCollection {
    public static final int PAGE_CAPACITY = 256;

    public ItemPortfolio(Properties properties) { super(properties, PAGE_CAPACITY); }

    @Override public String getDisplayName(Player player, ItemStack stack) { String name=getLegacyName(stack); return name.isEmpty()?null:name; }
    @Override public void setDisplayName(Player player, ItemStack stack, String name) { setLegacyName(stack,name); }

    @Override
    public ItemStack remove(Player player, ItemStack itemstack, ItemStack page) {
        if (page == null || page.isEmpty()) return ItemStack.EMPTY;
        List<ItemStack> pages = new ArrayList<>(getPages(itemstack));
        int requested = page.getCount();
        int removed = 0;
        ItemStack unit = page.copyWithCount(1);
        for (int i=pages.size()-1; i>=0 && removed<requested; --i) {
            if (ItemStack.isSameItemSameComponents(unit,pages.get(i))) { pages.remove(i); removed++; }
        }
        if (removed == 0) return ItemStack.EMPTY;
        setPages(itemstack,pages);
        return unit.copyWithCount(removed);
    }

    @Override
    public ItemStack addPage(Player player, ItemStack itemstack, ItemStack page) {
        if (page == null || page.isEmpty()) return page;
        if (page.getItem() instanceof IItemPageCollection other && page.getCount()==1) {
            for (ItemStack candidate : new ArrayList<>(other.getItems(player,page))) {
                ItemStack moved = other.remove(player,page,candidate.copy());
                ItemStack rest = addPage(player,itemstack,moved);
                if (!rest.isEmpty()) other.addPage(player,page,rest);
            }
            return page;
        }
        if (page.getItem() instanceof IItemOrderablePageProvider other && page.getCount()==1) {
            List<ItemStack> candidates = new ArrayList<>(other.getPageList(player,page));
            for (int i=candidates.size()-1; i>=0; --i) {
                ItemStack moved = other.removePage(player,page,i);
                ItemStack rest = addPage(player,itemstack,moved);
                if (!rest.isEmpty()) other.addPage(player,page,rest);
            }
            return page;
        }
        if (!Page.isBlank(page) && !Page.isLinkPanel(page) && Page.getSymbolId(page)==null) return page;
        insert(itemstack,page);
        return page.isEmpty()?ItemStack.EMPTY:page;
    }

    @Override public List<ItemStack> getItems(Player player, ItemStack itemstack) { return getPages(itemstack); }
}
