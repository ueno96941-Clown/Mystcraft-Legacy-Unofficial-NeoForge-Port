package com.xcompwiz.mystcraft.symbol;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Runtime symbol registry replacing the modifiable 1.12 Forge registry.
 *
 * <p>This is intentionally not a vanilla/NeoForge data registry yet. Legacy symbols
 * are executable logic objects and the old registry was explicitly modifiable. The
 * port keeps that semantic boundary while the Grammar and symbol logic APIs are rebuilt.</p>
 */
public final class SymbolRegistry {
    private static final Map<String, SymbolDefinition> BY_LEGACY_ID = new LinkedHashMap<>();
    private static final Map<ResourceLocation, SymbolDefinition> BY_KEY = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private SymbolRegistry() {}

    public static synchronized void bootstrapLegacyBuiltins() {
        if (bootstrapped) return;
        BuiltinLegacySymbols.registerAll();
        bootstrapped = true;
    }

    public static synchronized void register(SymbolDefinition symbol) {
        String raw = LegacySymbolId.qualify(symbol.legacyId());
        SymbolDefinition existingRaw = BY_LEGACY_ID.get(raw);
        if (existingRaw != null && !existingRaw.equals(symbol)) {
            throw new IllegalStateException("Duplicate legacy Mystcraft symbol id: " + raw);
        }
        SymbolDefinition existingKey = BY_KEY.get(symbol.key());
        if (existingKey != null && !existingKey.legacyId().equals(raw)) {
            throw new IllegalStateException("Canonical symbol-key collision between "
                    + existingKey.legacyId() + " and " + raw + " at " + symbol.key());
        }
        BY_LEGACY_ID.put(raw, symbol);
        BY_KEY.put(symbol.key(), symbol);
    }

    public static Optional<SymbolDefinition> resolveLegacy(String legacyId) {
        bootstrapLegacyBuiltins();
        String raw = LegacySymbolId.qualify(legacyId);
        SymbolDefinition exact = BY_LEGACY_ID.get(raw);
        if (exact != null) return Optional.of(exact);
        ResourceLocation canonical = LegacySymbolId.canonicalKey(raw);
        return canonical == null ? Optional.empty() : Optional.ofNullable(BY_KEY.get(canonical));
    }

    public static Optional<SymbolDefinition> resolveKey(ResourceLocation key) {
        bootstrapLegacyBuiltins();
        return Optional.ofNullable(BY_KEY.get(key));
    }

    public static boolean containsLegacy(String legacyId) {
        return resolveLegacy(legacyId).isPresent()
                || LegacyMaterialSymbolRegistry.containsLegacy(legacyId)
                || LegacyBiomeSymbolRegistry.containsLegacy(legacyId);
    }

    public static Optional<LegacyMaterialSymbolDefinition> resolveMaterialLegacy(String legacyId) {
        return LegacyMaterialSymbolRegistry.resolveLegacy(legacyId);
    }

    public static Optional<LegacyBiomeSymbolDefinition> resolveBiomeLegacy(String legacyId) {
        return LegacyBiomeSymbolRegistry.resolveLegacy(legacyId);
    }

    public static Collection<SymbolDefinition> values() {
        bootstrapLegacyBuiltins();
        return Collections.unmodifiableList(new ArrayList<>(BY_LEGACY_ID.values()));
    }

    public static List<SymbolDefinition> byCategory(SymbolCategory category) {
        bootstrapLegacyBuiltins();
        List<SymbolDefinition> result = new ArrayList<>();
        for (SymbolDefinition symbol : BY_LEGACY_ID.values()) {
            if (symbol.category() == category) result.add(symbol);
        }
        return List.copyOf(result);
    }
}
