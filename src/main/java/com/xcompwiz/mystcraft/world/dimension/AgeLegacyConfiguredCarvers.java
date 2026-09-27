package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.AgeFeatureKind;
import com.xcompwiz.mystcraft.world.worldgen.AgeFeaturePlan;
import com.xcompwiz.mystcraft.world.worldgen.LegacyMapGenCavesKernel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;

import java.util.ArrayList;
import java.util.List;

/** Builds runtime-only configured carvers/adapters for literal legacy terrain-alteration Symbols. */
public final class AgeLegacyConfiguredCarvers {
    private AgeLegacyConfiguredCarvers() {}

    public static List<Holder<ConfiguredWorldCarver<?>>> create(
            RegistryAccess registries,
            AgeFeaturePlan plan,
            long ageSeed) {

        var carverRegistry = registries.registryOrThrow(Registries.CONFIGURED_CARVER);
        CarverConfiguration template =
                carverRegistry.getHolderOrThrow(Carvers.CAVE).value().config();

        ArrayList<Holder<ConfiguredWorldCarver<?>>> out = new ArrayList<>();

        for (var entry : plan.entries()) {
            switch (entry.kind()) {
                // CP246: ordinary caves/ravines are engine features, not Mystcraft-only
                // geometry. Use Minecraft 1.21.1's own configured carvers so they honor
                // the modern -64..319 world, aquifers, carving masks and current terrain
                // contracts. Repeated pages still append repeated entries.
                case CAVES -> out.add(carverRegistry.getHolderOrThrow(Carvers.CAVE));

                case RAVINES -> out.add(carverRegistry.getHolderOrThrow(Carvers.CANYON));

                case TENDRILS -> {
                    Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                            ResourceLocation.parse(entry.materialBlockId()));
                    out.add(direct(
                            new LegacyMapGenCavesKernel(ageSeed, 15, 18, true),
                            block.defaultBlockState(),
                            template));
                }

                case SPHERES -> {
                    Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                            ResourceLocation.parse(entry.materialBlockId()));
                    out.add(direct(
                            new com.xcompwiz.mystcraft.world.worldgen.LegacyMapGenSpheresKernel(ageSeed),
                            block.defaultBlockState(),
                            template));
                }

                case FLOATING_ISLANDS -> {
                    Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                            ResourceLocation.parse(entry.materialBlockId()));
                    var carver = new AgeFloatingIslandsWorldCarver(
                            new com.xcompwiz.mystcraft.world.worldgen.LegacyMapGenFloatingIslandsKernel(ageSeed),
                            block.defaultBlockState(),
                            entry.biomeLegacySymbol());
                    out.add(Holder.direct(new ConfiguredWorldCarver<>(carver, template)));
                }

                case SKYLANDS -> {
                    var carver = new AgeSkylandsWorldCarver(
                            new com.xcompwiz.mystcraft.world.worldgen.LegacySkylandsKernel(ageSeed));
                    out.add(Holder.direct(new ConfiguredWorldCarver<>(carver, template)));
                }

                case HUGE_TREES -> {
                    var carver = new AgeHugeTreesWorldCarver(
                            new com.xcompwiz.mystcraft.world.worldgen.LegacyWorldGenMystBigTreeKernel(ageSeed));
                    out.add(Holder.direct(new ConfiguredWorldCarver<>(carver, template)));
                }

                default -> {
                }
            }
        }

        return List.copyOf(out);
    }

    private static Holder<ConfiguredWorldCarver<?>> direct(
            com.xcompwiz.mystcraft.world.worldgen.LegacyMapGenAdvancedKernel kernel,
            net.minecraft.world.level.block.state.BlockState replacementState,
            CarverConfiguration template) {

        AgeLegacyMapGenWorldCarver carver =
                new AgeLegacyMapGenWorldCarver(kernel, replacementState);
        ConfiguredWorldCarver<CarverConfiguration> configured =
                new ConfiguredWorldCarver<>(carver, template);
        return Holder.direct(configured);
    }
}
