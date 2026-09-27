package com.xcompwiz.mystcraft.symbol;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable first-pass analysis of the exact ordered Age symbol stream. */
public record SymbolAnalysis(List<SymbolSequenceEntry> ordered) {
    public SymbolAnalysis {
        ordered = List.copyOf(Objects.requireNonNull(ordered, "ordered"));
    }

    public List<SymbolSequenceEntry> byCategory(SymbolCategory category) {
        Objects.requireNonNull(category, "category");
        return ordered.stream()
                .filter(SymbolSequenceEntry::known)
                .filter(entry -> entry.category() == category)
                .toList();
    }

    public Map<SymbolCategory, List<SymbolSequenceEntry>> categories() {
        EnumMap<SymbolCategory, List<SymbolSequenceEntry>> result = new EnumMap<>(SymbolCategory.class);
        for (SymbolSequenceEntry entry : ordered) {
            if (entry.known()) {
                result.computeIfAbsent(entry.category(), ignored -> new ArrayList<>()).add(entry);
            }
        }
        EnumMap<SymbolCategory, List<SymbolSequenceEntry>> immutable = new EnumMap<>(SymbolCategory.class);
        result.forEach((category, entries) -> immutable.put(category, List.copyOf(entries)));
        return Collections.unmodifiableMap(immutable);
    }

    public List<SymbolSequenceEntry> unknown() {
        return ordered.stream().filter(entry -> !entry.known()).toList();
    }

    public List<SymbolSequenceEntry> duplicates() {
        return ordered.stream().filter(SymbolSequenceEntry::duplicate).toList();
    }

    public List<ResourceLocation> resolvedKeysInOrder() {
        return ordered.stream()
                .filter(SymbolSequenceEntry::known)
                .map(SymbolSequenceEntry::key)
                .toList();
    }

    public List<String> unknownLegacyIdsInOrder() {
        return unknown().stream().map(SymbolSequenceEntry::legacyId).toList();
    }

    public boolean hasCategory(SymbolCategory category) {
        Objects.requireNonNull(category, "category");
        return ordered.stream()
                .filter(SymbolSequenceEntry::known)
                .anyMatch(entry -> entry.category() == category);
    }

    public boolean fullyKnown() {
        return ordered.stream().allMatch(SymbolSequenceEntry::known);
    }
}
