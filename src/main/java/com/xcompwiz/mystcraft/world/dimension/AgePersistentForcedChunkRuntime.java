package com.xcompwiz.mystcraft.world.dimension;

import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;

/**
 * Replays persistent vanilla and NeoForge forced-chunk ownership for a
 * ServerLevel created after MinecraftServer#prepareLevels has already run.
 *
 * <p>Vanilla normally restores the chunks stored in chunks.dat during
 * MinecraftServer#prepareLevels, and NeoForge extends that startup pass with
 * ForcedChunkManager#reinstatePersistentChunks. Runtime Mystcraft Ages miss
 * that startup-only pass, so their saved tickets must be replayed explicitly
 * when the Age is installed.</p>
 */
public final class AgePersistentForcedChunkRuntime {
    private static final String FORCED_CHUNKS_FILE = "chunks";

    private AgePersistentForcedChunkRuntime() {}

    /** Returns the number of vanilla /forceload entries replayed. */
    public static int reinstate(ServerLevel level) {
        ForcedChunksSavedData data = level.getDataStorage()
                .get(ForcedChunksSavedData.factory(), FORCED_CHUNKS_FILE);
        if (data == null) return 0;

        int vanillaForced = 0;
        LongIterator iterator = data.getChunks().iterator();
        while (iterator.hasNext()) {
            long packed = iterator.nextLong();
            level.getChunkSource().updateChunkForced(new ChunkPos(packed), true);
            vanillaForced++;
        }

        // Replays block/entity owned NeoForge tickets and runs the registered
        // loading-validation callbacks, matching NeoForge's startup path.
        ForcedChunkManager.reinstatePersistentChunks(level, data);
        return vanillaForced;
    }
}
