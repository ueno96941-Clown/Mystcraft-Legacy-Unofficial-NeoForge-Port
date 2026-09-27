package com.xcompwiz.mystcraft.item;

import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.registry.MystItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Legacy Booster Pack: consumes one pack and produces a Folder containing
 * 7 rank-0, 4 rank-1, 4 rank-2 and 1 rank-3-or-higher random Symbol Pages.
 *
 * <p>The 1.12 implementation selected by SymbolManager item weight. The port
 * restores those legacy rank/item weights through {@code SymbolItemEconomy} and
 * uses the old {@code nextFloat() * totalWeight} selector semantics. Card-rank
 * boundaries and output counts are exact.</p>
 */
public final class ItemBoosterPack extends Item {
    public ItemBoosterPack(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.pass(held);

        ItemStack folder = generateBooster(player.getRandom());
        if (folder.isEmpty()) return InteractionResultHolder.pass(held);

        held.shrink(1);
        if (held.isEmpty()) {
            player.setItemInHand(hand, folder);
            return InteractionResultHolder.sidedSuccess(folder, false);
        }
        if (!player.getInventory().add(folder)) {
            held.grow(1);
            return InteractionResultHolder.pass(held);
        }
        return InteractionResultHolder.sidedSuccess(held, false);
    }

    public static ItemStack generateBooster(RandomSource random) {
        List<ItemStack> pages = new ArrayList<>(16);
        addRandomPages(random, pages, 7, 0, 0);
        addRandomPages(random, pages, 4, 1, 1);
        addRandomPages(random, pages, 4, 2, 2);
        addRandomPages(random, pages, 1, 3, Integer.MAX_VALUE);
        if (pages.isEmpty()) return ItemStack.EMPTY;

        ItemStack folder = new ItemStack(MystItems.FOLDER.get());
        ((ItemFolder) folder.getItem()).setPages(folder, pages);
        return folder;
    }

    private static void addRandomPages(RandomSource random, List<ItemStack> out,
                                       int count, int minRank, int maxRank) {
        for (int i = 0; i < count; ++i) {
            var selected = com.xcompwiz.mystcraft.symbol.SymbolItemEconomy.chooseRankRange(random, minRank, maxRank);
            if (selected != null) out.add(Page.createSymbolPage(selected.legacyId()));
        }
    }

}
