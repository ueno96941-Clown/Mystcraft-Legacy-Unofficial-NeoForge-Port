package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.blockentity.StarFissureBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;

/**
 * Modern equivalent of the 0.13.7.06 eight-pass Star Fissure TESR.
 *
 * <p>The legacy effect used vanilla End sky/portal textures.  The modern
 * TheEndPortalRenderer preserves that projected depth effect while avoiding
 * removed OpenGL texgen APIs.  The offsets collapse the effect to the original
 * 0.1-block-high fissure surface.</p>
 */
public final class StarFissureRenderer extends TheEndPortalRenderer<StarFissureBlockEntity> {
    public StarFissureRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected float getOffsetDown() {
        return 0.0F;
    }

    @Override
    protected float getOffsetUp() {
        return 0.1F;
    }
}
