package com.xcompwiz.mystcraft.treasure;

import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.registry.MystItems;
import com.xcompwiz.mystcraft.symbol.SymbolItemEconomy;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runtime equivalent of Mystcraft 0.13.7.06's {@code mystcraft_treasure} table.
 *
 * <p>The four vanilla chest hooks from the old mod intentionally are not reproduced:
 * their legacy {@code TreasureGenWrapper#addLoot} implementation was empty.  This
 * generator is for Mystcraft-owned structure treasure only (Small Libraries and the
 * Archivist-house lecterns).</p>
 */
public final class MystcraftTreasureGenerator {
    private static final int VIAL_WEIGHT = 50;
    private static final int BOOSTER_WEIGHT = 1000;
    private static final int LEATHER_WEIGHT = 50;
    private static final int PAPER_WEIGHT = 50;
    private static final int BASE_WEIGHT = VIAL_WEIGHT + BOOSTER_WEIGHT + LEATHER_WEIGHT + PAPER_WEIGHT;

    private MystcraftTreasureGenerator() {}

    /** Legacy pool rolls are uniformly 4..8 at zero luck. */
    public static List<ItemStack> generate(RandomSource random) {
        int rolls = 4 + random.nextInt(5);
        ArrayList<ItemStack> result = new ArrayList<>(rolls);
        for (int i = 0; i < rolls; ++i) {
            ItemStack stack = rollOne(random);
            if (!stack.isEmpty()) result.add(stack);
        }
        return result;
    }

    public static ItemStack rollOne(RandomSource random) {
        List<SymbolItemEconomy.RankedSymbol> symbols = SymbolItemEconomy.rankedSymbols();
        Map<Integer, Integer> rankWeights = SymbolItemEconomy.rankWeights();
        int symbolWeight = 0;
        for (var symbol : symbols) {
            symbolWeight += Math.max(0, rankWeights.getOrDefault(symbol.cardRank(), 0));
        }
        int total = BASE_WEIGHT + symbolWeight;
        if (total <= 0) return ItemStack.EMPTY;

        // Minecraft 1.12 LootPool#createLootRoll used rand.nextInt(totalWeight).
        // Keep that exact RNG consumption instead of a modulo of nextLong().
        int roll = random.nextInt(total);
        if ((roll -= VIAL_WEIGHT) < 0L) return new ItemStack(MystItems.INK_VIAL.get());
        if ((roll -= BOOSTER_WEIGHT) < 0L) return new ItemStack(MystItems.BOOSTER.get());
        if ((roll -= LEATHER_WEIGHT) < 0L) return new ItemStack(Items.LEATHER, 1 + random.nextInt(3));
        if ((roll -= PAPER_WEIGHT) < 0L) return new ItemStack(Items.PAPER, 1 + random.nextInt(6));

        for (var symbol : symbols) {
            int weight = Math.max(0, rankWeights.getOrDefault(symbol.cardRank(), 0));
            roll -= weight;
            if (roll < 0L) {
                ItemStack page = Page.createSymbolPage(symbol.legacyId());
                int maxStack = SymbolItemEconomy.treasureMaxStack(symbol.cardRank());
                // Exact 0.13.7.06 quirk: 1 + nextInt(max(1, maxStack - 1)).
                page.setCount(1 + random.nextInt(Math.max(1, maxStack - 1)));
                return page;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Legacy lectern helper: reroll the whole treasure pool up to 100 times and
     * take the first acceptable single item. Symbol pages must be card-rank >= 3.
     */
    public static ItemStack generateLecternItem(RandomSource random, boolean allowLinkingItems) {
        for (int attempt = 0; attempt < 100; ++attempt) {
            for (ItemStack stack : generate(random)) {
                if (stack.isEmpty()) continue;
                ItemStack one = stack.copyWithCount(1);
                String symbol = Page.getSymbolId(one);
                if (symbol != null) {
                    if (SymbolItemEconomy.cardRank(symbol) < 3) continue;
                    return one;
                }
                // The current treasure table contains no Linking Book entry. Keep the
                // parameter for the old InventoryFilter contract and future table parity.
                if (allowLinkingItems && one.getItem() instanceof com.xcompwiz.mystcraft.item.ItemLinking) {
                    return one;
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
