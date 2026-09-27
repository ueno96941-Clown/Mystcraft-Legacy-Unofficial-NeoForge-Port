package com.xcompwiz.mystcraft.symbol;

import net.minecraft.util.RandomSource;
import com.xcompwiz.mystcraft.api.impl.runtime.ApiSymbolOverrides;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Rebuilds the 0.13.7.06 card-rank economy used by treasure, Booster Packs and
 * the Archivist shop.
 *
 * <p>The original SymbolManager assigned one item weight to every card rank.
 * Rank weights were derived from the number of symbols in each rank after all
 * symbols (including dynamic biome/material symbols) had registered.  The port
 * computes the same table on demand so late runtime symbols participate without
 * reviving any instability data.</p>
 */
public final class SymbolItemEconomy {
    private SymbolItemEconomy() {}

    public record RankedSymbol(String legacyId, int cardRank) {}

    public static List<RankedSymbol> rankedSymbols() {
        LinkedHashMap<String, RankedSymbol> result = new LinkedHashMap<>();
        for (SymbolDefinition symbol : SymbolRegistry.values()) {
            if (symbol.cardRank() != null && SymbolAvailability.isSelectable(symbol.legacyId())) {
                result.put(symbol.legacyId(), new RankedSymbol(symbol.legacyId(), ApiSymbolOverrides.cardRank(symbol.legacyId()) != null ? ApiSymbolOverrides.cardRank(symbol.legacyId()) : symbol.cardRank()));
            }
        }
        for (LegacyMaterialSymbolDefinition symbol : LegacyMaterialSymbolRegistry.values()) {
            if (SymbolAvailability.isSelectable(symbol.legacyId()))
                result.put(symbol.legacyId(), new RankedSymbol(symbol.legacyId(), ApiSymbolOverrides.cardRank(symbol.legacyId()) != null ? ApiSymbolOverrides.cardRank(symbol.legacyId()) : symbol.cardRank()));
        }
        // Legacy ModSymbols.generateBiomeSymbols gave all generated biomes rank 2
        // except the End/SKY biome, which deliberately had no item/card rank.
        for (LegacyBiomeSymbolDefinition symbol : LegacyBiomeSymbolRegistry.values()) {
            if (symbol.legacyNumericId() != 9 && SymbolAvailability.isSelectable(symbol.legacyId())) {
                result.put(symbol.legacyId(), new RankedSymbol(symbol.legacyId(), ApiSymbolOverrides.cardRank(symbol.legacyId()) != null ? ApiSymbolOverrides.cardRank(symbol.legacyId()) : 2));
            }
        }
        return List.copyOf(result.values());
    }

    /** Exact legacy SymbolManager.buildCardRanks rank-weight algorithm. */
    public static Map<Integer, Integer> rankWeights() {
        List<RankedSymbol> symbols = rankedSymbols();
        HashMap<Integer, Integer> rankCounts = new HashMap<>();
        int maxRank = -1;
        for (RankedSymbol symbol : symbols) {
            rankCounts.merge(symbol.cardRank(), 1, Integer::sum);
            maxRank = Math.max(maxRank, symbol.cardRank());
        }

        HashMap<Integer, Integer> weights = new HashMap<>();
        int weight = 1;
        int lastTotal = 0;
        for (int rank = maxRank; rank >= 0; --rank) {
            int count = rankCounts.getOrDefault(rank, 0);
            if (weight != 1 && count > 0) {
                weight = Math.max(weight, lastTotal / count + 1);
            }
            weights.put(rank, weight);
            lastTotal = count * weight;
            weight += 1;
        }
        return Map.copyOf(weights);
    }

    public static int itemWeight(String legacyId) {
        Optional<RankedSymbol> symbol = rankedSymbols().stream()
                .filter(s -> s.legacyId().equals(LegacySymbolId.qualify(legacyId)))
                .findFirst();
        if (symbol.isEmpty()) return 0;
        return rankWeights().getOrDefault(symbol.get().cardRank(), 0);
    }

    public static int cardRank(String legacyId) {
        String id = LegacySymbolId.qualify(legacyId);
        for (RankedSymbol symbol : rankedSymbols()) {
            if (symbol.legacyId().equals(id)) return symbol.cardRank();
        }
        return -1;
    }

    /** Legacy custom Archivist price: 4 * (1 + card rank). */
    public static int archivistPrice(String legacyId) {
        int rank = cardRank(legacyId);
        return rank < 0 ? 100 : 4 * (1 + rank);
    }

    /** Legacy default treasure stack cap by card rank. */
    public static int treasureMaxStack(int rank) {
        return switch (rank) {
            case 0 -> 16;
            case 1 -> 8;
            case 2 -> 4;
            case 3 -> 2;
            default -> rank < 0 ? 0 : 1;
        };
    }

    /**
     * Selects a symbol from all ranks >= minRank using the original per-rank item
     * weights.  This matches InventoryVillager#getShopItem, which intentionally
     * used getSymbolsByRank(index + 1, null), not an exact-rank bucket.
     */
    public static RankedSymbol chooseAtLeastRank(RandomSource random, int minRank) {
        return chooseWeighted(random, rankedSymbols().stream()
                .filter(s -> s.cardRank() >= minRank)
                .toList());
    }

    public static RankedSymbol chooseRankRange(RandomSource random, int minRank, int maxRank) {
        return chooseWeighted(random, rankedSymbols().stream()
                .filter(s -> s.cardRank() >= minRank && s.cardRank() <= maxRank)
                .toList());
    }

    public static RankedSymbol chooseTreasureSymbol(RandomSource random) {
        return chooseWeighted(random, rankedSymbols());
    }

    private static RankedSymbol chooseWeighted(RandomSource random, List<RankedSymbol> candidates) {
        if (candidates.isEmpty()) return null;
        Map<Integer, Integer> weights = rankWeights();
        float total = 0.0F;
        for (RankedSymbol symbol : candidates) total += Math.max(0, weights.getOrDefault(symbol.cardRank(), 0));
        // Legacy WeightedItemSelector#getRandomItem consumed exactly one nextFloat().
        // If all weights are non-positive it fell back to an even nextFloat-based selection.
        if (total <= 0.0F) {
            float selection = random.nextFloat() * candidates.size();
            RankedSymbol last = null;
            for (RankedSymbol symbol : candidates) {
                selection -= 1.0F;
                if (selection <= 0.0F) return symbol;
                last = symbol;
            }
            return last;
        }

        float selection = random.nextFloat() * total;
        RankedSymbol last = null;
        for (RankedSymbol symbol : candidates) {
            int weight = Math.max(0, weights.getOrDefault(symbol.cardRank(), 0));
            selection -= weight;
            if (weight > 0) {
                if (selection <= 0.0F) return symbol;
                last = symbol;
            }
        }
        return last;
    }

    /** Total dynamic-symbol entry weight in the old mystcraft_treasure pool. */
    public static int totalSymbolWeight() {
        Map<Integer, Integer> weights = rankWeights();
        long total = 0L;
        for (RankedSymbol symbol : rankedSymbols()) total += Math.max(0, weights.getOrDefault(symbol.cardRank(), 0));
        return (int)Math.min(Integer.MAX_VALUE, total);
    }
}
