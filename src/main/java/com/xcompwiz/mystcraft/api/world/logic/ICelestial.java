package com.xcompwiz.mystcraft.api.world.logic;
import net.minecraft.client.renderer.texture.TextureManager; import net.minecraft.world.level.Level;
public interface ICelestial { boolean providesLight(); float getAltitudeAngle(long time,float partialTime); Long getTimeToDawn(long time); void render(TextureManager textureManager,Level worldObj,float partialTicks); }
