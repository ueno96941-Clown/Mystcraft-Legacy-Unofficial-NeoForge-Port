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

/** Exact ModelBookstand geometry from Mystcraft 0.13.7.06. */
final class LegacyBookStandBodyRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Mystcraft.MOD_ID, "textures/entity/bookstand.png");
    private static final String[] PARTS = {"leftarm", "post", "base", "rightarm"};
    private final ModelPart root = createLayer().bakeRoot();

    void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (String part : PARTS) {
            root.getChild(part).render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
        }
    }

    private static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        add(root, "leftarm", 4, 8, true,
                -0.5F, -0.5F, -1.5F, 6, 1, 3,
                0.25F, 0F, -0.25F, 0.5235988F, 0F, -0.2617994F);
        add(root, "post", 0, 8, true,
                -0.5F, 0F, -0.5F, 1, 6, 1,
                0F, 0F, 0F, 0F, 0F, 0F);
        add(root, "base", 0, 0, true,
                -2.5F, 0F, -2.5F, 5, 3, 5,
                0F, 5F, 0F, 0F, 0F, 0F);
        add(root, "rightarm", 4, 8, false,
                -0.5F, -0.5F, -1.5F, 6, 1, 3,
                -0.25F, 0F, -0.25F, -0.5235988F, 3.141593F, 0.2617994F);
        return LayerDefinition.create(mesh, 64, 32);
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
