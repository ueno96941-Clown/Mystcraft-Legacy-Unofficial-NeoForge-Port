package com.xcompwiz.mystcraft.villager;

import com.mojang.datafixers.util.Pair;
import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.world.worldgen.WritingDeskStructureProcessor;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Adds the legacy Archivist house to every vanilla village house pool.
 *
 * <p>Mystcraft 0.13.7.06 registered {@code ComponentVillageArchivistHouse}
 * with piece weight 20.  Modern villages are Jigsaw based, so the faithful
 * equivalent is to append the original house template to each of the five
 * vanilla house pools at server datapack load.  This mutates the decoded pool
 * additively instead of replacing vanilla JSON, preserving other mods' pool
 * entries.</p>
 */
@EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class ArchivistVillagePools {
    private static final ResourceLocation HOUSE =
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "village/archivist_house");
    private static final String[] VILLAGE_TYPES = {"plains", "snowy", "savanna", "desert", "taiga"};
    private static final int LEGACY_WEIGHT = 20;

    /** Identity guard: datapack reload can fire more than one tag update for the same pool objects. */
    private static final Set<StructureTemplatePool> PATCHED =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private ArchivistVillagePools() {}

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) return;
        RegistryAccess access = event.getRegistryAccess();
        Registry<StructureTemplatePool> registry = access.registryOrThrow(Registries.TEMPLATE_POOL);
        for (String type : VILLAGE_TYPES) {
            ResourceLocation poolId = ResourceLocation.withDefaultNamespace("village/" + type + "/houses");
            StructureTemplatePool pool = registry.get(poolId);
            if (pool != null) appendIfNeeded(pool);
        }
    }

    private static void appendIfNeeded(StructureTemplatePool pool) {
        if (!PATCHED.add(pool)) return;

        if (!(pool.rawTemplates instanceof ArrayList<?>)) {
            pool.rawTemplates = new ArrayList<>(pool.rawTemplates);
        }

        // Vanilla village houses use the legacy single-pool element path.  This
        // matches village terrain/liquid placement semantics more closely than a
        // generic SinglePoolElement while keeping the generated NBT unchanged.
        Holder<StructureProcessorList> processors = Holder.direct(
                new StructureProcessorList(java.util.List.of(WritingDeskStructureProcessor.INSTANCE)));
        StructurePoolElement element = SinglePoolElement.legacy(HOUSE.toString(), processors)
                .apply(StructureTemplatePool.Projection.RIGID);
        pool.rawTemplates.add(Pair.of(element, LEGACY_WEIGHT));
        for (int i = 0; i < LEGACY_WEIGHT; ++i) {
            pool.templates.add(element);
        }
    }
}
