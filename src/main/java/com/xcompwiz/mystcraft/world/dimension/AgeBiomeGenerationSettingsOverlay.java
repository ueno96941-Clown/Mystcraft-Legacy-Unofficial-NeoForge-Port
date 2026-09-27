package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.AgeLocalGenerationPlan;
import com.xcompwiz.mystcraft.world.worldgen.AgeTerrainMode;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;

/**
 * Builds an Age-local BiomeGenerationSettings overlay.
 *
 * <p>The source Biome and global registries are never mutated. Existing placed features
 * are copied, but source-biome carvers are deliberately suppressed: Mystcraft carving is
 * Symbol-controlled and supplied only by the Age's legacy MapGen carvers.</p>
 */
public final class AgeBiomeGenerationSettingsOverlay {
    /**
     * BiomeGenerationSettings.PlainBuilder accepts arbitrary non-negative feature-step indexes,
     * and ChunkGenerator iterates up to max(vanilla step count, indexed feature-list size).
     * Append Legacy IPopulate ports to TOP_LAYER_MODIFICATION after the inherited biome
     * features, so they run at the end of ordinary biome decoration and match Legacy
     * controller.populate() much more closely than
     * SURFACE_STRUCTURES. Modern original-mob spawning is a later ChunkStatus, so animal-before-
     * population ordering cannot be reproduced inside the decoration pipeline itself.
     */
    public static final int LEGACY_POPULATION_STEP = GenerationStep.Decoration.TOP_LAYER_MODIFICATION.ordinal();

    private AgeBiomeGenerationSettingsOverlay() {}

    public static BiomeGenerationSettings apply(
            RegistryAccess registries,
            BiomeGenerationSettings base,
            AgeLocalGenerationPlan plan,
            com.xcompwiz.mystcraft.world.worldgen.AgeFeaturePlan featurePlan,
            long ageSeed,
            AgeTerrainMode terrainMode,
            List<Holder<PlacedFeature>> legacyPopulationFeatures,
            Holder<PlacedFeature> legacyFlatSurfaceFeature) {

        BiomeGenerationSettings.PlainBuilder builder =
                new BiomeGenerationSettings.PlainBuilder();

        // Mystcraft 1.12 did not inherit arbitrary vanilla biome carvers. Carving is
        // Symbol-controlled, so the modern base carvers are intentionally NOT copied.
        for (Holder<ConfiguredWorldCarver<?>> carver :
                AgeLegacyConfiguredCarvers.create(registries, featurePlan, ageSeed)) {
            builder.addCarver(GenerationStep.Carving.AIR, carver);
        }

        if (legacyFlatSurfaceFeature != null) {
            // Legacy ChunkProviderMyst performed biome surface replacement immediately after
            // terrain generation and before biome decoration. RAW_GENERATION is the earliest
            // decoration slot available to a runtime direct feature and therefore keeps grass/dirt
            // in place before trees, flowers and other biome decorators run.
            builder.addFeature(GenerationStep.Decoration.RAW_GENERATION, legacyFlatSurfaceFeature);
        }

        var existing = base.features();
        for (int step = 0; step < existing.size(); step++) {
            for (Holder<PlacedFeature> feature : existing.get(step)) {
                // Vanilla 1.21 biomes carry monster_room/monster_room_deep as ordinary
                // decoration features. Legacy Mystcraft did not inherit the vanilla
                // ChunkGeneratorOverworld dungeon pass: SymbolDungeons supplied its own exact
                // eight-attempt population logic instead. Keeping both paths caused unrequested
                // dungeons and, in runtime Age regions, MonsterRoomFeature block-entity errors.
                if (isVanillaDungeonFeature(feature)) continue;
                // Modern 1.21 stores the vanilla End dragon-fight arena pieces directly in
                // minecraft:the_end biome decoration. Legacy Mystcraft TerrainEnd only borrowed
                // the End-shaped terrain density; it never invoked ChunkProviderEnd's dragon-fight
                // spike/platform generation. Copying these placed features into an Age therefore
                // leaks ten obsidian crystal towers (and the fixed obsidian arrival platform) into
                // every authored End Age. Suppress only those fight-only placements; ordinary End
                // biome decoration remains eligible when a biome actually supplies it.
                if (isVanillaEndFightFeature(feature)) continue;
                // Nether/End noise settings only expose a 0..127 worldgen column even though
                // Mystcraft Ages use the modern -64..319 build-height dimension type.  When an
                // authored Age combines those terrain families with ordinary Overworld biomes,
                // inherited vanilla PlacedFeatures may carry height providers that are impossible
                // in that short generation context.  In 1.21.1 ore_coal_upper resolves
                // [absolute 136 .. below_top 0], while fossil_lower resolves
                // [above_bottom 0 .. absolute -8]; UniformHeight warns on both empty ranges.
                // Legacy Mystcraft never inherited these modern biome decorators into its
                // Nether/End chunk-provider height contract, so suppress only the two known
                // impossible vanilla placements instead of changing terrain shape or silently
                // stretching their distributions.
                if (isImpossibleShortNoiseHeightFeature(feature, terrainMode)) continue;
                builder.addFeature(step, feature);
            }
        }

        // Custom Legacy population generators are material-bound per Age, so they cannot
        // live in the global registry. The caller creates one shared direct-holder set for the
        // whole Age; re-creating them per biome would cause duplicate execution in mixed-biome
        // generation regions. Run them after every vanilla decoration step.
        for (Holder<PlacedFeature> feature : legacyPopulationFeatures) {
            builder.addFeature(LEGACY_POPULATION_STEP, feature);
        }

        var featureRegistry = registries.registryOrThrow(Registries.PLACED_FEATURE);
        for (var injection : plan.placedFeatureInjections()) {
            ResourceLocation id = ResourceLocation.parse(injection.placedFeatureId());
            ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, id);
            Holder.Reference<PlacedFeature> holder = featureRegistry.getHolderOrThrow(key);
            builder.addFeature(injection.decorationStepOrdinal(), holder);
        }

