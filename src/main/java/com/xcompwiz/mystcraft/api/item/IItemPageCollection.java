package com.xcompwiz.mystcraft.api.item;
import net.minecraft.world.entity.player.Player; import net.minecraft.world.item.ItemStack; import java.util.List;
public interface IItemPageCollection extends IItemRenameable,IItemPageAcceptor { ItemStack remove(Player player,ItemStack itemstack,ItemStack page); List<ItemStack> getItems(Player player,ItemStack itemstack); }
