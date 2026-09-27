package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.linking.LinkProperties;
import net.minecraft.network.chat.Component;

/** User-visible localized names for legacy link-property identifiers. */
public final class LinkPropertyName {
    private LinkPropertyName() {}
    public static Component component(String property) {
        return switch (property) {
            case LinkProperties.INTRA_LINKING -> Component.translatable("linkeffect.intralinking.name");
            case LinkProperties.INTRA_LINKING_ONLY -> Component.translatable("linkeffect.intralinkingonly.name");
            case LinkProperties.GENERATE_PLATFORM -> Component.translatable("linkeffect.generateplatform.name");
            case LinkProperties.MAINTAIN_MOMENTUM -> Component.translatable("linkeffect.maintainmomentum.name");
            case LinkProperties.DISARM -> Component.translatable("linkeffect.disarm.name");
            case LinkProperties.RELATIVE -> Component.translatable("linkeffect.relative.name");
            default -> Component.literal(property);
        };
    }
}
