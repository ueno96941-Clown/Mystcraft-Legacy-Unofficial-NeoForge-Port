package com.xcompwiz.mystcraft.api.impl.runtime;

import com.xcompwiz.mystcraft.api.symbol.IAgeSymbol;
import com.xcompwiz.mystcraft.api.world.AgeDirector;
import com.xcompwiz.mystcraft.client.LegacySymbolName;
import com.xcompwiz.mystcraft.client.LegacySymbolVisuals;
import com.xcompwiz.mystcraft.instability.LegacySymbolInstability;
import com.xcompwiz.mystcraft.symbol.LegacyBiomeSymbolRegistry;
import com.xcompwiz.mystcraft.symbol.LegacyMaterialSymbolRegistry;
import com.xcompwiz.mystcraft.symbol.LegacySymbolId;
import com.xcompwiz.mystcraft.symbol.SymbolRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Public-API view over the port's exact legacy symbol identity. */
public final class ApiSymbolView implements IAgeSymbol {
    private final String legacyId;
    private final ResourceLocation key;

    public ApiSymbolView(String legacyId) {
        this.legacyId = LegacySymbolId.qualify(Objects.requireNonNull(legacyId, "legacyId"));
        ResourceLocation canonical = LegacySymbolId.canonicalKey(this.legacyId);
        if (canonical == null) throw new IllegalArgumentException("Invalid legacy symbol id: " + legacyId);
        this.key = canonical;
    }

    public String legacyId() { return legacyId; }

    @Override public ResourceLocation getRegistryName() { return key; }

    /** Built-in runtime logic is driven by the port grammar/worldgen bridge, not re-registered through this view. */
    @Override public void registerLogic(AgeDirector controller, long seed) {}

    /** Legacy score view. Runtime effects remain independently gated by InstabilityPolicy. */
    @Override public int instabilityModifier(int count) { return LegacySymbolInstability.instabilityModifier(legacyId, count); }

    @Override public boolean generatesConfigOption() { return false; }

    @Override public String getLocalizedName() { return LegacySymbolName.component(legacyId).getString(); }

    @Override public String[] getPoem() { return LegacySymbolVisuals.poem(legacyId); }

    @Override public boolean equals(Object obj) {
        return obj instanceof ApiSymbolView other && legacyId.equals(other.legacyId);
    }

    @Override public int hashCode() { return legacyId.hashCode(); }

    @Override public String toString() { return "MystcraftSymbol[" + legacyId + "]"; }

    public static String legacyIdFor(ResourceLocation identifier) {
        if (identifier == null) return null;
        var fixed = SymbolRegistry.resolveKey(identifier);
        if (fixed.isPresent()) return fixed.get().legacyId();
        for (var material : LegacyMaterialSymbolRegistry.values()) {
            if (identifier.equals(LegacySymbolId.canonicalKey(material.legacyId()))) return material.legacyId();
        }
        for (var biome : LegacyBiomeSymbolRegistry.values()) {
            if (identifier.equals(LegacySymbolId.canonicalKey(biome.legacyId()))) return biome.legacyId();
        }
        return null;
    }

    public static ApiSymbolView resolve(ResourceLocation identifier) {
        String id = legacyIdFor(identifier);
        return id == null ? null : new ApiSymbolView(id);
    }

    public static List<IAgeSymbol> all() {
        ArrayList<IAgeSymbol> out = new ArrayList<>();
        SymbolRegistry.values().forEach(s -> out.add(new ApiSymbolView(s.legacyId())));
        LegacyMaterialSymbolRegistry.values().forEach(s -> out.add(new ApiSymbolView(s.legacyId())));
        LegacyBiomeSymbolRegistry.values().forEach(s -> out.add(new ApiSymbolView(s.legacyId())));
        return List.copyOf(out);
    }
}
