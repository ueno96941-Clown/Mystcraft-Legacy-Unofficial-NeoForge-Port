package com.xcompwiz.mystcraft.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Static contract for post-registry dynamic biome/fluid Symbol reconstruction. */
public final class DynamicSymbolRegistrySourceHarness {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "." : args[0]);
        List<String> errors = new ArrayList<>();
        String biome = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/symbol/LegacyBiomeSymbolRegistry.java"));
        String material = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/symbol/LegacyMaterialSymbolRegistry.java"));
        String grammar = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/grammar/GrammarRuleRegistry.java"));
        String shortest = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/grammar/GrammarShortestPathIndex.java"));
        String lifecycle = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeRuntimeLifecycleEvents.java"));
        String resolver = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/worldgen/LegacyModernBiomeIdResolver.java"));

        if (!biome.contains("mystcraft:Biome6") || !biome.contains("minecraft:swamp"))
            errors.add("fixed 1.12 biome numeric compatibility table missing");
        if (!biome.contains("RUNTIME_ID_BASE = 1_000_000_000") || !biome.contains("stableRuntimeNumericId"))
            errors.add("runtime biome IDs must use deterministic non-legacy high range");
        if (!biome.contains("registryOrThrow(Registries.BIOME)") || !biome.contains("registry.keySet()"))
            errors.add("dynamic biome scan is not using the live 1.21 registry");
        if (!resolver.contains("minecraft:swamp_hills") || !resolver.contains("minecraft:swamp"))
            errors.add("legacy swamp_hills alias regression");

        if (!material.contains("BuiltInRegistries.FLUID.keySet()") || !material.contains("createLegacyBlock()"))
            errors.add("dynamic material registry is not scanning registered fluids/block states");
        if (!material.contains("fluid == Fluids.WATER") || !material.contains("fluid == Fluids.LAVA"))
            errors.add("vanilla water/lava must not duplicate fixed material symbols");
        if (!material.contains("getDensity() <= 0") || !material.contains("BlockSea") || !material.contains("BlockFluid"))
            errors.add("legacy gaseous skip / Sea+Fluid grammar contract missing");
        if (material.contains("setInstabilityFactors") || material.contains("factor_accessibility"))
            errors.add("dynamic fluid symbols must not restore instability runtime factors");

        if (!grammar.contains("rebuildForDynamicSymbols") || !shortest.contains("clearCache()"))
            errors.add("grammar/shortest-path caches cannot refresh after runtime symbols");
        int fluid = lifecycle.indexOf("bootstrapRuntimeFluids()");
        int biomes = lifecycle.indexOf("bootstrapRuntime(server.registryAccess())");
        int grammarRebuild = lifecycle.indexOf("rebuildForDynamicSymbols()");
        int restore = lifecycle.indexOf("restorePreparedAges(server)");
        if (fluid < 0 || biomes < 0 || grammarRebuild < 0 || restore < 0 || !(fluid < grammarRebuild && biomes < grammarRebuild && grammarRebuild < restore))
            errors.add("dynamic symbols/grammar must be final before eager Age restore");

        if (!errors.isEmpty()) {
            errors.forEach(System.err::println);
            throw new IllegalStateException("DynamicSymbolRegistrySourceHarness failed: " + errors.size());
        }
        System.out.println("DynamicSymbolRegistrySourceHarness: PASS");
    }
}
