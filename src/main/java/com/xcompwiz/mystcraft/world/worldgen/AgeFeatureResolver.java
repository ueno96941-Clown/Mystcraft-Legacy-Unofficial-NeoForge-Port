package com.xcompwiz.mystcraft.world.worldgen;

import com.xcompwiz.mystcraft.symbol.LegacyMaterialGrammarBinding;
import com.xcompwiz.mystcraft.symbol.LegacyMaterialSymbolDefinition;
import com.xcompwiz.mystcraft.symbol.LegacyMaterialSymbolRegistry;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Replays legacy block/biome modifier consumption for fixed Feature Symbols.
 */
public final class AgeFeatureResolver {
    private static final String STRUCTURE = "mystcraft:BlockStructure";
    private static final String FLUID = "mystcraft:BlockFluid";
    private static final String CRYSTAL = "mystcraft:BlockCrystal";

    private AgeFeatureResolver() {}

    public static AgeFeaturePlan resolve(List<String> effectiveSymbols) {
        return resolve(0L, effectiveSymbols);
    }

    public static AgeFeaturePlan resolve(long ageSeed, List<String> effectiveSymbols) {
        List<QueuedMaterial> materials = new ArrayList<>();
        List<String> biomeQueue = new ArrayList<>();
        List<AgeFeaturePlanEntry> out = new ArrayList<>();

        for (String symbol : effectiveSymbols) {
            if ("mystcraft:ModClear".equals(symbol)) {
                // Legacy AgeController.clearModifiers() removed both shared blocklist and biomelist.
                // Features later in the page stream must not see modifiers authored before Clear.
                materials.clear();
                biomeQueue.clear();
                continue;
            }

            LegacyMaterialSymbolDefinition material =
                    LegacyMaterialSymbolRegistry.resolveLegacy(symbol).orElse(null);
            if (material != null) {
                materials.add(0, new QueuedMaterial(material));
                continue;
            }

            if (symbol != null && symbol.matches("^mystcraft:Biome\\d+$")) {
                biomeQueue.add(symbol);
                continue;
            }

            switch (symbol) {
                case "mystcraft:Caves" ->
                        out.add(entry(AgeFeatureKind.CAVES, AgeFeatureStage.TERRAIN_ALTERATION,
                                "minecraft:air", null, 0, "Minecraft 1.21.1 Carvers.CAVE (modern cave/aquifer contract)"));

                case "mystcraft:Ravines" ->
                        out.add(entry(AgeFeatureKind.RAVINES, AgeFeatureStage.TERRAIN_ALTERATION,
                                "minecraft:air", null, 0, "Minecraft 1.21.1 Carvers.CANYON (modern ravine/canyon contract)"));

                case "mystcraft:HugeTrees" ->
                        out.add(entry(AgeFeatureKind.HUGE_TREES, AgeFeatureStage.TERRAIN_ALTERATION,
                                null, null, 0, "WorldGenMystBigTree terrain alteration"));

                case "mystcraft:LakesDeep" -> {
                    QueuedMaterial m = pop(materials, FLUID);
                    out.add(entry(AgeFeatureKind.DEEP_LAKES, AgeFeatureStage.POPULATION,
                            m == null ? "minecraft:lava" : m.modernBlockId(),
                            null, 8, "1/8 attempt; generated below sea level or with extra 1/10 chance above it"));
                }

                case "mystcraft:LakesSurface" -> {
                    QueuedMaterial m = pop(materials, FLUID);
                    out.add(entry(AgeFeatureKind.SURFACE_LAKES, AgeFeatureStage.POPULATION,
                            m == null ? "minecraft:water" : m.modernBlockId(),
                            null, 4, "1/4 attempt"));
                }

                case "mystcraft:Dungeons" ->
                        out.add(entry(AgeFeatureKind.DUNGEONS, AgeFeatureStage.POPULATION,
                                null, null, 0, "8 generation attempts per chunk"));

                case "mystcraft:Mineshafts" ->
                        out.add(entry(AgeFeatureKind.MINESHAFTS, AgeFeatureStage.BOTH,
                                null, null, 0, "Minecraft 1.21.1 mineshaft StructureSet placement"));

                case "mystcraft:NetherFort" ->
                        out.add(entry(AgeFeatureKind.NETHER_FORTRESS, AgeFeatureStage.BOTH,
                                null, null, 0, "Minecraft 1.21.1 nether-complex placement; fortress-only Mystcraft semantic"));

                case "mystcraft:Strongholds" ->
                        out.add(entry(AgeFeatureKind.STRONGHOLDS, AgeFeatureStage.BOTH,
                                null, null, 0, "Minecraft 1.21.1 stronghold StructureSet/ring placement"));

                case "mystcraft:Villages" ->
                        out.add(entry(AgeFeatureKind.VILLAGES, AgeFeatureStage.BOTH,
                                null, null, 0, "Minecraft 1.21.1 village StructureSet placement"));

                case "mystcraft:FloatIslands" -> {
                    String biome = popBiome(biomeQueue);
                    if (biome == null) {
                        biome = AgeBiomeFallbackResolver.pickLegacyId(ageSeed);
                    }
                    QueuedMaterial m = pop(materials, STRUCTURE);
                    out.add(entry(AgeFeatureKind.FLOATING_ISLANDS, AgeFeatureStage.TERRAIN_ALTERATION,
                            m == null ? "minecraft:stone" : m.modernBlockId(),
                            biome, 0, "missing biome falls back by Age-seeded random selection"));
                }

                case "mystcraft:TerModSpheres" -> {
                    QueuedMaterial m = pop(materials, STRUCTURE);
                    out.add(entry(AgeFeatureKind.SPHERES, AgeFeatureStage.TERRAIN_ALTERATION,
                            m == null ? "minecraft:cobblestone" : m.modernBlockId(),
                            null, 0, "MapGenSpheresMyst"));
                }

                case "mystcraft:Tendrils" -> {
                    QueuedMaterial m = pop(materials, STRUCTURE);
                    out.add(entry(AgeFeatureKind.TENDRILS, AgeFeatureStage.TERRAIN_ALTERATION,
                            m == null ? "minecraft:oak_log" : m.modernBlockId(),
                            null, 0, "MapGenCavesMyst(seed,15,18,material)"));
                }

                case "mystcraft:Skylands" ->
                        out.add(entry(AgeFeatureKind.SKYLANDS, AgeFeatureStage.TERRAIN_ALTERATION,
                                null, null, 0, "legacy noise cut/add terrain transform"));

                case "mystcraft:GenSpikes" -> {
                    QueuedMaterial m = pop(materials, STRUCTURE);
                    out.add(entry(AgeFeatureKind.SPIKES, AgeFeatureStage.POPULATION,
                            m == null ? "minecraft:stone" : m.modernBlockId(),
                            null, 18, "1/18 attempt when !flag"));
                }

                case "mystcraft:Obelisks" -> {
                    QueuedMaterial m = pop(materials, STRUCTURE);
                    out.add(entry(AgeFeatureKind.OBELISKS, AgeFeatureStage.POPULATION,
                            m == null ? "minecraft:obsidian" : m.modernBlockId(),
                            null, 128, "1/128 attempt; default obsidian"));
                }

                case "mystcraft:CryForm" -> {
                    QueuedMaterial m = pop(materials, CRYSTAL);
                    out.add(entry(AgeFeatureKind.CRYSTAL_FORMATION, AgeFeatureStage.POPULATION,
                            m == null ? "mystcraft:blockcrystal" : m.modernBlockId(),
                            null, 15, "1/15 attempt when !flag"));
                }

                case "mystcraft:StarFissure" ->
                        out.add(entry(AgeFeatureKind.STAR_FISSURE, AgeFeatureStage.POPULATION,
                                null, null, 0, "spawn-chunk-only; ordered population short-circuit with server-thread cross-chunk application"));

                case "mystcraft:DenseOres" ->
                        out.add(entry(AgeFeatureKind.DENSE_ORES, AgeFeatureStage.POPULATION,
                                null, null, 0, "1/2/3 pages => 2x/3x/5x total; mod ores and source Y distribution preserved; Instability fixed by page count"));

                default -> {
                }
            }
        }

        return new AgeFeaturePlan(out);
    }

