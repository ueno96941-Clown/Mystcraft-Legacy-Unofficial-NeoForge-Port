package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.AgeFeaturePlan;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.ArrayList;
import java.util.List;

/** Builds runtime-only direct PlacedFeatures for custom legacy population generators. */
public final class AgeLegacyPopulationPlacedFeatures {
    private AgeLegacyPopulationPlacedFeatures() {}

    public static List<Holder<PlacedFeature>> create(AgeFeaturePlan plan) {
        int denseCount = 0;
        for (var entry : plan.entries()) {
            if (entry.kind() == com.xcompwiz.mystcraft.world.worldgen.AgeFeatureKind.DENSE_ORES) denseCount++;
        }
        int authoredDensePages = denseCount;
        int effectiveDensePages = DenseOresPolicy.effectivePages(authoredDensePages);
        boolean denseAdded = false;
        ArrayList<AgeLegacyPopulationBatchFeature.PopulationStep> ordered = new ArrayList<>();

        for (var entry : plan.entries()) {
            if (entry.kind() == com.xcompwiz.mystcraft.world.worldgen.AgeFeatureKind.DENSE_ORES) {
                // CP291: tiered Dense Ores scaling, capped at three effective
                // pages; the third page is the explicit 5x forbidden tier. Collapse repeated symbols at the
                // first DenseOres position so the direct population stream keeps authored order.
                if (!denseAdded) {
                    ordered.add(AgeLegacyPopulationBatchFeature.PopulationStep.direct(
                            new AgeLegacyPopulationFeature(
                                    com.xcompwiz.mystcraft.world.worldgen.AgeFeatureKind.DENSE_ORES,
                                    "minecraft:air", effectiveDensePages)));
                    denseAdded = true;
                }
                continue;
            }
            switch (entry.kind()) {
                case VILLAGES -> {
                    ordered.add(AgeLegacyPopulationBatchFeature.PopulationStep.legacyStructure(entry.kind()));
                }
                case STAR_FISSURE -> {
                    ordered.add(AgeLegacyPopulationBatchFeature.PopulationStep.starFissureSentinel());
                }
                case MINESHAFTS, STRONGHOLDS, NETHER_FORTRESS -> {
                    ordered.add(AgeLegacyPopulationBatchFeature.PopulationStep.legacyStructure(entry.kind()));
                }
                case DUNGEONS, SPIKES, OBELISKS, CRYSTAL_FORMATION, DEEP_LAKES, SURFACE_LAKES -> {
                    String blockId = entry.materialBlockId();
                    if (blockId == null) blockId = "minecraft:air";
                    ordered.add(AgeLegacyPopulationBatchFeature.PopulationStep.direct(
                            new AgeLegacyPopulationFeature(entry.kind(), blockId, 1)));
                }
                default -> { }
            }
        }

        // Legacy MapGenScatteredFeatureMyst (Small Library) existed in every Age,
        // independent of authored Symbols. The batch therefore always exists.
        var batch = new AgeLegacyPopulationBatchFeature(ordered);
        var configured = new ConfiguredFeature<>(batch, NoneFeatureConfiguration.INSTANCE);
        return List.of(Holder.direct(new PlacedFeature(Holder.direct(configured), List.of())));
    }


}
