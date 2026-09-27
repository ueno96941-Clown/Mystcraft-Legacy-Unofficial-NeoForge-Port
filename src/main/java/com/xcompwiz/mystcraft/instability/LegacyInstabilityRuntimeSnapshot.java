package com.xcompwiz.mystcraft.instability;

import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import com.xcompwiz.mystcraft.world.agedata.AgeRecord;
import com.xcompwiz.mystcraft.world.agedata.AgeRegistryData;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * CP308 per-level/per-game-tick cache for the immutable portion of live Instability runtime setup.
 *
 * <p>Potion, environmental and Decay bridges all consume the same Age, profiler result, score and
 * Phase-2 provider selection during one level tick. Recomputing those values in every bridge is
 * pure duplicate work and can also repeat deck-persistence bookkeeping. This cache deliberately
 * does not cache or execute effect ticks, chunk traversal or random state, so gameplay timing and
 * RNG consumption remain unchanged.</p>
 */
public final class LegacyInstabilityRuntimeSnapshot {
    private static final Map<ServerLevel, Entry> CACHE = new WeakHashMap<>();

    private LegacyInstabilityRuntimeSnapshot() {}

    public static Optional<Snapshot> resolve(ServerLevel level) {
        if (level == null) return Optional.empty();
        long gameTime = level.getGameTime();
        synchronized (CACHE) {
            Entry cached = CACHE.get(level);
            if (cached != null && cached.gameTime == gameTime) return Optional.of(cached.snapshot);
        }

        var ageOptional = AgeManager.resolveByLevel(level.getServer(), level.dimension());
        if (ageOptional.isEmpty()) return Optional.empty();
        AgeRecord age = ageOptional.get();

        LegacyChunkProfilerData profiler = LegacyChunkProfilerData.get(level);
        int nonOre = profiler.ready()
                ? profiler.calculateNonOreInstability(LegacyInstabilityBlockManager.watchedBlocks())
                : 0;
        var score = AgeInstabilityScoreService.calculate(age, nonOre);
        var phase2 = LegacyInstabilityPhase2Service.prepare(age, score.recordedScore());
        if (phase2.deckOrdersChanged()) AgeRegistryData.get(level.getServer()).changed();

        Snapshot snapshot = new Snapshot(age, score, phase2);
        synchronized (CACHE) {
            CACHE.put(level, new Entry(gameTime, snapshot));
        }
        return Optional.of(snapshot);
    }

    public static void clear(ServerLevel level) {
        if (level == null) return;
        synchronized (CACHE) {
            CACHE.remove(level);
        }
    }

    public static void clearAll() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    public record Snapshot(
            AgeRecord age,
            AgeInstabilityScoreService.ScoreBreakdown score,
            LegacyInstabilityPhase2Service.Snapshot phase2) {}

    private record Entry(long gameTime, Snapshot snapshot) {}
}
