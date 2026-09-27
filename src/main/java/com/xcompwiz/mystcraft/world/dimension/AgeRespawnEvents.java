package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Restores Mystcraft 0.13.7.06's default canRespawnHere=true Age behavior. */
@EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class AgeRespawnEvents {
    private static final Map<UUID, ResourceKey<Level>> PENDING_AGE_RESPAWNS = new ConcurrentHashMap<>();

    private AgeRespawnEvents() {}

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (!(event.getOriginal() instanceof ServerPlayer original)) return;
        if (!(event.getEntity() instanceof ServerPlayer replacement)) return;

        ResourceKey<Level> dimension = original.level().dimension();
        if (!Mystcraft.MOD_ID.equals(dimension.location().getNamespace())) return;
        MinecraftServer server = original.getServer();
        if (server == null) return;

        // Only retain dimensions backed by a live persisted Age record. This avoids
        // turning an arbitrary mystcraft:* test dimension into a respawn target.
        if (AgeManager.resolveByLevel(server, dimension).filter(age -> !age.dead()).isEmpty()) return;
        PENDING_AGE_RESPAWNS.put(replacement.getUUID(), dimension);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceKey<Level> dimension = PENDING_AGE_RESPAWNS.remove(player.getUUID());
        if (dimension == null) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;
        ServerLevel target = server.getLevel(dimension);
        if (target == null) {
            var age = AgeManager.resolveByLevel(server, dimension).orElse(null);
            if (age == null || age.dead()) return;
            try {
                AgeRuntimeDimensionLoader.prepare(server, age);
                var installed = AgeRuntimeServerLevelInstaller.install(server, age);
                if (!installed.usable()) {
                    Mystcraft.LOGGER.error("Failed to restore Age {} as player respawn target: {}",
                            age.ageUid(), installed.status());
                    return;
                }
                target = installed.level();
            } catch (RuntimeException failure) {
                Mystcraft.LOGGER.error("Failed to prepare/install Age respawn target {}",
                        dimension.location(), failure);
                return;
            }
        }

        var spawn = target.getSharedSpawnPos();
        player.teleportTo(
                target,
                spawn.getX() + 0.5D,
                spawn.getY(),
                spawn.getZ() + 0.5D,
                java.util.Collections.emptySet(),
                player.getYRot(),
                player.getXRot());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING_AGE_RESPAWNS.clear();
    }
}
