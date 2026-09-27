package com.xcompwiz.mystcraft.instability;

import com.xcompwiz.mystcraft.Mystcraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Map;

/** CP299 live runtime bridge for Red/Blue/Purple/White Decay placement and extra ticks. */
@EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class LegacyInstabilityDecayRuntimeEvents {
    private LegacyInstabilityDecayRuntimeEvents() {}

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!Mystcraft.MOD_ID.equals(level.dimension().location().getNamespace())) return;
        if (!InstabilityPolicy.PENALTIES_ENABLED || !InstabilityPolicy.DECAY_ENABLED) return;

        var runtimeOptional = LegacyInstabilityRuntimeSnapshot.resolve(level);
        if (runtimeOptional.isEmpty()) return;
        var runtime = runtimeOptional.get();
        var age = runtime.age();
        var phase2 = runtime.phase2();
        int controllerScore = phase2.selection().controllerScore();
        Map<String, Integer> providerLevels = phase2.selection().providerLevels();
        if (controllerScore <= 0 || !hasDecay(providerLevels)) return;

        for (LevelChunk chunk : LegacyInstabilityActiveChunkCache.activeChunks(level)) {
            tickChunk(level, chunk, age.ageUid(), controllerScore, providerLevels);
        }
    }

    private static void tickChunk(ServerLevel level, LevelChunk chunk, int ageUid,
                                  int controllerScore, Map<String, Integer> levels) {
        for (String providerId : new String[]{"decayblue", "decaypurple", "decayred", "decaywhite"}) {
            Integer providerLevel = levels.get(providerId);
            if (providerLevel == null || providerLevel <= 0 || !InstabilityPolicy.allowProvider(providerId)) continue;
            for (int i = 0; i < providerLevel; i++) {
                LegacyInstabilityDecayRuntime.tickPlacement(level, chunk, providerId, providerLevel, i, controllerScore);
            }
            int extraInstances = providerLevel + ("decaywhite".equals(providerId) ? 1 : 0);
            for (int i = 0; i < extraInstances; i++) {
                LegacyInstabilityDecayRuntime.tickExtra(level, chunk, providerId, providerLevel, i);
            }
        }
    }

    private static boolean hasDecay(Map<String, Integer> levels) {
        return positive(levels, "decayblue") || positive(levels, "decaypurple")
                || positive(levels, "decayred") || positive(levels, "decaywhite");
    }
    private static boolean positive(Map<String, Integer> levels, String id) {
        Integer v = levels.get(id);
        return v != null && v > 0;
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        LegacyInstabilityRuntimeSnapshot.clear(level);
        LegacyInstabilityDecayRuntimeState.clearDimension(level);
        LegacyInstabilityActiveChunkCache.clear(level);
        LegacyInstabilityEntitySlotCache.clear(level);
    }
}
