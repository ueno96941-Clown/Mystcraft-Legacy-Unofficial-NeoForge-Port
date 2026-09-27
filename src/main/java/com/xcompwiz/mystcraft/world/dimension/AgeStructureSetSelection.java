package com.xcompwiz.mystcraft.world.dimension;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.ArrayList;
import java.util.List;

/** Resolves the Age-local StructureSet allow-list against Minecraft 1.21.1 placements. */
public final class AgeStructureSetSelection {
    private static final String MINESHAFTS = "minecraft:mineshafts";
    private static final String VILLAGES = "minecraft:villages";
    private static final String NETHER_COMPLEXES = "minecraft:nether_complexes";
    private static final String STRONGHOLDS = "minecraft:strongholds";
    private static final String NETHER_FORTRESS = "minecraft:fortress";

    private AgeStructureSetSelection() {}

    public static List<Holder<StructureSet>> resolve(
            RegistryAccess registries,
            com.xcompwiz.mystcraft.world.worldgen.AgeLocalGenerationPlan plan) {

        Registry<StructureSet> structureSets = registries.registryOrThrow(Registries.STRUCTURE_SET);
        Registry<Structure> structures = registries.registryOrThrow(Registries.STRUCTURE);
        ArrayList<Holder<StructureSet>> out = new ArrayList<>();

        for (String resourceId : plan.allowedStructureSetIds()) {
            if (MINESHAFTS.equals(resourceId)
                    || VILLAGES.equals(resourceId)
                    || STRONGHOLDS.equals(resourceId)) {
                // CP246: these symbols now select the native 1.21.1 StructureSet directly.
                // That preserves Mystcraft's on/off semantics while delegating spacing,
                // separation, biome tags and ring logic to the current Minecraft engine.
                out.add(structureSet(structureSets, resourceId));
                continue;
            }

            if (NETHER_COMPLEXES.equals(resourceId)) {
                // 0.13.7.06 SymbolNetherFort used MapGenNetherBridge regardless of the Age biome.
                // Keep the registered minecraft:fortress Structure holder so /locate fortress
                // and vanilla structure identity keep working, but restore the legacy 16x16-region
                // candidate math.  Generation-time biome compatibility is handled by the Age
                // chunk-generator bridge rather than by cloning/re-registering the Structure.
                Holder.Reference<Structure> fortress = structure(structures, NETHER_FORTRESS);
                out.add(Holder.direct(new StructureSet(fortress, new AgeLegacyNetherFortressPlacement())));
                continue;
            }

            out.add(structureSet(structureSets, resourceId));
        }

        return List.copyOf(out);
    }

    private static Holder.Reference<StructureSet> structureSet(
            Registry<StructureSet> registry, String resourceId) {
        ResourceKey<StructureSet> key = ResourceKey.create(
                Registries.STRUCTURE_SET, ResourceLocation.parse(resourceId));
        return registry.getHolderOrThrow(key);
    }

    private static Holder.Reference<Structure> structure(
            Registry<Structure> registry, String resourceId) {
        ResourceKey<Structure> key = ResourceKey.create(
                Registries.STRUCTURE, ResourceLocation.parse(resourceId));
        return registry.getHolderOrThrow(key);
    }
}
