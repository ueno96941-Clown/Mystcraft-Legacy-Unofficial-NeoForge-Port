package com.xcompwiz.mystcraft.network;

import com.xcompwiz.mystcraft.inventory.BookBinderMenu;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Common-classpath-safe clientbound Book Binder page-list receiver. */
public final class BinderPagesPayloadHandler {
    private BinderPagesPayloadHandler() {}

    public static void handle(BinderPagesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.containerMenu instanceof BookBinderMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.setClientPageDescriptors(payload.pageDescriptors());
            }
        });
    }
}
