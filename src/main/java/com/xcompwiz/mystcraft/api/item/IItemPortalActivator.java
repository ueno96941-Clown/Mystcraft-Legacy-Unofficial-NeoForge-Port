package com.xcompwiz.mystcraft.api.item;
import net.minecraft.core.BlockPos; import net.minecraft.world.entity.Entity; import net.minecraft.world.item.ItemStack; import net.minecraft.world.level.Level;
public interface IItemPortalActivator { void onPortalCollision(ItemStack stack,Level level,Entity entity,BlockPos pos); int getPortalColor(ItemStack stack,Level level); }
