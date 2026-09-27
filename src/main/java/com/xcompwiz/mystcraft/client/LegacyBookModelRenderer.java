package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.item.ItemLinking;
import com.xcompwiz.mystcraft.registry.MystItems;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Shared modern renderer for the legacy 3D Mystcraft book model. */
final class LegacyBookModelRenderer {
    private static final ResourceLocation AGEBOOK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "textures/entity/agebook.png");
    private static final ResourceLocation LINKBOOK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "textures/entity/linkbook.png");

    private final BookModel bookModel;

    LegacyBookModelRenderer(BlockEntityRendererProvider.Context context) {
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    LegacyBookModelRenderer(EntityRendererProvider.Context context) {
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    boolean canRender(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemLinking;
    }

    void render(ItemStack stack, float openAmount, PoseStack poseStack, MultiBufferSource buffers,
                int packedLight, int packedOverlay) {
        if (!canRender(stack)) return;
        ResourceLocation texture = stack.is(MystItems.AGEBOOK.get()) ? AGEBOOK_TEXTURE : LINKBOOK_TEXTURE;
        bookModel.setupAnim(0.0F, 0.0F, 0.0F, openAmount);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entitySolid(texture));
        bookModel.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }
}
