package com.xcompwiz.mystcraft.network;

import com.xcompwiz.mystcraft.Mystcraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server-bound rename request for the Writing Desk target slot. */
public record WritingDeskTitlePayload(int containerId, String title) implements CustomPacketPayload {
    public static final Type<WritingDeskTitlePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "writing_desk_title"));

    public static final StreamCodec<ByteBuf, WritingDeskTitlePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WritingDeskTitlePayload::containerId,
            ByteBufCodecs.stringUtf8(com.xcompwiz.mystcraft.blockentity.WritingDeskBlockEntity.MAX_TITLE_LENGTH), WritingDeskTitlePayload::title,
            WritingDeskTitlePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
