package com.xcompwiz.mystcraft.registry;

import com.mojang.serialization.MapCodec;
import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.world.dimension.MystcraftGridBiomeSource;
import com.xcompwiz.mystcraft.world.dimension.MystcraftTiledBiomeSource;
import com.xcompwiz.mystcraft.world.dimension.MystcraftZoomBiomeSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.biome.BiomeSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registers Mystcraft's runtime biome-source codecs with Minecraft's dispatch registry.
 *
 * <p>CP328 adds this registration for compatibility with systems that legitimately serialize
 * or fingerprint the live BiomeSource during ServerLevel construction. Mystcraft still treats
 * AgeRecord as the durable source of truth; registering these codecs does not move Age ownership
 * into level.dat.</p>
 */
public final class MystBiomeSources {
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(BuiltInRegistries.BIOME_SOURCE, Mystcraft.MOD_ID);

    public static final Supplier<MapCodec<? extends BiomeSource>> GRID =
            BIOME_SOURCES.register("grid", () -> MystcraftGridBiomeSource.CODEC);
    public static final Supplier<MapCodec<? extends BiomeSource>> TILED =
            BIOME_SOURCES.register("tiled", () -> MystcraftTiledBiomeSource.CODEC);
    public static final Supplier<MapCodec<? extends BiomeSource>> ZOOM =
            BIOME_SOURCES.register("zoom", () -> MystcraftZoomBiomeSource.CODEC);

    private MystBiomeSources() {}

    public static void register(IEventBus modEventBus) {
        BIOME_SOURCES.register(modEventBus);
    }
}
