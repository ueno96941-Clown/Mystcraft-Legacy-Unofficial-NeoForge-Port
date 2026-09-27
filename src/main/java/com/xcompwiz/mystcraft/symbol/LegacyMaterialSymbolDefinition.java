package com.xcompwiz.mystcraft.symbol;

import java.util.List;
import java.util.Objects;

/**
 * Data-only port of the 1.12 SymbolBlock + BlockDescriptor registration.
 *
 * <p>The persisted legacy ID deliberately retains the old block registry path and metadata,
 * while modernBlockId points at the 1.21.1 block that represents the same material.</p>
 */
public record LegacyMaterialSymbolDefinition(
        String legacyId,
        String legacyBlockId,
        int legacyMetadata,
        String modernBlockId,
        String legacyWord,
        int cardRank,
        List<LegacyMaterialGrammarBinding> grammarBindings) {

    public LegacyMaterialSymbolDefinition {
        legacyId = LegacySymbolId.qualify(Objects.requireNonNull(legacyId, "legacyId"));
        legacyBlockId = Objects.requireNonNull(legacyBlockId, "legacyBlockId");
        modernBlockId = Objects.requireNonNull(modernBlockId, "modernBlockId");
        legacyWord = Objects.requireNonNull(legacyWord, "legacyWord");
        if (legacyMetadata < 0) throw new IllegalArgumentException("legacyMetadata must be >= 0");
        if (cardRank < 0) throw new IllegalArgumentException("cardRank must be >= 0");
        grammarBindings = List.copyOf(Objects.requireNonNull(grammarBindings, "grammarBindings"));
    }

    public LegacyMaterialGrammarBinding bindingFor(String parentToken) {
        for (LegacyMaterialGrammarBinding binding : grammarBindings) {
            if (binding.parentToken().equals(parentToken)) return binding;
        }
        return null;
    }

    public boolean usableAs(String parentToken) {
        return bindingFor(parentToken) != null;
    }
}
