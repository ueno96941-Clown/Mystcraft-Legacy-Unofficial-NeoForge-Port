package com.xcompwiz.mystcraft.instability;

/** Constants retained from Mystcraft 0.13.7.06 for score-only restoration. */
public final class LegacyInstabilityData {
    public static final int PROFILE_CHUNKS = 400;
    public static final float BASELINE_TOLERANCE = 1.05F;
    public static final int EXTRA_CONTROLLER = 500;
    public static final int MISSING_CONTROLLER = 0;
    public static final int DANGLING_MODIFIER = 100;
    public static final int DANGLING_BLOCK = 50;
    public static final int DANGLING_BIOME = 100;
    public static final float CLEAR_PERCENTAGE = 0.20F;

    /** Legacy Mystcraft config default. Runtime activation is controlled separately. */
    public static final int DEFAULT_DIFFICULTY = 2;

    private LegacyInstabilityData() {}

    public static int applyDifficulty(int raw) {
        return applyDifficulty(raw, DEFAULT_DIFFICULTY);
    }

    public static int applyDifficulty(int raw, int difficulty) {
        float value = raw;
        switch (difficulty) {
            case 0 -> value *= 0.25F;
            case 1 -> value *= 0.50F;
            case 3 -> value *= 1.75F;
            default -> { }
        }
        return (int) value;
    }
}
