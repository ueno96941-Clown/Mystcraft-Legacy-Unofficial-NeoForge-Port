package com.xcompwiz.mystcraft.network;

import com.xcompwiz.mystcraft.Mystcraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Common-classpath-safe receiver for the clientbound Age visual payload. */
public final class AgeVisualPayloadHandler {
    private AgeVisualPayloadHandler() {}

    public static void handle(AgeVisualPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.snapshot().isEmpty()) {
                AgeClientVisualState.clear();
                return;
            }
            try {
                AgeClientVisualState.accept(payload);
            } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
                AgeClientVisualState.clear();
                Mystcraft.LOGGER.error("Rejected invalid Mystcraft Age visual snapshot for {}",
                        payload.dimensionId(), ex);
            }
        });
    }
}
