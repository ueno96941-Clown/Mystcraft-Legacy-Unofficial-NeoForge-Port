package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Small bounded memoization layer for point-sampled legacy GenLayer emulation.
 *
 * <p>The first 1.21 port represented each old GenLayer as a recursive single-cell
 * function. That is semantically convenient, but Huge/Large chains repeatedly ask
 * for the same parent cells and can multiply a single noise-biome query into
 * thousands of identical recursive calls. Vanilla 1.12 GenLayer operated on
 * rectangular int arrays and therefore reused those parent cells naturally.</p>
 *
 * <p>This direct-mapped cache restores that reuse without changing any legacy RNG
 * or interpolation rule. Collisions merely cause recomputation; they can never
 * change the returned biome index. The fixed size also prevents prepared/unloaded
 * Ages from retaining an ever-growing coordinate map.</p>
 */
public final class LegacyCachedBiomeLayer implements LegacyBiomeLayer {
    private static final int CACHE_SIZE = 8192;
    private static final int CACHE_MASK = CACHE_SIZE - 1;

    private final LegacyBiomeLayer delegate;
    private final AtomicReferenceArray<Entry> entries = new AtomicReferenceArray<>(CACHE_SIZE);

    public LegacyCachedBiomeLayer(LegacyBiomeLayer delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public int sample(int x, int z) {
        long key = (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
        int index = mix(key) & CACHE_MASK;
        Entry cached = entries.get(index);
        if (cached != null && cached.key() == key) {
            return cached.value();
        }

        int value = delegate.sample(x, z);
        entries.set(index, new Entry(key, value));
        return value;
    }

    private static int mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return (int) value;
    }

    private record Entry(long key, int value) {}
}