        return builder.build();
    }

    /**
     * Vanilla 1.21.1 placements whose UniformHeight ranges are empty when evaluated against
     * the 0..127 generation column used by NoiseGeneratorSettings.NETHER/END.
     *
     * <p>This is intentionally key-based and narrow.  Other inherited biome features keep their
     * vanilla semantics, and NORMAL/AMPLIFIED/FLAT Ages are untouched.  If a future Minecraft
     * version changes these placements, this compatibility list can be audited independently
     * without mutating global registries.</p>
     */
    private static boolean isImpossibleShortNoiseHeightFeature(
            Holder<PlacedFeature> feature, AgeTerrainMode terrainMode) {
        if (terrainMode != AgeTerrainMode.NETHER && terrainMode != AgeTerrainMode.END) return false;
        return feature.unwrapKey().map(key -> key.location()).map(id ->
                "minecraft".equals(id.getNamespace())
                        && ("ore_coal_upper".equals(id.getPath())
                        || "fossil_lower".equals(id.getPath())))
                .orElse(false);
    }

    private static boolean isVanillaDungeonFeature(Holder<PlacedFeature> feature) {
        return feature.unwrapKey().map(key -> key.location()).map(id ->
                "minecraft".equals(id.getNamespace())
                        && ("monster_room".equals(id.getPath()) || "monster_room_deep".equals(id.getPath())))
                .orElse(false);
    }

    private static boolean isVanillaEndFightFeature(Holder<PlacedFeature> feature) {
        return feature.unwrapKey().map(key -> key.location()).map(id ->
                "minecraft".equals(id.getNamespace())
                        && ("end_spike".equals(id.getPath()) || "end_platform".equals(id.getPath())))
                .orElse(false);
    }

}
