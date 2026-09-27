package com.xcompwiz.mystcraft.network;

import com.xcompwiz.mystcraft.Mystcraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Server -> client mirror of the variable-length legacy Book Binder page list. */
public record BinderPagesPayload(int containerId, List<String> pageDescriptors)
        implements CustomPacketPayload {
    public static final Type<BinderPagesPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "binder_pages"));

    private static final StreamCodec<ByteBuf, List<String>> LIST_CODEC = new StreamCodec<>() {
        @Override
        public List<String> decode(ByteBuf buffer) {
            int size = ByteBufCodecs.VAR_INT.decode(buffer);
            if (size < 0 || size > com.xcompwiz.mystcraft.inventory.BookBinderMenu.MAX_SYNCED_PAGE_DESCRIPTORS) {
                throw new IllegalArgumentException("Invalid Mystcraft Binder page descriptor count: " + size);
            }
            List<String> out = new ArrayList<>(size);
            var stringCodec = ByteBufCodecs.stringUtf8(com.xcompwiz.mystcraft.inventory.BookBinderMenu.MAX_PAGE_DESCRIPTOR_LENGTH);
            for (int i = 0; i < size; i++) out.add(stringCodec.decode(buffer));
            return List.copyOf(out);
        }

        @Override
        public void encode(ByteBuf buffer, List<String> value) {
            int size = Math.min(value.size(), com.xcompwiz.mystcraft.inventory.BookBinderMenu.MAX_SYNCED_PAGE_DESCRIPTORS);
            ByteBufCodecs.VAR_INT.encode(buffer, size);
            var stringCodec = ByteBufCodecs.stringUtf8(com.xcompwiz.mystcraft.inventory.BookBinderMenu.MAX_PAGE_DESCRIPTOR_LENGTH);
            for (int i = 0; i < size; i++) {
                String entry = value.get(i);
                stringCodec.encode(buffer, entry == null ? "$BLANK$" : entry);
            }
        }
    };

    public static final StreamCodec<ByteBuf, BinderPagesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BinderPagesPayload::containerId,
            LIST_CODEC, BinderPagesPayload::pageDescriptors,
            BinderPagesPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
