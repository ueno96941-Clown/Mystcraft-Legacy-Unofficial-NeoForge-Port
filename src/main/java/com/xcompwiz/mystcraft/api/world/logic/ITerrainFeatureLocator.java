package com.xcompwiz.mystcraft.api.world.logic;
import net.minecraft.core.BlockPos; import net.minecraft.world.level.Level;
public interface ITerrainFeatureLocator { BlockPos locate(Level world,String identifier,BlockPos pos,boolean genChunks); default boolean isInsideFeature(Level world,String identifier,BlockPos pos){return false;} }
