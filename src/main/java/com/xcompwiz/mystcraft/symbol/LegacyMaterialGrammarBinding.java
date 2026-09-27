package com.xcompwiz.mystcraft.symbol;

import java.util.Objects;

/** One legacy SymbolBlock grammar binding, e.g. BlockTerrain -> ModMat_stone_0 at rank 1. */
public record LegacyMaterialGrammarBinding(String parentToken, int rank) {
    public LegacyMaterialGrammarBinding {
        parentToken = Objects.requireNonNull(parentToken, "parentToken");
        if (rank < 0) throw new IllegalArgumentException("rank must be >= 0");
    }
}
