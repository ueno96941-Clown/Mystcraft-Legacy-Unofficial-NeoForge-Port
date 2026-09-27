package com.xcompwiz.mystcraft.world.dimension;

import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Per-FloatingIslands-carver accumulator. Each configured carver owns one instance, matching
 * legacy `BiomeReplacer` isolation when multiple FloatingIslands Symbols exist.
 */
public final class AgeFloatingIslandColumnMasks {
    public static final int MODERN_CARVER_CALLBACKS_PER_TARGET = 17 * 17;

    private final Map<Long, MutableEntry> masks = new HashMap<>();

    public synchronized Optional<Entry> mergeAndMaybeComplete(
            ChunkPos target,
            boolean[] incoming,
            String biomeLegacySymbol) {

        if (incoming.length != 256) {
            throw new IllegalArgumentException("floating island mask must be 256 columns");
        }

        MutableEntry entry = masks.computeIfAbsent(
                target.toLong(),
                ignored -> new MutableEntry(new boolean[256], biomeLegacySymbol));

        for (int i = 0; i < 256; ++i) {
            entry.columns[i] |= incoming[i];
        }
        entry.callbacks++;

        if (entry.callbacks >= MODERN_CARVER_CALLBACKS_PER_TARGET) {
            masks.remove(target.toLong());
            return Optional.of(new Entry(entry.columns, entry.biomeLegacySymbol));
        }
        return Optional.empty();
    }

    private static final class MutableEntry {
        private final boolean[] columns;
        private final String biomeLegacySymbol;
        private int callbacks;

        private MutableEntry(boolean[] columns, String biomeLegacySymbol) {
            this.columns = columns;
            this.biomeLegacySymbol = biomeLegacySymbol;
        }
    }

    public record Entry(boolean[] columns, String biomeLegacySymbol) {
        public Entry {
            columns = columns.clone();
        }

        @Override
        public boolean[] columns() {
            return columns.clone();
        }
    }
}
