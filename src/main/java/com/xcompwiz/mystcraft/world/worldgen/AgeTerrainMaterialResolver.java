package com.xcompwiz.mystcraft.world.worldgen;

import com.xcompwiz.mystcraft.symbol.LegacyMaterialSymbolDefinition;
import com.xcompwiz.mystcraft.symbol.LegacyMaterialSymbolRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Replays the legacy block-modifier queue far enough to resolve the material consumed by
 * the first terrain-generator Symbol.
 *
 * <p>Legacy SymbolBlock logic pushed block descriptors to the front of one shared blocklist.
 * Terrain generators then popped SEA first and TERRAIN second. Replaying that queue is
 * important because one ModMat symbol can be usable for several categories and because
 * an empty BlockTerrain branch must not steal the following BlockSea material.</p>
 */
public final class AgeTerrainMaterialResolver {
    private static final String TERRAIN = "mystcraft:BlockTerrain";
    private static final String SEA = "mystcraft:BlockSea";

    private AgeTerrainMaterialResolver() {}

    public static AgeTerrainMaterialSelection resolve(
            AgeTerrainMode mode, List<String> effectiveSymbols) {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(effectiveSymbols, "effectiveSymbols");

        String terrainBlock = "minecraft:stone";
        String seaBlock = mode == AgeTerrainMode.END ? "minecraft:air" : "minecraft:water";
        ArrayList<QueuedMaterial> blocklist = new ArrayList<>();

        for (String symbol : effectiveSymbols) {
            if ("mystcraft:ModClear".equals(symbol)) {
                blocklist.clear();
                continue;
            }
            if ("mystcraft:NoSea".equals(symbol)) {
                blocklist.add(0, QueuedMaterial.noSea());
                continue;
            }
            LegacyMaterialSymbolDefinition material = LegacyMaterialSymbolRegistry.resolveLegacy(symbol).orElse(null);
            if (material != null) {
                blocklist.add(0, QueuedMaterial.material(material));
                continue;
            }

            AgeTerrainMode thisMode = terrainMode(symbol);
            if (thisMode == AgeTerrainMode.UNKNOWN) continue;

            // Each registered terrain generator existed long enough to consume modifiers, even
            // if a later terrain Symbol replaced it in AgeController. Reset to that generator's
            // own defaults, consume its queue entries, then let later generators overwrite.
            terrainBlock = "minecraft:stone";
            seaBlock = thisMode == AgeTerrainMode.END ? "minecraft:air" : "minecraft:water";
            if (thisMode != AgeTerrainMode.VOID) {
                QueuedMaterial sea = popFirstUsable(blocklist, SEA);
                if (sea != null) seaBlock = sea.modernBlockId();
                QueuedMaterial terrain = popFirstUsable(blocklist, TERRAIN);
                if (terrain != null) terrainBlock = terrain.modernBlockId();
            }
        }

        return new AgeTerrainMaterialSelection(terrainBlock, seaBlock);
    }

    private static AgeTerrainMode terrainMode(String symbol) {
        if ("mystcraft:TerrainNormal".equals(symbol)) return AgeTerrainMode.NORMAL;
        if ("mystcraft:TerrainAmplified".equals(symbol)) return AgeTerrainMode.AMPLIFIED;
        if ("mystcraft:TerrainNether".equals(symbol)) return AgeTerrainMode.NETHER;
        if ("mystcraft:TerrainEnd".equals(symbol)) return AgeTerrainMode.END;
        if ("mystcraft:TerrainFlat".equals(symbol)) return AgeTerrainMode.FLAT;
        if ("mystcraft:TerrainVoid".equals(symbol)) return AgeTerrainMode.VOID;
        return AgeTerrainMode.UNKNOWN;
    }

    static boolean isMaterialConsumingTerrainGenerator(String symbol) {
        return "mystcraft:TerrainNormal".equals(symbol)
                || "mystcraft:TerrainAmplified".equals(symbol)
                || "mystcraft:TerrainNether".equals(symbol)
                || "mystcraft:TerrainEnd".equals(symbol)
                || "mystcraft:TerrainFlat".equals(symbol);
    }

    private static QueuedMaterial popFirstUsable(List<QueuedMaterial> blocklist, String category) {
        for (int i = 0; i < blocklist.size(); ++i) {
            QueuedMaterial candidate = blocklist.get(i);
            if (candidate.usableAs(category)) {
                blocklist.remove(i);
                return candidate;
            }
        }
        return null;
    }

    private record QueuedMaterial(String modernBlockId, LegacyMaterialSymbolDefinition definition, boolean seaOnly) {
        static QueuedMaterial material(LegacyMaterialSymbolDefinition definition) {
            return new QueuedMaterial(definition.modernBlockId(), definition, false);
        }

        static QueuedMaterial noSea() {
            return new QueuedMaterial("minecraft:air", null, true);
        }

        boolean usableAs(String category) {
            if (seaOnly) return SEA.equals(category);
            return definition != null && definition.usableAs(category);
        }
    }
}
