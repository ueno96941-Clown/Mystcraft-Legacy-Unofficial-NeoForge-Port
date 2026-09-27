package com.xcompwiz.mystcraft.instability;

/**
 * Central runtime gates for the restored Mystcraft 0.13.7.06 Instability system.
 *
 * <p>All legacy provider families are enabled in the release policy. The switches remain separate
 * so a provider family can be isolated during compatibility maintenance without changing score semantics.</p>
 */
public final class InstabilityPolicy {
    public static final boolean PENALTIES_ENABLED = true;

    public static final boolean POTION_ENABLED = true;
    public static final boolean BURNING_ENABLED = true;
    public static final boolean LIGHTNING_ENABLED = true;
    public static final boolean EXPLOSION_ENABLED = true;
    public static final boolean CRUMBLE_ENABLED = true;
    public static final boolean METEOR_ENABLED = true;
    public static final boolean DECAY_ENABLED = true;

    private InstabilityPolicy() {}

    public static boolean runtimeEnabled() { return PENALTIES_ENABLED; }

    public static int effectiveScore(int recordedScore) {
        return PENALTIES_ENABLED ? recordedScore : 0;
    }

    public static boolean allowProvider(String legacyProviderId) {
        if (!PENALTIES_ENABLED || legacyProviderId == null) return false;
        return switch (legacyProviderId) {
            case "burning" -> BURNING_ENABLED;
            case "lightning" -> LIGHTNING_ENABLED;
            case "explosions" -> EXPLOSION_ENABLED;
            case "crumble" -> CRUMBLE_ENABLED;
            case "meteors" -> METEOR_ENABLED;
            case "decayblue", "decaypurple", "decayred", "decaywhite" -> DECAY_ENABLED;
            default -> POTION_ENABLED;
        };
    }
}
