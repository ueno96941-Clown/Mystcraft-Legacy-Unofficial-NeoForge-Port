package com.xcompwiz.mystcraft.api.item;
import net.minecraft.world.entity.player.Player; import net.minecraft.world.item.ItemStack; import java.util.List;
public interface IItemPageProvider { List<ItemStack> getPageList(Player player, ItemStack itemstack); }
