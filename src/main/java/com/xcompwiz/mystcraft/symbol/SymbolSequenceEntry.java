package com.xcompwiz.mystcraft.symbol;

import net.minecraft.resources.ResourceLocation;
import java.util.Objects;

/**
 * One ordered symbol occurrence from a descriptive book/Age record.
 *
 * <p>Occurrences are deliberately not deduplicated. Legacy Mystcraft grammar is
 * order-sensitive and repeated pages may be meaningful, so this object records
 * duplicates instead of deleting them.</p>
 */
public record SymbolSequenceEntry(
        int index,
        String legacyId,
        ResourceLocation key,
        SymbolCategory category,
        int occurrence) {

    public SymbolSequenceEntry {
        if (index < 0) throw new IllegalArgumentException("index must be >= 0");
        if (occurrence < 1) throw new IllegalArgumentException("occurrence must be >= 1");
        legacyId = LegacySymbolId.qualify(Objects.requireNonNull(legacyId, "legacyId"));
        if ((key == null) != (category == null)) {
            throw new IllegalArgumentException("key and category must either both be present or both be absent");
        }
    }

    public boolean known() {
        return key != null;
    }

    public boolean duplicate() {
        return occurrence > 1;
    }
}
