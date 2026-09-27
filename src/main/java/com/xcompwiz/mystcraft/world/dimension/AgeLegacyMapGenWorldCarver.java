package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.LegacyMapGenAdvancedKernel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

import java.util.Objects;
import java.util.function.Function;

/**
 * Adapts one legacy MapGen source-chunk contribution to Minecraft 1.21.1's carver pass.
 *
 * <p>Modern ChunkGenerator already walks the same 8-chunk radius (17x17 source chunks)
 * for legacy carvers, so this adapter invokes only one source contribution per callback.
 * The modern RandomSource/config are intentionally ignored: Mystcraft's exact 1.12 seed
 * formula is owned by the compatibility kernel.</p>
 */
public final class AgeLegacyMapGenWorldCarver extends WorldCarver<CarverConfiguration> {
    private final LegacyMapGenAdvancedKernel kernel;
    private final net.minecraft.world.level.block.state.BlockState replacementState;

    public AgeLegacyMapGenWorldCarver(
            LegacyMapGenAdvancedKernel kernel,
            net.minecraft.world.level.block.state.BlockState replacementState) {
        super(CarverConfiguration.CODEC.codec());
        this.kernel = Objects.requireNonNull(kernel, "kernel");
        this.replacementState = Objects.requireNonNull(replacementState, "replacementState");
    }

    @Override
    public boolean isStartChunk(CarverConfiguration config, RandomSource random) {
        // Activation probability is part of the literal Mystcraft kernel (1/15 or 1/50).
        // Returning true ensures every source position reaches that exact check once.
        return true;
    }

    @Override
    public boolean carve(
            CarvingContext context,
            CarverConfiguration config,
            ChunkAccess targetChunk,
            Function<BlockPos, Holder<Biome>> biomeAccessor,
            RandomSource random,
            Aquifer aquifer,
            ChunkPos sourceChunkPos,
            CarvingMask carvingMask) {

        ChunkPos target = targetChunk.getPos();
        ChunkAccessLegacyTerrainBuffer buffer =
                new ChunkAccessLegacyTerrainBuffer(targetChunk, replacementState);

        synchronized (kernel) {
            kernel.generateFromSource(
                    sourceChunkPos.x,
                    sourceChunkPos.z,
                    target.x,
                    target.z,
                    buffer);
        }
        return true;
    }

}
