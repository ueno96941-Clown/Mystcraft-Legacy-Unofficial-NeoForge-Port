package com.xcompwiz.mystcraft.registry;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.entity.EntityLinkbook;
import com.xcompwiz.mystcraft.entity.EntityMeteor;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Legacy world-book entity registration (0.13.7.06 EntityLinkbook). */
public final class MystEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Mystcraft.MOD_ID);

    public static final Supplier<EntityType<EntityLinkbook>> LINKBOOK = ENTITY_TYPES.register(
            "myst.book",
            () -> EntityType.Builder.<EntityLinkbook>of(EntityLinkbook::new, MobCategory.MISC)
                    .sized(0.25F, 0.20F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(Mystcraft.MOD_ID + ":myst.book"));

    public static final Supplier<EntityType<EntityMeteor>> METEOR = ENTITY_TYPES.register(
            "myst.meteor",
            () -> EntityType.Builder.<EntityMeteor>of(EntityMeteor::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(192)
                    .updateInterval(2)
                    .build(Mystcraft.MOD_ID + ":myst.meteor"));

    private MystEntities() {}

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}
