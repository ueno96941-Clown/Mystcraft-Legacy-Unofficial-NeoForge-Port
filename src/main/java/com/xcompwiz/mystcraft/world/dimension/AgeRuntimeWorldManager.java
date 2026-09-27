package com.xcompwiz.mystcraft.world.dimension;

import com.mojang.serialization.Dynamic;
import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import com.xcompwiz.mystcraft.world.agedata.AgeRecord;
import com.xcompwiz.mystcraft.world.agedata.AgeRegistryData;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Runtime lifecycle owner for dynamically installed Mystcraft Ages.
 *
 * <p>Persisted Ages are restored as live ServerLevels during server startup.
 * Player data is resolved after the server startup lifecycle; keeping a saved
 * Mystcraft dimension lazy at that boundary causes vanilla to treat the saved
 * player dimension as missing and fall back to the Overworld spawn. Eagerly
 * installing durable Ages before players join preserves vanilla player
 * dimension/position restoration across restarts.</p>
 */
@SuppressWarnings("deprecation")
public final class AgeRuntimeWorldManager {
    private static final long IDLE_UNLOAD_DELAY_TICKS = 100L;
    private static final int MAX_IDLE_UNLOADS_PER_TICK = 1;
    private static final Map<MinecraftServer, IdleUnloadState> IDLE_UNLOAD_STATES = new IdentityHashMap<>();

    private AgeRuntimeWorldManager() {}

    private static final class IdleUnloadState {
        private boolean armed;
        private long tick;
        private final Map<ResourceKey<Level>, Long> idleSince = new HashMap<>();
        private final Map<ResourceKey<Level>, Long> portalHoldUntil = new HashMap<>();
    }

    public enum UnloadStatus {
        NOT_LOADED,
        UNLOADED,
        IN_USE,
        NOT_MYSTCRAFT_AGE,
        WRONG_THREAD,
        CONFLICT,
        CLOSE_FAILED
    }

    public record UnloadResult(UnloadStatus status, AgeRecord age, ServerLevel level, Throwable error) {
        public boolean unloaded() {
            return status == UnloadStatus.UNLOADED;
        }
    }

    /**
     * Rebuilds durable Age definitions before player login.
     *
     * <p>Dedicated servers retain the conservative CP248/249 behavior and install
     * every durable Age because arbitrary offline players can reconnect into any
     * saved dimension. Integrated servers have exactly one owner player whose
     * pre-login NBT is already available from WorldData. For that case we install
     * only the owner's saved Mystcraft dimension and leave every other Age merely
     * prepared. Vanilla can therefore restore the owner to the exact saved Age,
     * while a large single-player library no longer creates/ticks every ServerLevel
     * during startup.</p>
     */
    public static RestoreReport restorePreparedAges(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        AgeRuntimeDimensionLoader.clearPrepared();

        IntegratedLoginTarget loginTarget = resolveIntegratedLoginTarget(server);
        if (server.isSingleplayer()) {
            Mystcraft.LOGGER.debug(
                    "Integrated Age restore mode: {}, loginDimension={}",
                    loginTarget.mode(),
                    loginTarget.dimension() == null ? "<none>" : loginTarget.dimension().location());
        }

        int prepared = 0;
        int installed = 0;
        int alreadyLoaded = 0;
        int skippedDead = 0;
        int conflicts = 0;
        int failures = 0;
        for (AgeRecord age : AgeRegistryData.get(server).all()) {
            if (age.dead()) {
                skippedDead++;
                continue;
            }
            try {
                AgeRuntimeDimensionLoader.PrepareResult result = AgeRuntimeDimensionLoader.prepare(server, age);
                switch (result.status()) {
                    case PREPARED, ALREADY_PREPARED -> prepared++;
                    case ALREADY_LOADED -> alreadyLoaded++;
                    case DEAD -> {
                        skippedDead++;
                        continue;
                    }
                    case CONFLICT -> {
                        conflicts++;
                        Mystcraft.LOGGER.error(
                                "Cannot restore Mystcraft Age {} ({}): prepared definition conflict",
                                age.ageUid(), age.dimensionKey());
                        continue;
                    }
                }

                if (!shouldInstallDuringStartup(server, loginTarget, age)) {
                    continue;
                }

                AgeRuntimeServerLevelInstaller.InstallResult install =
                        AgeRuntimeServerLevelInstaller.install(server, age);
                switch (install.status()) {
                    case INSTALLED -> installed++;
                    case ALREADY_LOADED -> alreadyLoaded++;
                    case DEAD -> skippedDead++;
                    case CONFLICT, NOT_PREPARED, WRONG_THREAD -> {
                        failures++;
                        Mystcraft.LOGGER.error(
                                "Cannot install restored Mystcraft Age {} ({}): {}",
                                age.ageUid(), age.dimensionKey(), install.status());
                    }
                }
            } catch (RuntimeException | Error failure) {
                failures++;
                // One damaged Age must not make the entire save permanently
                // unopenable. Keep the failure visible and continue restoring
                // the remaining durable Ages.
                Mystcraft.LOGGER.error(
                        "Failed to restore Mystcraft Age {} ({})",
                        age.ageUid(), age.dimensionKey(), failure);
            }
        }
        return new RestoreReport(prepared, installed, alreadyLoaded, skippedDead, conflicts, failures);
    }

