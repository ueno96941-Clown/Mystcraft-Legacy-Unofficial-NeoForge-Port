package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xcompwiz.mystcraft.registry.MystItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** BEWLR for the legacy Book Stand and Lectern inventory/hand models. */
public final class DisplayFurnitureItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final LegacyBookStandBodyRenderer stand = new LegacyBookStandBodyRenderer();

    public DisplayFurnitureItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (stack.is(MystItems.LECTERN_BLOCK.get())) {
            // Same model-space origin as the world TESR, centered into a normal item cube.
            poseStack.translate(0.5D, 0.20D, 0.5D);
            // CP274: the baked item model already applies the legacy inventory yaw.
            // Do not add the extra 45-degree world yaw here; doing so turned the
            // original sloped lectern silhouette almost edge-on in the GUI.
            LegacyLecternBodyRenderer.render(poseStack, buffers, packedLight);
        } else {
            // Exact RenderBookstand body basis: center, vertical flip, then yaw.
            poseStack.translate(0.5D, 0.70D, 0.5D);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            // CP274: same as the original OBJ inventory model: GUI rotation is
            // supplied by the item model, while this renderer only supplies the
            // legacy TESR body basis.  The old extra 45 degrees made the arms read
            // as a flat T instead of the original V-shaped stand icon.
            stand.render(poseStack, buffers, packedLight);
        }
        poseStack.popPose();
    }
}
