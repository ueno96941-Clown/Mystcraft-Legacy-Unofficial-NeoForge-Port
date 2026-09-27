package com.xcompwiz.mystcraft.network;

import com.xcompwiz.mystcraft.world.dimension.MystcraftAgeLevelData;
import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherMode;
import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherPlan;
import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherRuntimeController;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Sends an explicit weather snapshot when a player arrives in a Mystcraft Age. */
public final class AgeWeatherClientSync {
    private AgeWeatherClientSync() {}

    public static void sendEntrySnapshot(ServerPlayer player, AgeWeatherPlan plan) {
        if (player == null || plan == null) return;
        ServerLevel level = player.serverLevel();
        if (!(level.getLevelData() instanceof MystcraftAgeLevelData data)) return;

        var state = data.weatherRuntimeState();
        AgeWeatherRuntimeController.prepareForEntry(plan, state);

        // Keep authoritative level data in lockstep with the snapshot. This also makes the
        // next vanilla/NeoForge weather tick compare against the state the player received.
        data.setRainTime(state.rainCounter());
        data.setThunderTime(state.thunderCounter());
        data.setRaining(state.raining());
        data.setThundering(state.thundering());

        // WeatherCloudy is intentionally non-precipitating. Its full legacy dark-sky visual
        // is handled by the dedicated visual hook, not vanilla's raining flag/gradient.
        float rainStrength = plan.mode() == AgeWeatherMode.CLOUDY ? 0.0F : state.rainStrength();
        float thunderStrength = state.thunderStrength();
        level.setRainLevel(rainStrength);
        level.setThunderLevel(thunderStrength);

        player.connection.send(new ClientboundGameEventPacket(
                state.raining() ? ClientboundGameEventPacket.START_RAINING : ClientboundGameEventPacket.STOP_RAINING,
                0.0F));
        player.connection.send(new ClientboundGameEventPacket(
                ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, rainStrength));
        player.connection.send(new ClientboundGameEventPacket(
                ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, thunderStrength));
    }
}
