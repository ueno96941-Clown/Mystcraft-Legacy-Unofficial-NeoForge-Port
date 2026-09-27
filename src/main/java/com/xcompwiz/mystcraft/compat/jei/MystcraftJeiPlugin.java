package com.xcompwiz.mystcraft.compat.jei;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.recipe.LinkingBookRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.resources.ResourceLocation;

/**
 * Optional JEI integration for Mystcraft's legacy special crafting recipes.
 *
 * <p>The real Linking Book recipe is a {@link net.minecraft.world.item.crafting.CustomRecipe}
 * because the exact Link Panel payload must be copied to the output. JEI cannot infer that
 * payload-aware ingredient/output layout on its own, so this plugin supplies the visual
 * extension while leaving the actual server-side recipe untouched.</p>
 */
@JeiPlugin
public final class MystcraftJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(
                LinkingBookRecipe.class,
                new LinkingBookJeiExtension());
    }
}
