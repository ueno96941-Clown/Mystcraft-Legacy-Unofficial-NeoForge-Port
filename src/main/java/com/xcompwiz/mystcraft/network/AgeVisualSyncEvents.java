package com.xcompwiz.mystcraft.network;

import com.xcompwiz.mystcraft.Mystcraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Covers Age entry paths that do not pass through a Mystcraft Linking Book. */
@EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class AgeVisualSyncEvents {
    private AgeVisualSyncEvents() {}

    @SubscribeEvent public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) AgeVisualSync.sendCurrentAge(p);
    }
    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) AgeVisualSync.sendCurrentAge(p);
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) AgeVisualSync.sendCurrentAge(p);
    }
}
