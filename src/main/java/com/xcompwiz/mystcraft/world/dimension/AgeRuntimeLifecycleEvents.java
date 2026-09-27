package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.grammar.GrammarRuleRegistry;
import com.xcompwiz.mystcraft.instability.LegacyInstabilityRuntimeSnapshot;
import com.xcompwiz.mystcraft.instability.LegacyInstabilityRuntimeRandom;
import com.xcompwiz.mystcraft.instability.LegacyInstabilityDecayRuntimeState;
import com.xcompwiz.mystcraft.instability.LegacyInstabilityEnvironmentalRuntimeState;
import com.xcompwiz.mystcraft.linking.PortalTransferRuntime;
import com.xcompwiz.mystcraft.portal.CrystalPortalProjectileSweep;
import com.xcompwiz.mystcraft.symbol.LegacyBiomeSymbolRegistry;
import com.xcompwiz.mystcraft.symbol.LegacyMaterialSymbolRegistry;
import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import com.xcompwiz.mystcraft.world.agedata.AgeRegistryData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Runtime persistence and restart hooks for dynamically installed Mystcraft Ages. */
@EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class AgeRuntimeLifecycleEvents {
    private AgeRuntimeLifecycleEvents() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();

        // CP331: discover only the dynamic compatibility surfaces that remain player-facing:
        // block-backed fluids and runtime biomes. Solid block materials stay on the legacy fixed table.
        // The scan is lazy/cached and grammar rebuilding is deferred until grammar is actually consumed.
        long perfStart = System.nanoTime();
        boolean fluidSymbolsChanged = LegacyMaterialSymbolRegistry.bootstrapRuntimeFluids();
        long afterFluids = System.nanoTime();
        boolean biomeSymbolsChanged = LegacyBiomeSymbolRegistry.bootstrapRuntime(server.registryAccess());
        long afterBiomes = System.nanoTime();
        if (fluidSymbolsChanged || biomeSymbolsChanged) {
            GrammarRuleRegistry.invalidateForDynamicSymbols();
            Mystcraft.LOGGER.info(
                    "Mystcraft dynamic Symbols refreshed: modFluidMaterials={}, runtimeBiomes={}",
                    LegacyMaterialSymbolRegistry.runtimeFluidSize(), LegacyBiomeSymbolRegistry.runtimeSize());
        }

        AgeRuntimeWorldManager.RestoreReport report = AgeRuntimeWorldManager.restorePreparedAges(server);
        long afterRestore = System.nanoTime();
        Mystcraft.LOGGER.debug(
                "Mystcraft startup perf: fluids={} ms, biomes={} ms, ageRestore={} ms, total={} ms",
                nanosToMillis(afterFluids - perfStart), nanosToMillis(afterBiomes - afterFluids),
                nanosToMillis(afterRestore - afterBiomes), nanosToMillis(afterRestore - perfStart));
        Mystcraft.LOGGER.debug(
                "Mystcraft runtime Age restore: prepared={}, installed={}, alreadyLoaded={}, deadSkipped={}, conflicts={}, failures={}",
                report.prepared(), report.installed(), report.alreadyLoaded(),
                report.skippedDead(), report.conflicts(), report.failures());
    }

    private static double nanosToMillis(long nanos) {
        return Math.round((nanos / 1_000_000.0D) * 1000.0D) / 1000.0D;
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        AgeRuntimeWorldManager.armIdleUnloading(player.getServer());
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        // CP318: advance/repair the previous Crystal Portal transaction first.
        // A fast minecart may reach the next portal only a few ticks later; giving
        // pending riding state its repair pass before the sweep minimizes stalls.
        PortalTransferRuntime.tick(event.getServer());
        CrystalPortalProjectileSweep.tick(event.getServer());
        AgeRuntimeWorldManager.tickIdleUnloading(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        // Make pending cross-chunk Legacy writes dirty before Minecraft's normal
        // per-level shutdown save starts. This is the primary durability barrier.
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().getNamespace().equals(Mystcraft.MOD_ID)) {
                AgeLegacyDeferredBlockWritesData.capture(level);
            }
        }
        int captured = AgeRuntimeWorldManager.captureAllLoaded(server);
        if (captured > 0) {
            // ServerStopping runs before MinecraftServer#stopServer performs the normal
            // world save. Flush the global Age registry here as a durability barrier so
            // a later shutdown failure cannot strand freshly captured runtime state.
            AgeRegistryData.flush(server);
            Mystcraft.LOGGER.debug("Captured and flushed runtime state for {} Mystcraft Age(s) before shutdown", captured);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        // Static staging/listeners must never leak across integrated-server restarts/tests.
        AgeRuntimeWorldBorderBridge.clearAll();
        AgeStarFissurePopulationBridge.clearAll();
        // Deferred Legacy population writes are server-instance state. Never let an
        // integrated-server restart/test reuse stale writes for an identical Age key.
        AgeLegacyDeferredBlockWrites.clearAll();
        PortalTransferRuntime.clear(event.getServer());
        AgeRuntimeWorldManager.clearIdleUnloading(event.getServer());
        AgeRuntimeDimensionLoader.clearPrepared();
        LegacyInstabilityRuntimeSnapshot.clearAll();
        LegacyInstabilityRuntimeRandom.clearAll();
        LegacyInstabilityDecayRuntimeState.clearAll();
        LegacyInstabilityEnvironmentalRuntimeState.clearAll();
        AgeAcceleratedTickRuntimeEvents.clearAllRuntimeState();
        AgeStormLightningRuntimeEvents.clearAllRuntimeState();
        AgeLegacyStructurePopulationBridge.clearDiagnostics();
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().location().getNamespace().equals(Mystcraft.MOD_ID)) return;
        AgeLegacyDeferredBlockWritesData.restore(level);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().location().getNamespace().equals(Mystcraft.MOD_ID)) return;
        AgeLegacyDeferredBlockWritesData.captureAndFlush(level);
        AgeRuntimeWorldBorderBridge.detach(level);
        AgeStarFissurePopulationBridge.clear(level.dimension());
    }

    @SubscribeEvent
    public static void onLevelSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().location().getNamespace().equals(Mystcraft.MOD_ID)) return;

        AgeLegacyDeferredBlockWritesData.captureAndFlush(level);

        MinecraftServer server = level.getServer();
        AgeManager.resolveByLevel(server, level.dimension()).ifPresent(age ->
                AgeRuntimeStateSync.captureAndFlush(server, age, level, age.visited()));
    }
}
