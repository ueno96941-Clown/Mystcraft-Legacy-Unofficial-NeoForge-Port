package com.xcompwiz.mystcraft.registry;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.item.ItemPortfolio;
import com.xcompwiz.mystcraft.item.ItemWritingDesk;
import com.xcompwiz.mystcraft.linking.LinkProperties;
import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.symbol.LegacyBiomeSymbolRegistry;
import com.xcompwiz.mystcraft.symbol.LegacyMaterialSymbolRegistry;
import com.xcompwiz.mystcraft.symbol.SymbolAvailability;
import com.xcompwiz.mystcraft.symbol.SymbolRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Legacy-style Mystcraft creative inventory split.
 *
 * <p>0.13.7.06 exposed ordinary Mystcraft items/blocks in a common tab and
 * Page variants in a dedicated searchable tab. Keeping those surfaces separate
 * avoids burying the usable items under the very large fixed/dynamic Symbol set.</p>
 */
public final class MystCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Mystcraft.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COMMON = TABS.register("common", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mystcraft.common"))
                    .icon(() -> MystItems.AGEBOOK.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // Keep the practical item/block shelf compact, as in the legacy common tab.
                        output.accept(MystItems.AGEBOOK.get());
                        output.accept(MystItems.UNLINKED_BOOK.get());
                        output.accept(MystItems.BOOSTER.get());
                        output.accept(MystItems.FOLDER.get());
                        output.accept(MystItems.PORTFOLIO.get());
                        output.accept(MystItems.WRITING_DESK_ITEM.get());
                        output.accept(ItemWritingDesk.topStack());
                        output.accept(MystItems.INK_VIAL.get());
                        output.accept(MystItems.INK_MIXER_BLOCK.get());
                        output.accept(MystItems.BOOK_BINDER_BLOCK.get());
                        output.accept(MystItems.BOOK_RECEPTACLE_BLOCK.get());
                        output.accept(MystItems.BOOK_STAND_BLOCK.get());
                        output.accept(MystItems.LECTERN_BLOCK.get());
                        output.accept(MystItems.LINK_MODIFIER_BLOCK.get());
                        output.accept(MystItems.CRYSTAL_BLOCK.get());

                        // Legacy common tab appended an "All Symbols" Portfolio after normal items.
                        output.accept(createAllSymbolsPortfolio());
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PAGES = TABS.register("pages", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mystcraft.pages"))
                    .icon(Page::createLinkPage)
                    .withSearchBar()
                    .displayItems((parameters, output) -> {
                        output.accept(Page.createLinkPage());

                        // Legacy ItemPage sorted link properties alphabetically and omitted Relative.
                        List<String> properties = new ArrayList<>(LinkProperties.LEGACY_CREATIVE_LINK_PANELS);
                        properties.sort(String.CASE_INSENSITIVE_ORDER);
                        for (String property : properties) output.accept(Page.createLinkPage(property));

                        // Legacy Page tab sorted Symbols by localized display name on the client.
                        // Build ItemStacks first so modern/dynamic Symbols follow the same visible ordering.
                        collectSortedSymbolPages().forEach(output::accept);
                    })
                    .build());


    private static ItemStack createAllSymbolsPortfolio() {
        ItemStack portfolio = new ItemStack(MystItems.PORTFOLIO.get());
        if (!(portfolio.getItem() instanceof ItemPortfolio item)) return portfolio;
        item.setLegacyName(portfolio, Component.translatable("myst.creative.notebook.all").getString());
        item.setPages(portfolio, collectSortedSymbolPages());
        return portfolio;
    }

    private static List<ItemStack> collectSortedSymbolPages() {
        // CP331: dynamic fluids are discovered lazily when the Page surface is actually requested.
        // This removes the old eager common-setup registry scan while preserving JEI/Creative visibility.
        LegacyMaterialSymbolRegistry.bootstrapRuntimeFluids();
        List<ItemStack> pages = new ArrayList<>();
        for (var symbol : SymbolRegistry.values()) {
            if (SymbolAvailability.isCreativeSelectable(symbol.legacyId())) pages.add(Page.createSymbolPage(symbol.legacyId()));
        }
        for (var symbol : LegacyMaterialSymbolRegistry.values()) {
            if (SymbolAvailability.isSelectable(symbol.legacyId())) pages.add(Page.createSymbolPage(symbol.legacyId()));
        }
        for (var symbol : LegacyBiomeSymbolRegistry.values()) {
            if (SymbolAvailability.isSelectable(symbol.legacyId())) pages.add(Page.createSymbolPage(symbol.legacyId()));
        }
        pages.sort(Comparator.comparing(stack -> stack.getHoverName().getString(), String.CASE_INSENSITIVE_ORDER));
        return pages;
    }

    private MystCreativeTabs() {}

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
