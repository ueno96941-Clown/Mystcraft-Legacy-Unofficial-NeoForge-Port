package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.agedata.AgeRecord;
import com.xcompwiz.mystcraft.world.worldgen.AgeWorldgenPlan;
import com.xcompwiz.mystcraft.world.worldgen.AgeWorldgenPlanner;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.LevelStem;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime hand-off layer for reserved Mystcraft Ages.
 *
 * <p>Minecraft 1.21.1 builds and freezes its worldgen registries during server
 * startup. This class therefore does not mutate the frozen LEVEL_STEM registry
 * and does not construct ServerLevel directly. Instead it validates a reserved
 * Age, builds the complete LevelStem contract, and stages that immutable
 * definition for {@link AgeRuntimeServerLevelInstaller}. The staged definition
 * is process-local and is rebuilt from SavedData after each server restart.</p>
 */
public final class AgeRuntimeDimensionLoader {
    private AgeRuntimeDimensionLoader() {}

    private static final Map<ResourceKey<LevelStem>, PreparedAgeDimension> PREPARED =
            new ConcurrentHashMap<>();

    public enum Status {
        ALREADY_LOADED,
        PREPARED,
        ALREADY_PREPARED,
        DEAD,
        CONFLICT
    }

    public record PreparedAgeDimension(
            AgeDimensionDefinition definition,
            AgeWorldgenPlan worldgenPlan,
            LevelStem levelStem) {
        public PreparedAgeDimension {
            Objects.requireNonNull(definition, "definition");
            Objects.requireNonNull(worldgenPlan, "worldgenPlan");
            Objects.requireNonNull(levelStem, "levelStem");
        }
    }

    public record PrepareResult(
            Status status,
            AgeDimensionDefinition definition,
            ServerLevel loadedLevel,
            PreparedAgeDimension prepared) {

        public boolean readyForRuntimeInstallation() {
            return status == Status.PREPARED || status == Status.ALREADY_PREPARED;
        }
        public boolean hasPreparedDefinition() {
            return prepared != null && (status == Status.PREPARED
                    || status == Status.ALREADY_PREPARED || status == Status.ALREADY_LOADED);
        }
    }

    /**
     * Resolves a reserved Age into the next safe runtime state.
     *
     * <p>This operation is idempotent. Repeated link attempts for the same Age
     * reuse the same staged definition instead of producing a second Age or a
     * second LevelStem.</p>
     */
    public static PrepareResult prepare(MinecraftServer server, AgeRecord age) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(age, "age");

        AgeDimensionDefinition definition = AgeDimensionDefinition.from(age);
        if (age.dead()) {
            return new PrepareResult(Status.DEAD, definition, null, null);
        }
        Optional<ServerLevel> loaded = definition.findLoadedLevel(server);
        ResourceKey<LevelStem> stemKey = definition.levelStemKey();
        AgeWorldgenPlan plan = AgeWorldgenPlanner.plan(age);
        if (loaded.isPresent()) {
            PreparedAgeDimension existing = PREPARED.get(stemKey);
            if (existing != null && sameIdentity(existing.definition(), definition)
                    && existing.worldgenPlan().equals(plan)) {
                return new PrepareResult(Status.ALREADY_LOADED, definition, loaded.get(), existing);
            }
            // A live dynamic Age should normally retain its prepared definition for
            // visual synchronization. Reconstruct it only if no conflicting staged
            // definition exists; never silently replace a mismatch.
            if (existing == null) {
                LevelStem stem = AgeLevelStemFactory.create(server.registryAccess(), definition, plan);
                PreparedAgeDimension rebuilt = new PreparedAgeDimension(definition, plan, stem);
                PreparedAgeDimension raced = PREPARED.putIfAbsent(stemKey, rebuilt);
                if (raced == null || (sameIdentity(raced.definition(), definition)
                        && raced.worldgenPlan().equals(plan))) {
                    return new PrepareResult(Status.ALREADY_LOADED, definition, loaded.get(),
                            raced == null ? rebuilt : raced);
                }
            }
            return new PrepareResult(Status.CONFLICT, definition, loaded.get(), existing);
        }
        PreparedAgeDimension existing = PREPARED.get(stemKey);
        if (existing != null) {
            if (sameIdentity(existing.definition(), definition) && existing.worldgenPlan().equals(plan)) {
                return new PrepareResult(Status.ALREADY_PREPARED, definition, null, existing);
            }
            return new PrepareResult(Status.CONFLICT, definition, null, existing);
        }

        LevelStem stem = AgeLevelStemFactory.create(server.registryAccess(), definition, plan);
        PreparedAgeDimension created = new PreparedAgeDimension(definition, plan, stem);
        PreparedAgeDimension raced = PREPARED.putIfAbsent(stemKey, created);
        if (raced == null) {
            return new PrepareResult(Status.PREPARED, definition, null, created);
        }
        if (sameIdentity(raced.definition(), definition) && raced.worldgenPlan().equals(plan)) {
            return new PrepareResult(Status.ALREADY_PREPARED, definition, null, raced);
        }
        return new PrepareResult(Status.CONFLICT, definition, null, raced);
    }

    public static Optional<PreparedAgeDimension> getPrepared(int ageUid) {
        return Optional.ofNullable(PREPARED.get(AgeDimensionKeys.levelStemKey(ageUid)));
    }

    /** Clears process-local staging state during server teardown/tests. */
    public static void clearPrepared() {
        PREPARED.clear();
    }

    private static boolean sameIdentity(AgeDimensionDefinition a, AgeDimensionDefinition b) {
        UUID au = a.targetUuid();
        UUID bu = b.targetUuid();
        return a.ageUid() == b.ageUid()
                && a.levelKey().equals(b.levelKey())
                && a.dimensionTypeKey().equals(b.dimensionTypeKey())
                && au.equals(bu)
                && a.seed() == b.seed();
    }
}
