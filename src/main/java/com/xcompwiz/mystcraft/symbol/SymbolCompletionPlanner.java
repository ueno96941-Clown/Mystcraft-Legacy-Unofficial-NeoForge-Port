package com.xcompwiz.mystcraft.symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Detects the mandatory branches that the legacy Age root grammar still needs to provide. */
public final class SymbolCompletionPlanner {
    /** Exact legacy GrammarRules.ROOT identity. */
    public static final String AGE_ROOT_TOKEN = "mystcraft:Age";

    private SymbolCompletionPlanner() {}

    public static SymbolCompletionPlan plan(long ageSeed, SymbolAnalysis analysis) {
        Objects.requireNonNull(analysis, "analysis");
        List<RequiredSymbolSlot> missing = new ArrayList<>();
        for (RequiredSymbolSlot slot : RequiredSymbolSlot.values()) {
            if (!analysis.hasCategory(slot.category())) {
                missing.add(slot);
            }
        }
        SymbolCompletionPlan.State state = missing.isEmpty()
                ? SymbolCompletionPlan.State.COMPLETE
                : SymbolCompletionPlan.State.GRAMMAR_EXPANSION_REQUIRED;
        return new SymbolCompletionPlan(ageSeed, AGE_ROOT_TOKEN, missing, state);
    }
}
