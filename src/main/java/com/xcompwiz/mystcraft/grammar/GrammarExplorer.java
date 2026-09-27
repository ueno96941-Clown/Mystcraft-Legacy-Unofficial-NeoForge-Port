package com.xcompwiz.mystcraft.grammar;

import java.util.ArrayList;
import java.util.List;
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

    public static GrammarRule getRandomRule(String token, Random random) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(random, "random");

        List<GrammarRule> rules = GrammarRuleRegistry.rulesFor(token);
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
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(random, "random");
        ArrayList<String> result = new ArrayList<>();
        exploreInto(token, random, result);
        return List.copyOf(result);
    }

    private static void exploreInto(String token, Random random, List<String> output) {
        GrammarRule rule = getRandomRule(token, random);
        if (rule == null) {
            output.add(token);
            return;
        }
        if (rule.values().isEmpty()) return;
        for (String child : rule.values()) {
            exploreInto(child, random, output);
        }
    }
}
