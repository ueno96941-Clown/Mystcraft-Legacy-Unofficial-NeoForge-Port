package com.xcompwiz.mystcraft.world.dimension;

import com.mojang.serialization.MapCodec;
import com.xcompwiz.mystcraft.world.worldgen.AgeGridBiomeIndex;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.stream.Stream;

/**
 * Runtime-only BiomeSource matching legacy BioConGrid coordinate selection.
 *
 * <p>Age LevelStems are constructed directly at runtime, so serialization is not used for
 * this process-local source. codec() uses an instance-unit codec because runtime Age stems are process-local and are reconstructed from Age data.</p>
 */
public final class MystcraftGridBiomeSource extends BiomeSource {
    /**
     * CP328: a registered, data-bearing codec is required even though Mystcraft rebuilds Ages
     * from AgeRecord data. ModernFix's stronghold cache fingerprints a BiomeSource through
     * BiomeSource.CODEC while ServerLevel is being constructed; an instance-only unit codec
     * cannot be identified by the BIOME_SOURCE dispatch registry and makes that fingerprint
     * fail. Keeping the biome list in the codec also prevents cache aliasing between Ages.
     */
    public static final MapCodec<MystcraftGridBiomeSource> CODEC = Biome.CODEC.listOf()
            .fieldOf("biomes")
            .xmap(MystcraftGridBiomeSource::new, source -> source.biomes)
            .stable();

    private final List<Holder<Biome>> biomes;

    public MystcraftGridBiomeSource(List<Holder<Biome>> biomes) {
        if (biomes.size() < 2) throw new IllegalArgumentException("Grid requires at least 2 biomes");
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
        // Noise-biome coordinates are quart coordinates in modern Minecraft.
        int blockX = x << 2;
        int blockZ = z << 2;
        int index = AgeGridBiomeIndex.indexForBlock(blockX, blockZ, biomes.size());
        return biomes.get(index);
    }
}
