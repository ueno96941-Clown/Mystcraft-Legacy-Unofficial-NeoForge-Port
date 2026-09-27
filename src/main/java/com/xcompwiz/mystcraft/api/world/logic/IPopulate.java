package com.xcompwiz.mystcraft.api.world.logic;
import java.util.Random; import net.minecraft.world.level.Level;
public interface IPopulate { boolean populate(Level worldObj, Random rand, int x, int y, boolean flag); }
