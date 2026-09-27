package com.xcompwiz.mystcraft.network;

import com.xcompwiz.mystcraft.Mystcraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BinderTitlePayload(int containerId, String title) implements CustomPacketPayload {
    public static final Type<BinderTitlePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "binder_title"));

    public static final StreamCodec<ByteBuf, BinderTitlePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BinderTitlePayload::containerId,
            ByteBufCodecs.stringUtf8(com.xcompwiz.mystcraft.blockentity.BookBinderBlockEntity.MAX_TITLE_LENGTH), BinderTitlePayload::title,
            BinderTitlePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
