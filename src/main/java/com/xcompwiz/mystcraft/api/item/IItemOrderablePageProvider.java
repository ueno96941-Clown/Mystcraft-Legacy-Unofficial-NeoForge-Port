package com.xcompwiz.mystcraft.api.item;
import net.minecraft.world.entity.player.Player; import net.minecraft.world.item.ItemStack;
public interface IItemOrderablePageProvider extends IItemPageProvider,IItemPageAcceptor { ItemStack setPage(Player player,ItemStack folder,ItemStack page,int index); ItemStack removePage(Player player,ItemStack folder,int index); }
