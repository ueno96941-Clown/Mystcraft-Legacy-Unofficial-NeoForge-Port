package com.xcompwiz.mystcraft.world.worldgen;

/** Minimal chunk-local write contract for the legacy Mystcraft huge-tree generator. */
public interface LegacyBigTreeBuffer {
    boolean isBedrock(int localX, int y, int localZ);
    void setLog(int localX, int y, int localZ);
    void setLeaves(int localX, int y, int localZ);
}
