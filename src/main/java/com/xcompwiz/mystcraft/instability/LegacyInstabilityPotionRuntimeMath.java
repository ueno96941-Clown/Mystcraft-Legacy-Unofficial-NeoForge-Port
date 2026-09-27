package com.xcompwiz.mystcraft.instability;

/**
 * Pure height-normalization helpers for the 0.13.7.06 potion-effect sampling model.
 *
 * <p>The legacy implementation sampled one of {@code Chunk#getEntityLists().length} vertical
 * entity lists. In Minecraft 1.12.2 that array represented sixteen 16-block-high sections across
 * Y=0..255. Modern 1.21.1 worlds are normally 384 blocks tall, so sampling one native section out
 * of twenty-four would silently reduce each provider's chance to inspect a populated vertical
 * region. The port therefore keeps the legacy sixteen-way choice and maps each choice onto one
 * contiguous virtual band spanning the dimension's actual build height.</p>
 */
public final class LegacyInstabilityPotionRuntimeMath {
    /** 0.13.7.06 always sampled from sixteen vertical entity lists in a normal 256-high world. */
    public static final int LEGACY_VERTICAL_SLOTS = 16;

    private LegacyInstabilityPotionRuntimeMath() {}

    public static Band band(int minBuildHeight, int buildHeight, int slot) {
        if (buildHeight <= 0) throw new IllegalArgumentException("buildHeight");
        if (slot < 0 || slot >= LEGACY_VERTICAL_SLOTS) throw new IllegalArgumentException("slot");

        // Integer partitioning keeps all bands contiguous and covers every buildable Y exactly once,
        // even for custom dimensions whose height is not divisible by sixteen.
        int minY = minBuildHeight + (int) (((long) buildHeight * slot) / LEGACY_VERTICAL_SLOTS);
        int maxExclusiveY = minBuildHeight + (int) (((long) buildHeight * (slot + 1)) / LEGACY_VERTICAL_SLOTS);
        return new Band(slot, minY, maxExclusiveY);
    }

    public static boolean containsY(Band band, int blockY) {
        return blockY >= band.minY() && blockY < band.maxExclusiveY();
    }

    public record Band(int slot, int minY, int maxExclusiveY) {
        public Band {
            if (slot < 0 || slot >= LEGACY_VERTICAL_SLOTS) throw new IllegalArgumentException("slot");
            if (maxExclusiveY < minY) throw new IllegalArgumentException("inverted band");
        }

        public int height() {
            return maxExclusiveY - minY;
        }
    }
}
