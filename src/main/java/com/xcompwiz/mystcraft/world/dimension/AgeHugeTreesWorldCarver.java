package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.LegacyWorldGenMystBigTreeKernel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

import java.util.Objects;
import java.util.function.Function;

/** CARVERS-stage adapter preserving the legacy MapGenAdvanced 17x17 source contribution model. */
public final class AgeHugeTreesWorldCarver extends WorldCarver<CarverConfiguration> {
    private final LegacyWorldGenMystBigTreeKernel kernel;

    public AgeHugeTreesWorldCarver(LegacyWorldGenMystBigTreeKernel kernel) {
        super(CarverConfiguration.CODEC.codec());
        this.kernel = Objects.requireNonNull(kernel, "kernel");
    }

    @Override
    public boolean isStartChunk(CarverConfiguration config, RandomSource random) {
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
        var buffer = new ChunkAccessLegacyBigTreeBuffer(targetChunk);
        kernel.generateFromSource(
                sourceChunkPos.x,
                sourceChunkPos.z,
                target.x,
                target.z,
                buffer);
        return true;
    }
}
