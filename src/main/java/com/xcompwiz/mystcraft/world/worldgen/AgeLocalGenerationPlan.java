package com.xcompwiz.mystcraft.world.worldgen;

import java.util.List;

/**
 * Age-local generation settings derived from the vanilla registry bridge.
 *
 * <p>Placed features can be overlaid onto per-Age biome generation settings.
 * Structure sets are kept as a separate allow-list because modern structure
 * placement is owned by ChunkGeneratorStructureState, not BiomeGenerationSettings.</p>
 */
public record AgeLocalGenerationPlan(
        List<AgePlacedFeatureInjection> placedFeatureInjections,
        List<String> allowedStructureSetIds) {

    public AgeLocalGenerationPlan {
        placedFeatureInjections = List.copyOf(placedFeatureInjections);
        allowedStructureSetIds = List.copyOf(allowedStructureSetIds);
    }
}
