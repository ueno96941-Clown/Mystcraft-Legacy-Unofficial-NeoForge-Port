package com.xcompwiz.mystcraft.api.hook;
import net.minecraft.resources.ResourceLocation; import net.minecraft.world.item.ItemStack;
public interface ItemFactory { ItemStack buildPage(); ItemStack buildSymbolPage(ResourceLocation identifier); ItemStack buildLinkPage(String... properties); ItemStack buildCollectionItem(String name,ResourceLocation... tokens); ItemStack buildCollectionItem(String name,ItemStack... pages); }
