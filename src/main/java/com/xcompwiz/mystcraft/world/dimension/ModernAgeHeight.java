package com.xcompwiz.mystcraft.world.dimension;

import net.minecraft.world.level.LevelHeightAccessor;

/**
 * Minecraft 1.21.1 vertical-world contract used by ordinary Mystcraft Ages.
 *
 * <p>The original 1.12 port used the historical 0..255 build range in several
 * runtime adapters. That was faithful to the old engine, not to a modern Age.
 * This class centralizes the 1.21.1 Overworld-shaped bounds so future code does
 * not re-introduce magic 0/255/256 constants.</p>
 */
public final class ModernAgeHeight {
    public static final int MIN_Y = -64;
    public static final int HEIGHT = 384;
    /** Exclusive maximum build height. */
    public static final int MAX_Y_EXCLUSIVE = MIN_Y + HEIGHT; // 320
    public static final int MAX_BLOCK_Y = MAX_Y_EXCLUSIVE - 1; // 319
    public static final int SEA_LEVEL = 63;
    public static final float NORMAL_CLOUD_HEIGHT = 192.0F;

    private ModernAgeHeight() {}

    /**
     * Maps one legacy 0..255 vertical cell onto the current level height.
     * Use start/end together when expanding one legacy voxel so the modern
     * 384-block range is covered without vertical gaps.
     */
    public static int legacyCellStart(LevelHeightAccessor level, int legacyY) {
        int clamped = Math.max(0, Math.min(255, legacyY));
        return level.getMinBuildHeight()
                + Math.floorDiv(clamped * level.getHeight(), 256);
    }

    public static int legacyCellEndInclusive(LevelHeightAccessor level, int legacyY) {
        int clamped = Math.max(0, Math.min(255, legacyY));
        int next = level.getMinBuildHeight()
                + Math.floorDiv((clamped + 1) * level.getHeight(), 256);
        return Math.max(legacyCellStart(level, clamped), next - 1);
    }

    /** Maps a legacy absolute Y sample to the center of its modern cell. */
    public static int legacySampleY(LevelHeightAccessor level, int legacyY) {
        int start = legacyCellStart(level, legacyY);
        int end = legacyCellEndInclusive(level, legacyY);
        return start + (end - start) / 2;
    }
}
