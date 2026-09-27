package com.xcompwiz.mystcraft.world.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xcompwiz.mystcraft.world.worldgen.LegacyBiomeLayer;
import com.xcompwiz.mystcraft.world.worldgen.LegacyZoomBiomeLayerFactory;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.stream.Stream;

/** Runtime-only generation BiomeSource for Huge/Large/Medium/Small/Tiny. */
public final class MystcraftZoomBiomeSource extends BiomeSource {
    /**
     * CP328: encode all inputs that affect the positional biome layout so external worldgen
     * caches (notably ModernFix's stronghold cache) get a stable, Age-specific fingerprint.
     */
    public static final MapCodec<MystcraftZoomBiomeSource> CODEC = RecordCodecBuilder.<MystcraftZoomBiomeSource>mapCodec(instance -> instance.group(
            Codec.LONG.fieldOf("age_seed").forGetter((MystcraftZoomBiomeSource source) -> source.ageSeed),
            Codec.INT.fieldOf("zoom_scale").forGetter((MystcraftZoomBiomeSource source) -> source.zoomScale),
            Biome.CODEC.listOf().fieldOf("biomes").forGetter((MystcraftZoomBiomeSource source) -> source.biomes)
    ).apply(instance, MystcraftZoomBiomeSource::new)).stable();

    private final long ageSeed;
    private final int zoomScale;
    private final List<Holder<Biome>> biomes;
    private final LegacyBiomeLayer layer;

    public MystcraftZoomBiomeSource(long ageSeed, int zoomScale, List<Holder<Biome>> biomes) {
        if (biomes.size() < 3) throw new IllegalArgumentException("Zoom family requires at least 3 biomes");
        this.ageSeed = ageSeed;
        this.zoomScale = zoomScale;
        this.biomes = List.copyOf(biomes);
        this.layer = LegacyZoomBiomeLayerFactory.create(ageSeed, zoomScale, biomes.size());
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
        int index = layer.sample(x, z);
        return biomes.get(index);
    }
}
