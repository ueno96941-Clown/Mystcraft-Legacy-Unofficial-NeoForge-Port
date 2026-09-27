package com.xcompwiz.mystcraft.grammar;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Mutable grammar-tree node used while reproducing the Mystcraft Legacy phrasal parser.
 *
 * <p>Terminal nodes correspond to symbols explicitly written in the descriptive book and
 * carry their original page-order position. Non-terminal nodes are grammar tokens introduced
 * while reverse-expanding those written symbols toward the {@code mystcraft:Age} root.</p>
 */
public final class GrammarNode {
    private final String token;
    private final boolean terminal;
    private GrammarRule selected;
    private GrammarNode parent;
    private List<GrammarNode> children = new ArrayList<>();
    private Integer leftPosition;
    private Integer rightPosition;

    public GrammarNode(String token) {
        this.token = Objects.requireNonNull(token, "token");
        this.terminal = false;
    }

    public GrammarNode(String token, int position) {
        this.token = Objects.requireNonNull(token, "token");
        this.terminal = true;
        this.leftPosition = position;
        this.rightPosition = position;
    }

    public String token() { return token; }
    public boolean terminal() { return terminal; }
    public GrammarRule selectedRule() { return selected; }
    public GrammarNode parent() { return parent; }
    public List<GrammarNode> children() { return List.copyOf(children); }

    Integer leftPosition() {
        if (leftPosition != null) return leftPosition;
        for (GrammarNode child : children) {
            Integer position = child.leftPosition();
            if (position != null && (leftPosition == null || leftPosition > position)) leftPosition = position;
        }
        if (leftPosition != null) return leftPosition;
        if (parent != null) {
            for (int i = 0; i < parent.children.size(); ++i) {
                if (parent.children.get(i) == this) {
                    if (parent.children.size() > i + 1) leftPosition = parent.children.get(i + 1).leftPosition();
                    break;
                }
            }
        }
        return leftPosition;
    }

    Integer rightPosition() {
        if (rightPosition != null) return rightPosition;
        for (GrammarNode child : children) {
            Integer position = child.rightPosition();
            if (position != null && (rightPosition == null || rightPosition < position)) rightPosition = position;
        }
        if (rightPosition != null) return rightPosition;
        if (parent != null) {
            for (int i = parent.children.size() - 1; i >= 0; --i) {
                if (parent.children.get(i) == this) {
                    if (i > 0) rightPosition = parent.children.get(i - 1).rightPosition();
                } else if (rightPosition != null) {
                    Integer sibling = parent.children.get(i).rightPosition();
                    if (sibling != null && rightPosition < sibling) rightPosition = sibling;
                }
            }
        }
        return rightPosition;
    }

    void setSelectedRule(GrammarRule selected) { this.selected = selected; }

    void addChild(GrammarNode child) {
        children.add(child);
        child.parent = this;
    }

    void addChild(int index, GrammarNode child) {
        children.add(index, child);
        child.parent = this;
    }

    void replaceFrom(GrammarNode subtree) {
        this.selected = subtree.selected;
        this.children = new ArrayList<>(subtree.children);
        for (GrammarNode child : children) child.parent = this;
        this.leftPosition = null;
        this.rightPosition = null;
    }

    GrammarNode deepCopy() {
        GrammarNode rootCopy = flatCopy(this);
        ArrayDeque<NodePair> queue = new ArrayDeque<>();
        queue.addLast(new NodePair(this, rootCopy));
        while (!queue.isEmpty()) {
            NodePair pair = queue.removeFirst();
            for (GrammarNode child : pair.original.children) {
                GrammarNode childCopy = flatCopy(child);
                pair.copy.addChild(childCopy);
                queue.addLast(new NodePair(child, childCopy));
            }
        }
        return rootCopy;
    }

    private static GrammarNode flatCopy(GrammarNode node) {
        GrammarNode copy = node.terminal ? new GrammarNode(node.token, node.leftPosition) : new GrammarNode(node.token);
        copy.leftPosition = node.leftPosition;
        copy.rightPosition = node.rightPosition;
        copy.selected = node.selected;
        return copy;
    }

    public String debugString() {
        return token + (selected != null ? ":" : "") + (terminal ? "*" : "") + "(" + children.size() + ")";
    }

    private record NodePair(GrammarNode original, GrammarNode copy) {}
}
