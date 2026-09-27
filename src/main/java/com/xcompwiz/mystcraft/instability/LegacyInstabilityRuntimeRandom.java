package com.xcompwiz.mystcraft.instability;

import net.minecraft.server.level.ServerLevel;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Runtime RNG bridge preserving the staged-port dry-run safety and the live legacy behavior.
 *
 * <p>When Instability is disabled, diagnostics use a per-dimension shadow RNG so they cannot perturb
 * unrelated world randomness. CP299 enables penalties, so calls intentionally consume the real
 * world RandomSource again, matching the shared-random behavior of 0.13.7.06.</p>
 */
public final class LegacyInstabilityRuntimeRandom {
    private static final ConcurrentMap<String, Random> SHADOW = new ConcurrentHashMap<>();

    private LegacyInstabilityRuntimeRandom() {}

    public static int nextInt(ServerLevel level, int bound) {
        if (level == null) throw new IllegalArgumentException("level");
        if (bound <= 0) throw new IllegalArgumentException("bound");
        if (InstabilityPolicy.PENALTIES_ENABLED) return level.getRandom().nextInt(bound);
        return shadow(level).nextInt(bound);
    }

    public static float nextFloat(ServerLevel level) {
        if (level == null) throw new IllegalArgumentException("level");
        if (InstabilityPolicy.PENALTIES_ENABLED) return level.getRandom().nextFloat();
        return shadow(level).nextFloat();
    }

    public static double nextGaussian(ServerLevel level) {
        if (level == null) throw new IllegalArgumentException("level");
        if (InstabilityPolicy.PENALTIES_ENABLED) return level.getRandom().nextGaussian();
        return shadow(level).nextGaussian();
    }

    private static Random shadow(ServerLevel level) {
        return SHADOW.computeIfAbsent(
                level.dimension().location().toString(), ignored -> new Random());
    }

    public static void clearDimension(ServerLevel level) {
        if (level != null) SHADOW.remove(level.dimension().location().toString());
    }

    public static void clearAll() {
        SHADOW.clear();
    }
}
