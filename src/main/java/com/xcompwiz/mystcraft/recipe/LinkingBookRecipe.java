package com.xcompwiz.mystcraft.recipe;

import com.xcompwiz.mystcraft.item.ItemLinkbookUnlinked;
import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.registry.MystRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Port of legacy RecipeLinkingbook: exactly one Link Panel Page plus one leather
 * cover in any two crafting slots produces an Unlinked Linking Book carrying the
 * exact Link Panel payload.
 */
public final class LinkingBookRecipe extends CustomRecipe {
    public LinkingBookRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack linkPanel = ItemStack.EMPTY;
        ItemStack cover = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (Page.isLinkPanel(stack)) {
                if (!linkPanel.isEmpty()) return false;
                linkPanel = stack;
            } else if (stack.is(Items.LEATHER)) {
                if (!cover.isEmpty()) return false;
                cover = stack;
            } else {
                return false;
            }
        }
        return !linkPanel.isEmpty() && !cover.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && Page.isLinkPanel(stack)) {
                return ItemLinkbookUnlinked.createItem(stack);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MystRecipes.LINKING_BOOK.get();
    }
}
