package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.agedata.AgeRecord;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable bridge from persisted Age identity to modern dimension keys.
 *
 * <p>This deliberately does not create a ServerLevel. It is the hand-off
 * object the future dimension loader/worldgen layer will consume.</p>
 */
public record AgeDimensionDefinition(
        int ageUid,
        ResourceKey<Level> levelKey,
        ResourceKey<DimensionType> dimensionTypeKey,
        UUID targetUuid,
        long seed) {

    public AgeDimensionDefinition {
        if (ageUid < 2) throw new IllegalArgumentException("Invalid Mystcraft AgeUID: " + ageUid);
        Objects.requireNonNull(levelKey, "levelKey");
        Objects.requireNonNull(dimensionTypeKey, "dimensionTypeKey");
        Objects.requireNonNull(targetUuid, "targetUuid");
    }

    public static AgeDimensionDefinition from(AgeRecord age) {
        Objects.requireNonNull(age, "age");
        if (!AgeDimensionKeys.matches(age.ageUid(), age.dimensionKey())) {
            throw new IllegalStateException(
                    "Age " + age.ageUid() + " has non-canonical DimensionKey: " + age.dimensionKey());
        }
        return new AgeDimensionDefinition(
                age.ageUid(),
                AgeDimensionKeys.levelKey(age.ageUid()),
                AgeDimensionKeys.AGE_DIMENSION_TYPE,
                age.uuid(),
                age.seed());
    }

    /** Resource key for the LevelStem definition paired with this Age. */
    public ResourceKey<LevelStem> levelStemKey() {
        return AgeDimensionKeys.levelStemKey(ageUid);
    }

    /** Returns the already-loaded world only; no dynamic creation occurs here. */
    public Optional<ServerLevel> findLoadedLevel(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return Optional.ofNullable(server.getLevel(levelKey));
    }
}
