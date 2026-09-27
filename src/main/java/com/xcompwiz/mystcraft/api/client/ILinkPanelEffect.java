package com.xcompwiz.mystcraft.api.client;

import com.xcompwiz.mystcraft.api.hook.RenderAPI;
import com.xcompwiz.mystcraft.api.linking.ILinkInfo;
import net.minecraft.world.item.ItemStack;

/**
 * Legacy API-v1 hook for drawing additional layers on a Mystcraft link panel.
 * Register instances through {@link RenderAPI#registerRenderEffect(ILinkPanelEffect)}.
 */
public interface ILinkPanelEffect {
    void render(int left, int top, int width, int height, ILinkInfo linkInfo, ItemStack bookclone);
    void onOpen();
}
