package com.xcompwiz.mystcraft.network;

import com.xcompwiz.mystcraft.Mystcraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server action for the legacy Book Binder page slider. */
public record BinderPageActionPayload(int containerId, int action, int index, boolean single)
        implements CustomPacketPayload {
    public static final int INSERT = 0;
    public static final int REMOVE = 1;

    public static final Type<BinderPageActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "binder_page_action"));

    public static final StreamCodec<ByteBuf, BinderPageActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BinderPageActionPayload::containerId,
            ByteBufCodecs.VAR_INT, BinderPageActionPayload::action,
            ByteBufCodecs.VAR_INT, BinderPageActionPayload::index,
            ByteBufCodecs.BOOL, BinderPageActionPayload::single,
            BinderPageActionPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
