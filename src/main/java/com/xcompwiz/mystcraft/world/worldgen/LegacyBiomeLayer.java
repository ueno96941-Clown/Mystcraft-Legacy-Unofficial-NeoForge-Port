package com.xcompwiz.mystcraft.world.worldgen;

/** Pure integer biome-index layer equivalent to the legacy GenLayer chain. */
public interface LegacyBiomeLayer {
    int sample(int x, int z);
}
