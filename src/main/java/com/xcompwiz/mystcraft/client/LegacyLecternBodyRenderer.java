package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xcompwiz.mystcraft.Mystcraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/**
 * Exact 1.21 rendering bridge for the 0.13.7.06 ModelLectern body.
 *
 * <p>The legacy block deliberately had no baked world model: RenderLectern
 * drew a {@code ModelPrism} and a narrow {@code ModelBox} against the
 * 64x32 {@code textures/entity/lectern.png}.  Stretching that texture over a
 * normal JSON cuboid exposes its UV guide pixels (the red/blue stripes seen
 * in early port builds), so the original geometry and UV rectangles are
 * reproduced here.</p>
 */
final class LegacyLecternBodyRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "textures/entity/lectern.png");
    private static final float TW = 64.0F;
    private static final float TH = 32.0F;

    private LegacyLecternBodyRenderer() {}

    static void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();

        // Legacy ModelPrism(base, 0,0, -8,-0.5,-8, 16,1,7,16,0),
        // then base.setRotationPoint(0,0.5,0), rendered at 1/16 scale.
        V p1 = new V(-0.5F, 0.0F,    -0.5F);
        V p2 = new V( 0.5F, 0.0F,    -0.5F);
        V p3 = new V( 0.5F, 0.4375F, -0.5F);
        V p4 = new V(-0.5F, 0.0625F, -0.5F);
        V p5 = new V(-0.5F, 0.0F,     0.5F);
        V p6 = new V( 0.5F, 0.0F,     0.5F);
        V p7 = new V( 0.5F, 0.4375F,  0.5F);
        V p8 = new V(-0.5F, 0.0625F,  0.5F);

        // Same vertex order and atlas rectangles as ModelPrism.java.
        quad(out, pose, packedLight, p7,p6,p2,p3, 32,0,39,16); // x+
        quad(out, pose, packedLight, p4,p1,p5,p8, 39,0,40,16); // x-
        quad(out, pose, packedLight, p2,p6,p5,p1,  0,0,16,16); // bottom
        quad(out, pose, packedLight, p7,p3,p4,p8, 16,0,32,16); // top
        quad(out, pose, packedLight, p4,p3,p2,p1,  0,16,16,23); // z-
        quad(out, pose, packedLight, p5,p6,p7,p8, 16,16,32,23); // z+

        // Legacy ModelBox(base,32,2, -8,-0.5,-7, 1,2,14,0).
        V b1 = new V(-0.5F,    0.0F,   -0.4375F);
        V b2 = new V(-0.4375F, 0.0F,   -0.4375F);
        V b3 = new V(-0.4375F, 0.125F, -0.4375F);
        V b4 = new V(-0.5F,    0.125F, -0.4375F);
        V b5 = new V(-0.5F,    0.0F,    0.4375F);
        V b6 = new V(-0.4375F, 0.0F,    0.4375F);
        V b7 = new V(-0.4375F, 0.125F,  0.4375F);
        V b8 = new V(-0.5F,    0.125F,  0.4375F);

        // Same atlas rectangles as ModelBox.java for width=1,height=2,depth=14.
        quad(out, pose, packedLight, b6,b2,b3,b7, 47,16,61,18); // x+
        quad(out, pose, packedLight, b1,b5,b8,b4, 32,16,46,18); // x-
        quad(out, pose, packedLight, b6,b5,b1,b2, 46, 2,47,16); // bottom
        quad(out, pose, packedLight, b3,b4,b8,b7, 47,16,48, 2); // top (legacy flipped V)
        quad(out, pose, packedLight, b2,b1,b4,b3, 46,16,47,18); // z-
        quad(out, pose, packedLight, b5,b6,b7,b8, 61,16,62,18); // z+
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose, int light,
                             V a, V b, V c, V d,
                             float u1, float v1, float u2, float v2) {
        Vector3f ab = new Vector3f(b.x-a.x, b.y-a.y, b.z-a.z);
        Vector3f ac = new Vector3f(c.x-a.x, c.y-a.y, c.z-a.z);
        Vector3f n = ab.cross(ac, new Vector3f()).normalize();

        // Mirrors the legacy TexturedQuad rectangle assignment closely:
        // first pair uses the far U edge, then walks around the quad.
        vertex(out, pose, light, a, u2/TW, v1/TH, n);
        vertex(out, pose, light, b, u1/TW, v1/TH, n);
        vertex(out, pose, light, c, u1/TW, v2/TH, n);
        vertex(out, pose, light, d, u2/TW, v2/TH, n);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, int light,
                               V p, float u, float v, Vector3f n) {
        out.addVertex(pose, p.x, p.y, p.z)
                .setColor(255,255,255,255)
                .setUv(u,v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, n.x, n.y, n.z);
    }

    private record V(float x, float y, float z) {}
}
