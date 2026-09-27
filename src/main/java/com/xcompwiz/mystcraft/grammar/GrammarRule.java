package com.xcompwiz.mystcraft.grammar;

import java.util.List;
import java.util.Objects;

public record GrammarRule(String parent, List<String> values, Integer rank) {
    public GrammarRule {
        parent = Objects.requireNonNull(parent, "parent");
        values = List.copyOf(Objects.requireNonNull(values, "values"));
    }
    public boolean generatedRule() { return rank != null; }
}
