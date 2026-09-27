package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.network.AgeClientVisualState;
import com.xcompwiz.mystcraft.Mystcraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.client.Minecraft;
import com.xcompwiz.mystcraft.world.worldgen.AgeBiomeTintBridge;
import com.xcompwiz.mystcraft.world.worldgen.AgeColorChannelPlan;
import com.xcompwiz.mystcraft.world.worldgen.AgeColorProviderMode;
import com.xcompwiz.mystcraft.world.worldgen.AgeFogColorBridge;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FogType;
import java.util.List;

@EventBusSubscriber(modid = Mystcraft.MOD_ID, value = Dist.CLIENT)
public final class MystClientEvents {
    private static final float UNDERWATER_FOG_LIGHTEN = 0.28F;
    private static String lastVisualDimension;
    private static AgeClientVisualState.Entry lastVisualEntry;

    private MystClientEvents() {}

    /**
     * Block tint is baked into rendered chunk meshes. The Age visual payload normally arrives
     * after the client level starts rendering, so merely updating AgeClientVisualState is not
     * enough: chunks built before the payload would keep their vanilla grass/foliage/water tint.
     * Rebuild once when the synchronized snapshot for the current dimension changes.
     */
    @SubscribeEvent
    public static void visualSnapshotChunkRefresh(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        // 0.13.7.06 installed wrappers into the live vanilla BlockColors table after bootstrap.
        // Reproduce that ordering guarantee once the client level exists.
        AgeLegacyBlockTintInstaller.installIfReady();
        if (mc.level == null || mc.levelRenderer == null) {
            lastVisualDimension = null;
            lastVisualEntry = null;
            return;
        }
        String dimension = mc.level.dimension().location().toString();
        AgeClientVisualState.Entry entry = AgeClientVisualState.get(dimension);
        if (!dimension.equals(lastVisualDimension) || entry != lastVisualEntry) {
            lastVisualDimension = dimension;
            lastVisualEntry = entry;
            if (entry != null) {
                // CP231 runtime: foliage used our wrapper but grass did not.  Re-assert the live
                // BlockColors entries at the exact point the Age payload becomes authoritative,
                // then invalidate baked chunk meshes so all tint callbacks are re-evaluated.
                AgeLegacyBlockTintInstaller.reinstallForSnapshot();
                mc.levelRenderer.allChanged();
            }
        }
    }


    @SubscribeEvent
    public static void computeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        String dimension = mc.level.dimension().location().toString();

        // Capture vanilla/NeoForge atmosphere before Mystcraft overrides it.
        AgeVanillaFogColorCache.capture(dimension, event.getRed(), event.getGreen(), event.getBlue());
        var entry = AgeClientVisualState.get(dimension);
        if (entry == null) return;

        // Cloud rendering re-enters LevelRenderer through AgeCloudRenderer. Keep vanilla atmospheric
        // fog for that nested pass so ColorFog does not strongly recolour the cloud layer.
        if (AgeCloudRenderer.isVanillaDelegationActive()) return;

        // Water immersion owns its own visual channel. ColorFog must never override underwater
        // visibility. Custom ColorWater uses a slightly lightened water colour as the underwater veil;
        // ordinary water keeps Minecraft's vanilla underwater fog unchanged.
        if (event.getCamera().getFluidInCamera() == FogType.WATER) {
            applyUnderwaterWaterTint(event, mc, entry.snapshot());
            return;
        }

        var channel = entry.snapshot().colors().get("fog");
        if (channel == null || channel.isEmpty()) return;
        float partial = (float) event.getPartialTick();
        var color = AgeFogColorBridge.resolve(
                channel, mc.level.getDayTime() / 12000.0F, mc.level.getTimeOfDay(partial));
        event.setRed(color.r());
        event.setGreen(color.g());
        event.setBlue(color.b());
    }

    private static void applyUnderwaterWaterTint(ViewportEvent.ComputeFogColor event, Minecraft mc,
                                                 com.xcompwiz.mystcraft.world.worldgen.AgeVisualSnapshot snapshot) {
        List<AgeColorChannelPlan> waterChannel = snapshot.colors().get("water");
        if (!hasCustomWaterColor(waterChannel)) return;

        BlockPos pos = BlockPos.containing(event.getCamera().getPosition());
        int vanillaWater = BiomeColors.getAverageWaterColor(mc.level, pos);
        int rgb = AgeBiomeTintBridge.resolve(snapshot, AgeBiomeTintBridge.Channel.WATER, vanillaWater);

        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        event.setRed(lighten(r));
        event.setGreen(lighten(g));
        event.setBlue(lighten(b));
    }

    private static boolean hasCustomWaterColor(List<AgeColorChannelPlan> channel) {
        if (channel == null || channel.isEmpty()) return false;
        for (AgeColorChannelPlan provider : channel) {
            if (provider == null) continue;
            if (provider.mode() == AgeColorProviderMode.CUSTOM_STATIC
                    || provider.mode() == AgeColorProviderMode.CUSTOM_DYNAMIC) return true;
        }
        return false;
    }

    private static float lighten(float component) {
        return component + (1.0F - component) * UNDERWATER_FOG_LIGHTEN;
    }

    @SubscribeEvent
    public static void clientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        AgeClientVisualState.clear();
        LegacySkyMeshCache.clearDynamic();
        lastVisualDimension = null;
        lastVisualEntry = null;
        AgeLegacyBlockTintInstaller.reset();
        AgeVanillaFogColorCache.clear();
    }
}