    private static AgeFeaturePlanEntry entry(
            AgeFeatureKind kind, AgeFeatureStage stage, String block,
            String biome, int chance, String notes) {
        return new AgeFeaturePlanEntry(kind, stage, block, biome, chance, notes);
    }

    private static String popBiome(List<String> queue) {
        return queue.isEmpty() ? null : queue.remove(queue.size() - 1);
    }

    private static QueuedMaterial pop(List<QueuedMaterial> queue, String category) {
        for (int i = 0; i < queue.size(); i++) {
            QueuedMaterial value = queue.get(i);
            if (value.usableFor(category)) {
                queue.remove(i);
                return value;
            }
        }
        return null;
    }

    private record QueuedMaterial(
            String modernBlockId,
            EnumSet<Category> categories) {

        QueuedMaterial(LegacyMaterialSymbolDefinition definition) {
            this(
                    definition.modernBlockId(),
                    categories(definition));
        }

        boolean usableFor(String category) {
            Category resolved = Category.fromLegacy(category);
            return resolved != null && categories.contains(resolved);
        }

        private static EnumSet<Category> categories(LegacyMaterialSymbolDefinition definition) {
            EnumSet<Category> out = EnumSet.noneOf(Category.class);
            for (LegacyMaterialGrammarBinding binding : definition.grammarBindings()) {
                Category c = Category.fromLegacy(binding.parentToken());
                if (c != null) out.add(c);
            }
            return out;
        }
    }

    private enum Category {
        STRUCTURE, FLUID, CRYSTAL;

        static Category fromLegacy(String value) {
            if (STRUCTURE_TOKEN.equals(value)) return STRUCTURE;
            if (FLUID_TOKEN.equals(value)) return FLUID;
            if (CRYSTAL_TOKEN.equals(value)) return CRYSTAL;
            return null;
        }

        private static final String STRUCTURE_TOKEN = "mystcraft:BlockStructure";
        private static final String FLUID_TOKEN = "mystcraft:BlockFluid";
        private static final String CRYSTAL_TOKEN = "mystcraft:BlockCrystal";
    }
}
