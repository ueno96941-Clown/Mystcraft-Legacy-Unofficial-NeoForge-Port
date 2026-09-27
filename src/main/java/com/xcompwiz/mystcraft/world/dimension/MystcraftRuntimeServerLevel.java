package com.xcompwiz.mystcraft.world.dimension;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Runtime ServerLevel whose world-generation seed is the durable Mystcraft Age seed.
 *
 * <p>Vanilla ServerLevel#getSeed() delegates to the server-wide WorldOptions seed, which is
 * correct for ordinary dimensions because they all share one world seed. Mystcraft Ages do not:
 * each Age persists its own seed. WorldGenRegion snapshots ServerLevel#getSeed() when a generation
 * region is created, so merely seeding the BiomeManager/RandomState is not sufficient.</p>
 *
 * <p>The construction ThreadLocal covers the narrow interval in which the ServerLevel superclass
 * can call virtual getSeed() before this subclass's fields are initialized. Runtime Age creation is
 * server-thread-only, so there is one construction seed per creating thread and no global mutable
 * seed singleton.</p>
 */
public final class MystcraftRuntimeServerLevel extends ServerLevel {
    private static final ThreadLocal<Long> CONSTRUCTION_SEED = new ThreadLocal<>();

    private long mystcraftAgeSeed;
    private boolean mystcraftAgeSeedReady;

    private MystcraftRuntimeServerLevel(
            MinecraftServer server,
            Executor executor,
            LevelStorageSource.LevelStorageAccess storageAccess,
            ServerLevelData levelData,
            ResourceKey<Level> dimension,
            LevelStem levelStem,
            ChunkProgressListener progressListener,
            boolean debug,
            long biomeZoomSeed,
            List<CustomSpawner> customSpawners,
            boolean tickTime,
            RandomSequences randomSequences,
            long ageSeed) {
        super(server, executor, storageAccess, levelData, dimension, levelStem, progressListener,
                debug, biomeZoomSeed, customSpawners, tickTime, randomSequences);
        this.mystcraftAgeSeed = ageSeed;
        this.mystcraftAgeSeedReady = true;
    }

    public static MystcraftRuntimeServerLevel create(
            MinecraftServer server,
            Executor executor,
            LevelStorageSource.LevelStorageAccess storageAccess,
            ServerLevelData levelData,
            ResourceKey<Level> dimension,
            LevelStem levelStem,
            ChunkProgressListener progressListener,
            boolean debug,
            long biomeZoomSeed,
            List<CustomSpawner> customSpawners,
            boolean tickTime,
            RandomSequences randomSequences,
            long ageSeed) {
        Objects.requireNonNull(server, "server");
        if (CONSTRUCTION_SEED.get() != null) {
            throw new IllegalStateException("Nested Mystcraft runtime ServerLevel construction");
        }
        CONSTRUCTION_SEED.set(ageSeed);
        try {
            return new MystcraftRuntimeServerLevel(server, executor, storageAccess, levelData,
                    dimension, levelStem, progressListener, debug, biomeZoomSeed, customSpawners,
                    tickTime, randomSequences, ageSeed);
        } finally {
            CONSTRUCTION_SEED.remove();
        }
    }

    @Override
    public long getSeed() {
        if (mystcraftAgeSeedReady) {
            return mystcraftAgeSeed;
        }
        Long constructing = CONSTRUCTION_SEED.get();
        return constructing != null ? constructing : super.getSeed();
    }
}
