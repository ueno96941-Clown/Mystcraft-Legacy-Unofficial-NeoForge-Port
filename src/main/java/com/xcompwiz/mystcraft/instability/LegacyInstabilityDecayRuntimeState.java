package com.xcompwiz.mystcraft.instability;

import net.minecraft.server.level.ServerLevel;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Private LCG state owned by live Decay placement/extra-tick effect instances. */
public final class LegacyInstabilityDecayRuntimeState {
    private static final ConcurrentMap<String, Integer> LCG = new ConcurrentHashMap<>();
    private LegacyInstabilityDecayRuntimeState() {}

    public static int advance(ServerLevel level, String providerId, int effectIndex, String role) {
        String key = level.dimension().location() + "|" + providerId + "|" + role + "|" + effectIndex;
        return LCG.compute(key, (ignored, previous) -> {
            int current = previous == null ? new Random().nextInt() : previous;
            return current * 3 + 1013904223;
        });
    }

    public static void clearDimension(ServerLevel level) {
        if (level == null) return;
        String prefix = level.dimension().location() + "|";
        LCG.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public static void clearAll() {
        LCG.clear();
    }
}
