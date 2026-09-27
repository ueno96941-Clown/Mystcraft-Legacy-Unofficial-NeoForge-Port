package com.xcompwiz.mystcraft.world.worldgen;

public interface LegacyFloatingIslandSurfaceBuffer extends LegacyFloatingIslandBuffer {
    boolean isStone(int localX, int y, int localZ);
    void setTop(int localX, int y, int localZ);
    void setFiller(int localX, int y, int localZ);
    void setSandstone(int localX, int y, int localZ);
    boolean fillerIsSand();
}
