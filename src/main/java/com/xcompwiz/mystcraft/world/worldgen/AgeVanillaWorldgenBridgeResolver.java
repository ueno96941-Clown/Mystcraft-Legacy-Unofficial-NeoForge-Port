package com.xcompwiz.mystcraft.world.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves legacy feature intents onto vanilla 1.21.1 worldgen registry targets where
 * a direct/close built-in equivalent exists.
 *
 * <p>This does not silently claim perfect legacy parity. Mappings with changed placement
 * or shape semantics are marked non-exact and kept separate from custom-required features.</p>
 */
public final class AgeVanillaWorldgenBridgeResolver {
    private AgeVanillaWorldgenBridgeResolver() {}

    public static AgeVanillaWorldgenBridgePlan resolve(AgeFeaturePlan featurePlan) {
        ArrayList<AgeVanillaWorldgenTarget> targets = new ArrayList<>();
        ArrayList<AgeFeatureKind> custom = new ArrayList<>();

        for (AgeFeaturePlanEntry entry : featurePlan.entries()) {
            switch (entry.kind()) {
                case MINESHAFTS -> targets.add(structure(
                        AgeFeatureKind.MINESHAFTS,
                        "minecraft:mineshafts",
                        false,
                        "Age-local adapter restores the 1.12 0.004 + distance placement predicate; piece layouts remain modern."));
                case STRONGHOLDS -> targets.add(structure(
                        AgeFeatureKind.STRONGHOLDS,
                        "minecraft:strongholds",
                        false,
                        "Age-local ring replacement restores the 1.12 Java-Random sequential ring/biome-search stream; biome set and piece layouts remain modern."));
                case VILLAGES -> targets.add(structure(
                        AgeFeatureKind.VILLAGES,
                        "minecraft:villages",
                        false,
                        "Age-local adapter restores Legacy 32/8 placement and excludes modern snowy villages; piece layouts remain modern."));
                case NETHER_FORTRESS -> targets.add(structure(
                        AgeFeatureKind.NETHER_FORTRESS,
                        "minecraft:nether_complexes",
                        false,
                        "Age-local adapter narrows nether_complexes to fortress-only and restores the 1.12 16-region/1-in-3/+4..11 placement math; piece layouts remain modern."));
                case DUNGEONS ->
                        custom.add(entry.kind());
                // Lakes already have direct Legacy WorldGenLakesAdv population ports with
                // material/fluid binding and the original 1/4 + 1/8 chance semantics. Mapping
                // them to modern lava-lake PlacedFeatures as well would execute the same Symbol
                // twice (and would turn default surface-water lakes into an extra lava pass).
                case SURFACE_LAKES, DEEP_LAKES -> custom.add(entry.kind());

                case CAVES, RAVINES, HUGE_TREES, FLOATING_ISLANDS, SPHERES, TENDRILS,
                     SKYLANDS, SPIKES, OBELISKS, CRYSTAL_FORMATION, STAR_FISSURE, DENSE_ORES ->
                        custom.add(entry.kind());
            }
        }

        return new AgeVanillaWorldgenBridgePlan(targets, custom);
    }

    private static AgeVanillaWorldgenTarget structure(
            AgeFeatureKind kind, String id, boolean exact, String note) {
        return new AgeVanillaWorldgenTarget(
                kind, AgeVanillaWorldgenTargetKind.STRUCTURE_SET, id, exact, note);
    }

    private static AgeVanillaWorldgenTarget feature(
            AgeFeatureKind kind, String id, boolean exact, String note) {
        return new AgeVanillaWorldgenTarget(
                kind, AgeVanillaWorldgenTargetKind.PLACED_FEATURE, id, exact, note);
    }
}
