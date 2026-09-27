package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.xcompwiz.mystcraft.world.worldgen.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FogType;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

/**
 * Modern consumer for AgeSkyRenderBridge.
 *
 * The bridge remains the semantic authority. This renderer owns only GPU-facing geometry.
 * Sun/moon use vanilla textures through the normal sky path when the Age has the ordinary
 * single celestial arrangement; custom/multiple celestial arrangements take this renderer path.
 */
public final class AgeSkyRenderer {
    private static final net.minecraft.resources.ResourceLocation SUN =
        net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    private static final net.minecraft.resources.ResourceLocation MOON =
        net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png");
    private static final net.minecraft.resources.ResourceLocation END_SKY =
        net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/environment/end_sky.png");
    private AgeSkyRenderer() {}

    public static boolean render(ClientLevel level, AgeVisualSnapshot snapshot, AgeSkyRenderBridge.RenderState state,
                                 Matrix4f modelView, Matrix4f projection, Camera camera, float partialTick,
                                 float rainStrength, float thunderStrength, Runnable setupFog, long ageSeed) {
        // This renderer is invoked only for a synchronized Mystcraft Age; always own its sky.

        // CP239: Water immersion must be owned by Minecraft's underwater fog/clear colour, not by
        // Mystcraft's terrestrial sky geometry. The CP236 dome intentionally ignores shader fog so
        // its horizon skirt can reproduce the old fixed-function sky/fog transition on land. Under
        // water that same property makes the land horizon leak through as coloured/black bands.
        //
        // Keep ownership of the sky pass (return true) so vanilla cannot draw a second sky behind
        // the water veil, but submit no dome, horizon, void, End-sky, sun/moon/stars, or sunset
        // geometry while the camera is immersed. ComputeFogColor already supplies the ColorWater-
        // based underwater veil, so the water fog remains the sole far-background authority.
        if (camera.getFluidInCamera() == FogType.WATER) {
            return true;
        }

        // Take ownership so vanilla does not draw a second incompatible sky. The exact celestial
        // draw list has already been resolved; GPU batches are intentionally kept in one class.
        setupFog.run();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Build/cache exact legacy CPU meshes now. GPU upload remains isolated from Symbol semantics.
        // This guarantees the final uploader cannot accidentally change star distributions or rainbow shape.
        LegacySkyMeshCache.prepare(ageSeed, state);
        PoseStack pose = new PoseStack();
        pose.mulPose(modelView);
        // Legacy ColorSky/ColorSkyNight: tint the upper sky plane. Natural/UNSET use level sky color.
        var vanillaSky=level.getSkyColor(camera.getPosition(), partialTick);
        int vanillaRgb=((int)(Math.max(0,Math.min(1,vanillaSky.x))*255)<<16)
                |((int)(Math.max(0,Math.min(1,vanillaSky.y))*255)<<8)
                |(int)(Math.max(0,Math.min(1,vanillaSky.z))*255);
        var cameraPos = BlockPos.containing(camera.getPosition());
        var weatherPlan = AgeWeatherResolver.resolveMode(snapshot.weather());
        float legacyTemperature = AgeLegacyPrecipitationClimateResolver.controlledTemperature(
                level, cameraPos.getX(), cameraPos.getY(), cameraPos.getZ(), weatherPlan);
        float vanillaCelestialAngle = level.getTimeOfDay(partialTick);
        var skyColorBeforeWeather=AgeSkyColorBridge.sky(snapshot,vanillaRgb,legacyTemperature,level.getDayTime(),vanillaCelestialAngle);
        var skyColor=skyColorBeforeWeather;
        // Legacy WorldProviderMyst applied rain then thunder attenuation after ColorSky resolution.
        // Natural sky already arrives weather-adjusted from ClientLevel; only custom ColorSky needs this pass.
        if (AgeSkyColorBridge.requiresLegacyWeatherPass(snapshot))
            skyColor=AgeSkyWeatherTintBridge.apply(skyColor,rainStrength,thunderStrength);
        // CP236 result-fidelity composition: the old upper sky plane was fogged by distance, so
        // ColorFog influenced only the apparent horizon rather than the whole hemisphere.
        // AgeSkyGeometry recreates that narrow horizon transition directly in vertex colours,
        // while terrain fog remains owned by ViewportEvent.ComputeFogColor.
        var fogProviders = snapshot.colors().get("fog");
        boolean customSkyFog = fogProviders != null && !fogProviders.isEmpty();
        AgeColor horizonColor;
        if (customSkyFog) {
            horizonColor = AgeFogColorBridge.resolve(
                    fogProviders, level.getDayTime() / 12000.0F, level.getTimeOfDay(partialTick));
        } else {
            // CP237: ColorSky never replaced ordinary atmospheric fog in 0.13.7.06.
            // Reuse the exact vanilla/NeoForge fog RGB captured by ComputeFogColor this frame
            // so a red custom sky still fades into the normal biome/time/weather horizon.
            horizonColor = AgeVanillaFogColorCache.get(level.dimension().location().toString());
            if (horizonColor == null) {
                // Defensive first-frame fallback before ComputeFogColor has fired. Keep this neutral
                // and short-lived rather than synthesizing a horizon from the custom sky itself.
                horizonColor = AgeFogColorBridge.resolve(
                        java.util.List.of(), level.getDayTime() / 12000.0F, level.getTimeOfDay(partialTick));
            }
        }
        // We render from inside the dome. Vanilla sky rendering normally has face culling enabled,
        // so an outward-wound sphere is invisible from the camera and only the clear/fog colour is seen.
        // Disable culling for this single batch, then restore it immediately.
        RenderSystem.disableCull();
        AgeSkyGpuUploader.drawIntrinsicColored(
                AgeSkyGeometry.verticalGradientDome(skyColor, horizonColor), pose.last().pose(), 1F);
        RenderSystem.enableCull();
        // Restore the caller-owned terrain fog state before sun/moon/stars. The dome itself does
        // not depend on shader fog, so no stale fog colour can bleach the sky.
        setupFog.run();
        for (var batch : AgeSkyBatchPlanner.plan(state)) {
            pose.pushPose();
            if(batch.mesh()!=AgeSkyBatchPlanner.Mesh.SUNSET_FAN) {
                pose.mulPose(Axis.YP.rotationDegrees(batch.yawDegrees()));
                pose.mulPose(Axis.XP.rotationDegrees(batch.pitchDegrees()));
            }
            if(batch.additive()) RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                                                       com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
            else RenderSystem.defaultBlendFunc();
            switch (batch.mesh()) {
                case NORMAL_STARS -> AgeSkyGpuUploader.drawColored(LegacySkyMeshCache.normalStars(), pose.last().pose(),batch.red(),batch.green(),batch.blue(),batch.alpha());
                case TWINKLE_LAYER -> AgeSkyGpuUploader.drawColored(LegacySkyMeshCache.twinkle(batch.symbolSeed(),batch.randomizedPeriod(),batch.randomizedAngle()).get(batch.layer()), pose.last().pose(),batch.red(),batch.green(),batch.blue(),batch.alpha());
                case RAINBOW -> AgeSkyGpuUploader.drawColored(LegacySkyMeshCache.rainbow(), pose.last().pose(),batch.red(),batch.green(),batch.blue(),batch.alpha());
                case SUN_QUAD -> AgeSkyGpuUploader.drawTexturedQuad(AgeSkyGeometry.celestialQuad(batch.size()), pose.last().pose(), SUN, batch.alpha());
                case MOON_QUAD -> {
                    int phase=batch.moonPhase(), x=phase%4, y=(phase/4)%2;
                    // Legacy moon reverses U across the quad.
                    AgeSkyGpuUploader.drawTexturedRegion(AgeSkyGeometry.celestialQuad(batch.size()), pose.last().pose(), MOON,
                            batch.alpha(), (x+1)/4F,(y+1)/2F,x/4F,y/2F);
                }
                case END_SKY_CUBE -> {
                    var c=state.endSkyColor();
                    AgeSkyGpuUploader.drawEndSkyLegacy(AgeSkyGeometry.endSkyCube(),pose.last().pose(),END_SKY,
                            c.r(),c.g(),c.b(),batch.alpha());
                }
                case SUNSET_FAN -> {
                    // Legacy: rotate 90 X, flip by celestial sine, then -angle around Z.
                    pose.mulPose(Axis.XP.rotationDegrees(90F));
                    if(Math.sin(batch.pitchDegrees()*Math.PI/180.0)<0) pose.mulPose(Axis.ZP.rotationDegrees(180F));
                    pose.mulPose(Axis.ZP.rotationDegrees(-batch.yawDegrees()));
                    AgeSkyGpuUploader.drawSunsetFan(AgeSkyGeometry.sunsetFan(batch.alpha()),pose.last().pose(),batch.red(),batch.green(),batch.blue());
                }
                case HORIZON -> {
                    // 1.12 drew a finite lower sky grid here while fixed-function fog hid its
                    // outer edge. Replaying that mesh in the 1.21.1 shader pipeline exposes the
                    // grid as a large horizontal rectangle/band near the horizon. Modern sky/fog
                    // already owns that transition, so preserve the HORIZON semantic in the plan
                    // but do not submit the obsolete lower plane. VOID below remains explicit.
                }
                case VOID -> {
                    double horizonDst=camera.getPosition().y-state.horizonHeight();
                    if(horizonDst<0) {
                        pose.translate(0,12,0);
                        AgeSkyGpuUploader.drawColored(LegacySkyMeshCache.lowerSky(),pose.last().pose(),0,0,0,1);
                        float topY=-((float)(horizonDst+65.0));
                        AgeSkyGpuUploader.drawColored(AgeSkyGeometry.voidEnclosure(topY),pose.last().pose(),0,0,0,1);
                    }
                }
                default -> { /* textured sun/moon/end-sky and camera-relative horizon/void handled below */ }
            }
            pose.popPose();
        }
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        return true;
    }
}
