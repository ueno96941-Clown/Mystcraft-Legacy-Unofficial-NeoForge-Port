package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.agedata.AgeRecord;
import com.xcompwiz.mystcraft.world.agedata.AgeRegistryData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;

import java.util.Objects;

/** Keeps runtime ServerLevel state and the durable Mystcraft Age record aligned. */
public final class AgeRuntimeStateSync {
    private AgeRuntimeStateSync() {}

    /**
     * Establishes the initial spawn once and persists it immediately.
     */
    public static BlockPos ensureInitialSpawn(MinecraftServer server, AgeRecord age, ServerLevel level) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(age, "age");
        Objects.requireNonNull(level, "level");

        boolean registryDirty = false;
        BlockPos spawn = age.spawn();
        if (spawn == null) {
            spawn = AgeSpawnResolver.resolveInitialSpawn(level, age);
            age.setSpawn(spawn);
            AgeRegistryData.get(server).changed();
            registryDirty = true;
        }
        level.setDefaultSpawnPos(spawn, 0.0F);
        AgeStarFissureRuntime.Outcome fissure = AgeStarFissureRuntime.ensureProcessed(level, age);
        if (fissure.processedNow()) {
            if (fissure.generated()) {
                // Generated fissure blocks must reach disk before either one-shot marker does.
                // Otherwise a crash could persist processed=true while the fissure chunks vanish.
                level.save(null, true, false);
                age.setStarFissureGenerated(true);
            }
            age.setStarFissureProcessed(true);
            AgeRegistryData.get(server).changed();
            // Both markers belong to Overworld SavedData, not the Age DimensionDataStorage.
            AgeRegistryData.flush(server);
            registryDirty = false;
        }
        if (level.getLevelData() instanceof MystcraftAgeLevelData data) {
            data.setInitialized(true);
        }
        if (registryDirty) {
            // The method contract says initial spawn persistence is immediate. A dirty flag alone
            // would otherwise wait for the next Overworld autosave.
            AgeRegistryData.flush(server);
        }
        return spawn;
    }

    /**
     * Copies authoritative runtime values into SavedData. Call on successful
     * visits and level save/unload boundaries.
     */
    public static void capture(MinecraftServer server, AgeRecord age, ServerLevel level, boolean visited) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(age, "age");
        Objects.requireNonNull(level, "level");

        age.setSpawn(level.getSharedSpawnPos());
        age.setWorldTime(level.getGameTime());
        age.setDayTime(level.getDayTime());
        ServerLevelData data = (ServerLevelData) level.getLevelData();
        age.setWeatherState(data.getClearWeatherTime(), data.getThunderTime(), data.getRainTime(),
                data.isThundering(), data.isRaining());
        if (visited) age.setVisited(true);
        AgeRegistryData.get(server).changed();
    }

    /** Captures runtime state and makes the global Age registry durable now. */
    public static void captureAndFlush(MinecraftServer server, AgeRecord age, ServerLevel level, boolean visited) {
        capture(server, age, level, visited);
        AgeRegistryData.flush(server);
    }
}
