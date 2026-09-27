package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherPlan;
import com.xcompwiz.mystcraft.world.worldgen.AgeWeatherRuntimeController;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Applies Mystcraft weather-controller state after each runtime Age tick. */
@EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class AgeWeatherRuntimeEvents {
    private AgeWeatherRuntimeEvents() {}

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().location().getNamespace().equals(Mystcraft.MOD_ID)) return;
        if (!(level.getLevelData() instanceof MystcraftAgeLevelData data)) return;

        var ageOptional = AgeManager.resolveByLevel(level.getServer(), level.dimension());
        if (ageOptional.isEmpty()) return;

        var prepared = AgeRuntimeDimensionLoader.getPrepared(ageOptional.get().ageUid()).orElse(null);
        if (prepared == null) return;

        AgeWeatherPlan plan = prepared.worldgenPlan().weatherPlan();
        var state = data.weatherRuntimeState();

        AgeWeatherRuntimeController.tick(
                plan,
                state,
                bound -> level.getRandom().nextInt(bound));

        // Mirror Mystcraft-authoritative state into the ServerLevelData fields that vanilla
        // and clients already understand. ServerLevelData exposes these setters in 1.21.1.
        data.setRainTime(state.rainCounter());
        data.setThunderTime(state.thunderCounter());
        data.setRaining(state.raining());
        data.setThundering(state.thundering());

        // Preserve the legacy gradual/fixed visual strengths where modern vanilla can do so
        // without changing precipitation semantics. Cloudy is special: legacy rendered full
        // cloud/rain darkness while explicitly disabling rain and snow. Applying rainLevel=1
        // in vanilla would also enable precipitation, so that visual override is deferred to
        // the dedicated client/weather-biome hook.
        float appliedRainStrength = plan.mode() == com.xcompwiz.mystcraft.world.worldgen.AgeWeatherMode.CLOUDY
                ? 0.0F
                : state.rainStrength();
        level.setRainLevel(appliedRainStrength);
        level.setThunderLevel(state.thunderStrength());
    }
}
