package com.xcompwiz.mystcraft.api.world.logic;
import net.minecraft.world.level.biome.Biome; import java.util.List;
public interface IBiomeController { List<Biome> getValidSpawnBiomes(); Biome getBiomeAtCoords(int x,int z); Biome[] getBiomesAtCoords(Biome[] reuse,int x,int z,int xSize,int zSize,boolean useCache); Biome[] getBiomesForGeneration(Biome[] reuse,int x,int z,int xSize,int zSize); void cleanupCache(); }
