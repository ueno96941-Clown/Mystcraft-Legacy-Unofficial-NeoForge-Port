package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xcompwiz.mystcraft.block.BlockBookReceptacle;
import com.xcompwiz.mystcraft.blockentity.BookReceptacleBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Restores the closed book sitting in the 0.13.7.06 Book Receptacle. */
public final class BookReceptacleRenderer implements BlockEntityRenderer<BookReceptacleBlockEntity> {
    private final LegacyBookModelRenderer legacyBook;

    public BookReceptacleRenderer(BlockEntityRendererProvider.Context context) {
        this.legacyBook = new LegacyBookModelRenderer(context);
    }

    @Override
    public void render(BookReceptacleBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        ItemStack stack = be.getBook();
        if (stack.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        Direction rotation = be.getBlockState().getValue(BlockBookReceptacle.ROTATION);
        switch (rotation) {
            case UP -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case DOWN, WEST -> { }
        }
        poseStack.scale(0.8F, 0.8F, 0.8F);

        if (legacyBook.canRender(stack)) {
            legacyBook.render(stack, 0.0F, poseStack, buffers, packedLight, packedOverlay);
        } else {
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                    packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffers, be.getLevel(), 0);
        }
        poseStack.popPose();
    }
}
