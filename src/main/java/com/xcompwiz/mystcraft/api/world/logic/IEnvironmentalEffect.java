package com.xcompwiz.mystcraft.api.world.logic;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
/** Legacy environmental-effect hook retained for API compatibility. Built-in CP299 effects use the modern runtime adapters. */
public interface IEnvironmentalEffect { void tick(Level worldObj, LevelChunk chunk); }
