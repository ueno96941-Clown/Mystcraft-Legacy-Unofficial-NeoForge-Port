package com.xcompwiz.mystcraft.grammar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Faithful 1.21.1-side port of Mystcraft Legacy GrammarGenerator#getRandomRule/explore.
 *
 * <p>This class intentionally consumes {@link Random} in the same way as the 1.12.2
 * WeightedItemSelector path: one nextFloat() per rule choice, weighted by the rank
 * weights calculated by {@link GrammarRuleRegistry}. If every candidate has zero
 * weight, selection falls back to an even distribution.</p>
 */
public final class GrammarExplorer {
    private GrammarExplorer() {}

    /**
     * CP346: random-completion-only recursion caps.
     *
     * <p>These limits do not remove or rewrite explicitly-authored Pages. They only stop the
     * grammar filler from recursively manufacturing oversized plural/modifier chains while
     * completing missing parts of an Age.</p>
     *
     * <p>The value is the maximum number of semantic children produced by the corresponding
     * left-recursive "*Adv" grammar. A value of 1 disables the recursive branch entirely;
     * a value of 2 permits one recursive step and therefore at most two children.</p>
     */
    private static final Map<String, Integer> RANDOM_COMPLETION_RECURSION_LIMITS = Map.ofEntries(
            Map.entry("mystcraft:BiomesAdv", 2),
            Map.entry("mystcraft:SunsAdv", 2),
            Map.entry("mystcraft:MoonsAdv", 2),
            Map.entry("mystcraft:StarfieldsAdv", 2),
            Map.entry("mystcraft:FeatureSmallAdv", 2),
            Map.entry("mystcraft:FeatureMediumAdv", 2),
            Map.entry("mystcraft:FeatureLargeAdv", 2),
            Map.entry("mystcraft:EffectsAdv", 2),

            Map.entry("mystcraft:AngleAdv", 1),
            Map.entry("mystcraft:PeriodAdv", 1),
            Map.entry("mystcraft:PhaseAdv", 1),
            Map.entry("mystcraft:ColorAdv", 1),
            Map.entry("mystcraft:GradientAdv", 1)
    );

    public static GrammarRule getRandomRule(String token, Random random) {
        return getRandomRule(token, random, List.copyOf(GrammarRuleRegistry.rulesFor(token)));
    }

    private static GrammarRule getRandomRule(String token, Random random, List<GrammarRule> rules) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(random, "random");

        if (rules.isEmpty()) return null;

        float totalWeight = 0.0F;
        for (GrammarRule rule : rules) {
            totalWeight += GrammarRuleRegistry.weightOf(rule);
        }

        if (totalWeight <= 0.0F) {
            return getRandomRuleEvenly(rules, random);
        }

        GrammarRule last = null;
        float selection = random.nextFloat() * totalWeight;
        for (GrammarRule rule : rules) {
            float weight = GrammarRuleRegistry.weightOf(rule);
            selection -= weight;
            if (weight > 0.0F) {
                if (selection <= 0.0F) return rule;
                last = rule;
            }
        }
        return last;
    }

    private static GrammarRule getRandomRuleEvenly(List<GrammarRule> rules, Random random) {
        GrammarRule last = null;
        float selection = random.nextFloat() * rules.size();
        for (GrammarRule rule : rules) {
            selection -= 1.0F;
            if (selection <= 0.0F) return rule;
            last = rule;
        }
        return last;
    }

    /**
     * Recursively expands a grammar token to terminal tokens using the supplied Random.
     * A token with no registered expansion rule is terminal and is emitted unchanged.
     * A selected rule with an empty RHS emits nothing, matching legacy behavior.
     */
    public static List<String> explore(String token, Random random) {
        return exploreInternal(token, random, false);
    }

    /**
     * Age-generation-only bounded expansion used while filling missing symbols.
     * Public/API grammar generation deliberately keeps the legacy unbounded semantics.
     */
    public static List<String> exploreRandomCompletion(String token, Random random) {
        return exploreInternal(token, random, true);
    }

    private static List<String> exploreInternal(String token, Random random, boolean boundedRandomCompletion) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(random, "random");
        ArrayList<String> result = new ArrayList<>();
        exploreInto(token, random, result, new HashMap<>(), boundedRandomCompletion);
        return List.copyOf(result);
    }

    private static void exploreInto(String token, Random random, List<String> output,
                                    Map<String, Integer> recursiveDepth,
                                    boolean boundedRandomCompletion) {
        List<GrammarRule> rules = GrammarRuleRegistry.rulesFor(token);
        if (rules.isEmpty()) {
            output.add(token);
            return;
        }

        int semanticLimit = boundedRandomCompletion
                ? RANDOM_COMPLETION_RECURSION_LIMITS.getOrDefault(token, Integer.MAX_VALUE)
                : Integer.MAX_VALUE;
        int depth = recursiveDepth.getOrDefault(token, 0);
        int maxRecursiveDepth = semanticLimit == Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(0, semanticLimit - 1);

        List<GrammarRule> eligible = rules;
        if (depth >= maxRecursiveDepth) {
            ArrayList<GrammarRule> filtered = new ArrayList<>(rules.size());
            for (GrammarRule candidate : rules) {
                if (!candidate.values().contains(token)) filtered.add(candidate);
            }
            if (!filtered.isEmpty()) eligible = filtered;
        }

        GrammarRule rule = getRandomRule(token, random, eligible);
        if (rule == null) {
            output.add(token);
            return;
        }
        if (rule.values().isEmpty()) return;

        boolean recursive = rule.values().contains(token);
        if (recursive) recursiveDepth.put(token, depth + 1);
        try {
            for (String child : rule.values()) {
                exploreInto(child, random, output, recursiveDepth, boundedRandomCompletion);
            }
        } finally {
            if (recursive) {
                if (depth == 0) recursiveDepth.remove(token);
                else recursiveDepth.put(token, depth);
            }
        }
    }
}