    private static boolean shouldInstallDuringStartup(
            MinecraftServer server, IntegratedLoginTarget loginTarget, AgeRecord age) {
        if (!server.isSingleplayer()) {
            // Dedicated-server late join remains conservative until a dedicated
            // pre-login playerdata resolver is implemented/tested.
            return true;
        }
        if (loginTarget.mode() == IntegratedLoginMode.UNREADABLE) {
            // Never sacrifice restart correctness when an existing owner tag cannot
            // be decoded. This is the rare safety fallback to CP249 eager restore.
            return true;
        }
        ResourceKey<Level> wanted = loginTarget.dimension();
        return wanted != null && wanted.equals(AgeDimensionDefinition.from(age).levelKey());
    }

    private static IntegratedLoginTarget resolveIntegratedLoginTarget(MinecraftServer server) {
        if (!server.isSingleplayer()) {
            return new IntegratedLoginTarget(IntegratedLoginMode.NOT_APPLICABLE, null);
        }
        var playerTag = server.getWorldData().getLoadedPlayerTag();
        if (playerTag == null) {
            return new IntegratedLoginTarget(IntegratedLoginMode.NO_SAVED_PLAYER, null);
        }
        if (!playerTag.contains("Dimension")) {
            Mystcraft.LOGGER.warn("Integrated player tag has no Dimension field; using eager Age restore fallback");
            return new IntegratedLoginTarget(IntegratedLoginMode.UNREADABLE, null);
        }
        try {
            ResourceKey<Level> dimension = DimensionType.parseLegacy(
                            new Dynamic<>(NbtOps.INSTANCE, playerTag.get("Dimension")))
                    .resultOrPartial(message -> Mystcraft.LOGGER.warn(
                            "Could not parse integrated player Dimension: {}", message))
                    .orElse(null);
            if (dimension == null) {
                return new IntegratedLoginTarget(IntegratedLoginMode.UNREADABLE, null);
            }
            return new IntegratedLoginTarget(IntegratedLoginMode.RESOLVED, dimension);
        } catch (RuntimeException failure) {
            Mystcraft.LOGGER.warn(
                    "Failed to inspect integrated player's saved Dimension; using eager Age restore fallback",
                    failure);
            return new IntegratedLoginTarget(IntegratedLoginMode.UNREADABLE, null);
        }
    }

    private enum IntegratedLoginMode {
        NOT_APPLICABLE,
        NO_SAVED_PLAYER,
        RESOLVED,
        UNREADABLE
    }

    private record IntegratedLoginTarget(IntegratedLoginMode mode, ResourceKey<Level> dimension) {}

    public record RestoreReport(
            int prepared,
            int installed,
            int alreadyLoaded,
            int skippedDead,
            int conflicts,
            int failures) {}

    /**
     * Saves and removes one live Mystcraft Age from the server world map.
     * Ages containing players are deliberately not unloaded.
     */
    public static UnloadResult unload(MinecraftServer server, ResourceKey<Level> levelKey) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(levelKey, "levelKey");
        if (!server.isSameThread()) {
            return new UnloadResult(UnloadStatus.WRONG_THREAD, null, null, null);
        }

        ServerLevel level = server.getLevel(levelKey);
        if (level == null) {
            return new UnloadResult(UnloadStatus.NOT_LOADED, null, null, null);
        }

