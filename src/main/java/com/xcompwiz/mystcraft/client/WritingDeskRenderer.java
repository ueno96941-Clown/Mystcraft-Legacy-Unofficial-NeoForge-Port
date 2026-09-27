package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xcompwiz.mystcraft.block.BlockWritingDesk;
import com.xcompwiz.mystcraft.blockentity.WritingDeskBlockEntity;
import com.xcompwiz.mystcraft.registry.MystBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Exact 0.13.7.06 Writing Desk TESR geometry and target-item transform. */
public final class WritingDeskRenderer implements BlockEntityRenderer<WritingDeskBlockEntity> {
    private final LegacyBookModelRenderer legacyBook;
    private final LegacyWritingDeskBodyRenderer body = new LegacyWritingDeskBodyRenderer();

    public WritingDeskRenderer(BlockEntityRendererProvider.Context context) {
        this.legacyBook = new LegacyBookModelRenderer(context);
    }

    @Override
    public void render(WritingDeskBlockEntity desk, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Direction facing = desk.getBlockState().getValue(BlockWritingDesk.FACING);
        int horizontalFacingIndex = facing.get2DDataValue(); // SOUTH=0,WEST=1,NORTH=2,EAST=3; same as 1.12.
        boolean backing = hasBackboard(desk);
        int paperCount = desk.getItem(WritingDeskBlockEntity.PAPER_SLOT).getCount();

        // Exact legacy RenderWritingDesk body transform:
        // translate(x+.5,y+1.5,z+.5), X90, Y90, Z90, then Y(90*horizontal index).
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.5D, 0.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F * horizontalFacingIndex));
        body.render(poseStack, buffers, packedLight, backing, paperCount);
        poseStack.popPose();

        ItemStack target = desk.getTarget();
        if (target.isEmpty()) return;

        // Exact legacy item-space transform from RenderWritingDesk.renderItems().
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F * horizontalFacingIndex));

        if (legacyBook.canRender(target)) {
            poseStack.translate(-0.15D, 1.0D, 1.0D);
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            poseStack.scale(0.8F, 0.8F, 0.8F);
            legacyBook.render(target, 1.22F, poseStack, buffers, packedLight, packedOverlay);
        } else {
            poseStack.translate(-0.35D, 1.001D, 1.0D);
            poseStack.scale(0.7F, 0.7F, 0.7F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            // Map rendering had a dedicated 1.12 branch. For all normal desk targets,
            // preserve the old nested translate/scale before FIXED item rendering.
            poseStack.translate(0.0D, 0.3D, 0.02D);
            poseStack.scale(0.7F, 0.7F, 0.7F);
            Minecraft.getInstance().getItemRenderer().renderStatic(target, ItemDisplayContext.FIXED,
                    packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffers, desk.getLevel(), 0);
        }
        poseStack.popPose();
    }

    private static boolean hasBackboard(WritingDeskBlockEntity desk) {
        if (desk.getLevel() == null) return false;
        BlockState above = desk.getLevel().getBlockState(desk.getBlockPos().above());
        return above.is(MystBlocks.WRITING_DESK.get())
                && above.hasProperty(BlockWritingDesk.IS_TOP)
                && above.getValue(BlockWritingDesk.IS_TOP);
    }

    @Override
    public AABB getRenderBoundingBox(WritingDeskBlockEntity blockEntity) {
        // NeoForge frustum-culls BERs against this box.  The legacy desk is rendered once
        // from the lower-main block entity but visibly spans the neighbouring foot block
        // and the optional two-block upper backboard.  The default one-block AABB can
        // therefore disappear while part of the furniture is still on screen.
        BlockPos main = blockEntity.getBlockPos();
        Direction facing = blockEntity.getBlockState().getValue(BlockWritingDesk.FACING);
        BlockPos foot = main.relative(facing);
        int minX = Math.min(main.getX(), foot.getX());
        int minZ = Math.min(main.getZ(), foot.getZ());
        int maxX = Math.max(main.getX(), foot.getX()) + 1;
        int maxZ = Math.max(main.getZ(), foot.getZ()) + 1;
        return new AABB(minX, main.getY(), minZ, maxX, main.getY() + 2.0D, maxZ).inflate(0.125D);
    }

    @Override
    public boolean shouldRenderOffScreen(WritingDeskBlockEntity blockEntity) {
        // Keep the original TESR-style behaviour for the multi-block model.  The finite
        // bounding box above still gives NeoForge the true visible scope for section/frustum
        // bookkeeping instead of the default lower-main unit cube.
        return true;
    }
}
