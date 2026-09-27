package com.xcompwiz.mystcraft.network;

import com.xcompwiz.mystcraft.Mystcraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LinkBookActivatePayload(int containerId) implements CustomPacketPayload {
    public static final Type<LinkBookActivatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "activate_link_book"));

    public static final StreamCodec<ByteBuf, LinkBookActivatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LinkBookActivatePayload::containerId,
            LinkBookActivatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
