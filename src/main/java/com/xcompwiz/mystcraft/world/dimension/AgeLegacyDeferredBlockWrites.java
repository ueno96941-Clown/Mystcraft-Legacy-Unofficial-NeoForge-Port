package com.xcompwiz.mystcraft.world.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Defers Legacy 1.12 population block writes which fall outside the active 1.21.1
 * FEATURES write radius, without clamping or changing their authored coordinates.
 *
 * <p>Legacy generators commonly chose {@code chunk + 8..23} origins and then wrote
 * 16x16-ish shapes, so crossing into the next chunk (and occasionally the chunk after
 * that) was normal. 1.21.1's WorldGenRegion rejects writes beyond the current
 * ChunkStep block-state write radius. We retain the exact target position and stage only
 * those rejected writes. They are replayed either when the target chunk itself reaches
 * the Mystcraft population batch, or after it is already FULL on the server thread.</p>
 */
final class AgeLegacyDeferredBlockWrites {
    private record Write(BlockPos pos, BlockState state, int flags) {}

    /** Stable disk form used by the server-thread SavedData snapshot. */
    record PersistedWrite(BlockPos pos, BlockState state, int flags) {}

    private static final int FEATURES_WRITE_RADIUS =
            ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FEATURES).blockStateWriteRadius();

    private static final ConcurrentMap<ResourceKey<Level>, ConcurrentMap<Long, ArrayDeque<Write>>> PENDING =
            new ConcurrentHashMap<>();

    private AgeLegacyDeferredBlockWrites() {}

    /**
     * Writes immediately when legal for the active WorldGenRegion; otherwise preserves the
     * exact block position by staging it for the target chunk.
     */
    static boolean setBlock(WorldGenLevel level, BlockPos pos, BlockState state, int flags) {
        if (!(level instanceof WorldGenRegion region)) {
            return level.setBlock(pos, state, flags);
        }

        ChunkPos center = region.getCenter();
        int targetX = Math.floorDiv(pos.getX(), 16);
        int targetZ = Math.floorDiv(pos.getZ(), 16);
        if (Math.abs(center.x - targetX) <= FEATURES_WRITE_RADIUS
                && Math.abs(center.z - targetZ) <= FEATURES_WRITE_RADIUS) {
            return region.setBlock(pos, state, flags);
        }

        stage(region.getLevel(), targetX, targetZ, new Write(pos.immutable(), state, flags));
        return true;
    }

    /** Replay writes aimed at the chunk whose FEATURES pass is running now. */
    static int drainForCurrentChunk(WorldGenLevel level, int chunkBlockX, int chunkBlockZ) {
        if (!(level instanceof WorldGenRegion region)) return 0;
        return drainIntoWorldgen(region, chunkBlockX >> 4, chunkBlockZ >> 4);
    }

    /** Replay pending writes once their target chunk is already FULL and loaded. */
    static int flushFullChunks(ServerLevel level, int maxChunks) {
        ConcurrentMap<Long, ArrayDeque<Write>> byChunk = PENDING.get(level.dimension());
        if (byChunk == null || byChunk.isEmpty()) return 0;

        int flushed = 0;
        for (Long key : byChunk.keySet()) {
            if (flushed >= maxChunks) break;
            int chunkX = ChunkPos.getX(key);
            int chunkZ = ChunkPos.getZ(key);
            if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) continue;

            ArrayList<Write> drained = new ArrayList<>();
            byChunk.compute(key, (ignored, writes) -> {
                if (writes == null) return null;
                synchronized (writes) {
                    drained.addAll(writes);
                }
                return null;
            });
            if (drained.isEmpty()) continue;
            for (Write write : drained) {
                level.setBlock(write.pos(), write.state(), write.flags());
            }
            flushed++;
        }

        // Do not opportunistically remove the per-dimension map here. A worldgen worker can
        // stage a new write immediately after an isEmpty() observation; removing the same map
        // object would then orphan that fresh write. Explicit level/server lifecycle cleanup
        // owns dimension-map removal instead.
        return flushed;
    }


    /** Thread-safe immutable snapshot used by the level save lifecycle. */
    static List<PersistedWrite> snapshot(ResourceKey<Level> dimension) {
        ConcurrentMap<Long, ArrayDeque<Write>> byChunk = PENDING.get(dimension);
        if (byChunk == null || byChunk.isEmpty()) return List.of();
        ArrayList<PersistedWrite> out = new ArrayList<>();
        for (ArrayDeque<Write> writes : byChunk.values()) {
            synchronized (writes) {
                for (Write write : writes) {
                    out.add(new PersistedWrite(write.pos(), write.state(), write.flags()));
                }
            }
        }
        return List.copyOf(out);
    }

    /** Replace one dimension's live queue from its last durable snapshot. */
    static void restore(ResourceKey<Level> dimension, List<PersistedWrite> writes) {
        PENDING.remove(dimension);
        if (writes == null || writes.isEmpty()) return;
        ConcurrentMap<Long, ArrayDeque<Write>> byChunk = new ConcurrentHashMap<>();
        for (PersistedWrite persisted : writes) {
            int chunkX = Math.floorDiv(persisted.pos().getX(), 16);
            int chunkZ = Math.floorDiv(persisted.pos().getZ(), 16);
            ArrayDeque<Write> queue = byChunk.computeIfAbsent(
                    ChunkPos.asLong(chunkX, chunkZ), ignored -> new ArrayDeque<>());
            synchronized (queue) {
                queue.addLast(new Write(persisted.pos().immutable(), persisted.state(), persisted.flags()));
            }
        }
        PENDING.put(dimension, byChunk);
    }

    static void clear(ResourceKey<Level> dimension) {
        PENDING.remove(dimension);
    }

    static void clearAll() {
        PENDING.clear();
    }

    static int pendingWriteCount(ResourceKey<Level> dimension) {
        ConcurrentMap<Long, ArrayDeque<Write>> byChunk = PENDING.get(dimension);
        if (byChunk == null) return 0;
        int total = 0;
        for (ArrayDeque<Write> writes : byChunk.values()) {
            synchronized (writes) {
                total += writes.size();
            }
        }
        return total;
    }

    private static void stage(ServerLevel level, int chunkX, int chunkZ, Write write) {
        ConcurrentMap<Long, ArrayDeque<Write>> byChunk =
                PENDING.computeIfAbsent(level.dimension(), ignored -> new ConcurrentHashMap<>());
        long key = ChunkPos.asLong(chunkX, chunkZ);
        // CP314: mutate the per-chunk queue under ConcurrentHashMap#compute. Merely synchronizing
        // the ArrayDeque is insufficient: a drain could remove the mapping after a worker had
        // obtained the old queue reference but before that worker acquired the queue monitor,
        // allowing the worker to append to a detached queue and silently lose the write.
        byChunk.compute(key, (ignored, writes) -> {
            ArrayDeque<Write> queue = writes == null ? new ArrayDeque<>() : writes;
            synchronized (queue) {
                queue.addLast(write);
            }
            return queue;
        });
    }

    private static int drainIntoWorldgen(WorldGenRegion region, int chunkX, int chunkZ) {
        ConcurrentMap<Long, ArrayDeque<Write>> byChunk = PENDING.get(region.getLevel().dimension());
        if (byChunk == null) return 0;
        long key = ChunkPos.asLong(chunkX, chunkZ);
        ArrayList<Write> drained = new ArrayList<>();
        // CP314: detach and snapshot atomically for this chunk key. Concurrent stage() calls for
        // the same key are serialized by ConcurrentHashMap and will either join this drain or
        // create a fresh mapped queue afterwards; neither path can append to an orphaned deque.
        byChunk.compute(key, (ignored, writes) -> {
            if (writes == null) return null;
            synchronized (writes) {
                drained.addAll(writes);
            }
            return null;
        });
        if (drained.isEmpty()) return 0;

        int count = 0;
        for (Write write : drained) {
            // Every staged position belongs to this target chunk, which is always legal
            // while that chunk is the center of its own FEATURES pass.
            region.setBlock(write.pos(), write.state(), write.flags());
            count++;
        }
        // Keep the per-dimension map installed until explicit lifecycle cleanup; see
        // flushFullChunks() for the race avoided by not removing an observed-empty map.
        return count;
    }
}
