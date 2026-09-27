package com.xcompwiz.mystcraft.symbol;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Runtime compatibility policy for legacy symbol blacklisting.
 *
 * <p>0.13.7.06 kept the persisted page identity but removed blacklisted symbols
 * from executable/selection registries. The port mirrors that split.</p>
 */
public final class SymbolAvailability {
    private static final Set<String> BLACKLISTED = new LinkedHashSet<>();
    /** Legacy grammar-only no-op terminals retained for save/grammar compatibility. */
    private static final Set<String> INTERNAL_ONLY = Set.of(
            "mystcraft:FeatureSmallDummy",
            "mystcraft:FeatureMediumDummy",
            "mystcraft:FeatureLargeDummy");

    private SymbolAvailability() {}

    public static synchronized boolean blacklist(String legacyId) {
        if (legacyId == null || legacyId.isBlank()) return false;
        return BLACKLISTED.add(LegacySymbolId.qualify(legacyId));
    }

    public static synchronized boolean isBlacklisted(String legacyId) {
        if (legacyId == null || legacyId.isBlank()) return false;
        return BLACKLISTED.contains(LegacySymbolId.qualify(legacyId));
    }

    /** Ordinary gameplay selection: Writing Desk, boosters, economy and random selection. */
    public static boolean isSelectable(String legacyId) {
        if (legacyId == null || legacyId.isBlank()) return false;
        String qualified = LegacySymbolId.qualify(legacyId);
        return !INTERNAL_ONLY.contains(qualified) && !isBlacklisted(qualified);
    }

    /** Explicit authored-page execution. */
    public static boolean isExecutable(String legacyId) {
        if (legacyId == null || legacyId.isBlank()) return false;
        String qualified = LegacySymbolId.qualify(legacyId);
        return !INTERNAL_ONLY.contains(qualified) && !isBlacklisted(qualified);
    }

    /** Creative Page surface; excludes internal grammar dummies. */
    public static boolean isCreativeSelectable(String legacyId) {
        return isExecutable(legacyId);
    }

    public static synchronized Set<String> blacklistedIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(BLACKLISTED));
    }
}
