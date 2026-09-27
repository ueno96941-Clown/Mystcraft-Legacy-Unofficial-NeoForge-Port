package com.xcompwiz.mystcraft.symbol;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Metadata identity for a legacy Age symbol. Logic hooks are added in later checkpoints. */
public record SymbolDefinition(
        String legacyId,
        ResourceLocation key,
        SymbolCategory category,
        Integer cardRank,
        String legacyImplementation) {

    public SymbolDefinition {
        legacyId = LegacySymbolId.qualify(Objects.requireNonNull(legacyId, "legacyId"));
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(category, "category");
        legacyImplementation = legacyImplementation == null ? "" : legacyImplementation;
    }

    public static SymbolDefinition legacy(String legacyId, SymbolCategory category, Integer cardRank, String legacyImplementation) {
        ResourceLocation key = LegacySymbolId.canonicalKey(legacyId);
        if (key == null) throw new IllegalArgumentException("Invalid legacy Mystcraft symbol id: " + legacyId);
        return new SymbolDefinition(legacyId, key, category, cardRank, legacyImplementation);
    }
}
