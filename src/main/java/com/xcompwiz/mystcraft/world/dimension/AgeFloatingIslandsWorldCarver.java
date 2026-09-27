package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.LegacyMapGenFloatingIslandsKernel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

import java.util.Objects;
import java.util.function.Function;

/**
 * Runtime CARVERS-stage adapter for the legacy floating-island geometry.
 *
 * <p>The column mask is handed to `AgeFloatingIslandColumnMasks`, which unions source
 * contributions for the target chunk. 13H-10 consumes that mask for legacy surface/filler
 * and quart-biome replacement.</p>
 */
public final class AgeFloatingIslandsWorldCarver extends WorldCarver<CarverConfiguration> {
    private final LegacyMapGenFloatingIslandsKernel kernel;
    private final BlockState replacement;
    private final BlockState top;
    private final BlockState filler;
    private final BlockState sandstone;
    private final String biomeLegacySymbol;
    private final AgeFloatingIslandColumnMasks masks = new AgeFloatingIslandColumnMasks();

    public AgeFloatingIslandsWorldCarver(
            LegacyMapGenFloatingIslandsKernel kernel,
            BlockState replacement,
            String biomeLegacySymbol) {
        super(CarverConfiguration.CODEC.codec());
        this.kernel = Objects.requireNonNull(kernel, "kernel");
        this.replacement = Objects.requireNonNull(replacement, "replacement");
        this.biomeLegacySymbol = biomeLegacySymbol;

        var surface = com.xcompwiz.mystcraft.world.worldgen.LegacyBiomeSurfaceRegistry.resolve(
                biomeLegacySymbol);
        this.top = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.parse(surface.topBlockId()))
                .defaultBlockState();
        this.filler = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.parse(surface.fillerBlockId()))
                .defaultBlockState();
        this.sandstone = net.minecraft.world.level.block.Blocks.SANDSTONE.defaultBlockState();
    }

    @Override
    public boolean isStartChunk(CarverConfiguration config, RandomSource random) {
        // Exact 1/192 activation remains inside the legacy kernel.
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
        var buffer = new ChunkAccessFloatingIslandBuffer(
                targetChunk, replacement, top, filler, sandstone);

        // Legacy MapGen uses one mutable Random and one NoiseGeneratorOctaves instance.
        // Synchronizing preserves that execution model under modern parallel chunk generation.
        synchronized (kernel) {
            kernel.generateFromSource(
                    sourceChunkPos.x,
                    sourceChunkPos.z,
                    target.x,
                    target.z,
                    buffer);
        }

        masks.mergeAndMaybeComplete(
                target,
                buffer.copyModifiedColumns(),
                biomeLegacySymbol)
                .ifPresent(entry -> AgeFloatingIslandBiomeFinalizer.apply(
                        context.registryAccess(),
                        targetChunk,
                        entry.columns(),
                        entry.biomeLegacySymbol()));

        return true;
    }
}
