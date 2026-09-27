package com.xcompwiz.mystcraft.api.world.logic;
import net.minecraft.world.level.chunk.ChunkAccess;
public interface ITerrainGenerator { void generateTerrain(int chunkX,int chunkZ,ChunkAccess primer); }
