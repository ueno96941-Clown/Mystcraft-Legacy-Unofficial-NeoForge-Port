package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xcompwiz.mystcraft.block.BlockBookDisplay;
import com.xcompwiz.mystcraft.blockentity.BookDisplayBlockEntity;
import com.xcompwiz.mystcraft.registry.MystBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Restores the legacy open-book display on the Book Stand and Lectern. */
public final class BookDisplayRenderer implements BlockEntityRenderer<BookDisplayBlockEntity> {
    private final LegacyBookModelRenderer legacyBook;
    private final LegacyBookStandBodyRenderer legacyStand = new LegacyBookStandBodyRenderer();
    private final PageItemRenderer pageRenderer = new PageItemRenderer();

    public BookDisplayRenderer(BlockEntityRendererProvider.Context context) {
        this.legacyBook = new LegacyBookModelRenderer(context);
    }

    @Override
    public void render(BookDisplayBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        boolean lectern = be.getBlockState().is(MystBlocks.LECTERN.get());
        Direction facing = be.getBlockState().hasProperty(BlockBookDisplay.FACING)
                ? be.getBlockState().getValue(BlockBookDisplay.FACING)
                : Direction.NORTH;

        // Both displays were TESR furniture in 0.13.7.06.  Render the exact
        // legacy bodies instead of approximate baked JSON models.
        poseStack.pushPose();
        if (lectern) {
            poseStack.translate(0.5D, 0.0D, 0.5D);
            Direction legacyFacing = facing.getAxis() == Direction.Axis.Z ? facing.getOpposite() : facing;
            poseStack.mulPose(Axis.YP.rotationDegrees(legacyFacing.toYRot() + 90.0F));
            LegacyLecternBodyRenderer.render(poseStack, bufferSource, packedLight);
        } else {
            // RenderBookstand: translate(x+.5,y+.5,z+.5), Z180, Y(45*rotationIndex).
            // Cardinal projection of the old 8-way index is Direction#toYRot.
            poseStack.translate(0.5D, 0.5D, 0.5D);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
            legacyStand.render(poseStack, bufferSource, packedLight);
        }
        poseStack.popPose();

        ItemStack stack = be.getBook();
        if (stack.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);

        if (lectern) {
            Direction legacyFacing = facing.getAxis() == Direction.Axis.Z ? facing.getOpposite() : facing;
            poseStack.mulPose(Axis.YP.rotationDegrees(legacyFacing.toYRot() + 90.0F));

            // Exact legacy RenderLectern item transform.
            poseStack.translate(0.0D, 0.255D, 0.0D);
            poseStack.mulPose(Axis.ZP.rotationDegrees(110.0F));
            if (legacyBook.canRender(stack)) {
                poseStack.scale(0.8F, 0.8F, 0.8F);
                legacyBook.render(stack, 1.22F, poseStack, bufferSource, packedLight, packedOverlay);
            } else {
                poseStack.translate(0.0D, 0.20D, 0.0D);
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.translate(0.0D, 0.25D, 0.0D);
                if (stack.getItem() instanceof com.xcompwiz.mystcraft.item.ItemPage) {
                    // CP244: server logs proved the Symbol Page is already inside the generated
                    // Lectern.  Calling ItemRenderer from a BER did not reliably re-enter the
                    // PAGE BEWLR path, so render the stack-sensitive Mystcraft page directly.
                    // PageItemRenderer uses 0..1 item-space; recenter it exactly as FIXED would.
                    poseStack.translate(-0.5D, -0.5D, -0.5D);
                    pageRenderer.renderByItem(stack, ItemDisplayContext.FIXED, poseStack, bufferSource,
                            packedLight, OverlayTexture.NO_OVERLAY);
                } else {
                    Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                            packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, be.getLevel(), 0);
                }
            }
        } else {
            // Legacy RenderBookstand path.
            if (!legacyBook.canRender(stack)) {
                poseStack.popPose();
                return;
            }
            poseStack.translate(0.0D, 0.55D, 0.0D);
            // 0.13.7.06: rotate(90 + 45*rotationIndex, 0, -1, 0).
            // Direction#toYRot is the cardinal projection of the old 8-way index,
            // so converting the negative Y axis to modern +Y requires negating
            // the whole angle, not just the facing component.
            poseStack.mulPose(Axis.YP.rotationDegrees(-(90.0F + facing.toYRot())));
            poseStack.mulPose(Axis.ZP.rotationDegrees(120.0F));
            poseStack.scale(0.8F, 0.8F, 0.8F);
            legacyBook.render(stack, 1.05F, poseStack, bufferSource, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

}
