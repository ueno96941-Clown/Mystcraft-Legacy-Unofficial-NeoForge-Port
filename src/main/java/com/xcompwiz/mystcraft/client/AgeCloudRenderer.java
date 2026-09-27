package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xcompwiz.mystcraft.world.worldgen.AgeColor;
import com.xcompwiz.mystcraft.world.worldgen.AgeColorChannelPlan;
import com.xcompwiz.mystcraft.world.worldgen.AgeColorProviderMode;
import com.xcompwiz.mystcraft.world.worldgen.AgeVisualSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

/**
 * CP228 cloud bridge.
 *
 * <p>Mystcraft 0.13.7.06 copied the then-current vanilla cloud renderer and only supplied
 * an Age-specific cloud colour and cloud height. Carrying that 1.12 geometry forward into
 * 1.21.1 caused camera-relative slabs, incorrect side faces and rotation artefacts. The
 * faithful modern equivalent is therefore to let 1.21.1's {@code LevelRenderer} own the
 * geometry/state/cache/Fast-vs-Fancy path and inject only those two Mystcraft values.</p>
 */
public final class AgeCloudRenderer {
    /** Minecraft 1.21.1 normal cloud height used when no terrain symbol overrides it. */
    public static final float MODERN_NORMAL_CLOUD_HEIGHT =
            com.xcompwiz.mystcraft.world.dimension.ModernAgeHeight.NORMAL_CLOUD_HEIGHT;

    /**
     * Re-entering LevelRenderer is deliberate: the nested call must fall through the
     * DimensionSpecialEffects hook once, otherwise this class would recurse forever.
     */
    private static final ThreadLocal<Boolean> VANILLA_DELEGATION =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private AgeCloudRenderer() {}

    public static boolean isVanillaDelegationActive() {
        return VANILLA_DELEGATION.get();
    }

    private static boolean hasCustomChannelColor(AgeVisualSnapshot snapshot, String channelKey) {
        List<AgeColorChannelPlan> providers = snapshot.colors().get(channelKey);
        if (providers == null || providers.isEmpty()) return false;
        for (AgeColorChannelPlan provider : providers) {
            if (provider == null) continue;
            if (provider.mode() == AgeColorProviderMode.CUSTOM_STATIC
                    || provider.mode() == AgeColorProviderMode.CUSTOM_DYNAMIC) {
                return true;
            }
        }
        return false;
    }

    /** True when the cloud channel contains an actual Mystcraft colour provider. */
    public static boolean hasCustomCloudColor(AgeVisualSnapshot snapshot) {
        return hasCustomChannelColor(snapshot, "cloud");
    }

    /** True when ColorFog is custom and the cloud pass needs fog isolation. */
    public static boolean hasCustomFogColor(AgeVisualSnapshot snapshot) {
        return hasCustomChannelColor(snapshot, "fog");
    }

    /**
     * If neither Mystcraft colour nor height differs, and no custom fog needs isolation from
     * clouds, there is no reason to intercept the render at all.
     */
    public static boolean canUseDirectVanilla(AgeVisualSnapshot snapshot) {
        return Math.abs(snapshot.cloudHeight() - MODERN_NORMAL_CLOUD_HEIGHT) < 0.001F
                && !hasCustomCloudColor(snapshot)
                && !hasCustomFogColor(snapshot);
    }

    /**
     * Render through Minecraft 1.21.1's own cloud renderer.
     *
     * <p>Height is injected by offsetting the camera Y supplied to vanilla. Its dimension
     * effects advertise the modern 1.21.1 normal height (192), so shifting the synthetic camera by
     * {@code 192 - targetHeight} produces the requested Legacy relative cloud plane while all
     * modern camera transforms remain untouched.</p>
     *
     * <p>For a custom Mystcraft colour, vanilla still builds its own weather/daylight cloud
     * colour. A shader multiplier converts that final vanilla colour into the already-resolved
     * Legacy Mystcraft target colour. This leaves vanilla's cloud mesh/cache untouched and also
     * permits dynamic gradients without forcing a cloud-buffer rebuild every frame.</p>
     */
    public static boolean renderVanilla(ClientLevel level, PoseStack poseStack,
                                        Matrix4f frustumMatrix, Matrix4f projectionMatrix,
                                        float partialTick, double camX, double camY, double camZ,
                                        float targetCloudHeight, AgeColor targetColor,
                                        boolean applyMystcraftColor) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer == null || level == null) return false;

        double syntheticCamY = camY + (MODERN_NORMAL_CLOUD_HEIGHT - targetCloudHeight);
        float[] previous = RenderSystem.getShaderColor().clone();

        if (applyMystcraftColor && targetColor != null) {
            Vec3 vanilla = level.getCloudColor(partialTick);
            RenderSystem.setShaderColor(
                    ratio(targetColor.r(), (float) vanilla.x),
                    ratio(targetColor.g(), (float) vanilla.y),
                    ratio(targetColor.b(), (float) vanilla.z),
                    1.0F);
        }

        VANILLA_DELEGATION.set(Boolean.TRUE);
        try {
            mc.levelRenderer.renderClouds(
                    poseStack, frustumMatrix, projectionMatrix, partialTick,
                    camX, syntheticCamY, camZ);
            return true;
        } finally {
            VANILLA_DELEGATION.remove();
            RenderSystem.setShaderColor(previous[0], previous[1], previous[2], previous[3]);
        }
    }

    private static float ratio(float target, float vanilla) {
        float t = clamp(target);
        if (Math.abs(vanilla) < 0.0001F) return t <= 0.0001F ? 0.0F : 1.0F;
        // Shader colour is a multiplier rather than a colour replacement. Keep a generous
        // finite ceiling only as a guard against pathological modded cloud-colour providers.
        return Math.max(0.0F, Math.min(16.0F, t / vanilla));
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
