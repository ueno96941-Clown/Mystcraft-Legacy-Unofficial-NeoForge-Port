package com.xcompwiz.mystcraft.symbol;

import com.xcompwiz.mystcraft.world.worldgen.LegacyModernBiomeIdResolver;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Runtime audit companion to {@link SymbolPortAudit} for the registries that
 * 0.13.7.06 created dynamically after normal Symbol registration.
 *
 * <p>This deliberately checks coverage rather than a hard-coded modern count:
 * datapacks and other mods are allowed to add biomes/fluids. The fixed legacy
 * Symbol table remains the only table with an invariant size (110).</p>
 */
public final class DynamicSymbolPortAudit {
    public static final int EXPECTED_BUILTIN_MATERIAL_COUNT = 35;

    private DynamicSymbolPortAudit() {}

    public static BiomeAudit auditBiomes(RegistryAccess access) {
        LegacyBiomeSymbolRegistry.bootstrapRuntime(access);
        Registry<Biome> registry = access.registryOrThrow(Registries.BIOME);

        LinkedHashSet<String> represented = new LinkedHashSet<>();
        for (LegacyBiomeSymbolDefinition definition : LegacyBiomeSymbolRegistry.values()) {
            represented.add(LegacyModernBiomeIdResolver.resolveModernId(definition.modernBiomeId()));
        }

        LinkedHashSet<ResourceLocation> missing = new LinkedHashSet<>();
        for (ResourceLocation key : registry.keySet()) {
            if (!represented.contains(key.toString())) missing.add(key);
        }
        return new BiomeAudit(registry.size(), LegacyBiomeSymbolRegistry.runtimeSize(), Set.copyOf(missing));
    }

    public static MaterialAudit auditMaterialsAndFluids() {
        LegacyMaterialSymbolRegistry.bootstrapRuntimeFluids();

        LinkedHashSet<ResourceLocation> unresolvedBlocks = new LinkedHashSet<>();
        for (LegacyMaterialSymbolDefinition definition : LegacyMaterialSymbolRegistry.values()) {
            ResourceLocation blockId = ResourceLocation.tryParse(definition.modernBlockId());
            if (blockId == null || !BuiltInRegistries.BLOCK.containsKey(blockId)) {
                if (blockId != null) unresolvedBlocks.add(blockId);
            }
        }

        Set<ResourceLocation> eligibleFluidBlocks = LegacyMaterialSymbolRegistry.runtimeEligibleFluidBlockIds();
        Set<ResourceLocation> registeredFluidBlocks = LegacyMaterialSymbolRegistry.runtimeRegisteredFluidBlockIds();
        LinkedHashSet<ResourceLocation> missingFluidBlocks = new LinkedHashSet<>(eligibleFluidBlocks);
        missingFluidBlocks.removeAll(registeredFluidBlocks);

        int builtinCount = LegacyMaterialSymbolRegistry.size() - LegacyMaterialSymbolRegistry.runtimeFluidSize();
        return new MaterialAudit(
                builtinCount,
                LegacyMaterialSymbolRegistry.runtimeFluidSize(),
                Set.copyOf(unresolvedBlocks),
                Set.copyOf(missingFluidBlocks));
    }

    public record BiomeAudit(int registryBiomeCount, int syntheticRuntimeBiomeCount,
                             Set<ResourceLocation> missingBiomeIds) {
        public boolean complete() { return missingBiomeIds.isEmpty(); }
    }

    public record MaterialAudit(int builtinMaterialCount, int runtimeFluidSymbolCount,
                                Set<ResourceLocation> unresolvedBlockIds,
                                Set<ResourceLocation> missingEligibleFluidBlocks) {
        public boolean builtinCountMatchesLegacy() {
            return builtinMaterialCount == EXPECTED_BUILTIN_MATERIAL_COUNT;
        }

        public boolean complete() {
            return builtinCountMatchesLegacy()
                    && unresolvedBlockIds.isEmpty()
                    && missingEligibleFluidBlocks.isEmpty();
        }
    }
}
