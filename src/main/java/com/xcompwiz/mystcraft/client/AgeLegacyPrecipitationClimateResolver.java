package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.world.worldgen.AgeLegacyBiomeTemperatureMath;
import com.xcompwiz.mystcraft.world.worldgen.AgePrecipitationBridge;
import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherPlan;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

/** Client-side replay of the legacy BiomeWrapperMyst precipitation classification. */
final class AgeLegacyPrecipitationClimateResolver {
    // Minecraft 1.12 Biome.TEMPERATURE_NOISE = new NoiseGeneratorPerlin(new Random(1234L), 1).
    // 1.21.1's one-octave PerlinSimplexNoise with LegacyRandomSource preserves that old stream.
    private static final PerlinSimplexNoise LEGACY_TEMPERATURE_NOISE =
            new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(1234L)), List.of(0));

    private AgeLegacyPrecipitationClimateResolver() {}

    static float controlledTemperature(ClientLevel level, int x, int y, int z, AgeWeatherPlan plan) {
        BlockPos pos = new BlockPos(x, y, z);
        Biome biome = level.getBiome(pos).value();
        float controlledBase = plan.temperature(biome.getBaseTemperature());
        double noise = LEGACY_TEMPERATURE_NOISE.getValue(x / 8.0D, z / 8.0D, false);
        return AgeLegacyBiomeTemperatureMath.adjusted(controlledBase, y, noise);
    }

    static AgePrecipitationBridge.Type precipitation(
            ClientLevel level, int x, int y, int z, AgeWeatherPlan plan) {
        BlockPos pos = new BlockPos(x, y, z);
        Biome biome = level.getBiome(pos).value();

        // Legacy canRain/getEnableSnow were position-independent biome flags. Modern Biome no
        // longer stores a separate snow flag, so use its base climate at legacy sea-level height
        // only as the default input; forced Rain/Snow/Storm Symbols override both flags anyway.
        BlockPos climateBase = new BlockPos(x, AgeLegacyBiomeTemperatureMath.LEGACY_HEIGHT_THRESHOLD, z);
        Biome.Precipitation base = biome.getPrecipitationAt(climateBase);
        boolean baseRain = base == Biome.Precipitation.RAIN;
        boolean baseSnow = base == Biome.Precipitation.SNOW;

        float adjusted = controlledTemperature(level, x, y, z, plan);
        return AgePrecipitationBridge.precipitationAtControlledTemperature(
                plan, baseRain, baseSnow, adjusted);
    }
}
