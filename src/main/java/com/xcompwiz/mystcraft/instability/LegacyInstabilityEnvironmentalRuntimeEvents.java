package com.xcompwiz.mystcraft.instability;

import com.xcompwiz.mystcraft.Mystcraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** CP299 live runtime bridge for Burning, Lightning, Explosion, Crumble and Meteor. */
@EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class LegacyInstabilityEnvironmentalRuntimeEvents {
    private static final Map<String, String> PLAN_SIGNATURES = new ConcurrentHashMap<>();

    private LegacyInstabilityEnvironmentalRuntimeEvents() {}

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!Mystcraft.MOD_ID.equals(level.dimension().location().getNamespace())) return;

        var runtimeOptional = LegacyInstabilityRuntimeSnapshot.resolve(level);
        if (runtimeOptional.isEmpty()) return;
        var runtime = runtimeOptional.get();
        var phase2 = runtime.phase2();
        var environmental = LegacyInstabilityEnvironmentalPlanService.build(phase2.selection().providerLevels());
        // Refresh even when the new environmental plan is empty. The original controller discarded
        // its effect objects when provider levels changed to/from zero as well.
        refreshEffectStateIfPlanChanged(level, phase2.selection().providerLevels());
        if (environmental.environmentalPlans().isEmpty()) return;

        for (LevelChunk chunk : LegacyInstabilityActiveChunkCache.activeChunks(level)) {
            tickChunk(level, chunk, environmental);
        }
    }

    private static void tickChunk(
            ServerLevel level,
            LevelChunk chunk,
            LegacyInstabilityEnvironmentalPlanService.Plan plan) {
        for (var environmental : plan.environmentalPlans()) {
            for (int effectIndex = 0; effectIndex < environmental.effectInstances(); effectIndex++) {
                LegacyInstabilityEnvironmentalRuntimeAdapter.tick(level, chunk, environmental, effectIndex);
            }
        }
    }

    private static void refreshEffectStateIfPlanChanged(
            ServerLevel level,
            Map<String, Integer> providerLevels) {
        String dimension = level.dimension().location().toString();
        String signature = environmentalSignature(providerLevels);
        String previous = PLAN_SIGNATURES.put(dimension, signature);
        if (previous != null && !previous.equals(signature)) {
            // Original InstabilityController.reconstruct() discarded all environmental effect
            // objects whenever the rounded score/provider levels changed. Re-seed private LCGs too.
            LegacyInstabilityEnvironmentalRuntimeState.clearDimension(level);
        }
    }

    static String environmentalSignature(Map<String, Integer> providerLevels) {
        ArrayList<String> entries = new ArrayList<>();
        if (providerLevels != null) {
            for (var entry : providerLevels.entrySet()) {
                if (LegacyInstabilityEnvironmentalCatalog.environmental(entry.getKey()).isEmpty()) continue;
                Integer level = entry.getValue();
                if (level == null || level <= 0) continue;
                entries.add(entry.getKey() + "=" + level);
            }
        }
        entries.sort(Comparator.naturalOrder());
        return String.join(";", entries);
    }


    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        LegacyInstabilityRuntimeSnapshot.clear(level);
        String dimension = level.dimension().location().toString();
        LegacyInstabilityEnvironmentalRuntimeState.clearDimension(level);
        LegacyInstabilityRuntimeRandom.clearDimension(level);
        LegacyInstabilityActiveChunkCache.clear(level);
        LegacyInstabilityEntitySlotCache.clear(level);
        PLAN_SIGNATURES.remove(dimension);
    }
}
