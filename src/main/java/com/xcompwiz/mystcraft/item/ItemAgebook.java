package com.xcompwiz.mystcraft.item;

import com.xcompwiz.mystcraft.linking.LinkOptions;
import com.xcompwiz.mystcraft.api.item.IItemRenameable;
import com.xcompwiz.mystcraft.api.item.IItemWritable;
import com.xcompwiz.mystcraft.api.item.IItemPageProvider;
import net.minecraft.resources.ResourceLocation;
import com.xcompwiz.mystcraft.linking.LinkProperties;
import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.symbol.SymbolRemappings;
import com.xcompwiz.mystcraft.util.LegacyItemData;
import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Compatibility-first Descriptive Book implementation.
 *
 * <p>This ports the book payload and page/author behavior from 0.13.7.06.
 * Established destinations can now use the Stage-1B book GUI/link transport.
 * Age allocation and visited-state locking remain delegated to the world-data milestone.</p>
 */
public class ItemAgebook extends ItemLinking implements IItemRenameable, IItemWritable, IItemPageProvider {
    public static final String KEY_PAGES = "Pages";
    public static final String KEY_AUTHORS = "Authors";

    public ItemAgebook(Properties properties) {
        super(properties);
    }

    @Override
    public String getDisplayName(Player player, ItemStack stack) { return LinkOptions.getDisplayName(stack); }

    @Override
    public void setDisplayName(Player player, ItemStack stack, String name) {
        LinkOptions.setDisplayName(stack, name == null ? "" : name);
        if (player instanceof ServerPlayer serverPlayer) {
            var server = serverPlayer.getServer();
            if (server != null) {
                AgeManager.resolveReservation(server, stack).ifPresent(age -> {
                    age.setAgeName(name == null ? "" : name);
                    com.xcompwiz.mystcraft.world.agedata.AgeRegistryData.flush(server);
                });
            }
        }
    }

    @Override
    public boolean writeSymbol(Player player, ItemStack stack, ResourceLocation symbol) {
        return symbol != null && writeSymbolLegacy(stack, player.level().registryAccess(), player, symbol.toString());
    }

    @Override
    public List<ItemStack> getPageList(Player player, ItemStack stack) {
        HolderLookup.Provider registries = player != null ? player.level().registryAccess() : null;
        if (registries != null) return getPageList(stack, registries);
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null ? Collections.emptyList() : getPageList(stack, server.registryAccess());
    }

    @Override
    protected void initialize(Level level, ItemStack stack, Entity entity) {
        if (LegacyItemData.hasData(stack)) return;

        // Legacy fallback behavior: an otherwise empty Descriptive Book gets
        // Generate Platform and one plain Link Panel page.
        LinkOptions.setFlag(stack, LinkProperties.GENERATE_PLATFORM, true);
        if (level != null) {
            addPages(stack, level.registryAccess(), Collections.singleton(Page.createLinkPage()));
        }
    }

    @Override
    protected void validate(Level level, ItemStack stack, Entity entity) {
        super.validate(level, stack, entity);
        if (!hasPageList(stack)) {
            addPages(stack, level.registryAccess(), Collections.singleton(Page.createLinkPage()));
        }
    }

    /**
     * Mirrors the legacy Book Binder result initialization: write pages,
     * author and title, then apply the first Link Panel's flags to the book.
     */
    public static void create(ItemStack agebook, Player player, List<ItemStack> pages, String pendingTitle) {
        LegacyItemData.set(agebook, new CompoundTag());

        ItemAgebook item = (ItemAgebook) agebook.getItem();
        HolderLookup.Provider registries = player.level().registryAccess();
        item.addPages(agebook, registries, pages);
        item.addAuthor(agebook, player.getName().getString());
        LinkOptions.setDisplayName(agebook, pendingTitle == null ? "" : pendingTitle);

        if (!pages.isEmpty() && Page.isLinkPanel(pages.getFirst())) {
            Page.applyLinkPanel(pages.getFirst(), agebook);
        }
    }

    /**
     * Legacy definition of a not-yet-created Age book, adapted to modern
     * dimension keys: no target dimension yet and first page is a Link Panel.
     */
    public static boolean isNewAgebook(ItemStack stack, HolderLookup.Provider registries) {
        if (!(stack.getItem() instanceof ItemAgebook)) return false;
        if (!LegacyItemData.hasData(stack)) return false;
        if (LinkOptions.getDimensionKey(stack) != null || LinkOptions.getLegacyDimensionId(stack) != null) return false;

        List<ItemStack> pages = ((ItemAgebook) stack.getItem()).getPageList(stack, registries);
        return !pages.isEmpty() && Page.isLinkPanel(pages.getFirst());
    }

