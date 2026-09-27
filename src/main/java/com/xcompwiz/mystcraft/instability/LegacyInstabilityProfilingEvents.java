package com.xcompwiz.mystcraft.instability;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import com.xcompwiz.mystcraft.world.agedata.AgeRegistryData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Diagnostic 400-chunk profiler. The completed values are no longer allowed to
 * change Dense Ores Instability; fixed page-count tiers remain authoritative in CP294.
 */
@EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class LegacyInstabilityProfilingEvents {
    private static final QueueState STATE = new QueueState();

    private LegacyInstabilityProfilingEvents() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;
        if (!shouldProfile(level)) return;
        if (LegacyChunkProfilerData.get(level).ready()) return;

        ChunkPos loaded = chunk.getPos();
        // Original 0.13.7.06 only profiled a center after its entire 3x3 neighborhood
        // had completed population. Queue every possible center touched by this load.
        for (int dz = -1; dz <= 1; ++dz) {
            for (int dx = -1; dx <= 1; ++dx) {
                enqueue(level.dimension(), new ChunkPos(loaded.x + dx, loaded.z + dz));
            }
        }
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        // One full-chunk scan per server tick prevents the profiler from creating a
        // visible generation hitch while still finishing a 400-chunk sample quickly.
        Pending pending = STATE.queue.pollFirst();
        if (pending == null) return;
        STATE.queued.remove(pending);
        ServerLevel level = event.getServer().getLevel(pending.level());
        if (level == null || !shouldProfile(level)) return;

        LegacyChunkProfilerData profiler = LegacyChunkProfilerData.get(level);
        if (profiler.ready()) return;
        ChunkPos pos = new ChunkPos(pending.chunkPos());
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null || !hasCompleteResidentNeighborhood(level, pos)) return;

        var watched = LegacyInstabilityBlockManager.watchedBlocks();
        boolean changed = profiler.profile(level, chunk, watched);
        if (!changed || !profiler.ready()) return;

        AgeManager.resolveByLevel(event.getServer(), level.dimension()).ifPresent(age -> {
            int nonOre = profiler.calculateNonOreInstability(watched);
            var score = AgeInstabilityScoreService.calculate(age, nonOre);
            var phase2 = LegacyInstabilityPhase2Service.prepare(age, score.recordedScore());
            if (phase2.deckOrdersChanged()) AgeRegistryData.get(event.getServer()).changed();
        });
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        STATE.queue.clear();
        STATE.queued.clear();
    }

    private static void enqueue(ResourceKey<Level> level, ChunkPos pos) {
        Pending pending = new Pending(level, pos.toLong());
        if (STATE.queued.add(pending)) STATE.queue.addLast(pending);
    }

    private static boolean hasCompleteResidentNeighborhood(ServerLevel level, ChunkPos center) {
        for (int dz = -1; dz <= 1; ++dz) {
            for (int dx = -1; dx <= 1; ++dx) {
                if (level.getChunkSource().getChunkNow(center.x + dx, center.z + dz) == null) return false;
            }
        }
        return true;
    }

    private static boolean shouldProfile(ServerLevel level) {
        // Only Mystcraft Ages consume non-ore profiler output. The old Overworld baseline
        // is not part of release scoring, so avoid hundreds of unnecessary full-height chunk scans.
        return Mystcraft.MOD_ID.equals(level.dimension().location().getNamespace());
    }

    private record Pending(ResourceKey<Level> level, long chunkPos) {}
    private static final class QueueState {
        // Chunk load notifications are not relied upon to be server-thread-only. Keep the
        // producer side safe even if a loader callback originates from a chunk worker; the
        // expensive profile scan itself remains serialized on ServerTickEvent.Post.
        final ConcurrentLinkedDeque<Pending> queue = new ConcurrentLinkedDeque<>();
        final Set<Pending> queued = ConcurrentHashMap.newKeySet();
    }
}
