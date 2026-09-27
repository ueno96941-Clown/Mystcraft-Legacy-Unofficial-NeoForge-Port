package com.xcompwiz.mystcraft.symbol;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Audits every fixed legacy 0.13.7.06 Symbol against the current port state.
 *
 * <p>PARTIAL is deliberately temporary. "Symbol系統完了" is only valid when no
 * fixed Symbol remains PARTIAL and dynamic material/biome audits also pass.</p>
 */
public final class SymbolPortAudit {
    public static final int EXPECTED_FIXED_SYMBOL_COUNT = 110;

    private static final Set<String> DUMMY = Set.of(
            "mystcraft:MoonDark",
            "mystcraft:StarsDark",
            "mystcraft:SunDark",
            "mystcraft:FeatureLargeDummy",
            "mystcraft:FeatureMediumDummy",
            "mystcraft:FeatureSmallDummy");


    /**
     * Fixed Symbols whose port is functional but still uses a known modern approximation
     * instead of the exact Legacy 0.13.7.06 implementation. Keep these out of the
     * "complete" bucket until the approximation itself is replaced or explicitly accepted.
     */
    private static final Set<String> PARTIAL_LEGACY_SEMANTICS = Set.of(
            // Legacy Native used the 1.12 BiomeProvider/GenLayer stack; the live port still
            // uses the 1.21.1 OVERWORLD MultiNoise preset. Replacing this changes the live
            // BiomeSource contract and must be developed/tested with runtime worldgen.
            "mystcraft:BioConNative",

            // Legacy placement math is restored, but structure pieces/layouts are modern.
            // Exact parity would require porting the corresponding 1.12 vanilla structure
            // implementations themselves, not a small Mystcraft-side correction.
            "mystcraft:Mineshafts",
            "mystcraft:Strongholds",
            "mystcraft:Villages",
            "mystcraft:NetherFort");

    private static final Set<String> PARTIAL_PENDING_RUNTIME_OR_CLIENT = Set.of(
            // Exact 0.13.7.06 density/noise kernels and legacy biome profiles are now ported,
            // but the live 1.21.1 ChunkGenerator adapter intentionally remains runtime-gated.
            "mystcraft:TerrainNormal",
            "mystcraft:TerrainAmplified",
            "mystcraft:TerrainNether",
            "mystcraft:TerrainEnd",

            // The legacy climate/water-color source data has been recovered and preserved.
            // The current client tint bridge still reads modern biome tint until the legacy
            // per-position colorizer/client path can be runtime-verified.
            "mystcraft:ColorGrassNat",
            "mystcraft:ColorFoliageNat",
            "mystcraft:ColorWaterNat");

    private SymbolPortAudit() {}

    public static AuditResult auditFixedSymbols() {
        Collection<SymbolDefinition> definitions = SymbolRegistry.values();
        LinkedHashMap<String, SymbolPortStatus> statuses = new LinkedHashMap<>();
        for (SymbolDefinition definition : definitions) {
            String id = definition.legacyId();
            SymbolPortStatus status;
            if (DUMMY.contains(id)) {
                status = SymbolPortStatus.LEGACY_DUMMY_NO_OP;
            } else if (PARTIAL_LEGACY_SEMANTICS.contains(id)) {
                status = SymbolPortStatus.PARTIAL_LEGACY_SEMANTICS;
            } else if (PARTIAL_PENDING_RUNTIME_OR_CLIENT.contains(id)) {
                status = SymbolPortStatus.PARTIAL_PENDING_RUNTIME_OR_CLIENT;
            } else {
                status = SymbolPortStatus.FULLY_IMPLEMENTED;
            }
            statuses.put(id, status);
        }
        return new AuditResult(Map.copyOf(statuses), statuses.size());
    }

    public record AuditResult(Map<String, SymbolPortStatus> statuses, int fixedSymbolCount) {
        public AuditResult {
            statuses = Map.copyOf(statuses);
        }

        public long count(SymbolPortStatus status) {
            return statuses.values().stream().filter(status::equals).count();
        }

        public boolean fixedRegistryCountMatchesLegacy() {
            return fixedSymbolCount == EXPECTED_FIXED_SYMBOL_COUNT;
        }

        public boolean fixedSymbolsComplete() {
            return fixedRegistryCountMatchesLegacy()
                    && count(SymbolPortStatus.PARTIAL_LEGACY_SEMANTICS) == 0
                    && count(SymbolPortStatus.PARTIAL_PENDING_RUNTIME_OR_CLIENT) == 0;
        }
    }
}
