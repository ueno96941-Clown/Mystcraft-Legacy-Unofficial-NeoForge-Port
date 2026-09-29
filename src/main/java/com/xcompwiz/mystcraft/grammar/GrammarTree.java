package com.xcompwiz.mystcraft.grammar;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Mystcraft Legacy grammar-tree phrasal parser and completion pass.
 *
 * <p>This is a compatibility-oriented port of the 0.13.7.06 {@code GrammarTree} behavior.
 * Explicit symbols are parsed right-to-left into a forest, then {@link #getExpanded(Random)}
 * reconnects that forest to the Age root, expands the remaining grammar holes and returns the
 * final terminal symbol sequence. Legacy mixed-case grammar/symbol strings are deliberately kept
 * as strings instead of being forced through modern ResourceLocation validation.</p>
 */
public final class GrammarTree {
    public static final String DEFAULT_ROOT = "mystcraft:Age";

    private final GrammarNode root;
    /** More recently discovered nodes are kept at the front, matching the legacy linked list. */
    private final List<GrammarNode> unexplored = new LinkedList<>();
    private List<GrammarNode> subroots = new ArrayList<>();
    private List<String> terminals = List.of();

    public GrammarTree() { this(DEFAULT_ROOT); }

    public GrammarTree(String rootToken) {
        this.root = new GrammarNode(Objects.requireNonNull(rootToken, "rootToken"));
    }

    /**
     * Builds the legacy phrasal forest from the explicitly written symbol sequence.
     * The caller owns the supplied Random; equal-length shortest-path ties consume it exactly
     * through {@link GrammarPathSelector}.
     */
    public void parseTerminals(List<String> inputTerminals, Random random) {
        Objects.requireNonNull(inputTerminals, "inputTerminals");
        Objects.requireNonNull(random, "random");
        if (!terminals.isEmpty() || !subroots.isEmpty() || root.selectedRule() != null || !root.children().isEmpty()) {
            throw new IllegalStateException("GrammarTree instances are single-use for parseTerminals");
        }

        terminals = Collections.unmodifiableList(new ArrayList<>(inputTerminals));
        for (int i = terminals.size(); i > 0; --i) {
            buildSubtree(new GrammarNode(terminals.get(i - 1), i - 1), random);
        }
    }

    /**
     * Merges parsed orphan subtrees into the Age tree and expands every remaining grammar hole.
     * This follows the legacy 0.13.7.06 getExpanded() insertion semantics rather than rebuilding
     * the explicit Page sequence from a flattened tree, which is important for preserving Page
     * order and duplicate positions.
     */
    public List<String> getExpanded(Random random) {
        return getExpandedInternal(random, false);
    }

    /** Age-generation-only expansion with bounded recursive random completion. */
    public List<String> getExpandedRandomCompletion(Random random) {
        return getExpandedInternal(random, true);
    }

    private List<String> getExpandedInternal(Random random, boolean boundedRandomCompletion) {
        Objects.requireNonNull(random, "random");

        ArrayList<String> out = new ArrayList<>(terminals);

        unexplored.clear();
        if (root.selectedRule() == null) unexplored.add(root);

        // Legacy pre-pass: deterministically expand holes that have exactly one rule.
        for (int i = 0; i < unexplored.size(); ++i) {
            List<GrammarRule> rules = GrammarRuleRegistry.rulesFor(unexplored.get(i).token());
            if (rules.size() == 1) {
                expandUnexploredNode(i--, rules.get(0));
            }
        }

        // Attach as much of the phrasal forest as possible to the main tree.
        ArrayList<GrammarNode> failedSubroots = new ArrayList<>();
        while (!subroots.isEmpty()) {
            if (unexplored.isEmpty()) {
                failedSubroots.addAll(subroots);
                break;
            }
            GrammarNode subroot = subroots.remove(0);
            if (!connectSubtreeShortest(subroot, random)) failedSubroots.add(subroot);
        }
        subroots = failedSubroots;

        // Locate unfulfilled leaf tokens relative to the original explicit Page positions.
        Map<Integer, List<String>> insertLeft = new LinkedHashMap<>();
        Map<Integer, List<String>> insertRight = new LinkedHashMap<>();
        getInsertions(root, insertLeft, insertRight);

        // Expand and insert from right to left so original terminal positions remain stable.
        for (int i = out.size() + 1; i > 0; --i) {
            List<String> products = insertRight.get(i - 1);
            if (products != null) {
                for (int j = products.size(); j > 0; --j) {
                    out.addAll(i, boundedRandomCompletion
                            ? GrammarExplorer.exploreRandomCompletion(products.get(j - 1), random)
                            : GrammarExplorer.explore(products.get(j - 1), random));
                }
            }
            products = insertLeft.get(i - 1);
            if (products != null) {
                for (int j = products.size(); j > 0; --j) {
                    out.addAll(i - 1, boundedRandomCompletion
                            ? GrammarExplorer.exploreRandomCompletion(products.get(j - 1), random)
                            : GrammarExplorer.explore(products.get(j - 1), random));
                }
            }
        }

        unexplored.clear();
        return List.copyOf(out);
    }

    public List<String> terminals() { return terminals; }
    public GrammarNode root() { return root; }
    public List<GrammarNode> orphanedSubroots() { return List.copyOf(subroots); }
    public List<GrammarNode> unexploredNodes() { return List.copyOf(unexplored); }

    /** Stable structural dump used by smoke tests without exposing mutation operations. */
    public List<String> structuralSnapshot() {
        ArrayList<String> result = new ArrayList<>();
        snapshotInto("root", root, result);
        for (int i = 0; i < subroots.size(); ++i) snapshotInto("orphan[" + i + "]", subroots.get(i), result);
        return List.copyOf(result);
    }

    private void snapshotInto(String prefix, GrammarNode node, List<String> out) {
        out.add(prefix + "=" + node.debugString());
        List<GrammarNode> children = node.children();
        for (int i = 0; i < children.size(); ++i) snapshotInto(prefix + "/" + i, children.get(i), out);
    }

    private void buildSubtree(GrammarNode subroot, Random random) {
        for (GrammarNode node : List.copyOf(unexplored)) {
            List<GrammarRule> path = GrammarPathSelector.chooseShortestPath(subroot.token(), node.token(), random);
            if (path != null) {
                for (GrammarRule rule : path) subroot = reverseExpand(subroot, rule);
                replaceNodeWithTree(node, subroot);
                return;
            }
        }

        List<GrammarRule> rules = GrammarRuleRegistry.parentRulesFor(subroot.token());
        while (rules.size() == 1) {
            GrammarRule rule = rules.get(0);
            if (rule.parent().equals(root.token())) break; // Do not prematurely attach directly to root.
            subroot = reverseExpand(subroot, rule);
            rules = GrammarRuleRegistry.parentRulesFor(subroot.token());
        }
        subroots.add(subroot);
        addUnexploredNodes(subroot);
    }

    /** Legacy connectSubtreeShortest(). */
    private boolean connectSubtreeShortest(GrammarNode subroot, Random random) {
        List<GrammarRule> rules = GrammarRuleRegistry.parentRulesFor(subroot.token());
        if (rules.isEmpty()) return false;

        for (GrammarNode node : List.copyOf(unexplored)) {
            if (node.token().equals(subroot.token())) {
                replaceNodeWithTree(node, subroot);
                return true;
            }

            ArrayList<GrammarRule> options = new ArrayList<>();
            for (GrammarRule rule : rules) {
                if (node.token().equals(rule.parent())) options.add(rule);
            }
            if (!options.isEmpty() && totalWeight(options) > 0.0F) {
                subroot = reverseExpand(subroot, chooseWeighted(options, random));
                replaceNodeWithTree(node, subroot);
                return true;
            }

            List<GrammarRule> path = GrammarPathSelector.chooseShortestPath(subroot.token(), node.token(), random);
            if (path != null) {
                for (GrammarRule rule : path) subroot = reverseExpand(subroot, rule);
                replaceNodeWithTree(node, subroot);
                return true;
            }
        }
        return false;
    }

    /** WeightedItemSelector-compatible selection for the direct producer-rule branch. */
    private static GrammarRule chooseWeighted(List<GrammarRule> rules, Random random) {
        float total = totalWeight(rules);
        if (total <= 0.0F) return chooseEvenly(rules, random);

        GrammarRule last = null;
        float selection = random.nextFloat() * total;
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

    private static GrammarRule chooseEvenly(List<GrammarRule> rules, Random random) {
        GrammarRule last = null;
        float selection = random.nextFloat() * rules.size();
        for (GrammarRule rule : rules) {
            selection -= 1.0F;
            if (selection <= 0.0F) return rule;
            last = rule;
        }
        return last;
    }

    private static float totalWeight(List<GrammarRule> rules) {
        float total = 0.0F;
        for (GrammarRule rule : rules) total += GrammarRuleRegistry.weightOf(rule);
        return total;
    }

    private void replaceNodeWithTree(GrammarNode node, GrammarNode subroot) {
        unexplored.remove(node);
        node.replaceFrom(subroot);
        addUnexploredNodes(node);
    }

    private void addUnexploredNodes(GrammarNode subroot) {
        ArrayDeque<GrammarNode> nodes = new ArrayDeque<>();
        nodes.addLast(subroot);
        while (!nodes.isEmpty()) {
            GrammarNode node = nodes.removeFirst();
            for (GrammarNode child : node.children()) {
                if (child.selectedRule() != null) {
                    nodes.addLast(child);
                } else if (!child.terminal()) {
                    unexplored.add(0, child);
                }
            }
        }
    }

    /** Legacy expandUnexploredNode(). */
    private void expandUnexploredNode(int index, GrammarRule rule) {
        GrammarNode node = unexplored.remove(index);
        node.setSelectedRule(rule);
        List<String> products = rule.values();
        for (int i = products.size(); i > 0; --i) {
            GrammarNode newNode = new GrammarNode(products.get(i - 1));
            node.addChild(0, newNode);
            unexplored.add(index, newNode);
        }
    }

    private void getInsertions(GrammarNode node, Map<Integer, List<String>> insertLeft,
                               Map<Integer, List<String>> insertRight) {
        for (GrammarNode child : node.children()) getInsertions(child, insertLeft, insertRight);

        if (node.children().isEmpty() && node.selectedRule() == null && !node.terminal()) {
            Integer left = node.leftPosition();
            if (left == null) {
                Integer right = node.rightPosition();
                if (right == null) {
                    left = terminals.size();
                } else {
                    insertRight.computeIfAbsent(right, ignored -> new ArrayList<>()).add(node.token());
                }
            }
            if (left != null) {
                insertLeft.computeIfAbsent(left, ignored -> new ArrayList<>()).add(node.token());
            }
        }
    }

    /**
     * Grows a subtree upward using one producing rule. The existing subtree replaces the
     * rightmost matching RHS token, exactly as in the legacy implementation.
     */
    static GrammarNode reverseExpand(GrammarNode subroot, GrammarRule rule) {
        Objects.requireNonNull(rule, "rule");
        GrammarNode newRoot = new GrammarNode(rule.parent());
        newRoot.setSelectedRule(rule);
        List<String> products = rule.values();
        for (int i = products.size(); i > 0; --i) {
            String product = products.get(i - 1);
            if (subroot != null && product.equals(subroot.token())) {
                newRoot.addChild(0, subroot);
                subroot = null;
            } else {
                newRoot.addChild(0, new GrammarNode(product));
            }
        }
        return newRoot;
    }
}
