package com.xcompwiz.mystcraft.api.item;
import net.minecraft.resources.ResourceLocation; import net.minecraft.world.entity.player.Player; import net.minecraft.world.item.ItemStack;
public interface IItemWritable { boolean writeSymbol(Player player,ItemStack stack,ResourceLocation symbol); }
