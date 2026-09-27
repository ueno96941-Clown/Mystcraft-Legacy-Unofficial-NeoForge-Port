package com.xcompwiz.mystcraft.network;

import com.xcompwiz.mystcraft.Mystcraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client Age visual contract.
 *
 * <p>The payload intentionally carries a compact textual snapshot rather than legacy Symbol IDs.
 * Persisted Symbol strings therefore remain byte-for-byte legacy-compatible while the wire format
 * is independent of modern ResourceLocation lowercase restrictions.</p>
 */
public record AgeVisualPayload(String dimensionId, long ageSeed, String snapshot)
        implements CustomPacketPayload {
    public static final Type<AgeVisualPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "age_visual"));

    public static final StreamCodec<ByteBuf, AgeVisualPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AgeVisualPayload::dimensionId,
            ByteBufCodecs.VAR_LONG, AgeVisualPayload::ageSeed,
            ByteBufCodecs.STRING_UTF8, AgeVisualPayload::snapshot,
            AgeVisualPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
