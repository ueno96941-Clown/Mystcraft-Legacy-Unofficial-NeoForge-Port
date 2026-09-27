package com.xcompwiz.mystcraft.network;

import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import com.xcompwiz.mystcraft.world.dimension.AgeRuntimeDimensionLoader;
import com.xcompwiz.mystcraft.world.worldgen.AgeVisualSnapshotCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sends the visual plan after a player has successfully arrived in an Age. */
public final class AgeVisualSync {
    private AgeVisualSync() {}

    public static void sendCurrentAge(ServerPlayer player) {
        var server = player.getServer();
        if (server == null) return;
        var resolved = AgeManager.resolveByLevel(server, player.serverLevel().dimension());
        if (resolved.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new AgeVisualPayload(
                    player.serverLevel().dimension().location().toString(), 0L, ""));
            return;
        }
        var age = resolved.get();
        if (age.dead()) {
            PacketDistributor.sendToPlayer(player, new AgeVisualPayload(
                    player.serverLevel().dimension().location().toString(), 0L, ""));
            return;
        }
        AgeRuntimeDimensionLoader.PrepareResult result = AgeRuntimeDimensionLoader.prepare(server, age);
        if (!result.hasPreparedDefinition()) {
            // Never publish a stale process-local plan when prepare reports a conflict
            // (or the Age is already live and no prepared object is returned).
            // A live Age should already have received its snapshot during link/login;
            // clearing is safer than applying a plan that does not match this record.
            if (result.status() == AgeRuntimeDimensionLoader.Status.CONFLICT
                    || result.status() == AgeRuntimeDimensionLoader.Status.DEAD) {
                PacketDistributor.sendToPlayer(player, new AgeVisualPayload(
                        player.serverLevel().dimension().location().toString(), 0L, ""));
            }
            return;
        }
        AgeRuntimeDimensionLoader.PreparedAgeDimension prepared = result.prepared();
        if (prepared == null) return;
        PacketDistributor.sendToPlayer(player, new AgeVisualPayload(
                player.serverLevel().dimension().location().toString(),
                age.seed(),
                AgeVisualSnapshotCodec.encode(prepared.worldgenPlan())));

        // Runtime Ages are installed after normal server startup and may receive their first
        // PlayerChangedDimensionEvent before LevelTickEvent.Post has mirrored Mystcraft's
        // weather controller into vanilla/client-visible weather state. Send the authoritative
        // entry snapshot now so fixed Rain/Storm modes are visible on the first visit.
        AgeWeatherClientSync.sendEntrySnapshot(player, prepared.worldgenPlan().weatherPlan());
    }
}
