package com.xcompwiz.mystcraft.network;

import com.xcompwiz.mystcraft.blockentity.BookBinderBlockEntity;
import com.xcompwiz.mystcraft.inventory.BookBinderMenu;
import com.xcompwiz.mystcraft.inventory.LinkBookMenu;
import com.xcompwiz.mystcraft.inventory.LinkModifierMenu;
import com.xcompwiz.mystcraft.inventory.WritingDeskMenu;
import com.xcompwiz.mystcraft.blockentity.WritingDeskBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MystNetwork {
    private MystNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(BinderTitlePayload.TYPE, BinderTitlePayload.STREAM_CODEC, MystNetwork::handleBinderTitle);
        registrar.playToServer(BinderPageActionPayload.TYPE, BinderPageActionPayload.STREAM_CODEC, MystNetwork::handleBinderPageAction);
        registrar.playToClient(BinderPagesPayload.TYPE, BinderPagesPayload.STREAM_CODEC, BinderPagesPayloadHandler::handle);
        registrar.playToServer(LinkBookActivatePayload.TYPE, LinkBookActivatePayload.STREAM_CODEC, MystNetwork::handleBookActivate);
        registrar.playToServer(WritingDeskTitlePayload.TYPE, WritingDeskTitlePayload.STREAM_CODEC, MystNetwork::handleWritingDeskTitle);
        registrar.playToServer(LinkModifierTextPayload.TYPE, LinkModifierTextPayload.STREAM_CODEC, MystNetwork::handleLinkModifierText);
        registrar.playToClient(AgeVisualPayload.TYPE, AgeVisualPayload.STREAM_CODEC, AgeVisualPayloadHandler::handle);
    }


    private static void handleBinderTitle(BinderTitlePayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof BookBinderMenu menu
                && menu.containerId == payload.containerId()
                && menu.stillValid(player)) {
            String title = payload.title();
            if (title.length() > BookBinderBlockEntity.MAX_TITLE_LENGTH) {
                title = title.substring(0, BookBinderBlockEntity.MAX_TITLE_LENGTH);
            }
            menu.setPendingTitleFromNetwork(title);
        }
    }

    private static void handleBinderPageAction(BinderPageActionPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player instanceof ServerPlayer serverPlayer
                && player.containerMenu instanceof BookBinderMenu menu
                && menu.containerId == payload.containerId()
                && menu.stillValid(serverPlayer)) {
            menu.handlePageAction(serverPlayer, payload.action(), payload.index(), payload.single());
        }
    }

    private static void handleLinkModifierText(LinkModifierTextPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof LinkModifierMenu menu
                && menu.containerId == payload.containerId()
                && menu.stillValid(player)) {
            String value = payload.value() == null ? "" : payload.value();
            if (value.length() > 21) value = value.substring(0, 21);
            if (payload.field() == LinkModifierTextPayload.FIELD_TITLE) menu.setTitleFromNetwork(value);
            else if (payload.field() == LinkModifierTextPayload.FIELD_SEED) menu.setSeedFromNetwork(value);
        }
    }

    private static void handleWritingDeskTitle(WritingDeskTitlePayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof WritingDeskMenu menu
                && menu.containerId == payload.containerId()
                && menu.stillValid(player)) {
            String title = payload.title() == null ? "" : payload.title();
            if (title.length() > WritingDeskBlockEntity.MAX_TITLE_LENGTH) {
                title = title.substring(0, WritingDeskBlockEntity.MAX_TITLE_LENGTH);
            }
            menu.setTargetTitleFromNetwork(title);
        }
    }

    private static void handleBookActivate(LinkBookActivatePayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player instanceof ServerPlayer serverPlayer
                && player.containerMenu instanceof LinkBookMenu menu
                && menu.containerId == payload.containerId()
                && menu.stillValid(serverPlayer)) {
            menu.activate(serverPlayer);
        }
    }
}
