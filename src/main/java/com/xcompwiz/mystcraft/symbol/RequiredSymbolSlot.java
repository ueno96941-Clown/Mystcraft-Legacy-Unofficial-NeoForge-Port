package com.xcompwiz.mystcraft.symbol;

/**
 * Required controller slots in the legacy Mystcraft Age root grammar.
 *
 * <p>Legacy 0.13.7.06 expands {@code mystcraft:Age} to TerrainGen,
 * BiomeController, Weather and Lighting before optional branches. These are
 * requirements, not hard-coded default symbols.</p>
 */
public enum RequiredSymbolSlot {
    TERRAIN(SymbolCategory.TERRAIN, "mystcraft:TerrainGen"),
    BIOME_CONTROLLER(SymbolCategory.BIOME_CONTROLLER, "mystcraft:BiomeController"),
    WEATHER(SymbolCategory.WEATHER, "mystcraft:Weather"),
    LIGHTING(SymbolCategory.LIGHTING, "mystcraft:Lighting");

    private final SymbolCategory category;
    private final String grammarToken;

    RequiredSymbolSlot(SymbolCategory category, String grammarToken) {
        this.category = category;
        this.grammarToken = grammarToken;
    }

    public SymbolCategory category() {
        return category;
    }

    /** Exact mixed-case legacy grammar token identity. */
    public String grammarToken() {
        return grammarToken;
    }
}
