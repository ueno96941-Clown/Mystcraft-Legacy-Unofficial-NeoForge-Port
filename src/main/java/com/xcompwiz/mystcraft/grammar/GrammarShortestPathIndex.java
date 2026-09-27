package com.xcompwiz.mystcraft.grammar;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reverse shortest-path index used by the legacy GrammarTree parser.
 *
 * <p>The direction is intentionally the same as Mystcraft Legacy 0.13.7.06:
 * paths begin at a rule which produces {@code subtreeToken}, then walk upward
 * through rules which produce each successive parent until {@code nodeToken}
 * is reached. Applying the returned rules in list order with reverseExpand()
 * therefore grows a supplied symbol subtree upward toward an existing grammar
 * node.</p>
 */
public final class GrammarShortestPathIndex {
    private static final Map<String, Map<String, List<List<GrammarRule>>>> CACHE = new LinkedHashMap<>();

    private GrammarShortestPathIndex() {}

    public static synchronized List<List<GrammarRule>> shortestPaths(String subtreeToken, String nodeToken) {
        Objects.requireNonNull(subtreeToken, "subtreeToken");
        Objects.requireNonNull(nodeToken, "nodeToken");
        GrammarRuleRegistry.bootstrap();

        Map<String, List<List<GrammarRule>>> allPaths = CACHE.computeIfAbsent(
                subtreeToken,
                GrammarShortestPathIndex::calculatePathsFrom
        );
        List<List<GrammarRule>> paths = allPaths.get(nodeToken);
        return paths == null ? List.of() : paths;
    }

    /** Clears paths after the runtime symbol set changes. */
    public static synchronized void clearCache() {
        CACHE.clear();
    }

    public static synchronized void clearCacheForTests() {
        clearCache();
    }

    private static Map<String, List<List<GrammarRule>>> calculatePathsFrom(String token) {
        LinkedHashMap<String, List<List<GrammarRule>>> allPaths = new LinkedHashMap<>();
        ArrayDeque<VisitPair> toVisit = new ArrayDeque<>();

        for (GrammarRule producer : GrammarRuleRegistry.parentRulesFor(token)) {
            toVisit.addLast(new VisitPair(producer.parent(), List.of(producer)));
        }

        while (!toVisit.isEmpty()) {
            VisitPair element = toVisit.removeFirst();
            String target = element.target();
            if (target.equals(token)) continue;

            List<GrammarRule> path = element.path();
            List<List<GrammarRule>> pathsToTarget = allPaths.computeIfAbsent(target, ignored -> new ArrayList<>());

            if (!pathsToTarget.isEmpty() && pathsToTarget.get(0).size() > path.size()) {
                pathsToTarget.clear();
            }

            if (pathsToTarget.isEmpty() || pathsToTarget.get(0).size() == path.size()) {
                pathsToTarget.add(path);
                for (GrammarRule producer : GrammarRuleRegistry.parentRulesFor(target)) {
                    ArrayList<GrammarRule> extended = new ArrayList<>(path.size() + 1);
                    extended.addAll(path);
                    extended.add(producer);
                    toVisit.addLast(new VisitPair(producer.parent(), List.copyOf(extended)));
                }
            }
        }

        LinkedHashMap<String, List<List<GrammarRule>>> frozen = new LinkedHashMap<>();
        for (Map.Entry<String, List<List<GrammarRule>>> entry : allPaths.entrySet()) {
            frozen.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(frozen);
    }

    private record VisitPair(String target, List<GrammarRule> path) {}
}
