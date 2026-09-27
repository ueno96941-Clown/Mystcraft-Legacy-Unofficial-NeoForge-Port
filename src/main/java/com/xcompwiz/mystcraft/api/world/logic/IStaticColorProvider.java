package com.xcompwiz.mystcraft.api.world.logic;
import com.xcompwiz.mystcraft.api.util.Color; import net.minecraft.core.BlockPos; import net.minecraft.world.level.Level; import net.minecraft.world.level.biome.Biome;
public interface IStaticColorProvider { String FOLIAGE="foliage",GRASS="grass",WATER="water"; Color getStaticColor(Level worldObj,Biome biome,BlockPos pos); }
