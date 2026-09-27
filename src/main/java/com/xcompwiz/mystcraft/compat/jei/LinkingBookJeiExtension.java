package com.xcompwiz.mystcraft.compat.jei;

import com.xcompwiz.mystcraft.item.ItemLinkbookUnlinked;
import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.recipe.LinkingBookRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * Tells JEI how to display the payload-aware legacy Linking Book recipe.
 *
 * <p>Displayed inputs intentionally use a real Link Panel stack rather than a blank Page,
 * because the runtime recipe accepts only {@link Page#isLinkPanel(ItemStack)} pages. The
 * output is built through the same helper used by the actual recipe so JEI shows a valid
 * unlinked Linking Book carrying the Link Panel payload.</p>
 */
public final class LinkingBookJeiExtension implements ICraftingCategoryExtension<LinkingBookRecipe> {
    @Override
    public void setRecipe(
            RecipeHolder<LinkingBookRecipe> recipeHolder,
            IRecipeLayoutBuilder builder,
            ICraftingGridHelper craftingGridHelper,
            IFocusGroup focuses) {

        ItemStack linkPanel = Page.createLinkPage();
        ItemStack leather = new ItemStack(Items.LEATHER);
        ItemStack output = ItemLinkbookUnlinked.createItem(linkPanel);

        craftingGridHelper.createAndSetInputs(
                builder,
                List.of(List.of(linkPanel), List.of(leather)),
                0,
                0);
        craftingGridHelper.createAndSetOutputs(builder, List.of(output));
        builder.setShapeless();
    }
}
