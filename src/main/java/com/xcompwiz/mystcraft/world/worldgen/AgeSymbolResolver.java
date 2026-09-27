package com.xcompwiz.mystcraft.world.worldgen;

import com.xcompwiz.mystcraft.grammar.GrammarTree;
import com.xcompwiz.mystcraft.symbol.SymbolAvailability;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Legacy-compatible bridge from the symbols explicitly written into an Age to the
 * completed terminal sequence consumed by world-generation planning.
 *
 * <p>Mystcraft 0.13.7.06 deliberately constructed two separate {@link Random}
 * instances from the Age seed: one for {@code parseTerminals()} shortest-path ties
 * and one for {@code getExpanded()}. Reusing one Random would shift the legacy
 * sequence, so this class preserves that boundary exactly.</p>
 */
public final class AgeSymbolResolver {
    private AgeSymbolResolver() {}

    public static List<String> resolve(long ageSeed, List<String> requestedSymbols) {
        Objects.requireNonNull(requestedSymbols, "requestedSymbols");

        List<String> requested = requestedSymbols.stream()
                .filter(SymbolAvailability::isExecutable)
                .toList();

        GrammarTree tree = new GrammarTree();
        tree.parseTerminals(requested, new Random(ageSeed));
        return tree.getExpanded(new Random(ageSeed));
    }
}
