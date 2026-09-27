package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.network.AgeClientVisualState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xcompwiz.mystcraft.world.worldgen.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Shared client effects object for every runtime Mystcraft Age.
 * Per-Age differences come from AgeClientVisualState, not from mutable global registries.
 */
public final class AgeDimensionSpecialEffects extends DimensionSpecialEffects {
    public AgeDimensionSpecialEffects() {
        super(AgeCloudRenderer.MODERN_NORMAL_CLOUD_HEIGHT, true, SkyType.NORMAL, false, false);
    }

    @Override public Vec3 getBrightnessDependentFogColor(Vec3 fog, float brightness) { return fog; }
    @Override public boolean isFoggyAt(int x, int y) { return false; }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick,
                             Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix,
                             boolean isFoggy, Runnable setupFog) {
        var entry = AgeClientVisualState.get(level.dimension().location().toString());
        if (entry == null) {
            // CP257: this effects object is installed only for a Mystcraft Age. Never let the
            // client fall back to LevelRenderer's vanilla sky while the visual snapshot is still
            // in flight: that fallback includes the vanilla sun/moon/stars and can flash or appear
            // to move independently of the authored celestial plan. The clear/fog colour remains
            // visible for these few frames; once the snapshot arrives AgeSkyRenderer owns the pass.
            return true;
        }

        AgeVisualSnapshot snap = entry.snapshot();
        AgeSkyPlan sky = new AgeSkyPlan(snap.celestials(), snap.drawHorizon(), snap.drawVoid(),
                snap.horizonHeight(), snap.cloudHeight(), snap.endSkyBackground(), snap.endSkyGradient());
        AgeSkyRenderBridge.RenderState state = AgeSkyRenderBridge.resolve(
                sky, level.getDayTime(), partialTick, level.getRainLevel(partialTick),
                level.getStarBrightness(partialTick), entry.ageSeed());

        return AgeSkyRenderer.render(level, snap, state, modelViewMatrix, projectionMatrix, camera, partialTick, level.getRainLevel(partialTick), level.getThunderLevel(partialTick), setupFog, entry.ageSeed());
    }

    /**
     * NeoForge 1.21.1 lightmap extension hook.
     * Legacy Bright/Dark scales the block-light component; vanilla sky-light/weather/flicker math
     * has already produced the incoming RGB, so apply the equivalent channel ratio here.
     */
    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken,
                                     float blockLightRedFlicker, float skyLight, int pixelX, int pixelY,
                                     Vector3f colors) {
        var entry=AgeClientVisualState.get(level.dimension().location().toString());
        if(entry==null)return;
        AgeLightingPlan plan=new AgeLightingPlan(entry.snapshot().lighting());
        float delta=AgeLightmapBridge.brightnessDelta(plan,pixelX);
        colors.set(Math.max(0F,Math.min(1F,colors.x()+delta)),
                   Math.max(0F,Math.min(1F,colors.y()+delta)),
                   Math.max(0F,Math.min(1F,colors.z()+delta)));
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level,int ticks,float partialTick,LightTexture lightTexture,double camX,double camY,double camZ) {
        var entry=AgeClientVisualState.get(level.dimension().location().toString());
        if(entry==null)return false;
        AgeWeatherPlan plan=AgeWeatherResolver.resolveMode(entry.snapshot().weather());
        // OFF and CLOUDY must not allow vanilla precipitation geometry.
        if(AgePrecipitationHookBridge.suppressVanilla(plan))return true;
        int weatherRadius=AgeWeatherRenderDistanceContract.radius(
                Minecraft.getInstance().options.graphicsMode().get()!=GraphicsStatus.FAST);
        if(AgePrecipitationHookBridge.needsCustomSnowRenderer(plan)
                || AgePrecipitationHookBridge.needsCustomRainRenderer(plan)) {
            // Legacy WeatherRendererMyst selected rain vs snow per column from the controlled
            // biome temperature. Render both filtered passes so a forced Rain Age can still
            // become snow at sufficiently high altitude, exactly as 1.12 allowed.
            AgeRainRenderer.render(level,ticks,partialTick,camX,camY,camZ,plan,weatherRadius);
            AgeSnowRenderer.render(level,ticks,partialTick,camX,camY,camZ,plan,weatherRadius);
            return true;
        }
        // WeatherOn and cyclic modes retain biome-sensitive vanilla precipitation.
        return false;
    }

    @Override
    public boolean tickRain(ClientLevel level,int ticks,Camera camera) {
        var entry=AgeClientVisualState.get(level.dimension().location().toString());
        if(entry==null)return false;
        AgeWeatherPlan plan=AgeWeatherResolver.resolveMode(entry.snapshot().weather());
        if(AgePrecipitationHookBridge.suppressVanilla(plan))return true;
        if(AgePrecipitationHookBridge.needsCustomSnowRenderer(plan)||AgePrecipitationHookBridge.needsCustomRainRenderer(plan)){
            AgePrecipitationTicker.tick(level,ticks,camera,plan);
            return true;
        }
        return false;
    }
    @Override
    public boolean renderClouds(ClientLevel level,int ticks,float partialTick,PoseStack poseStack,double camX,double camY,double camZ,
                                Matrix4f modelViewMatrix,Matrix4f projectionMatrix) {
        // AgeCloudRenderer deliberately re-enters LevelRenderer so Minecraft 1.21.1 owns
        // cloud geometry/camera transforms. The nested pass must therefore fall through.
        if (AgeCloudRenderer.isVanillaDelegationActive()) return false;

        var entry=AgeClientVisualState.get(level.dimension().location().toString());
        if(entry==null)return false;
        AgeVisualSnapshot snap=entry.snapshot();

        // Ordinary cloud colour + Legacy normal height requires no interception at all.
        if (AgeCloudRenderer.canUseDirectVanilla(snap)) return false;

        boolean customColor = AgeCloudRenderer.hasCustomCloudColor(snap);
        AgeColor color = customColor
                ? AgeCloudColorBridge.resolve(snap, level.getDayTime(), level.getTimeOfDay(partialTick),
                    level.getRainLevel(partialTick), level.getThunderLevel(partialTick))
                : null;
        return AgeCloudRenderer.renderVanilla(level,poseStack,modelViewMatrix,projectionMatrix,partialTick,
                camX,camY,camZ,snap.cloudHeight(),color,customColor);
    }


}
