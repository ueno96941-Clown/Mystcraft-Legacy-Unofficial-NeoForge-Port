package com.xcompwiz.mystcraft.world.dimension;

import com.mojang.serialization.MapCodec;
import com.xcompwiz.mystcraft.world.worldgen.AgeTiledBiomeIndex;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.stream.Stream;

/** Runtime-only BiomeSource matching legacy BioConTiled coordinate selection. */
public final class MystcraftTiledBiomeSource extends BiomeSource {
    /** See {@link MystcraftGridBiomeSource#CODEC} for the CP328 compatibility rationale. */
    public static final MapCodec<MystcraftTiledBiomeSource> CODEC = Biome.CODEC.listOf()
            .fieldOf("biomes")
            .xmap(MystcraftTiledBiomeSource::new, source -> source.biomes)
            .stable();

    private final List<Holder<Biome>> biomes;

    public MystcraftTiledBiomeSource(List<Holder<Biome>> biomes) {
        if (biomes.size() < 2) throw new IllegalArgumentException("Tiled requires at least 2 biomes");
        this.biomes = List.copyOf(biomes);
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return biomes.stream();
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        // Modern getNoiseBiome receives quart/generation coordinates. Legacy Tiled's
        // generation query passed those coordinates straight into getBiomeAtCoords,
        // unlike Grid which multiplied them by four first.
        int index = AgeTiledBiomeIndex.indexForGenerationCell(x, z, biomes.size());
        return biomes.get(index);
    }
}
