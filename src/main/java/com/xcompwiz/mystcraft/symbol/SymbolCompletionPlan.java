package com.xcompwiz.mystcraft.symbol;

import java.util.List;
import java.util.Objects;

/**
 * Deterministic hand-off from ordered symbol analysis to the future GrammarTree port.
 *
 * <p>This object deliberately does not invent terminal symbols. Legacy Mystcraft fills
 * missing required branches by expanding the {@code mystcraft:Age} grammar with a
 * {@code Random(ageSeed)}. Checkpoint 13B records that work without prematurely
 * implementing weighted grammar selection.</p>
 */
public record SymbolCompletionPlan(
        long ageSeed,
        String rootGrammarToken,
        List<RequiredSymbolSlot> missingRequiredSlots,
        State state) {

    public enum State {
        COMPLETE,
        GRAMMAR_EXPANSION_REQUIRED
    }

    public SymbolCompletionPlan {
        rootGrammarToken = Objects.requireNonNull(rootGrammarToken, "rootGrammarToken");
        missingRequiredSlots = List.copyOf(Objects.requireNonNull(missingRequiredSlots, "missingRequiredSlots"));
        state = Objects.requireNonNull(state, "state");
        if (missingRequiredSlots.isEmpty() != (state == State.COMPLETE)) {
            throw new IllegalArgumentException("Completion state does not match missing required slots");
        }
    }

    public boolean requiresGrammarExpansion() {
        return state == State.GRAMMAR_EXPANSION_REQUIRED;
    }

    public List<String> missingGrammarTokens() {
        return missingRequiredSlots.stream().map(RequiredSymbolSlot::grammarToken).toList();
    }
}
