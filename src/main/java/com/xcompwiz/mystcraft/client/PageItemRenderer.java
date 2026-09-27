package com.xcompwiz.mystcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.api.impl.runtime.ApiWordRegistry;
import com.xcompwiz.mystcraft.page.Page;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Random;

/**
 * Stack-sensitive 1.21.1 replacement for 0.13.7.06 PageBuilder.
 *
 * <p>The old client generated a 128x128 sprite by enlarging page_background to
 * 160x160, stamping up to four 64x64 Narayan words at the four legacy target
 * rectangles, then downscaling by 0.8.  Rendering those same normalized target
 * rectangles directly preserves the visible geometry while also supporting
 * runtime biome/material symbols that cannot be pre-baked into resource JSON.</p>
 */
public final class PageItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation PAGE = ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "textures/item/page_background.png");
    private static final ResourceLocation COMPONENTS = ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "textures/symbolcomponents.png");
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "textures/item/white.png");

    public PageItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        // BEWLR item space follows the normal baked-item 0..1 quad convention.
        // Do not shift to -0.5..+0.5 here: that double-centers the page after the
        // item transform and is what made the parchment/glyph sit off its GUI slot.
        // Keep the legacy 160px composition mapped directly onto this 0..1 square.
        quad(poseStack, buffers, PAGE, 0F, 0F, 1F, 1F, 0.500F,
                0F, 0F, 1F, 1F, 0xFFFFFFFF, packedLight, packedOverlay);

        if (Page.isLinkPanel(stack)) {
            // PageBuilder.BasicPageSprite: x=25..135, y=30..75 on the 160px
            // working canvas, followed by the same 0.8 scale as the page.
            quad(poseStack, buffers, WHITE,
                    25F / 160F, 1F - 75F / 160F,
                    135F / 160F, 1F - 30F / 160F, 0.502F,
                    0F, 0F, 1F, 1F, 0xFF000000, packedLight, packedOverlay);
        } else if (!Page.isBlank(stack)) {
            String symbol = Page.getSymbolId(stack);
            if (symbol != null) drawSymbol(poseStack, buffers, symbol, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void drawSymbol(PoseStack poseStack, MultiBufferSource buffers, String symbol,
                                   int packedLight, int packedOverlay) {
        String[] poem = LegacySymbolVisuals.poem(symbol);
        // Exact normalized PageBuilder targets from its 160x160 composition image:
        // 0 top=(48,0), 1 right=(96,48), 2 bottom=(48,96), 3 left=(0,48), each 64x64.
        if (poem.length > 0) drawWord(poseStack, buffers, poem[0], 48F/160F, 96F/160F, packedLight, packedOverlay);
        if (poem.length > 1) drawWord(poseStack, buffers, poem[1], 96F/160F, 48F/160F, packedLight, packedOverlay);
        if (poem.length > 2) drawWord(poseStack, buffers, poem[2], 48F/160F, 0F/160F, packedLight, packedOverlay);
        if (poem.length > 3) drawWord(poseStack, buffers, poem[3], 0F/160F, 48F/160F, packedLight, packedOverlay);
    }

    private static void drawWord(PoseStack poseStack, MultiBufferSource buffers, String word,
                                 float x, float y, int packedLight, int packedOverlay) {
        var custom = word == null ? null : ApiWordRegistry.get(word);
        ResourceLocation source = custom == null ? COMPONENTS : custom.imageSource();
        int[] components = componentsFor(word);
        float size = 64F / 160F;
        for (int i = 0; i < components.length; i++) {
            int component = components[i];
            int rgb = 0;
            if (custom != null && !custom.colors().isEmpty()) {
                rgb = custom.colors().get(Math.min(i, custom.colors().size() - 1));
            }
            float u0 = (component % 8) / 8F;
            float v0 = (component / 8) / 8F;
            float u1 = u0 + 1F / 8F;
            float v1 = v0 + 1F / 8F;
            quad(poseStack, buffers, source, x, y, x + size, y + size, 0.503F,
                    u0, v0, u1, v1, 0xFF000000 | (rgb & 0xFFFFFF), packedLight, packedOverlay);
        }
    }

    private static int[] componentsFor(String word) {
        if (word == null) return new int[]{0};
        var custom = ApiWordRegistry.get(word);
        if (custom != null && !custom.components().isEmpty()) {
            return custom.components().stream().mapToInt(Integer::intValue).toArray();
        }
        int[] known = LegacySymbolRenderer.componentsForItem(word);
        if (known != null) return known;
        Random random = new Random(word.toLowerCase(java.util.Locale.ROOT).hashCode());
        int maxComponent = 20;
        int count = random.nextInt(10) + 3;
        if (word.toLowerCase(java.util.Locale.ROOT).startsWith("easter")) { count = 4; maxComponent = 8; }
        int[] generated = new int[count];
        for (int i = 0; i < count; i++) generated[i] = random.nextInt(maxComponent) + 4;
        return generated;
    }

    private static void quad(PoseStack poseStack, MultiBufferSource buffers, ResourceLocation texture,
                             float x0, float y0, float x1, float y1, float z,
                             float u0, float v0, float u1, float v1, int color,
                             int packedLight, int packedOverlay) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose pose = poseStack.last();
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        // Texture coordinates use top-left origin; item-space y is bottom-up.
        consumer.addVertex(pose, x0, y0, z).setColor(r,g,b,a).setUv(u0,v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(pose,0,0,1);
        consumer.addVertex(pose, x1, y0, z).setColor(r,g,b,a).setUv(u1,v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(pose,0,0,1);
        consumer.addVertex(pose, x1, y1, z).setColor(r,g,b,a).setUv(u1,v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(pose,0,0,1);
        consumer.addVertex(pose, x0, y1, z).setColor(r,g,b,a).setUv(u0,v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(pose,0,0,1);
    }
}
