package com.xcompwiz.mystcraft.instability;

import com.xcompwiz.mystcraft.Mystcraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * CP294 runtime bridge for legacy Potion instability providers.
 *
 * <p>The old Age controller invoked every active environmental effect once for every actively
 * ticking chunk. Modern NeoForge has no direct equivalent hook used by this port, so this event
 * mirrors the established CP253/Storm traversal: player simulation-distance chunks plus forced
 * chunks, with block-ticking-range validation where applicable.</p>
 *
 * <p>CP299 intentionally enables the final addEffect boundary for controlled live
 * Runtime event bridge for the restored legacy potion providers.</p>
 */
@EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class LegacyInstabilityPotionRuntimeEvents {
    private LegacyInstabilityPotionRuntimeEvents() {}

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!Mystcraft.MOD_ID.equals(level.dimension().location().getNamespace())) return;

        var runtimeOptional = LegacyInstabilityRuntimeSnapshot.resolve(level);
        if (runtimeOptional.isEmpty()) return;
        var runtime = runtimeOptional.get();
        var phase2 = runtime.phase2();
        var phase3 = LegacyInstabilityPhase3PlanService.build(phase2.selection().providerLevels());
        if (phase3.potionPlans().isEmpty()) return;

        for (LevelChunk chunk : LegacyInstabilityActiveChunkCache.activeChunks(level)) {
            tickChunk(level, chunk, phase3);
        }
    }

    private static void tickChunk(
            ServerLevel level,
            LevelChunk chunk,
            LegacyInstabilityPhase3PlanService.Plan plan) {
        for (var potionPlan : plan.potionPlans()) {
            LegacyInstabilityPotionRuntimeAdapter.tick(level, chunk, potionPlan);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        LegacyInstabilityRuntimeSnapshot.clear(level);
        LegacyInstabilityRuntimeRandom.clearDimension(level);
        LegacyInstabilityActiveChunkCache.clear(level);
        LegacyInstabilityEntitySlotCache.clear(level);
    }
}
