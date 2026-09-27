package com.xcompwiz.mystcraft.instability;

import net.minecraft.server.level.ServerLevel;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Runtime-only LCG state for legacy environmental effect instances. */
public final class LegacyInstabilityEnvironmentalRuntimeState {
    private static final ConcurrentMap<String, Integer> UPDATE_LCG = new ConcurrentHashMap<>();

    private LegacyInstabilityEnvironmentalRuntimeState() {}

    public static int advance(ServerLevel level, String providerId, int effectIndex) {
        String key = key(level, providerId, effectIndex);
        return UPDATE_LCG.compute(key, (ignored, current) -> {
            // Legacy effect objects seeded their private LCG with new Random().nextInt(), not world.rand.
            int previous = current != null ? current : new Random().nextInt();
            return LegacyInstabilityEnvironmentalRuntimeMath.advanceLcg(previous);
        });
    }

    public static void clearDimension(ServerLevel level) {
        if (level == null) return;
        String prefix = level.dimension().location() + "|";
        UPDATE_LCG.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private static String key(ServerLevel level, String providerId, int effectIndex) {
        return level.dimension().location() + "|" + providerId + "|" + effectIndex;
    }

    public static void clearAll() {
        UPDATE_LCG.clear();
    }
}