        AgeRecord age = AgeManager.resolveByLevel(server, levelKey).orElse(null);
        if (age == null || !levelKey.location().getNamespace().equals(Mystcraft.MOD_ID)) {
            return new UnloadResult(UnloadStatus.NOT_MYSTCRAFT_AGE, age, level, null);
        }
        if (!level.players().isEmpty() || ForcedChunkManager.hasForcedChunks(level)) {
            // Do not tear down an Age that is explicitly kept alive by players or
            // persistent vanilla/NeoForge forced-chunk ownership. NeoForge's own
            // ServerLevel idle-tick gate uses this combined check; using only
            // ServerLevel#getForcedChunks would miss mod-owned block/entity tickets.
            // Transient internal tickets are drained by ServerLevel.close().
            return new UnloadResult(UnloadStatus.IN_USE, age, level, null);
        }

        Map<ResourceKey<Level>, ServerLevel> levels = server.forgeGetWorldMap();
        // Validate ownership before emitting save/unload lifecycle effects. A raced replacement must
        // never receive a spurious Unload event for the still-live authoritative world.
        synchronized (server) {
            if (levels.get(levelKey) != level) {
                return new UnloadResult(UnloadStatus.CONFLICT, age, level, null);
            }
        }

        AgeRuntimeStateSync.capture(server, age, level, age.visited());
        level.save(null, true, false);

        // Remove first, then publish Unload: observers now see the same world-map state implied by the event.
        synchronized (server) {
            if (levels.get(levelKey) != level) {
                return new UnloadResult(UnloadStatus.CONFLICT, age, level, null);
            }
            levels.remove(levelKey);
            // NeoForge tracks the last 100 tick durations in an IdentityHashMap keyed
            // by ServerLevel#dimension(). forgeGetWorldMap()/markWorldsDirty() do not
            // prune that side map, so remove the exact key object owned by this closing
            // level. Otherwise repeatedly opening many Ages retains stale tick arrays.
            server.perWorldTickTimes.remove(level.dimension());
            server.markWorldsDirty();
        }
        NeoForge.EVENT_BUS.post(new LevelEvent.Unload(level));

