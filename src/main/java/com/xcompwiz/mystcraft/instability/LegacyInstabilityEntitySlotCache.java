package com.xcompwiz.mystcraft.instability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-game-tick cache for the modern equivalent of one legacy Chunk#getEntityLists() slot.
 *
 * <p>0.13.7.06 selected a vertical list already owned by the chunk; it did not execute a fresh
 * spatial lookup for every Potion provider. CP294 had to emulate that with an AABB query. CP323
 * keeps exactly the same narrow-band query and candidate ordering, but reuses its result if another
 * Potion provider happens to select the same chunk/slot later in the same tick. Potion application
 * itself does not move or remove entities, so this does not change the provider result.</p>
 */
public final class LegacyInstabilityEntitySlotCache {
    private static final Map<String, DimensionCache> CACHE = new ConcurrentHashMap<>();

    private LegacyInstabilityEntitySlotCache() {}

    public static List<Entity> entitiesForSlot(ServerLevel level, LevelChunk chunk, int slot) {
        if (level == null || chunk == null) return List.of();
        if (slot < 0 || slot >= LegacyInstabilityPotionRuntimeMath.LEGACY_VERTICAL_SLOTS) return List.of();

        String dimension = level.dimension().location().toString();
        long gameTime = level.getGameTime();
        DimensionCache dimensionCache = CACHE.compute(dimension, (key, old) ->
                old != null && old.gameTime == gameTime ? old : new DimensionCache(gameTime));
        long key = slotKey(chunk.getPos().toLong(), slot);
        return dimensionCache.byChunkSlot.computeIfAbsent(key, ignored -> query(level, chunk, slot));
    }

    private static List<Entity> query(ServerLevel level, LevelChunk chunk, int slot) {
        var band = LegacyInstabilityPotionRuntimeMath.band(level.getMinBuildHeight(), level.getHeight(), slot);
        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        AABB bounds = new AABB(minX, band.minY(), minZ, minX + 16, band.maxExclusiveY(), minZ + 16);

        return List.copyOf(level.getEntitiesOfClass(Entity.class, bounds, entity -> {
            if (entity == null || entity.isRemoved()) return false;
            ChunkPos entityChunk = entity.chunkPosition();
            return entityChunk.x == chunkPos.x
                    && entityChunk.z == chunkPos.z
                    && LegacyInstabilityPotionRuntimeMath.containsY(band, entity.getBlockY());
        }));
    }

    private static long slotKey(long chunkPos, int slot) {
        return chunkPos ^ (0x9E3779B97F4A7C15L * (slot + 1L));
    }

    public static void clear(ServerLevel level) {
        if (level != null) CACHE.remove(level.dimension().location().toString());
    }

    private static final class DimensionCache {
        private final long gameTime;
        private final Map<Long, List<Entity>> byChunkSlot = new ConcurrentHashMap<>();

        private DimensionCache(long gameTime) {
            this.gameTime = gameTime;
        }
    }
}
