package com.xcompwiz.mystcraft.grammar;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Legacy tie-breaking helper for equal-length shortest grammar paths. */
public final class GrammarPathSelector {
    private GrammarPathSelector() {}

    public static List<GrammarRule> chooseShortestPath(String subtreeToken, String nodeToken, Random random) {
        Objects.requireNonNull(random, "random");
        List<List<GrammarRule>> paths = GrammarShortestPathIndex.shortestPaths(subtreeToken, nodeToken);
        if (paths.isEmpty()) return null;

        // Legacy GrammarTree#getShortestPath calls WeightedItemSelector on plain Lists.
        // Lists do not carry a weight, so the selector falls back to equal probability.
        float selection = random.nextFloat() * paths.size();
        List<GrammarRule> last = null;
        for (List<GrammarRule> path : paths) {
            selection -= 1.0F;
            if (selection <= 0.0F) return path;
            last = path;
        }
        return last;
    }
}
