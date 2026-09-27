package com.xcompwiz.mystcraft.instability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure 0.13.7.06-style Deck/Provider selection. No provider effect is instantiated or run. */
public final class LegacyInstabilitySelection {
    private LegacyInstabilitySelection() {}

    /** Original controller rounds toward zero to the smallest registered activation cost. */
    public static int roundControllerScore(int difficultyAdjustedScore) {
        int unit = LegacyInstabilityDeckCatalog.SMALLEST_CARD_COST;
        return difficultyAdjustedScore - difficultyAdjustedScore % unit;
    }

    public static SelectionResult select(
            int difficultyAdjustedScore,
            boolean ageInstabilityEnabled,
            Map<String, ? extends List<String>> deckOrders) {
        int controllerScore = roundControllerScore(difficultyAdjustedScore);
        LinkedHashMap<String, Integer> levels = new LinkedHashMap<>();
        ArrayList<DeckSelection> traces = new ArrayList<>();

        for (String deckName : LegacyInstabilityDeckCatalog.deckBuildOrder()) {
            int deckCost = LegacyInstabilityDeckCatalog.deckCost(deckName);
            int remaining = controllerScore - deckCost;
            List<String> order = deckOrders == null ? null : deckOrders.get(deckName);
            if (order == null) order = List.of();

            if (!ageInstabilityEnabled) {
                traces.add(new DeckSelection(deckName, deckCost, controllerScore, remaining,
                        List.of(), null, 0, remaining, StopReason.AGE_DISABLED));
                continue;
            }
            if (remaining < 0) {
                traces.add(new DeckSelection(deckName, deckCost, controllerScore, remaining,
                        List.of(), null, 0, remaining, StopReason.DECK_COST_UNAFFORDABLE));
                continue;
            }

            ArrayList<String> selected = new ArrayList<>();
            String stopCard = null;
            int stopCost = 0;
            StopReason reason = StopReason.DECK_EXHAUSTED;
            for (String card : order) {
                int cardCost = LegacyInstabilityDeckCatalog.cardCost(card);
                remaining -= cardCost;
                if (remaining < 0) {
                    stopCard = card;
                    stopCost = cardCost;
                    reason = StopReason.CARD_UNAFFORDABLE;
                    break;
                }
                selected.add(card);
                levels.merge(card, 1, Integer::sum);
            }
            traces.add(new DeckSelection(deckName, deckCost, controllerScore, controllerScore - deckCost,
                    List.copyOf(selected), stopCard, stopCost, remaining, reason));
        }

        return new SelectionResult(difficultyAdjustedScore, controllerScore, ageInstabilityEnabled,
                Collections.unmodifiableMap(levels), List.copyOf(traces));
    }

    public enum StopReason {
        AGE_DISABLED,
        DECK_COST_UNAFFORDABLE,
        CARD_UNAFFORDABLE,
        DECK_EXHAUSTED
    }

    public record DeckSelection(
            String deckName,
            int deckCost,
            int controllerScore,
            int startingRemaining,
            List<String> selectedCards,
            String stopCard,
            int stopCardCost,
            int endingRemaining,
            StopReason stopReason) {
        public String compactTrace() {
            return deckName + "{cost=" + deckCost
                    + ",start=" + startingRemaining
                    + ",selected=" + selectedCards
                    + ",stop=" + (stopCard == null ? stopReason : stopCard + "/" + stopCardCost)
                    + ",end=" + endingRemaining + "}";
        }
    }

    public record SelectionResult(
            int difficultyAdjustedScore,
            int controllerScore,
            boolean ageInstabilityEnabled,
            Map<String, Integer> providerLevels,
            List<DeckSelection> decks) {
        public List<String> compactDeckTrace() {
            ArrayList<String> out = new ArrayList<>(decks.size());
            for (DeckSelection deck : decks) out.add(deck.compactTrace());
            return List.copyOf(out);
        }
    }
}
