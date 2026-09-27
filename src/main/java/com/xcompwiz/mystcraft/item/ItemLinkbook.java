package com.xcompwiz.mystcraft.item;

import com.xcompwiz.mystcraft.linking.LinkOptions;
import com.xcompwiz.mystcraft.api.item.IItemRenameable;
import com.xcompwiz.mystcraft.api.item.IItemPageProvider;
import com.xcompwiz.mystcraft.page.Page;
import net.minecraft.world.entity.player.Player;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ItemLinkbook extends ItemLinking implements IItemRenameable, IItemPageProvider {
    public ItemLinkbook(Properties properties) {
        super(properties);
    }

    @Override
    protected void initialize(Level level, ItemStack stack, Entity entity) {
        if (level != null) {
            LinkOptions.capturePosition(stack, level, entity);
        }
    }

    @Override
    public String getDisplayName(Player player, ItemStack stack) { return LinkOptions.getDisplayName(stack); }

    @Override
    public void setDisplayName(Player player, ItemStack stack, String name) { LinkOptions.setDisplayName(stack, name == null ? "" : name); }

    @Override
    public List<ItemStack> getPageList(Player player, ItemStack stack) { return List.of(Page.createLinkPage()); }

    public void initializeAt(Level level, ItemStack stack, Entity entity) {
        initialize(level, stack, entity);
    }
}