        try {
            level.close();
        } catch (IOException ex) {
            Mystcraft.LOGGER.error("Failed to close runtime Mystcraft Age {} ({})",
                    age.ageUid(), levelKey.location(), ex);
            return new UnloadResult(UnloadStatus.CLOSE_FAILED, age, level, ex);
        }
        return new UnloadResult(UnloadStatus.UNLOADED, age, level, null);
    }

    /**
     * Arms automatic idle unloading only after a real player has completed login.
     *
     * <p>Startup restoration deliberately keeps every durable Age live until this
     * point so vanilla can resolve a player's saved Mystcraft dimension before any
     * restored Age is allowed to disappear again.</p>
     */
    public static void armIdleUnloading(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        if (!server.isSameThread()) {
            throw new IllegalStateException("Runtime Age idle manager must be armed on the server thread");
        }
        IdleUnloadState state = IDLE_UNLOAD_STATES.computeIfAbsent(server, ignored -> new IdleUnloadState());
        if (!state.armed) {
            state.armed = true;
            state.tick = 0L;
            state.idleSince.clear();
            Mystcraft.LOGGER.debug(
                    "Age idle unload armed after player login: delayTicks={}, liveAges={}",
                    IDLE_UNLOAD_DELAY_TICKS, countLiveAges(server));
        }
    }


    /**
     * Temporarily pins an Age while a Crystal Portal transfer is being assembled.
     *
     * <p>CP249's 100-tick idle unload remains the normal policy.  Portal traffic
     * only needs a short transactional hold so vehicles and their passengers can
     * cross on adjacent server ticks without the destination disappearing between
     * members of the same transfer group.</p>
     */
    public static void holdForPortalTransfer(MinecraftServer server, ResourceKey<Level> levelKey, long ticks) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(levelKey, "levelKey");
        if (!server.isSameThread()) {
            throw new IllegalStateException("Mystcraft portal Age hold must run on the server thread");
        }
        if (!levelKey.location().getNamespace().equals(Mystcraft.MOD_ID)) return;
        IdleUnloadState state = IDLE_UNLOAD_STATES.computeIfAbsent(server, ignored -> new IdleUnloadState());
        long until = state.tick + Math.max(1L, ticks);
        state.portalHoldUntil.merge(levelKey, until, Math::max);
        state.idleSince.remove(levelKey);
    }

    /**
     * Runs once after the server tick. Empty, unforced Mystcraft Ages are saved and
     * closed after a 100 tick grace period. Re-entry before the deadline cancels the
     * timer, avoiding unload/reinstall churn during short round trips.
     */
    public static void tickIdleUnloading(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        if (!server.isSameThread()) return;

        IdleUnloadState state = IDLE_UNLOAD_STATES.computeIfAbsent(server, ignored -> new IdleUnloadState());
        if (!state.armed) return;
        long now = ++state.tick;

        List<ServerLevel> snapshot = new ArrayList<>(server.forgeGetWorldMap().values());
        int unloadAttempts = 0;
        for (ServerLevel level : snapshot) {
            ResourceKey<Level> levelKey = level.dimension();
            if (!levelKey.location().getNamespace().equals(Mystcraft.MOD_ID)) continue;

            AgeRecord age = AgeManager.resolveByLevel(server, levelKey).orElse(null);
            if (age == null || age.dead()) {
                state.idleSince.remove(levelKey);
                continue;
            }

            Long portalHold = state.portalHoldUntil.get(levelKey);
            if (portalHold != null) {
                if (portalHold > now) {
                    state.idleSince.remove(levelKey);
                    continue;
                }
                state.portalHoldUntil.remove(levelKey);
            }

            boolean hasPlayers = !level.players().isEmpty();
            boolean forced = ForcedChunkManager.hasForcedChunks(level);
            if (hasPlayers || forced) {
                Long idleStart = state.idleSince.remove(levelKey);
                if (idleStart != null && hasPlayers) {
                    Mystcraft.LOGGER.debug(
                            "Age idle timer cancelled by player re-entry: {} after {} ticks",
                            levelKey.location(), now - idleStart);
                } else if (idleStart != null) {
                    Mystcraft.LOGGER.debug(
                            "Age idle timer cancelled by forced chunks: {} after {} ticks",
                            levelKey.location(), now - idleStart);
                }
                continue;
            }

            Long idleStart = state.idleSince.get(levelKey);
            if (idleStart == null) {
                state.idleSince.put(levelKey, now);
                Mystcraft.LOGGER.debug(
                        "Age idle timer started: {} ({} ticks grace)",
                        levelKey.location(), IDLE_UNLOAD_DELAY_TICKS);
                continue;
            }

            long idleTicks = now - idleStart;
            if (idleTicks < IDLE_UNLOAD_DELAY_TICKS) continue;
            if (unloadAttempts >= MAX_IDLE_UNLOADS_PER_TICK) continue;
            unloadAttempts++;

            // Remove the deadline before unload. Any refusal starts a fresh grace
            // period on a later tick instead of retrying every tick and spamming IO/logs.
            state.idleSince.remove(levelKey);
            UnloadResult result = unload(server, levelKey);
            if (result.unloaded()) {
                Mystcraft.LOGGER.debug(
                        "Age idle unload: {} after {} ticks, liveAges={}",
                        levelKey.location(), idleTicks, countLiveAges(server));
            } else if (result.status() != UnloadStatus.NOT_LOADED) {
                Mystcraft.LOGGER.debug(
                        "Age idle unload deferred: {} status={}, liveAges={}",
                        levelKey.location(), result.status(), countLiveAges(server));
            }
        }

        // Drop timers for worlds that were removed by another lifecycle path.
        state.idleSince.keySet().removeIf(key -> server.getLevel(key) == null);
        state.portalHoldUntil.keySet().removeIf(key -> server.getLevel(key) == null);
    }

    /** Clears integrated/dedicated server instance state across shutdown/restart. */
    public static void clearIdleUnloading(MinecraftServer server) {
        if (server != null) IDLE_UNLOAD_STATES.remove(server);
    }

    public static int countLiveAges(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        int count = 0;
        for (ResourceKey<Level> key : server.forgeGetWorldMap().keySet()) {
            if (key.location().getNamespace().equals(Mystcraft.MOD_ID)
                    && AgeManager.resolveByLevel(server, key).isPresent()) {
                count++;
            }
        }
        return count;
    }

    /** Captures all currently loaded Ages before the normal server save/close sequence. */
    public static int captureAllLoaded(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        if (!server.isSameThread()) {
            throw new IllegalStateException("Runtime Age capture must run on the server thread");
        }
        int captured = 0;
        List<ServerLevel> snapshot = new ArrayList<>(server.forgeGetWorldMap().values());
        for (ServerLevel level : snapshot) {
            if (!level.dimension().location().getNamespace().equals(Mystcraft.MOD_ID)) continue;
            AgeRecord age = AgeManager.resolveByLevel(server, level.dimension()).orElse(null);
            if (age == null) continue;
            AgeRuntimeStateSync.capture(server, age, level, age.visited());
            captured++;
        }
        return captured;
    }
}