    public void addPages(ItemStack stack, HolderLookup.Provider registries, Collection<ItemStack> pages) {
        if (stack.isEmpty() || pages.isEmpty()) return;
        List<ItemStack> combined = new ArrayList<>(getPageList(stack, registries));
        for (ItemStack page : pages) {
            if (!page.isEmpty()) combined.add(page.copy());
        }
        setPageList(stack, registries, combined);
    }

    public List<ItemStack> getPageList(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) return Collections.emptyList();
        CompoundTag data = LegacyItemData.copy(stack);
        if (!data.contains(KEY_PAGES, Tag.TAG_LIST)) return Collections.emptyList();

        ListTag list = data.getList(KEY_PAGES, Tag.TAG_COMPOUND);
        List<ItemStack> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            ItemStack page = ItemStack.parseOptional(registries, list.getCompound(i));
            if (!page.isEmpty()) result.add(page);
        }

        // 0.13.7.06 migrated obsolete symbol pages lazily when books were loaded.
        // Keep the same compatibility boundary so imported books remain usable.
        List<ItemStack> remapped = SymbolRemappings.remapPages(result);
        if (!samePages(result, remapped)) {
            setPageList(stack, registries, remapped);
        }
        return remapped;
    }

    public void setPageList(ItemStack stack, HolderLookup.Provider registries, Collection<ItemStack> pages) {
        ListTag encoded = new ListTag();
        for (ItemStack page : pages) {
            if (page.isEmpty()) continue;
            Tag saved = page.saveOptional(registries);
            if (saved instanceof CompoundTag compound) {
                encoded.add(compound);
            }
        }
        LegacyItemData.update(stack, data -> data.put(KEY_PAGES, encoded));
    }

    public void addAuthor(ItemStack stack, String author) {
        if (author == null || author.isBlank()) return;
        LegacyItemData.update(stack, data -> {
            ListTag authors = data.getList(KEY_AUTHORS, Tag.TAG_STRING);
            for (int i = 0; i < authors.size(); i++) {
                if (author.equals(authors.getString(i))) return;
            }
            authors.add(StringTag.valueOf(author));
            data.put(KEY_AUTHORS, authors);
        });
    }

    public Collection<String> getAuthors(ItemStack stack) {
        CompoundTag data = LegacyItemData.copy(stack);
        ListTag authors = data.getList(KEY_AUTHORS, Tag.TAG_STRING);
        List<String> result = new ArrayList<>(authors.size());
        for (int i = 0; i < authors.size(); i++) {
            result.add(authors.getString(i));
        }
        return Collections.unmodifiableList(result);
    }

    public boolean writeSymbol(ItemStack stack, HolderLookup.Provider registries, Player player, net.minecraft.resources.ResourceLocation symbol) {
        return symbol != null && writeSymbolLegacy(stack, registries, player, symbol.toString());
    }

    /** Writes the exact persisted 0.13.7.06 Symbol ID string without ResourceLocation normalization. */
    public boolean writeSymbolLegacy(ItemStack stack, HolderLookup.Provider registries, Player player, String legacySymbolId) {
        if (legacySymbolId == null || legacySymbolId.isBlank()) return false;
        // Legacy ItemAgebook refused all page writing once the target Age had been
        // visited. Keep the book's authored definition frozen at the same visible
        // lifecycle boundary rather than allowing a page mutation that cannot
        // safely rewrite an already-generated runtime dimension.
        if (player instanceof ServerPlayer serverPlayer) {
            var server = serverPlayer.getServer();
            if (server != null && AgeManager.resolveReservation(server, stack)
                    .map(age -> age.visited())
                    .orElse(false)) {
                return false;
            }
        }

        List<ItemStack> pages = getPageList(stack, registries);
        for (ItemStack page : pages) {
            if (Page.isBlank(page)) {
                Page.setSymbolId(page, legacySymbolId);
                setPageList(stack, registries, pages);
                addAuthor(stack, player.getName().getString());
                return true;
            }
        }
        return false;
    }

    private static boolean samePages(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.isSameItemSameComponents(a.get(i), b.get(i))) return false;
        }
        return true;
    }

    private boolean hasPageList(ItemStack stack) {
        return LegacyItemData.copy(stack).contains(KEY_PAGES, Tag.TAG_LIST);
    }
}
