package com.xcompwiz.mystcraft.api.world;
import com.xcompwiz.mystcraft.api.util.ColorGradient;
import com.xcompwiz.mystcraft.api.world.logic.*;
import net.minecraft.world.level.biome.BiomeSource;
public interface AgeDirector {
    void setModifier(String id,Modifier obj); void setModifier(String id,Object obj); Modifier popModifier(String id); void clearModifiers();
    long getTime(); int getInstabilityScore(); float getCloudHeight(); double getHorizon(); int getAverageGroundLevel(); int getSeaLevel(); long getSeed(); BiomeSource getBiomeProvider(); ColorGradient getSunriseSunsetColor();
    /** Retained for source compatibility; project policy forces the effective runtime value to zero. */ void addInstability(int instability);
    void setCloudHeight(float height); void setHorizon(double height); void setAverageGroundLevel(int height); void setSeaLevel(int height); void setDrawHorizon(boolean flag); void setDrawVoid(boolean flag); void setPvPEnabled(boolean flag);
    void registerInterface(IBiomeController reg); void registerInterface(ITerrainGenerator reg); void registerInterface(ILightingController reg); void registerInterface(IWeatherController reg); void registerInterface(ICelestial reg); void registerInterface(ITerrainAlteration reg); void registerInterface(IChunkProviderFinalization reg); void registerInterface(IPopulate reg); void registerInterface(ITerrainFeatureLocator reg); void registerInterface(ISpawnModifier reg); void registerInterface(IDynamicColorProvider reg,String type); void registerInterface(IStaticColorProvider reg,String type); void registerInterface(IEnvironmentalEffect reg);
}
