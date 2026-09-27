package com.xcompwiz.mystcraft.symbol;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Non-destructive first-pass symbol analyzer.
 *
 * <p>This checkpoint intentionally does not apply grammar, defaults, instability,
 * or worldgen behavior. It only preserves order and records what is known.</p>
 */
public final class SymbolAnalyzer {
    private SymbolAnalyzer() {}

    public static SymbolAnalysis analyze(List<String> legacyIds) {
        List<SymbolSequenceEntry> entries = new ArrayList<>();
        Map<String, Integer> occurrences = new HashMap<>();
        for (int index = 0; index < legacyIds.size(); index++) {
            String raw = LegacySymbolId.qualify(legacyIds.get(index));
            SymbolDefinition definition = SymbolRegistry.resolveLegacy(raw).orElse(null);
            LegacyMaterialSymbolDefinition material =
                    definition == null ? SymbolRegistry.resolveMaterialLegacy(raw).orElse(null) : null;
            LegacyBiomeSymbolDefinition biome =
                    definition == null && material == null
                            ? SymbolRegistry.resolveBiomeLegacy(raw).orElse(null)
                            : null;

            String identity;
            ResourceLocation key = null;
            SymbolCategory category = null;
            if (definition != null) {
                key = definition.key();
                category = definition.category();
                identity = "key:" + key;
            } else if (material != null) {
                key = LegacySymbolId.canonicalKey(material.legacyId());
                category = SymbolCategory.MODIFIER;
                identity = "key:" + key;
            } else if (biome != null) {
                key = LegacySymbolId.canonicalKey(biome.legacyId());
                category = SymbolCategory.MODIFIER;
                identity = "key:" + key;
            } else {
                // Unknown IDs remain distinct by their exact persisted legacy identity.
                identity = "legacy:" + raw;
            }

            int occurrence = occurrences.merge(identity, 1, Integer::sum);
            entries.add(new SymbolSequenceEntry(index, raw, key, category, occurrence));
        }
        return new SymbolAnalysis(entries);
    }
}
