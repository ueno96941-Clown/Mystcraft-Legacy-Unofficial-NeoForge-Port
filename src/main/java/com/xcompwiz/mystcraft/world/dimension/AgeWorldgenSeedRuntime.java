package com.xcompwiz.mystcraft.world.dimension;

import net.minecraft.server.level.ServerLevel;

import java.util.Objects;

/**
 * Verifies/rebinds the seed-bearing vanilla worldgen state created inside ServerLevel to the
 * durable Mystcraft Age seed.
 *
 * <p>{@link MystcraftRuntimeServerLevel#getSeed()} is already active while the ServerLevel
 * superclass constructs ChunkMap, so ChunkMap's RandomState and ChunkGeneratorStructureState
 * are born with the Age seed. Do not replace that RandomState after construction: the existing
 * generator state retains a reference to it. Only StructureCheck needs repair because vanilla
 * ServerLevel constructs StructureCheck from MinecraftServer's global WorldOptions seed
 * directly instead of calling ServerLevel#getSeed().</p>
 */
public final class AgeWorldgenSeedRuntime {
    private AgeWorldgenSeedRuntime() {}

    public static void install(ServerLevel level, long ageSeed) {
        Objects.requireNonNull(level, "level");

        if (level.getSeed() != ageSeed) {
            throw new IllegalStateException("Mystcraft runtime ServerLevel seed mismatch before worldgen install");
        }

        var chunkSource = level.getChunkSource();
        var randomState = chunkSource.randomState();

        // ChunkMap constructs both RandomState and the ordinary generator structure state from
        // ServerLevel#getSeed(). The subclass construction seed therefore makes these correct
        // before any chunk request can occur. Assert the generator state seed instead of
        // allocating a second RandomState and splitting the two objects onto different instances.
        if (chunkSource.getGeneratorState().getLevelSeed() != ageSeed) {
            throw new IllegalStateException("Mystcraft Age ChunkGeneratorStructureState seed mismatch");
        }

        // ServerLevel itself is the exception: its StructureCheck constructor receives the
        // server-wide WorldOptions seed directly. StructureManager keeps this same object, so
        // repair its seed and RandomState in place before the level is published to the server.
        level.structureCheck.randomState = randomState;
        level.structureCheck.seed = ageSeed;
    }
}
