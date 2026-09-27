package com.xcompwiz.mystcraft.instability;

/**
 * Pure coordinate helpers for adapting 0.13.7.06 environmental effects to a modern Age height.
 *
 * <p>The legacy effects encoded Y as an unsigned 8-bit value. Crumble used 0..255 directly while
 * Explosions added one and therefore used 1..256. These helpers preserve the exact old coordinates
 * when the target height is 256, and linearly spread the same 256 samples across a taller/shorter
 * modern build height.</p>
 */
public final class LegacyInstabilityEnvironmentalRuntimeMath {
    public static final int LEGACY_VERTICAL_STEPS = 256;
    private static final int LEGACY_MAX_RAW_Y = 255;

    private LegacyInstabilityEnvironmentalRuntimeMath() {}

    public static int advanceLcg(int updateLcg) {
        return updateLcg * LegacyInstabilityEnvironmentalCatalog.LEGACY_LCG_MULTIPLIER
                + LegacyInstabilityEnvironmentalCatalog.LEGACY_LCG_ADDEND;
    }

    public static int coordsFromLcg(int updateLcg) {
        return updateLcg >> LegacyInstabilityEnvironmentalCatalog.LEGACY_LCG_OUTPUT_SHIFT;
    }

    public static int localX(int coords) {
        return coords & LegacyInstabilityEnvironmentalCatalog.LEGACY_X_MASK;
    }

    public static int localZ(int coords) {
        return (coords >> LegacyInstabilityEnvironmentalCatalog.LEGACY_Z_SHIFT)
                & LegacyInstabilityEnvironmentalCatalog.LEGACY_Z_MASK;
    }

    public static int legacyRawY(int coords) {
        return (coords >> LegacyInstabilityEnvironmentalCatalog.LEGACY_Y_SHIFT)
                & LegacyInstabilityEnvironmentalCatalog.LEGACY_Y_MASK;
    }

    /** Maps legacy Crumble Y=0..255 onto the complete modern buildable range. */
    public static int mapCrumbleY(int legacyRawY, int minBuildHeight, int buildHeight) {
        validateRawY(legacyRawY);
        if (buildHeight <= 0) throw new IllegalArgumentException("buildHeight");
        if (buildHeight == 1) return minBuildHeight;
        // End-point preserving mapping: 0 -> min, 255 -> min + height - 1.
        return minBuildHeight + (int) (((long) legacyRawY * (buildHeight - 1)) / LEGACY_MAX_RAW_Y);
    }

    /**
     * Maps legacy Explosion Y=(raw+1), i.e. 1..256, onto min+1..maxBuildHeightExclusive.
     * The upper endpoint deliberately remains one block above the highest buildable block, matching
     * the old effect's ability to center an explosion at Y=256 in a 0..255 world.
     */
    public static int mapExplosionY(int legacyRawY, int minBuildHeight, int buildHeight) {
        validateRawY(legacyRawY);
        if (buildHeight <= 0) throw new IllegalArgumentException("buildHeight");
        if (buildHeight == 1) return minBuildHeight + 1;
        return minBuildHeight + 1
                + (int) (((long) legacyRawY * (buildHeight - 1)) / LEGACY_MAX_RAW_Y);
    }

    private static void validateRawY(int legacyRawY) {
        if (legacyRawY < 0 || legacyRawY > LEGACY_MAX_RAW_Y) {
            throw new IllegalArgumentException("legacyRawY");
        }
    }
}
