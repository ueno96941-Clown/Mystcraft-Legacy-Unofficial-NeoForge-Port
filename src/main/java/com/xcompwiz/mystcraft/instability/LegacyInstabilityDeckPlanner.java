package com.xcompwiz.mystcraft.instability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Rebuilds the persisted 0.13.7.06 deck order without activating any gameplay effect.
 *
 * <p>The migration behavior mirrors the legacy controller: saved cards that still exist are kept
 * in their saved order; stale cards are dropped; newly introduced/missing cards are shuffled with
 * the Age-seeded shared Random and appended to the bottom. Unknown deck names are retained in
 * storage for forward/backward compatibility but are ignored by selection.</p>
 */
public final class LegacyInstabilityDeckPlanner {
    private LegacyInstabilityDeckPlanner() {}

    public static ReconcileResult reconcile(long ageSeed, Map<String, ? extends List<String>> persisted) {
        LinkedHashMap<String, List<String>> result = deepCopy(persisted);
        LinkedHashSet<String> dirty = new LinkedHashSet<>();
        Random random = new Random(ageSeed);

        for (String deckName : LegacyInstabilityDeckCatalog.deckBuildOrder()) {
            LegacyCardMultiset remaining = new LegacyCardMultiset(LegacyInstabilityDeckCatalog.cards(deckName));
            ArrayList<String> rebuilt = new ArrayList<>();
            List<String> saved = persisted == null ? null : persisted.get(deckName);
            if (saved == null) {
                saved = List.of();
                dirty.add(deckName);
            }

            for (String card : saved) {
                if (card != null && remaining.removeOne(card)) {
                    rebuilt.add(card);
                } else {
                    dirty.add(deckName);
                }
            }

            List<String> missing = remaining.expandedEntries();
            if (!missing.isEmpty()) {
                Collections.shuffle(missing, random);
                rebuilt.addAll(missing);
                dirty.add(deckName);
            }

            List<String> frozen = List.copyOf(rebuilt);
            if (!frozen.equals(saved)) dirty.add(deckName);
            result.put(deckName, frozen);
        }

        return new ReconcileResult(freeze(result), Set.copyOf(dirty));
    }

    private static LinkedHashMap<String, List<String>> deepCopy(Map<String, ? extends List<String>> source) {
        LinkedHashMap<String, List<String>> copy = new LinkedHashMap<>();
        if (source == null) return copy;
        for (Map.Entry<String, ? extends List<String>> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) continue;
            ArrayList<String> cards = new ArrayList<>();
            if (entry.getValue() != null) {
                for (String card : entry.getValue()) if (card != null && !card.isBlank()) cards.add(card);
            }
            copy.put(entry.getKey(), List.copyOf(cards));
        }
        return copy;
    }

    private static Map<String, List<String>> freeze(Map<String, List<String>> source) {
        LinkedHashMap<String, List<String>> frozen = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            frozen.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(frozen);
    }

    /** Immutable result. dirtyDecks is empty only when no persisted order changed. */
    public record ReconcileResult(Map<String, List<String>> deckOrders, Set<String> dirtyDecks) {
        public boolean changed() { return !dirtyDecks.isEmpty(); }
    }

    /**
     * Local model of Guava 21 {@code HashMultiset}. In 21.0 it is backed by a Java-8
     * {@code HashMap<E, Count>}; {@code HashMultiset.create(Iterable)} uses an expected distinct
     * count of 11 for an ordinary Collection, producing a 16-bucket HashMap table initially.
     * Iteration follows table-bucket order and insertion order inside each collision chain.
     *
     * <p>HashMap does not shrink when saved cards are removed. We therefore capture the table
     * capacity reached by the complete legacy Deck and order the remaining distinct providers by
     * their final Java-8 bucket and original insertion position. This matters only during migration
     * when 0.13.7.06 consumes saved cards before shuffling newly missing cards.</p>
     */
    private static final class LegacyCardMultiset {
        private static final int INITIAL_TABLE_CAPACITY = 16; // inferDistinctElements(non-Multiset) => 11.
        private final ArrayList<Entry> entries = new ArrayList<>();
        private final int tableCapacity;

        LegacyCardMultiset(List<String> cards) {
            for (String card : cards) add(card);
            int capacity = INITIAL_TABLE_CAPACITY;
            while (entries.size() > (int) (capacity * 0.75F)) capacity <<= 1;
            this.tableCapacity = capacity;
        }

        private void add(String id) {
            for (Entry entry : entries) {
                if (entry.id.equals(id)) {
                    entry.count++;
                    return;
                }
            }
            entries.add(new Entry(id, 1, entries.size()));
        }

        boolean removeOne(String id) {
            for (int i = 0; i < entries.size(); i++) {
                Entry entry = entries.get(i);
                if (!entry.id.equals(id)) continue;
                entry.count--;
                if (entry.count == 0) entries.remove(i);
                return true;
            }
            return false;
        }

        List<String> expandedEntries() {
            ArrayList<Entry> ordered = new ArrayList<>(entries);
            final int mask = tableCapacity - 1;
            ordered.sort((left, right) -> {
                int lb = spread(left.id.hashCode()) & mask;
                int rb = spread(right.id.hashCode()) & mask;
                if (lb != rb) return Integer.compare(lb, rb);
                return Integer.compare(left.insertionIndex, right.insertionIndex);
            });

            ArrayList<String> out = new ArrayList<>();
            for (Entry entry : ordered) {
                for (int i = 0; i < entry.count; i++) out.add(entry.id);
            }
            return out;
        }

        private static int spread(int hash) {
            return hash ^ (hash >>> 16);
        }

        private static final class Entry {
            final String id;
            final int insertionIndex;
            int count;
            Entry(String id, int count, int insertionIndex) {
                this.id = id;
                this.count = count;
                this.insertionIndex = insertionIndex;
            }
        }
    }
}
