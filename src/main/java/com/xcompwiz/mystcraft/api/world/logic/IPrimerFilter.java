package com.xcompwiz.mystcraft.api.world.logic;
import net.minecraft.world.level.block.state.BlockState;
/** Chunk-generation block-state filter. Return null to suppress the proposed state. */
public interface IPrimerFilter { BlockState filter(int x,int y,int z,BlockState state); }
