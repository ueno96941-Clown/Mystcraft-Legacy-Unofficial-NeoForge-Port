package com.xcompwiz.mystcraft.registry;

import com.xcompwiz.mystcraft.Mystcraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Legacy 0.13.7.06 Mystcraft sound-event identities. */
public final class MystSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Mystcraft.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> LINK_POP = register("linking.pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> LINK = register("linking.link");
    public static final DeferredHolder<SoundEvent, SoundEvent> LINK_DISARM = register("linking.link-disarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> LINK_FOLLOWING = register("linking.link-following");
    public static final DeferredHolder<SoundEvent, SoundEvent> LINK_INTRA = register("linking.link-intra");
    public static final DeferredHolder<SoundEvent, SoundEvent> LINK_FISSURE = register("linking.link-fissure");
    public static final DeferredHolder<SoundEvent, SoundEvent> LINK_PORTAL = register("linking.link-portal");
    public static final DeferredHolder<SoundEvent, SoundEvent> METEOR_ROAR = register("entity.meteor.roar");

    private MystSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> register(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, id)));
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
