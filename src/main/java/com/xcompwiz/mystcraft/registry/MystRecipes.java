package com.xcompwiz.mystcraft.registry;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.recipe.LinkingBookRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Recipe serializers which were code recipes rather than legacy JSON recipes. */
public final class MystRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Mystcraft.MOD_ID);

    public static final Supplier<RecipeSerializer<LinkingBookRecipe>> LINKING_BOOK = SERIALIZERS.register(
            "internal/linkingbook", () -> new SimpleCraftingRecipeSerializer<>(LinkingBookRecipe::new));

    private MystRecipes() {}

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }
}
