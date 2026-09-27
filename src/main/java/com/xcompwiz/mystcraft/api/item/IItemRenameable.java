package com.xcompwiz.mystcraft.api.item;
import net.minecraft.world.entity.player.Player; import net.minecraft.world.item.ItemStack;
public interface IItemRenameable { String getDisplayName(Player player,ItemStack stack); void setDisplayName(Player player,ItemStack stack,String name); }
