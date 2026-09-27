package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.xcompwiz.mystcraft.entity.EntityMeteor;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Modern renderer for the 0.13.7.06 meteor.
 *
 * <p>The legacy {@code ModelMeteor} is a fifteen-layer stepped solid whose widths are
 * 8..15..8 model units.  {@code RenderMeteor} scaled those model units by
 * {@code meteor.scale / 10} and bound vanilla's End Portal texture.  CP299 temporarily drew
 * no mesh at all; CP300 restores that visible contract without depending on obsolete ModelBase.</p>
 */
public final class MeteorEntityRenderer extends EntityRenderer<EntityMeteor> {
    private static final ResourceLocation END_PORTAL_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");
    private static final int LAYERS = 15;
    private static final int MID = LAYERS / 2;

    public MeteorEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.75F;
    }

    @Override
    public void render(EntityMeteor entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();

        // Legacy RenderMeteor: scale(scale/10), rotate yaw around -Y, then pitch around +Z.
        float modelScale = entity.getScale() / 10.0F;
        poseStack.scale(modelScale, modelScale, modelScale);
        float renderYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float renderPitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.mulPose(Axis.YN.rotationDegrees(renderYaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(renderPitch));

        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(END_PORTAL_TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        for (int i = 0; i < LAYERS; i++) {
            int width = LAYERS - Math.abs(MID - i); // 8,9,...15,...9,8
            int min = -width / 2;                  // preserves legacy integer division
            float x0 = min;
            float x1 = min + width;
            float y0 = -LAYERS / 2 + i;            // -7 .. +7
            float y1 = y0 + 1.0F;
            float z0 = min;
            float z1 = min + width;
            cube(out, pose, x0, y0, z0, x1, y1, z1);
        }

        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    private static void cube(VertexConsumer out, PoseStack.Pose pose,
                             float x0, float y0, float z0, float x1, float y1, float z1) {
        // No-cull render type deliberately keeps this robust against the legacy face winding.
        quad(out, pose, new V(x0,y0,z0), new V(x1,y0,z0), new V(x1,y1,z0), new V(x0,y1,z0)); // -Z
        quad(out, pose, new V(x1,y0,z1), new V(x0,y0,z1), new V(x0,y1,z1), new V(x1,y1,z1)); // +Z
        quad(out, pose, new V(x0,y0,z1), new V(x0,y0,z0), new V(x0,y1,z0), new V(x0,y1,z1)); // -X
        quad(out, pose, new V(x1,y0,z0), new V(x1,y0,z1), new V(x1,y1,z1), new V(x1,y1,z0)); // +X
        quad(out, pose, new V(x0,y1,z0), new V(x1,y1,z0), new V(x1,y1,z1), new V(x0,y1,z1)); // +Y
        quad(out, pose, new V(x0,y0,z1), new V(x1,y0,z1), new V(x1,y0,z0), new V(x0,y0,z0)); // -Y
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose, V a, V b, V c, V d) {
        Vector3f ab = new Vector3f(b.x-a.x, b.y-a.y, b.z-a.z);
        Vector3f ac = new Vector3f(c.x-a.x, c.y-a.y, c.z-a.z);
        Vector3f n = ab.cross(ac, new Vector3f()).normalize();
        vertex(out, pose, a, 0.0F, 1.0F, n);
        vertex(out, pose, b, 1.0F, 1.0F, n);
        vertex(out, pose, c, 1.0F, 0.0F, n);
        vertex(out, pose, d, 0.0F, 0.0F, n);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, V p, float u, float v, Vector3f n) {
        out.addVertex(pose, p.x, p.y, p.z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, n.x, n.y, n.z);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMeteor entity) {
        return END_PORTAL_TEXTURE;
    }

    private record V(float x, float y, float z) {}
}
