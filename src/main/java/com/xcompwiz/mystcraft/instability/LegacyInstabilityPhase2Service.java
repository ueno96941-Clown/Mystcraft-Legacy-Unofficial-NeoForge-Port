package com.xcompwiz.mystcraft.instability;

import com.xcompwiz.mystcraft.world.agedata.AgeRecord;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Side-effect-free Phase-2 controller facade, except for durable Deck-order reconciliation on the
 * supplied AgeRecord. Provider gameplay effects are deliberately not constructed or scheduled.
 */
public final class LegacyInstabilityPhase2Service {
    private LegacyInstabilityPhase2Service() {}

    /** Ensure the Age carries a complete, current deck order. Safe to call repeatedly. */
    public static boolean ensureDeckPersistence(AgeRecord age) {
        var reconciled = LegacyInstabilityDeckPlanner.reconcile(age.seed(), age.instabilityDeckOrders());
        if (!reconciled.changed()) return false;
        age.setInstabilityDeckOrders(reconciled.deckOrders());
        return true;
    }

    /** Reconcile decks and calculate which providers would be active at the supplied recorded score. */
    public static Snapshot prepare(AgeRecord age, int difficultyAdjustedRecordedScore) {
        var reconciled = LegacyInstabilityDeckPlanner.reconcile(age.seed(), age.instabilityDeckOrders());
        if (reconciled.changed()) age.setInstabilityDeckOrders(reconciled.deckOrders());
        var selection = LegacyInstabilitySelection.select(
                difficultyAdjustedRecordedScore,
                age.storedInstabilityEnabled(),
                reconciled.deckOrders());
        return new Snapshot(reconciled.changed(), currentDecksOnly(reconciled.deckOrders()), selection);
    }

    private static Map<String, List<String>> currentDecksOnly(Map<String, List<String>> all) {
        LinkedHashMap<String, List<String>> out = new LinkedHashMap<>();
        for (String deck : LegacyInstabilityDeckCatalog.deckBuildOrder()) {
            out.put(deck, all.getOrDefault(deck, List.of()));
        }
        return java.util.Collections.unmodifiableMap(out);
    }

    public record Snapshot(
            boolean deckOrdersChanged,
            Map<String, List<String>> deckOrders,
            LegacyInstabilitySelection.SelectionResult selection) {}
}
