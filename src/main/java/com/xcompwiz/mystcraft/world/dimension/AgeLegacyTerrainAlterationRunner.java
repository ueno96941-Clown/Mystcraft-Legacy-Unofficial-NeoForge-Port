package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.AgeFeatureKind;
import com.xcompwiz.mystcraft.world.worldgen.AgeFeaturePlan;
import com.xcompwiz.mystcraft.world.worldgen.LegacyMapGenCavesKernel;
import com.xcompwiz.mystcraft.world.worldgen.LegacyMapGenRavineKernel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Executes literal/custom terrain alterations against one generated chunk for isolated validation.
 *
 * <p>Call order follows the AgeFeaturePlan/effective Symbol order. This runner is intentionally
 * separated from the final generation-stage hook so the 1.12 algorithms remain testable in
 * isolation.</p>
 */
public final class AgeLegacyTerrainAlterationRunner {
    private AgeLegacyTerrainAlterationRunner() {}

    public static void apply(
            ChunkAccess chunk,
            AgeFeaturePlan featurePlan,
            long ageSeed) {

        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        for (var entry : featurePlan.entries()) {
            switch (entry.kind()) {
                case CAVES -> {
                    var buffer = new ChunkAccessLegacyTerrainBuffer(
                            chunk, Blocks.AIR.defaultBlockState());
                    new LegacyMapGenCavesKernel(ageSeed, 15, 40, false)
                            .generate(chunkX, chunkZ, buffer);
                }
                case RAVINES -> {
                    var buffer = new ChunkAccessLegacyTerrainBuffer(
                            chunk, Blocks.AIR.defaultBlockState());
                    new LegacyMapGenRavineKernel(ageSeed)
                            .generate(chunkX, chunkZ, buffer);
                }
                case TENDRILS -> {
                    Block block = BuiltInRegistries.BLOCK.get(
                            ResourceLocation.parse(entry.materialBlockId()));
                    var buffer = new ChunkAccessLegacyTerrainBuffer(
                            chunk, block.defaultBlockState());
                    new LegacyMapGenCavesKernel(ageSeed, 15, 18, true)
                            .generate(chunkX, chunkZ, buffer);
                }
                case SPHERES -> {
                    Block block = BuiltInRegistries.BLOCK.get(
                            ResourceLocation.parse(entry.materialBlockId()));
                    var buffer = new ChunkAccessLegacyTerrainBuffer(
                            chunk, block.defaultBlockState());
                    new com.xcompwiz.mystcraft.world.worldgen.LegacyMapGenSpheresKernel(ageSeed)
                            .generate(chunkX, chunkZ, buffer);
                }
                case HUGE_TREES -> {
                    var buffer = new ChunkAccessLegacyBigTreeBuffer(chunk);
                    var kernel = new com.xcompwiz.mystcraft.world.worldgen.LegacyWorldGenMystBigTreeKernel(ageSeed);
                    for (int sx = chunkX - 8; sx <= chunkX + 8; sx++) {
                        for (int sz = chunkZ - 8; sz <= chunkZ + 8; sz++) {
                            kernel.generateFromSource(sx, sz, chunkX, chunkZ, buffer);
                        }
                    }
                }
                case SKYLANDS -> {
                    int[] cutoff = new com.xcompwiz.mystcraft.world.worldgen.LegacySkylandsKernel(ageSeed)
                            .cutoffHeights(chunkX, chunkZ);
                    var pos = new net.minecraft.core.BlockPos.MutableBlockPos();
                    int minX = chunk.getPos().getMinBlockX();
                    int minZ = chunk.getPos().getMinBlockZ();
                    int minY = chunk.getMinBuildHeight();
                    int maxY = chunk.getMaxBuildHeight() - 1;
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            int height = ModernAgeHeight.legacySampleY(chunk, cutoff[(x << 4) | z]);
                            for (int y = minY; y <= maxY; y++) {
                                pos.set(minX + x, y, minZ + z);
                                var state = chunk.getBlockState(pos);
                                if (!state.isAir() && (y <= height || !state.getFluidState().isEmpty())) {
                                    chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                                }
                            }
                        }
                    }
                }
                default -> {
                }
            }
        }
    }
}
