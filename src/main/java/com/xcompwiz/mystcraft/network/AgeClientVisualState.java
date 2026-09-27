package com.xcompwiz.mystcraft.network;

import com.xcompwiz.mystcraft.world.worldgen.AgeVisualSnapshot;
import com.xcompwiz.mystcraft.world.worldgen.AgeVisualSnapshotCodec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-consumed cache of server-authored Age visual snapshots.
 *
 * <p>This class intentionally contains no net.minecraft.client references, so the payload
 * registration path remains safe to load on a dedicated server. Client render hooks are the
 * only consumers of the cache.</p>
 */
public final class AgeClientVisualState {
    private static final Map<String, Entry> BY_DIMENSION = new ConcurrentHashMap<>();

    private AgeClientVisualState() {}

    public record Entry(long ageSeed, String snapshotText, AgeVisualSnapshot snapshot) {}

    public static void accept(AgeVisualPayload payload) {
        BY_DIMENSION.put(payload.dimensionId(), new Entry(
                payload.ageSeed(), payload.snapshot(), AgeVisualSnapshotCodec.decode(payload.snapshot())));
    }

    public static Entry get(String dimensionId) {
        return BY_DIMENSION.get(dimensionId);
    }

    public static void clear() {
        BY_DIMENSION.clear();
    }
}
