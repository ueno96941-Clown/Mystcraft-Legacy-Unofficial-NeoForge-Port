package com.xcompwiz.mystcraft.api.world.logic;
import net.minecraft.world.level.Level; import net.minecraft.world.level.chunk.ChunkAccess;
public interface ITerrainAlteration {
    void alterTerrain(Level worldObj,int chunkX,int chunkZ,ChunkAccess primer);
    default IPrimerFilter getGenerationFilter(Level world,ChunkAccess primer,int chunkX,int chunkZ){return null;}
}
