package com.xcompwiz.mystcraft.network;

import com.xcompwiz.mystcraft.Mystcraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server-bound legacy Link Modifier text edit (book title or Age seed). */
public record LinkModifierTextPayload(int containerId, int field, String value) implements CustomPacketPayload {
    public static final int FIELD_TITLE = 0;
    public static final int FIELD_SEED = 1;
    public static final Type<LinkModifierTextPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "link_modifier_text"));
    public static final StreamCodec<ByteBuf, LinkModifierTextPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LinkModifierTextPayload::containerId,
            ByteBufCodecs.VAR_INT, LinkModifierTextPayload::field,
            ByteBufCodecs.stringUtf8(21), LinkModifierTextPayload::value,
            LinkModifierTextPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
