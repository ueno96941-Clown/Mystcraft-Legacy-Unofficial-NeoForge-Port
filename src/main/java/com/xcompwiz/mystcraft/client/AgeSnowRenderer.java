package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.xcompwiz.mystcraft.world.worldgen.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;

/** Forced-weather snow pass using Legacy 1.12 radial geometry and climate classification. */
public final class AgeSnowRenderer {
    private static final ResourceLocation SNOW = ResourceLocation.withDefaultNamespace("textures/environment/snow.png");

    private AgeSnowRenderer() {}

    public static boolean render(ClientLevel level, int ticks, float partial,
                                 double camX, double camY, double camZ,
                                 AgeWeatherPlan plan, int radius) {
        var cols = AgeSnowRenderBridge.columns(
                ticks, partial, camX, camY, camZ, plan.rainingStrength(), radius);
        if (cols.isEmpty()) return true;

        // Legacy WeatherRendererMyst explicitly disabled back-face culling for precipitation.
        // These camera-facing radial sheets are double-sided; leaving modern culling enabled
        // makes many columns disappear depending on view direction, producing sparse white
        // fragments instead of a continuous rain/snow curtain.
        // CP300A: NeoForge 1.21.1 does not guarantee that a custom
        // DimensionSpecialEffects precipitation pass inherits an enabled depth test.
        // Without this explicit state, the weather quads can render through terrain
        // (most visibly forced snow inside caves). Vanilla precipitation is terrain-occluded,
        // so make that contract explicit for both forced rain and forced snow.
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
        RenderSystem.setShader(GameRenderer::getParticleShader);
        RenderSystem.setShaderTexture(0, SNOW);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);

        for (var c : cols) {
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, c.x(), c.z());
            int lo = AgeWeatherColumnBoundsLegacyMath.lowerY(c.minY(), surface);
            int hi = AgeWeatherColumnBoundsLegacyMath.upperY(c.maxY(), surface);
            if (hi == lo) continue;

            if (AgeLegacyPrecipitationClimateResolver.precipitation(level, c.x(), lo, c.z(), plan)
                    != AgePrecipitationBridge.Type.SNOW) continue;

            float x = (float) (c.x() + .5 - camX);
            float z = (float) (c.z() + .5 - camZ);
            float y0 = (float) (lo - camY);
            float y1 = (float) (hi - camY);
            float a = c.alpha();
            float xo = c.xOffset();
            float zo = c.zOffset();
            int light = AgeWeatherPackedLightBridge.snowPacked(LevelRenderer.getLightColor(
                    level, new BlockPos(c.x(), AgeWeatherColumnBoundsLegacyMath.lightSampleY(surface, camY), c.z())));

            b.addVertex(x-xo,y1,z-zo).setUv(c.uOffset(),lo*.25F+c.vOffset()).setColor(1,1,1,a).setLight(light);
            b.addVertex(x+xo,y1,z+zo).setUv(1F+c.uOffset(),lo*.25F+c.vOffset()).setColor(1,1,1,a).setLight(light);
            b.addVertex(x+xo,y0,z+zo).setUv(1F+c.uOffset(),hi*.25F+c.vOffset()).setColor(1,1,1,a).setLight(light);
            b.addVertex(x-xo,y0,z-zo).setUv(c.uOffset(),hi*.25F+c.vOffset()).setColor(1,1,1,a).setLight(light);
        }

        MeshData mesh = b.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
        Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        return true;
    }
}
