package com.xcompwiz.mystcraft.registry;

import com.google.common.collect.ImmutableSet;
import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.block.BlockWritingDesk;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;
import java.util.stream.Collectors;

/** Modern registry bridge for Mystcraft's legacy Archivist profession. */
public final class MystVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(BuiltInRegistries.POINT_OF_INTEREST_TYPE, Mystcraft.MOD_ID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(BuiltInRegistries.VILLAGER_PROFESSION, Mystcraft.MOD_ID);

    /**
     * 1.12 professions did not require a workstation.  The Writing Desk is the
     * closest faithful modern job-site: the original Archivist house generated one.
     * Only the main lower half is a POI so the four-part furniture is one job site.
     */
    public static final DeferredHolder<PoiType, PoiType> ARCHIVIST_POI = POI_TYPES.register(
            "archivist",
            () -> new PoiType(mainDeskStates(), 1, 1));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> ARCHIVIST = PROFESSIONS.register(
            "archivist",
            () -> createProfession(ARCHIVIST_POI));

    private MystVillagers() {}

    private static Set<BlockState> mainDeskStates() {
        return MystBlocks.WRITING_DESK.get().getStateDefinition().getPossibleStates().stream()
                .filter(state -> !state.getValue(BlockWritingDesk.IS_TOP) && !state.getValue(BlockWritingDesk.IS_FOOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static VillagerProfession createProfession(Holder<PoiType> poi) {
        ResourceKey<PoiType> key = poi.unwrapKey().orElseThrow();
        return new VillagerProfession(
                Mystcraft.MOD_ID + ":archivist",
                holder -> holder.is(key),
                holder -> holder.is(key),
                ImmutableSet.of(),
                ImmutableSet.of(),
                SoundEvents.VILLAGER_WORK_LIBRARIAN);
    }

    public static void register(IEventBus bus) {
        POI_TYPES.register(bus);
        PROFESSIONS.register(bus);
    }
}
