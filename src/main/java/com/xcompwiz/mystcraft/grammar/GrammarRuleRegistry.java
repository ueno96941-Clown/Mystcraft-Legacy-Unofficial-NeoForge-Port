package com.xcompwiz.mystcraft.grammar;

import com.xcompwiz.mystcraft.symbol.LegacyBiomeSymbolDefinition;
import com.xcompwiz.mystcraft.symbol.LegacyBiomeSymbolRegistry;
import com.xcompwiz.mystcraft.symbol.LegacyMaterialSymbolDefinition;
import com.xcompwiz.mystcraft.symbol.LegacyMaterialSymbolRegistry;
import com.xcompwiz.mystcraft.symbol.SymbolAvailability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GrammarRuleRegistry {
    private static final Map<String, List<GrammarRule>> BY_PARENT = new LinkedHashMap<>();
    private static final Map<String, List<GrammarRule>> REVERSE = new LinkedHashMap<>();
    private static final Map<String, Map<Integer, Integer>> RANK_WEIGHTS = new LinkedHashMap<>();
    /** API/IMC rules survive runtime dynamic-symbol grammar rebuilds. */
    private static final List<GrammarRule> EXTERNAL_RULES = new ArrayList<>();
    private static boolean frozen;

    private GrammarRuleRegistry() {}

    public static synchronized void bootstrap() {
        if (frozen) return;
        BuiltinLegacyGrammar.registerAll();
        registerLegacyMaterialRules();
        registerLegacyBiomeRules();
        for (GrammarRule rule : EXTERNAL_RULES) register(rule);
        buildRankWeights();
        frozen = true;
    }



    /**
     * Invalidates an already-built grammar after runtime biome/fluid discovery without rebuilding it
     * immediately. The next real grammar consumer performs the normal lazy bootstrap.
     */
    public static synchronized void invalidateForDynamicSymbols() {
        if (!frozen) return;
        BY_PARENT.clear();
        REVERSE.clear();
        RANK_WEIGHTS.clear();
        frozen = false;
        GrammarShortestPathIndex.clearCache();
    }

    /**
     * Rebuilds the frozen grammar after dynamic biome/fluid symbols are discovered.
     * The legacy 1.12 registry was populated after mod loading; on 1.21.1 the same
     * effect is achieved by rebuilding from the immutable built-ins plus the current
     * runtime symbol registries before Ages are restored/generated.
     */
    public static synchronized void rebuildForDynamicSymbols() {
        invalidateForDynamicSymbols();
        bootstrap();
    }

    static synchronized void register(GrammarRule rule) {
        if (frozen) throw new IllegalStateException("Grammar registry is frozen");
        Objects.requireNonNull(rule, "rule");
        if (rule.values().stream().anyMatch(SymbolAvailability::isBlacklisted)) return;
        BY_PARENT.computeIfAbsent(rule.parent(), k -> new ArrayList<>()).add(rule);
        for (String value : rule.values()) REVERSE.computeIfAbsent(value, k -> new ArrayList<>()).add(rule);
    }

    /** Registers an API-provided legacy grammar rule and keeps it across dynamic rebuilds. */
    public static synchronized void registerExternalRule(GrammarRule rule) {
        Objects.requireNonNull(rule, "rule");
        if (!EXTERNAL_RULES.contains(rule)) EXTERNAL_RULES.add(rule);
        rebuildForDynamicSymbols();
    }

    public static synchronized List<GrammarRule> externalRules() {
        return List.copyOf(EXTERNAL_RULES);
    }

    private static void registerLegacyMaterialRules() {
        for (LegacyMaterialSymbolDefinition material : LegacyMaterialSymbolRegistry.values()) {
            material.grammarBindings().forEach(binding ->
                    register(new GrammarRule(
                            binding.parentToken(),
                            List.of(material.legacyId()),
                            binding.rank())));
        }
    }

    /**
     * Legacy ModSymbols dynamically registered every biome symbol under GrammarData.BIOME.
     * All ordinary biomes used grammar rank 1; the End/Sky biome was explicitly unranked.
     */
    private static void registerLegacyBiomeRules() {
        for (LegacyBiomeSymbolDefinition biome : LegacyBiomeSymbolRegistry.values()) {
            Integer rank = biome.legacyNumericId() == 9 ? null : 1;
            register(new GrammarRule(
                    "mystcraft:Biome",
                    List.of(biome.legacyId()),
                    rank));
        }
    }

    private static void buildRankWeights() {
        for (Map.Entry<String, List<GrammarRule>> entry : BY_PARENT.entrySet()) {
            int maxRank = -1;
            for (GrammarRule rule : entry.getValue()) if (rule.rank() != null) maxRank = Math.max(maxRank, rule.rank());
            if (maxRank < 0) continue;
            int[] sizes = new int[maxRank + 1];
            for (GrammarRule rule : entry.getValue()) if (rule.rank() != null) sizes[rule.rank()]++;
            LinkedHashMap<Integer, Integer> weights = new LinkedHashMap<>();
            int weight = 1;
            int lastTotal = 0;
            for (int i = sizes.length - 1; i >= 0; --i) {
                int count = sizes[i];
                if (weight != 1 && count > 0) weight = Math.max(weight, lastTotal / count + 1);
                weights.put(i, weight);
                lastTotal = count * weight;
                weight++;
            }
            RANK_WEIGHTS.put(entry.getKey(), Collections.unmodifiableMap(weights));
        }
    }

    public static List<GrammarRule> rulesFor(String parent) { bootstrap(); return List.copyOf(BY_PARENT.getOrDefault(parent, List.of())); }
    public static List<GrammarRule> parentRulesFor(String value) { bootstrap(); return List.copyOf(REVERSE.getOrDefault(value, List.of())); }
    public static int weightOf(GrammarRule rule) {
        bootstrap();
        if (rule.rank() == null) return 0;
        return RANK_WEIGHTS.getOrDefault(rule.parent(), Map.of()).getOrDefault(rule.rank(), 0);
    }
    public static Map<Integer, Integer> rankWeights(String parent) { bootstrap(); return RANK_WEIGHTS.getOrDefault(parent, Map.of()); }
    public static int ruleCount() { bootstrap(); return BY_PARENT.values().stream().mapToInt(List::size).sum(); }
    public static int parentTokenCount() { bootstrap(); return BY_PARENT.size(); }
}
