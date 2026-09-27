package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.symbol.LegacyBiomeSymbolDefinition;
import com.xcompwiz.mystcraft.symbol.LegacyBiomeSymbolRegistry;
import com.xcompwiz.mystcraft.world.worldgen.LegacyBiomeSurfaceDefinition;
import com.xcompwiz.mystcraft.world.worldgen.LegacyBiomeSurfaceRegistry;
import com.xcompwiz.mystcraft.world.worldgen.LegacyModernBiomeIdResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Restores the biome surface pass which Legacy ChunkProviderMyst ran after base terrain.
 *
 * <p>Mystcraft 0.13.7.06 called Biome#genTerrainBlocks on every column after the selected
 * ITerrainGenerator had filled the ChunkPrimer. TerrainFlat therefore still received the
 * biome's top/filler blocks (Forest => grass/dirt) when its terrain material was vanilla
 * stone. Modern FlatLevelSource has an empty buildSurface(), so without this bridge a
 * Stone Block + Flat + Forest Age remains bare stone and biome decorators such as trees
 * cannot place.</p>
 *
 * <p>The 1.12 base biome terrain routine only replaced vanilla STONE columns. Keeping that
 * guard is important: an authored End Stone/Netherrack/etc. terrain material must not be
 * silently converted to grass just because the selected biome is Forest.</p>
 */
public final class AgeLegacyFlatBiomeSurfaceFeature extends Feature<NoneFeatureConfiguration> {
    public AgeLegacyFlatBiomeSurfaceFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int chunkX = Math.floorDiv(origin.getX(), 16) * 16;
        int chunkZ = Math.floorDiv(origin.getZ(), 16) * 16;
        boolean changed = false;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; ++dx) {
            for (int dz = 0; dz < 16; ++dz) {
                int x = chunkX + dx;
                int z = chunkZ + dz;
                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (surfaceY <= level.getMinBuildHeight()) continue;

                pos.set(x, surfaceY, z);
                if (!level.getBlockState(pos).is(Blocks.STONE)) continue;

                SurfaceBlocks surface = surfaceBlocks(level, pos);
                level.setBlock(pos, surface.top(), 2);
                changed = true;

                // Vanilla 1.12's ordinary biome surface depth is normally around three blocks.
                // This is deliberately limited to STONE so non-stone Mystcraft terrain materials
                // preserve their authored appearance exactly as the old genTerrainBlocks pass did.
                for (int depth = 1; depth <= 3; ++depth) {
                    pos.set(x, surfaceY - depth, z);
                    if (!level.getBlockState(pos).is(Blocks.STONE)) break;
                    level.setBlock(pos, surface.filler(), 2);
                }
            }
        }
        return changed;
    }

    private static SurfaceBlocks surfaceBlocks(WorldGenLevel level, BlockPos pos) {
        String modernId = level.getBiome(pos).unwrapKey()
                .map(key -> key.location().toString())
                .orElse("");

        // Prefer the explicit 1.12 numeric-biome compatibility table. Several old biomes
        // are aliases in 1.21 (for example Stone Beach -> stony_shore), so path heuristics
        // cannot reliably recover the old top/filler blocks. Synthetic 1.21+/modded biome
        // symbols deliberately fall through to the modern-name compatibility fallback below.
        for (LegacyBiomeSymbolDefinition definition : LegacyBiomeSymbolRegistry.values()) {
            if (definition.legacyNumericId() >= 1_000_000_000) continue;
            if (!LegacyModernBiomeIdResolver.resolveModernId(definition.modernBiomeId()).equals(modernId)) continue;
            LegacyBiomeSurfaceDefinition surface = LegacyBiomeSurfaceRegistry.resolve(definition.legacyId());
            return new SurfaceBlocks(
                    BuiltInRegistries.BLOCK.get(ResourceLocation.parse(surface.topBlockId())).defaultBlockState(),
                    BuiltInRegistries.BLOCK.get(ResourceLocation.parse(surface.fillerBlockId())).defaultBlockState());
        }

        String path = ResourceLocation.tryParse(modernId) == null
                ? ""
                : ResourceLocation.parse(modernId).getPath();

        // Runtime/modded biome fallback where no 1.12 numeric identity exists.
        if (path.contains("mushroom")) {
            return new SurfaceBlocks(Blocks.MYCELIUM.defaultBlockState(), Blocks.DIRT.defaultBlockState());
        }
        if (path.equals("desert") || path.contains("beach") || path.contains("shore")) {
            if (path.contains("stony") || path.contains("stone")) {
                return new SurfaceBlocks(Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState());
            }
            return new SurfaceBlocks(Blocks.SAND.defaultBlockState(), Blocks.SAND.defaultBlockState());
        }
        if (path.contains("badlands")) {
            return new SurfaceBlocks(Blocks.RED_SAND.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState());
        }
        if (path.startsWith("nether_") || path.equals("nether_wastes")) {
            return new SurfaceBlocks(Blocks.NETHERRACK.defaultBlockState(), Blocks.NETHERRACK.defaultBlockState());
        }
        if (path.equals("the_end") || path.contains("end_")) {
            return new SurfaceBlocks(Blocks.END_STONE.defaultBlockState(), Blocks.END_STONE.defaultBlockState());
        }
        return new SurfaceBlocks(Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState());
    }

    private record SurfaceBlocks(BlockState top, BlockState filler) {}
}
