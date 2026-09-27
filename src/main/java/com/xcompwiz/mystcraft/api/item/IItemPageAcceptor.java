package com.xcompwiz.mystcraft.api.item;
import net.minecraft.world.entity.player.Player; import net.minecraft.world.item.ItemStack;
public interface IItemPageAcceptor { ItemStack addPage(Player player,ItemStack itemstack,ItemStack page); }
