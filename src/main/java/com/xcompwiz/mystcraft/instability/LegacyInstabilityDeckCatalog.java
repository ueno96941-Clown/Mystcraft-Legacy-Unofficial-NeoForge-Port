package com.xcompwiz.mystcraft.instability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dependency-free transcription of the active Instability decks from Mystcraft 0.13.7.06.
 *
 * <p>The order in {@link #deckBuildOrder()} is intentionally not alphabetical. 0.13.7.06 stored
 * deck definitions in a five-entry {@code HashMap}; with the legacy deck names and Java's stable
 * String hashes, {@code createDecks()} iterated them as death, harsh, eating, basic, destructive.
 * The original controller reused one {@link java.util.Random} across that iteration, so preserving
 * this order is required for seed-compatible first-time shuffles.</p>
 *
 * <p>Inactive/commented legacy providers (burning,g, crumblebedrock, decayblack, erosion) are not
 * present here. Runtime effects are controlled separately by {@link InstabilityPolicy}; this catalog
 * only restores the deterministic card-selection model.</p>
 */
public final class LegacyInstabilityDeckCatalog {
    public static final int SMALLEST_CARD_COST = 500;

    private static final List<String> DECK_BUILD_ORDER = List.of(
            "death", "harsh", "eating", "basic", "destructive");

    private static final Map<String, Integer> DECK_COSTS;
    private static final Map<String, Integer> CARD_COSTS;
    private static final Map<String, List<String>> DECK_CARDS;

    static {
        LinkedHashMap<String, Integer> deckCosts = new LinkedHashMap<>();
        deckCosts.put("basic", 0);
        deckCosts.put("harsh", 2500);
        deckCosts.put("destructive", 10000);
        deckCosts.put("eating", 15000);
        deckCosts.put("death", 20000);
        DECK_COSTS = Collections.unmodifiableMap(deckCosts);

        LinkedHashMap<String, Integer> costs = new LinkedHashMap<>();
        LinkedHashMap<String, List<String>> cards = new LinkedHashMap<>();
        for (String deck : deckCosts.keySet()) cards.put(deck, new ArrayList<>());

        // Registration order is copied from InstabilityData.initialize() in 0.13.7.06.
        register(costs, cards, "blindness", 1000, copies("eating", 1));
        register(costs, cards, "blindness,g", 1500, copies("death", 1));
        register(costs, cards, "enemyregen,g", 1000, copies("basic", 5), copies("harsh", 2));
        register(costs, cards, "enemyresist,g", 1000, copies("basic", 2), copies("harsh", 1));
        register(costs, cards, "fatigue", 500, copies("basic", 5));
        register(costs, cards, "fatigue,g", 1000, copies("harsh", 5));
        register(costs, cards, "hunger", 500, copies("basic", 8), copies("harsh", 2));
        register(costs, cards, "hunger,g", 1000, copies("harsh", 5));
        register(costs, cards, "nausea", 1000, copies("eating", 1));
        register(costs, cards, "nausea,g", 1500, copies("death", 1));
        register(costs, cards, "poison", 500, copies("basic", 9), copies("harsh", 3));
        register(costs, cards, "poison,g", 1000, copies("harsh", 5));
        register(costs, cards, "slow", 500, copies("basic", 6), copies("harsh", 1));
        register(costs, cards, "slow,g", 1000, copies("harsh", 5));
        register(costs, cards, "weakness", 500, copies("basic", 8), copies("harsh", 2));
        register(costs, cards, "weakness,g", 1000, copies("harsh", 5));
        register(costs, cards, "wither", 1000,
                copies("harsh", 1), copies("destructive", 1), copies("eating", 2));
        register(costs, cards, "wither,g", 2000,
                copies("destructive", 1), copies("eating", 1), copies("death", 1));
        register(costs, cards, "burning", 500, copies("harsh", 1));
        register(costs, cards, "crumble", 2000, copies("destructive", 6));
        register(costs, cards, "decayblue", 2000, copies("eating", 2), copies("death", 1));
        register(costs, cards, "decaypurple", 2000, copies("eating", 2), copies("death", 1));
        register(costs, cards, "decayred", 2000, copies("eating", 2), copies("death", 1));
        register(costs, cards, "decaywhite", 5000, copies("eating", 1), copies("death", 3));
        register(costs, cards, "explosions", 1000, copies("destructive", 8));
        register(costs, cards, "lightning", 1000, copies("harsh", 4), copies("destructive", 4));
        register(costs, cards, "meteors", 1000, copies("destructive", 4));

        CARD_COSTS = Collections.unmodifiableMap(costs);
        LinkedHashMap<String, List<String>> frozenCards = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : cards.entrySet()) {
            frozenCards.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        DECK_CARDS = Collections.unmodifiableMap(frozenCards);
    }

    private LegacyInstabilityDeckCatalog() {}

    public static List<String> deckBuildOrder() {
        return DECK_BUILD_ORDER;
    }

    public static int deckCost(String deck) {
        Integer value = DECK_COSTS.get(deck);
        return value == null ? 0 : value;
    }

    public static int cardCost(String providerId) {
        Integer value = CARD_COSTS.get(providerId);
        return value == null ? 0 : value;
    }

    public static List<String> cards(String deck) {
        List<String> value = DECK_CARDS.get(deck);
        return value == null ? List.of() : value;
    }

    public static Map<String, Integer> cardCosts() {
        return CARD_COSTS;
    }

    public static Map<String, Integer> deckCosts() {
        return DECK_COSTS;
    }

    public static boolean isKnownDeck(String deck) {
        return DECK_COSTS.containsKey(deck);
    }

    public static boolean isKnownCard(String card) {
        return CARD_COSTS.containsKey(card);
    }

    private static DeckCopies copies(String deck, int count) {
        return new DeckCopies(deck, count);
    }

    private static void register(
            Map<String, Integer> costs,
            Map<String, List<String>> decks,
            String id,
            int activationCost,
            DeckCopies... placements) {
        if (costs.put(id, activationCost) != null) {
            throw new IllegalStateException("Duplicate legacy Instability provider: " + id);
        }
        for (DeckCopies placement : placements) {
            List<String> deck = decks.get(placement.deck());
            if (deck == null) throw new IllegalStateException("Unknown legacy deck: " + placement.deck());
            for (int i = 0; i < placement.count(); i++) deck.add(id);
        }
    }

    private record DeckCopies(String deck, int count) {
        private DeckCopies {
            if (deck == null || deck.isBlank()) throw new IllegalArgumentException("deck");
            if (count < 0) throw new IllegalArgumentException("count");
        }
    }
}
