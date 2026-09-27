package com.xcompwiz.mystcraft.world.dimension;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;

/**
 * Removes Minecraft 1.18+ implicit noise caves from Mystcraft's Normal/Amplified
 * base terrain while retaining the modern terrain surface/height contract.
 *
 * <p>Legacy 0.13.7.06 generated ordinary terrain first and only added caves when
 * SymbolCaves registered its MapGenCavesMyst terrain alteration.  Minecraft 1.21.1's
 * overworld NoiseRouter instead bakes entrances/cheese/spaghetti/noodle caves directly
 * into finalDensity.  Merely suppressing biome carvers therefore still leaves every Age
 * full of caves.  This adapter reconstructs vanilla's final-density post-processing from
 * the registered sloped-cheese terrain function, but deliberately omits cave composition.
 * Mystcraft's authored Caves/Ravines carvers remain the only cave-carving layer.</p>
 */
final class AgeNoiseRouterIsolation {
    private AgeNoiseRouterIsolation() {}

    static NoiseGeneratorSettings withoutImplicitOverworldCaves(
            RegistryAccess registries,
            NoiseGeneratorSettings base,
            boolean amplified) {
        var densityRegistry = registries.registryOrThrow(Registries.DENSITY_FUNCTION);
        ResourceKey<DensityFunction> slopedCheeseKey = ResourceKey.create(
                Registries.DENSITY_FUNCTION,
                ResourceLocation.withDefaultNamespace(
                        amplified ? "overworld_amplified/sloped_cheese" : "overworld/sloped_cheese"));

        DensityFunction slopedCheese = new DensityFunctions.HolderHolder(
                densityRegistry.getHolderOrThrow(slopedCheeseKey));
        DensityFunction slid = slideOverworld(amplified, slopedCheese);
        DensityFunction finalDensity = DensityFunctions.mul(
                DensityFunctions.interpolated(DensityFunctions.blendDensity(slid)),
                DensityFunctions.constant(0.64D)).squeeze();

        NoiseRouter old = base.noiseRouter();
        NoiseRouter isolated = new NoiseRouter(
                old.barrierNoise(),
                old.fluidLevelFloodednessNoise(),
                old.fluidLevelSpreadNoise(),
                old.lavaNoise(),
                old.temperature(),
                old.vegetation(),
                old.continents(),
                old.erosion(),
                old.depth(),
                old.ridges(),
                old.initialDensityWithoutJaggedness(),
                finalDensity,
                old.veinToggle(),
                old.veinRidged(),
                old.veinGap());

        return new NoiseGeneratorSettings(
                base.noiseSettings(),
                base.defaultBlock(),
                base.defaultFluid(),
                isolated,
                base.surfaceRule(),
                base.spawnTarget(),
                base.seaLevel(),
                base.disableMobGeneration(),
                base.aquifersEnabled(),
                base.oreVeinsEnabled(),
                base.useLegacyRandomSource());
    }

    /** Exact 1.21.1 NoiseRouterData.slideOverworld contract. */
    private static DensityFunction slideOverworld(boolean amplified, DensityFunction input) {
        return slide(
                input,
                -64,
                384,
                amplified ? 16 : 80,
                amplified ? 0 : 64,
                -0.078125D,
                0,
                24,
                amplified ? 0.4D : 0.1171875D);
    }

    /** Exact 1.21.1 NoiseRouterData.slide expression, copied structurally not by registry mutation. */
    private static DensityFunction slide(
            DensityFunction input,
            int minY,
            int height,
            int topSize,
            int topOffset,
            double topTarget,
            int bottomOffset,
            int bottomSize,
            double bottomTarget) {
        DensityFunction topGradient = DensityFunctions.yClampedGradient(
                minY + height - topSize,
                minY + height - topOffset,
                1.0D,
                0.0D);
        DensityFunction top = DensityFunctions.lerp(topGradient, topTarget, input);
        DensityFunction bottomGradient = DensityFunctions.yClampedGradient(
                minY + bottomOffset,
                minY + bottomSize,
                0.0D,
                1.0D);
        return DensityFunctions.lerp(bottomGradient, bottomTarget, top);
    }
}
