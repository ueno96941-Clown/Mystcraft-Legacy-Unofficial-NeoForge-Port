package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.entity.EntityLinkbook;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Renders the legacy physical book using the same open-book model as Book Stand/Lectern. */
public final class LinkBookEntityRenderer extends EntityRenderer<EntityLinkbook> {
    private static final ResourceLocation FALLBACK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "textures/entity/linkbook.png");
    private final LegacyBookModelRenderer legacyBook;

    public LinkBookEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.legacyBook = new LegacyBookModelRenderer(context);
        this.shadowRadius = 0.15F;
    }

    @Override
    public void render(EntityLinkbook entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        // 0.13.7.06 RenderLinkbook: y + 1/16, yaw+90 about -Y, Z+90, scale .8.
        poseStack.translate(0.0D, 0.0625D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-(entityYaw + 90.0F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.scale(0.8F, 0.8F, 0.8F);
        legacyBook.render(entity.getBook(), 1.2F, poseStack, buffers, packedLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityLinkbook entity) {
        return FALLBACK_TEXTURE;
    }
}
