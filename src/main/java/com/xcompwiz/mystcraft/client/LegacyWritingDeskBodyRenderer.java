package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xcompwiz.mystcraft.Mystcraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Exact modernized geometry for 0.13.7.06 ModelWritingDesk.
 *
 * <p>The old Writing Desk was rendered as one TESR from the lower-main tile
 * entity; the neighbouring foot and optional two-block backboard were logical
 * blocks only.  Reconstructing the original ModelRenderer parts here avoids the
 * stretched full-atlas UVs that a baked JSON approximation produced.</p>
 */
final class LegacyWritingDeskBodyRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Mystcraft.MOD_ID, "textures/entity/desk.png");

    private static final String[] BASE = {
            "bottomShelf", "middleShelf", "deskTop", "deskMiddle",
            "deskLeft", "deskRight", "deskBack", "deskMiddleBottom"
    };
    private static final String[] BACKBOARD = {
            "deskTopBack", "deskTopLeft", "deskTopRight", "deskTopTop",
            "angleLeft", "angleRight", "cupboardLeft", "cupboardRight"
    };

    private final ModelPart root = createLayer().bakeRoot();

    void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                boolean backing, int paperCount) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (String name : BASE) root.getChild(name).render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        if (backing) {
            for (String name : BACKBOARD) root.getChild(name).render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        }
        if (paperCount > 0) root.getChild("paper2").render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        if (paperCount > 1) root.getChild("paper3").render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        if (paperCount > 2) root.getChild("paper1").render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        if (paperCount > 27) root.getChild("paperStack2").render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        if (paperCount > 47) root.getChild("paperStack1").render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
    }

    private static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        add(root, "bottomShelf", 0,34, true, 0,0,0, 14,1,15, -7,23,-8, 0,0,0);
        add(root, "middleShelf", 0,17, true, 0,0,0, 30,2,15, -7,15,-8, 0,0,0);
        add(root, "deskTop", 0,0, true, 0,0,0, 32,1,16, -8,8,-8, 0,0,0);
        add(root, "deskMiddle", 94,36, true, 0,0,0, 2,6,15, 7,9,-8, 0,0,0);
        add(root, "deskLeft", 90,1, true, 0,0,0, 1,15,16, -8,9,-8, 0,0,0);
        add(root, "deskRight", 90,1, false, 0,0,0, 1,15,16, 23,9,-8, 0,0,0);
        add(root, "deskBack", 128,0, true, 0,0,0, 30,15,1, -7,9,7, 0,0,0);
        add(root, "deskMiddleBottom", 77,42, true, 0,0,0, 1,7,15, 7,17,-8, 0,0,0);

        add(root, "deskTopBack", 128,16, true, 0,0,0, 32,12,1, -8,-4,7, 0,0,0);
        add(root, "deskTopLeft", 146,40, true, 0,0,0, 1,12,6, -8,-4,1, 0,0,0);
        add(root, "deskTopRight", 146,40, true, 0,0,0, 1,12,6, 23,-4,1, 0,0,0);
        add(root, "deskTopTop", 128,29, true, 0,0,0, 30,1,6, -7,-4,1, 0,0,0);
        add(root, "angleLeft", 128,40, true, 0,0,0, 1,15,8, -7.99f,-4,1, -0.6457718f,0,0);
        add(root, "angleRight", 128,40, false, 0,0,0, 1,15,8, 22.99f,-4,1, -0.6457718f,0,0);
        add(root, "cupboardLeft", 160,40, true, 0,0,0, 7,11,4, -7,-3,3, 0,0,0);
        add(root, "cupboardRight", 182,40, true, 0,0,0, 7,11,4, 16,-3,3, 0,0,0);

        // Legacy ModelRenderer accepted zero-height boxes for loose paper. Modern
        // ModelPart emits coincident faces for a 0-thickness cube, which produces
        // the white/noisy z-fighting seen on the desk. Give the sheet an
        // imperceptible 1/64-pixel thickness while retaining the old footprint,
        // UV origin, rotation and placement.
        final float paperThickness = 0.015625f;
        add(root, "paper1", 0,60, true, -2,0,0, 7,paperThickness,5, 16.5f,14.8f,-6.3f, 0,-0.1047198f,0);
        add(root, "paper2", 0,60, true, -2,0,0, 7,paperThickness,5, 16.5f,14.8f,-4.9f, 0,0.2094395f,0);
        add(root, "paper3", 0,60, true, -2,0,0, 7,paperThickness,5, 16.5f,14.8f,-5.9f, 0,0,0);
        add(root, "paperStack1", 0,60, true, -2,0,0, 7,1,5, 16.5f,13,-5, 0,-0.0174533f,0);
        add(root, "paperStack2", 0,60, true, 0,0,0, 7,1,5, 14.5f,14,-5, 0,0.0698132f,0);

        return LayerDefinition.create(mesh, 256, 128);
    }

    private static void add(PartDefinition root, String name, int u, int v, boolean mirror,
                            float bx, float by, float bz, float sx, float sy, float sz,
                            float px, float py, float pz, float rx, float ry, float rz) {
        CubeListBuilder cube = CubeListBuilder.create().texOffs(u, v).mirror(mirror)
                .addBox(bx, by, bz, sx, sy, sz);
        PartPose pose = (rx == 0F && ry == 0F && rz == 0F)
                ? PartPose.offset(px, py, pz)
                : PartPose.offsetAndRotation(px, py, pz, rx, ry, rz);
        root.addOrReplaceChild(name, cube, pose);
    }
}
