package com.xcompwiz.mystcraft.instability;

import com.xcompwiz.mystcraft.Mystcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CP324 one-tick write overlay for restored Instability block mutations.
 *
 * <p>Mystcraft 0.13.7.06 performed Crumble/Decay mutations directly while iterating the current
 * chunk's environmental effects. On 1.21.1 each {@code setBlock(..., UPDATE_ALL)} is substantially
 * more expensive because it may fan out through lighting, neighbour shape updates, chunk dirtiness
 * and network tracking. The restored runtime can also target the same position more than once in a
 * single server tick, especially once several Decay providers are active.</p>
 *
 * <p>This batch keeps an in-memory state overlay for the remainder of the current level tick. Later
 * Instability decisions read the overlay first, so the logical ordering of Crumble/Decay state
 * changes is preserved. The final state for each touched position is committed once, at LOWEST
 * priority after the normal Instability Post-tick handlers. Normal vanilla random ticks remain
 * immediate and do not use this queue.</p>
 */
@EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class LegacyInstabilityBlockMutationBatch {
    private static final Map<String, Batch> BATCHES = new ConcurrentHashMap<>();

    private LegacyInstabilityBlockMutationBatch() {}

    public static BlockState effectiveState(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return null;
        Batch batch = current(level, false);
        if (batch != null) {
            Mutation mutation = batch.mutations.get(pos.asLong());
            if (mutation != null) return mutation.state;
        }
        return level.getBlockState(pos);
    }

    /** Queue the final logical state for this position for the current game tick. */
    public static boolean queue(ServerLevel level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null) return false;
        Batch batch = current(level, true);
        long key = pos.asLong();
        BlockState before = effectiveState(level, pos);
        if (state.equals(before)) return false;
        Mutation existing = batch.mutations.get(key);
        if (existing == null) {
            batch.mutations.put(key, new Mutation(pos.immutable(), state));
        } else {
            // LinkedHashMap retains the first-touch commit order while the overlay tracks the
            // most recent logical state, matching sequential effect evaluation as closely as possible.
            batch.mutations.put(key, new Mutation(existing.pos, state));
        }
        return true;
    }

    private static Batch current(ServerLevel level, boolean create) {
        String dimension = level.dimension().location().toString();
        long gameTime = level.getGameTime();
        Batch batch = BATCHES.get(dimension);
        if (batch != null && batch.gameTime != gameTime) {
            // Defensive recovery: a previous tick should have been flushed by the LOWEST handler.
            flush(level, batch);
            BATCHES.remove(dimension, batch);
            batch = null;
        }
        if (batch == null && create) {
            batch = new Batch(gameTime);
            BATCHES.put(dimension, batch);
        }
        return batch;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!Mystcraft.MOD_ID.equals(level.dimension().location().getNamespace())) return;
        String dimension = level.dimension().location().toString();
        Batch batch = BATCHES.remove(dimension);
        if (batch != null) flush(level, batch);
    }

    private static void flush(ServerLevel level, Batch batch) {
        if (level == null || batch == null || batch.mutations.isEmpty()) return;
        for (Mutation mutation : batch.mutations.values()) {
            BlockState current = level.getBlockState(mutation.pos);
            if (mutation.state.equals(current)) continue;
            level.setBlock(mutation.pos, mutation.state, Block.UPDATE_ALL);
        }
        batch.mutations.clear();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BATCHES.remove(level.dimension().location().toString());
    }

    private static final class Batch {
        private final long gameTime;
        private final LinkedHashMap<Long, Mutation> mutations = new LinkedHashMap<>();

        private Batch(long gameTime) {
            this.gameTime = gameTime;
        }
    }

    private record Mutation(BlockPos pos, BlockState state) {}
}
