package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.LegacyModernBiomeIdResolver;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;

/**
 * Converts the legacy 16x16 column biome mask into Minecraft 1.21.1's 4x4x4 quart-biome palette.
 *
 * <p>1.21.1 cannot represent 1-biome-per-block-column. The compatibility rule is therefore:
 * if any of the sixteen old columns inside a 4x4 quart cell were modified, that quart cell is
 * assigned the FloatingIslands biome for all Y quart cells. This intentionally preserves all
 * legacy affected columns at the cost of expanding edges by at most three blocks.</p>
 */
public final class AgeFloatingIslandBiomeFinalizer {
    private AgeFloatingIslandBiomeFinalizer() {}

    public static void apply(
            RegistryAccess registries,
            ChunkAccess chunk,
            boolean[] modifiedColumns,
            String legacyBiomeSymbol) {

        if (modifiedColumns.length != 256) {
            throw new IllegalArgumentException("floating island mask must be 256 columns");
        }

        var biomeRegistry = registries.registryOrThrow(Registries.BIOME);
        String modernId = LegacyModernBiomeIdResolver.resolve(legacyBiomeSymbol);
        Holder.Reference<Biome> targetBiome = biomeRegistry.getHolderOrThrow(
                ResourceKey.create(Registries.BIOME, ResourceLocation.parse(modernId)));

        boolean[][] quartXZ = new boolean[4][4];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                if (modifiedColumns[x + z * 16]) {
                    quartXZ[x >> 2][z >> 2] = true;
                }
            }
        }

        for (LevelChunkSection section : chunk.getSections()) {
            PalettedContainerRO<Holder<Biome>> existing = section.getBiomes();
            PalettedContainer<Holder<Biome>> mutable;

            if (existing instanceof PalettedContainer<?> raw) {
                @SuppressWarnings("unchecked")
                PalettedContainer<Holder<Biome>> cast =
                        (PalettedContainer<Holder<Biome>>) raw;
                mutable = cast;
            } else {
                mutable = new PalettedContainer<>(
                        biomeRegistry.asHolderIdMap(),
                        existing.get(0, 0, 0),
                        PalettedContainer.Strategy.SECTION_BIOMES);
                for (int qy = 0; qy < 4; qy++) {
                    for (int qz = 0; qz < 4; qz++) {
                        for (int qx = 0; qx < 4; qx++) {
                            mutable.set(qx, qy, qz, existing.get(qx, qy, qz));
                        }
                    }
                }
                section.biomes = mutable;
            }

            for (int qy = 0; qy < 4; qy++) {
                for (int qz = 0; qz < 4; qz++) {
                    for (int qx = 0; qx < 4; qx++) {
                        if (quartXZ[qx][qz]) {
                            mutable.set(qx, qy, qz, targetBiome);
                        }
                    }
                }
            }
        }
    }
}
