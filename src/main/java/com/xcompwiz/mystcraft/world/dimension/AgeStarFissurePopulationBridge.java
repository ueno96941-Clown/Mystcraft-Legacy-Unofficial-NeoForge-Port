package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.AgeStarFissureLegacyPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Carries the ordered Legacy Star Fissure population decision across the async FEATURES pass.
 *
 * <p>The actual fissure can cross farther than the FEATURES write radius. Worldgen therefore
 * consumes the correct batch RNG and records the plan, then the server thread applies that plan
 * after the spawn chunk has reached FULL. A prior successful Village records suppression instead,
 * matching AgeController's Java short-circuit semantics.</p>
 */
final class AgeStarFissurePopulationBridge {
    enum Kind { PLAN, SUPPRESSED }

    record Pending(Kind kind, int spawnChunkX, int spawnChunkZ, AgeStarFissureLegacyPlan.Plan plan) {
        static Pending plan(int x, int z, AgeStarFissureLegacyPlan.Plan plan) {
            return new Pending(Kind.PLAN, x, z, plan);
        }

        static Pending suppressed(int x, int z) {
            return new Pending(Kind.SUPPRESSED, x, z, null);
        }
    }

    private static final ConcurrentMap<ResourceKey<Level>, Pending> PENDING = new ConcurrentHashMap<>();

    private AgeStarFissurePopulationBridge() {}

    static boolean stageIfSpawnChunk(
            WorldGenLevel level, RandomSource random, int chunkBlockX, int chunkBlockZ) {
        ServerLevel serverLevel = serverLevel(level);
        if (serverLevel == null || !isSpawnChunk(serverLevel, chunkBlockX, chunkBlockZ)) return false;

        int chunkX = chunkBlockX >> 4;
        int chunkZ = chunkBlockZ >> 4;
        AgeStarFissureLegacyPlan.Plan plan = AgeStarFissureLegacyPlan.create(
                chunkBlockX, chunkBlockZ, random::nextInt);
        PENDING.put(serverLevel.dimension(), Pending.plan(chunkX, chunkZ, plan));
        return true;
    }

    static void suppressIfSpawnChunk(WorldGenLevel level, int chunkBlockX, int chunkBlockZ) {
        ServerLevel serverLevel = serverLevel(level);
        if (serverLevel == null || !isSpawnChunk(serverLevel, chunkBlockX, chunkBlockZ)) return;
        PENDING.put(serverLevel.dimension(), Pending.suppressed(chunkBlockX >> 4, chunkBlockZ >> 4));
    }

    static Pending take(ServerLevel level, int spawnChunkX, int spawnChunkZ) {
        Pending pending = PENDING.remove(level.dimension());
        if (pending == null) return null;
        if (pending.spawnChunkX() != spawnChunkX || pending.spawnChunkZ() != spawnChunkZ) return null;
        return pending;
    }

    static void clear(ResourceKey<Level> dimension) {
        PENDING.remove(dimension);
    }

    static void clearAll() {
        PENDING.clear();
    }

    private static ServerLevel serverLevel(WorldGenLevel level) {
        return level instanceof WorldGenRegion region ? region.getLevel() : null;
    }

    private static boolean isSpawnChunk(ServerLevel level, int chunkBlockX, int chunkBlockZ) {
        BlockPos spawn = level.getSharedSpawnPos();
        return Math.floorDiv(spawn.getX(), 16) * 16 == chunkBlockX
                && Math.floorDiv(spawn.getZ(), 16) * 16 == chunkBlockZ;
    }
}
