package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.agedata.AgeRecord;
import com.xcompwiz.mystcraft.world.worldgen.AgeWorldgenPlanner;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Installs a prepared Mystcraft Age as a live {@link ServerLevel} without
 * mutating the frozen LEVEL_STEM registry.
 *
 * <p>NeoForge exposes {@link MinecraftServer#forgeGetWorldMap()} and
 * {@link MinecraftServer#markWorldsDirty()} specifically for internal dynamic
 * world bookkeeping. The installer uses that supported bridge, then posts the
 * same LevelEvent.Load lifecycle event NeoForge posts for startup dimensions.</p>
 *
 * <p>Runtime unloading is owned by {@link AgeRuntimeWorldManager}; installation
 * and removal therefore share the same NeoForge world-map bookkeeping contract.</p>
 */
@SuppressWarnings("deprecation")
public final class AgeRuntimeServerLevelInstaller {
    private AgeRuntimeServerLevelInstaller() {}

    public enum Status {
        ALREADY_LOADED,
        INSTALLED,
        NOT_PREPARED,
        DEAD,
        CONFLICT,
        WRONG_THREAD
    }

    public record InstallResult(
            Status status,
            AgeDimensionDefinition definition,
            ServerLevel level) {
        public boolean usable() {
            return status == Status.ALREADY_LOADED || status == Status.INSTALLED;
        }
    }

    /**
     * Installs the already-prepared Age. Callers normally invoke
     * {@link AgeRuntimeDimensionLoader#prepare(MinecraftServer, AgeRecord)} first.
     */
    public static InstallResult install(MinecraftServer server, AgeRecord age) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(age, "age");

        AgeDimensionDefinition definition = AgeDimensionDefinition.from(age);
        if (age.dead()) {
            return new InstallResult(Status.DEAD, definition, null);
        }
        ServerLevel loaded = server.getLevel(definition.levelKey());
        if (loaded != null) {
            return new InstallResult(Status.ALREADY_LOADED, definition, loaded);
        }
        if (!server.isSameThread()) {
            return new InstallResult(Status.WRONG_THREAD, definition, null);
        }

        var preparedOptional = AgeRuntimeDimensionLoader.getPrepared(age.ageUid());
        if (preparedOptional.isEmpty()) {
            return new InstallResult(Status.NOT_PREPARED, definition, null);
        }

        AgeRuntimeDimensionLoader.PreparedAgeDimension prepared = preparedOptional.get();
        if (!sameIdentity(prepared.definition(), definition)
                || !prepared.worldgenPlan().equals(AgeWorldgenPlanner.plan(age))) {
            // Identity alone is insufficient: pages/symbols can define a different
            // generator while retaining the same UID/UUID/seed. Never install a
            // staged LevelStem that no longer matches the durable Age definition.
            return new InstallResult(Status.CONFLICT, definition, null);
        }

        ServerLevel overworld = server.overworld();
        ServerLevelData overworldData = server.getWorldData().overworldData();
        BlockPos fallbackSpawn = overworld.getSharedSpawnPos();
        MystcraftAgeLevelData levelData = new MystcraftAgeLevelData(
                server.getWorldData(), overworldData, age, fallbackSpawn);

        // ServerLevel expects the obfuscated biome seed, exactly as vanilla
        // MinecraftServer#createLevels supplies it. Mystcraft uses the Age's
        // persistent seed rather than the Overworld seed. Vanilla secondary
        // dimensions also share the Overworld RandomSequences SavedData; keep
        // that modern server-wide contract while retaining Mystcraft's own clock.
        long biomeZoomSeed = BiomeManager.obfuscateSeed(age.seed());

        // Vanilla MinecraftServer#createLevels passes its dedicated background
        // worker executor into ServerLevel. Passing MinecraftServer itself here would
        // route ChunkMap/ServerChunkCache worker tasks back onto the main server event
        // loop, defeating the constructor's worker-executor contract and risking
        // stalls/deadlocks under chunk generation pressure. MinecraftServer initializes
        // that private executor from Util.backgroundExecutor(), so use the same public
        // source directly for runtime Ages.
        ServerLevel level = MystcraftRuntimeServerLevel.create(
                server,
                Util.backgroundExecutor(),
                server.storageSource,
                levelData,
                definition.levelKey(),
                prepared.levelStem(),
                NoOpChunkProgressListener.INSTANCE,
                server.getWorldData().isDebugWorld(),
                biomeZoomSeed,
                List.of(),
                true,
                overworld.getRandomSequences(),
                age.seed());

        // ServerLevel/ChunkMap normally derive noise/structure seeds from the server-wide
        // WorldOptions seed. Mystcraft Ages have independent durable seeds, so repair every
        // seed-bearing runtime worldgen object before any chunk can be requested.
        AgeWorldgenSeedRuntime.install(level, age.seed());

        AgeStructureStateInstaller.install(
                level,
                prepared.worldgenPlan().localGenerationPlan(),
                age.seed());

        // Legacy MapGenStructure prepared starts before population but placed structure pieces
        // from each Symbol's IPopulate call. Suppress 1.21.1's earlier biome-decoration
        // placement while retaining the starts/references/locators for the ordered batch.
        AgeLegacyStructurePlacementRuntime.install(level, age.seed());

        Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, ServerLevel> levels =
                server.forgeGetWorldMap();
        synchronized (server) {
            ServerLevel raced = levels.get(definition.levelKey());
            if (raced != null) {
                try {
                    level.close();
                } catch (Exception ignored) {
                    // The raced instance is authoritative; the unregistered
                    // candidate owns no players and must not replace it.
                }
                return new InstallResult(Status.ALREADY_LOADED, definition, raced);
            }
            levels.put(definition.levelKey(), level);
            server.markWorldsDirty();
        }

        // Legacy Mystcraft resolves and persists an Age spawn the first time the
        // world becomes usable. Do this only after registration so chunk loading
        // observes the ServerLevel as part of the live server world set.
        //
        // Runtime-created dimensions also miss MinecraftServer#prepareLevels, the
        // startup-only pass that restores vanilla and NeoForge persistent forced
        // chunks. Replay that state after publishing Load, matching the startup
        // relationship where LevelEvent.Load precedes forced-ticket restoration.
        boolean loadPosted = false;
        try {
            // Vanilla secondary dimensions receive an Overworld-border delegate
            // during MinecraftServer#createLevels. Runtime Ages are born after
            // that startup pass, so install the same contract explicitly.
            AgeRuntimeWorldBorderBridge.attach(overworld, level);
            // Match startup dimensions: publish LevelEvent.Load before any start-region/spawn
            // chunk request. AgeSpawnResolver#getHeightmapPos can synchronously advance chunks
            // through worldgen, so all seed/structure state must already be installed and mods
            // must see the Level.Load lifecycle boundary before that first request.
            loadPosted = true;
            NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
            AgeRuntimeStateSync.ensureInitialSpawn(server, age, level);
            // Legacy MystWorldGenerator built a 5x5 cobblestone pad at the Age world spawn
            // when the spawn chunk populated.  Runtime Ages bypass that old IWorldGenerator
            // hook, so restore the same first-install invariant explicitly.
            AgeLegacySpawnPlatform.ensureForFirstVisit(level, age);
            AgePersistentForcedChunkRuntime.reinstate(level);
            return new InstallResult(Status.INSTALLED, definition, level);
        } catch (RuntimeException | Error failure) {
            synchronized (server) {
                if (levels.get(definition.levelKey()) == level) {
                    levels.remove(definition.levelKey());
                    server.markWorldsDirty();
                }
            }
            if (loadPosted) {
                try {
                    NeoForge.EVENT_BUS.post(new LevelEvent.Unload(level));
                } catch (RuntimeException | Error unloadFailure) {
                    failure.addSuppressed(unloadFailure);
                }
            }
            // If failure occurred before Load was posted, no lifecycle subscriber
            // can detach this runtime-only listener for us. The call is idempotent
            // and also acts as a safety net when Unload listeners throw.
            AgeRuntimeWorldBorderBridge.detach(level);
            try {
                level.close();
            } catch (Exception closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            throw failure;
        }
    }

    private static boolean sameIdentity(AgeDimensionDefinition a, AgeDimensionDefinition b) {
        return a.ageUid() == b.ageUid()
                && a.levelKey().equals(b.levelKey())
                && a.dimensionTypeKey().equals(b.dimensionTypeKey())
                && a.targetUuid().equals(b.targetUuid())
                && a.seed() == b.seed();
    }

    /** Minimal progress listener for dimensions created after startup. */
    private enum NoOpChunkProgressListener implements ChunkProgressListener {
        INSTANCE;

        @Override
        public void updateSpawnPos(ChunkPos center) {}

        @Override
        public void onStatusChange(ChunkPos pos, ChunkStatus status) {}

        @Override
        public void start() {}

        @Override
        public void stop() {}
    }
}
