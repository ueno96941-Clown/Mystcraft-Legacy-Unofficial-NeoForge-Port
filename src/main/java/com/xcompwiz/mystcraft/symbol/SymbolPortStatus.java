package com.xcompwiz.mystcraft.symbol;

/** Explicit implementation state used by the fixed-symbol completion audit. */
public enum SymbolPortStatus {
    FULLY_IMPLEMENTED,
    LEGACY_DUMMY_NO_OP,
    INTENTIONALLY_DISABLED_INSTABILITY,
    INTENTIONALLY_REMOVED_DECAY,
    PARTIAL_LEGACY_SEMANTICS,
    PARTIAL_PENDING_RUNTIME_OR_CLIENT
}
