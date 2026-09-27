package com.xcompwiz.mystcraft.instability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-level/per-game-tick snapshot of chunks that receive legacy Instability effects.
 *
 * <p>Mystcraft 0.13.7.06 did not rediscover active chunks independently for Potion,
 * environmental and Decay effects. The Age controller was invoked from the chunk tick path and
 * then iterated its already-built effect list for that chunk. The 1.21.1 port has to bridge from
 * a level tick, but doing three full simulation-distance traversals per tick is needless overhead.
 * CP323 keeps the same chunk membership and ordering while sharing the traversal between all
 * restored Instability families.</p>
 */
public final class LegacyInstabilityActiveChunkCache {
    private static final Map<String, Snapshot> CACHE = new ConcurrentHashMap<>();

    private LegacyInstabilityActiveChunkCache() {}

    public static List<LevelChunk> activeChunks(ServerLevel level) {
        if (level == null) return List.of();
        String dimension = level.dimension().location().toString();
        long gameTime = level.getGameTime();
        Snapshot existing = CACHE.get(dimension);
        if (existing != null && existing.gameTime == gameTime) return existing.chunks;

        ArrayList<LevelChunk> chunks = new ArrayList<>();
        HashSet<Long> visited = new HashSet<>();
        int simulationDistance = level.getServer().getPlayerList().getSimulationDistance();

        // Preserve the pre-CP323 traversal order exactly: players, dz, dx, then forced chunks.
        for (ServerPlayer player : level.players()) {
            ChunkPos center = player.chunkPosition();
            for (int dz = -simulationDistance; dz <= simulationDistance; dz++) {
                for (int dx = -simulationDistance; dx <= simulationDistance; dx++) {
                    int cx = center.x + dx;
                    int cz = center.z + dz;
                    long packed = ChunkPos.asLong(cx, cz);
                    if (!visited.add(packed)) continue;
                    if (!level.getChunkSource().chunkMap.getDistanceManager().inBlockTickingRange(packed)) continue;
                    LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk != null) chunks.add(chunk);
                }
            }
        }

        for (long packed : level.getForcedChunks()) {
            if (!visited.add(packed)) continue;
            ChunkPos pos = new ChunkPos(packed);
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk != null) chunks.add(chunk);
        }

        List<LevelChunk> frozen = List.copyOf(chunks);
        CACHE.put(dimension, new Snapshot(gameTime, frozen));
        return frozen;
    }

    public static void clear(ServerLevel level) {
        if (level != null) CACHE.remove(level.dimension().location().toString());
    }

    private record Snapshot(long gameTime, List<LevelChunk> chunks) {}
}
