package com.xcompwiz.mystcraft.blockentity;

import com.xcompwiz.mystcraft.inventory.BookBinderMenu;
import com.xcompwiz.mystcraft.item.ItemAgebook;
import com.xcompwiz.mystcraft.linking.LinkOptions;
import com.xcompwiz.mystcraft.item.ItemPageContainer;
import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.registry.MystBlockEntities;
import com.xcompwiz.mystcraft.registry.MystItems;
import com.xcompwiz.mystcraft.registry.MystBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Legacy Book Binder storage model.
 *
 * <p>0.13.7.06 had exactly one real inventory slot (the cover) and an ordered,
 * effectively unbounded page list manipulated by the horizontal page slider.
 * Earlier port checkpoints temporarily represented pages as sixteen normal
 * inventory slots; this class restores the original separation while retaining
 * migration support for those checkpoint saves.</p>
 */
public final class BookBinderBlockEntity extends BlockEntity implements Container, MenuProvider {
    /** CP223 regression seed captured from Age 2 where the floating Snow Layer anomaly reproduced. */
    public static final int MAX_TITLE_LENGTH = 21;
    public static final int COVER_SLOT = 0;
    public static final int SLOT_COUNT = 1;
    private static final int PORT_MIGRATION_SLOT_COUNT = 17;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final List<ItemStack> pages = new ArrayList<>();
    private String pendingTitle = "";

    public BookBinderBlockEntity(BlockPos pos, BlockState state) {
        super(MystBlockEntities.BOOK_BINDER.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mystcraft.book_binder");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BookBinderMenu(containerId, playerInventory, this, this.worldPosition, this.pendingTitle, getPages());
    }

    public String getPendingTitle() { return pendingTitle; }

    public void setPendingTitle(String title) {
        String normalized = title == null ? "" : title;
        if (normalized.length() > MAX_TITLE_LENGTH) normalized = normalized.substring(0, MAX_TITLE_LENGTH);
        if (!pendingTitle.equals(normalized)) {
            pendingTitle = normalized;
            setChanged();
        }
    }

    public List<ItemStack> getPages() {
        List<ItemStack> out = new ArrayList<>(pages.size());
        for (ItemStack page : pages) out.add(page.copyWithCount(1));
        return out;
    }

    public int getPageCount() { return pages.size(); }

    public ItemStack getPage(int index) {
        return index >= 0 && index < pages.size() ? pages.get(index).copyWithCount(1) : ItemStack.EMPTY;
    }

    public boolean canBuildItem() {
        if (!isValidCover(items.get(COVER_SLOT))) return false;
        if (pendingTitle.isBlank()) return false;
        if (pages.isEmpty() || !Page.isLinkPanel(pages.getFirst())) return false;
        for (int i = 1; i < pages.size(); i++) if (Page.isLinkPanel(pages.get(i))) return false;
        return true;
    }

    /**
     * Legacy result preview. 0.13.7.06 exposed only a plain Agebook here; the
     * exact stack taken by the player was authored later at the pickup boundary.
     */
    public ItemStack getCraftedItem() {
        return canBuildItem() ? new ItemStack(MystItems.AGEBOOK.get()) : ItemStack.EMPTY;
    }

    /**
     * Authors the exact normal-pickup result stack from one immutable snapshot of
     * the current server-authoritative Binder state, then consumes the Binder.
     */
    public boolean buildItem(ItemStack result, Player player) {
        if (!canBuildItem() || result.isEmpty() || !result.is(MystItems.AGEBOOK.get())) return false;
        List<ItemStack> snapshot = snapshotPages();
        String titleSnapshot = pendingTitle;
        ItemAgebook.create(result, player, snapshot, titleSnapshot);
        consumeBuildInputs();
        return true;
    }

    /**
     * QUICK_MOVE must transfer a fully-authored stack before the Binder can be
     * consumed. The menu calls {@link #consumeBuiltItem()} only after transfer.
     */
    public ItemStack createBuiltItemSnapshot(Player player) {
        if (!canBuildItem()) return ItemStack.EMPTY;
        List<ItemStack> snapshot = snapshotPages();
        String titleSnapshot = pendingTitle;
        ItemStack result = new ItemStack(MystItems.AGEBOOK.get());
        ItemAgebook.create(result, player, snapshot, titleSnapshot);
        return result;
    }

    /** Consumes one cover plus the authored page/title state after a successful build. */
    public void consumeBuiltItem() {
        // createBuiltItemSnapshot() and this call run back-to-back on the single
        // server thread. Do not revalidate after the output has already moved;
        // revalidation failure at that point would duplicate the authored book.
        consumeBuildInputs();
    }

    private List<ItemStack> snapshotPages() {
        List<ItemStack> snapshot = new ArrayList<>(pages.size());
        for (ItemStack page : pages) snapshot.add(page.copyWithCount(1));
        return List.copyOf(snapshot);
    }

    private void consumeBuildInputs() {
        items.get(COVER_SLOT).shrink(1);
        if (items.get(COVER_SLOT).isEmpty()) items.set(COVER_SLOT, ItemStack.EMPTY);
        pages.clear();
        pendingTitle = "";
        setChanged();
    }

    public ItemStack insertPage(ItemStack source, int index, boolean single) {
        if (source.isEmpty()) return ItemStack.EMPTY;
        index = Math.max(0, Math.min(index, pages.size()));
        if (source.is(MystItems.FOLDER.get()) && source.getItem() instanceof ItemPageContainer container) {
            // Legacy insertFromFolder was bidirectional. An empty Folder receives
            // Binder pages; a populated Folder inserts its pages in slot order.
            if (container.isEmpty(source)) {
                int moved = Math.min(pages.size(), container.pageCapacity());
                if (moved > 0) {
                    List<ItemStack> transfer = new ArrayList<>(moved);
                    for (int i = 0; i < moved; i++) transfer.add(pages.get(i).copyWithCount(1));
                    container.setPages(source, transfer);
                    pages.subList(0, moved).clear();
                    setChanged();
                }
                return source;
            }

            int insertion = index;
            while (!container.isEmpty(source)) {
                ItemStack page = container.removeFirst(source);
                if (page.isEmpty()) break;
                pages.add(insertion++, page.copyWithCount(1));
            }
            setChanged();
            return source;
        }
        if (!isValidPageInput(source)) return source;

        int toMove = single ? 1 : source.getCount();
        while (toMove-- > 0 && !source.isEmpty()) {
            ItemStack page;
            if (source.is(Items.PAPER)) page = Page.createPage();
            else page = source.copyWithCount(1);
            pages.add(index++, page);
            source.shrink(1);
        }
        setChanged();
        return source;
    }

    public ItemStack removePage(int index) {
        if (index < 0 || index >= pages.size()) return ItemStack.EMPTY;
        ItemStack out = pages.remove(index);
        setChanged();
        return out;
    }

    public int importPageContainer(ItemStack source) {
        if (!(source.getItem() instanceof ItemPageContainer container)) return 0;
        int moved = 0;
        while (!container.isEmpty(source)) {
            ItemStack page = container.removeFirst(source);
            if (page.isEmpty()) break;
            pages.add(page.copyWithCount(1));
            moved++;
        }
        if (moved > 0) setChanged();
        return moved;
    }

    public static boolean isValidCover(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.LEATHER)) return true;
        if (stack.is(MystItems.FOLDER.get()) && stack.getItem() instanceof ItemPageContainer container) {
            // InventoryFolder.isEmpty in 0.13.7.06 treated a legacy Name tag as
            // non-empty even when the Folder contained no pages.
            return container.isEmpty(stack) && container.getLegacyName(stack).isEmpty();
        }
        return false;
    }

    public static boolean isValidPageInput(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(MystItems.PAGE.get()) || stack.is(Items.PAPER));
    }

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { return items.getFirst().isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == COVER_SLOT ? items.getFirst() : ItemStack.EMPTY; }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != COVER_SLOT) return ItemStack.EMPTY;
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return slot == COVER_SLOT ? ContainerHelper.takeItem(items, slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == COVER_SLOT) items.set(COVER_SLOT, isValidCover(stack) ? stack : ItemStack.EMPTY);
        setChanged();
    }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == COVER_SLOT && isValidCover(stack); }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        items.clear();
        pages.clear();
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        pages.clear();

        // Read the modern one-slot representation first.
        ContainerHelper.loadAllItems(tag, items, registries);
        pendingTitle = tag.getString("title");

        if (tag.contains("pages", Tag.TAG_LIST)) {
            ListTag list = tag.getList("pages", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack parsed = ItemStack.parseOptional(registries, list.getCompound(i));
                if (!parsed.isEmpty()) pages.add(parsed.copyWithCount(1));
            }
        } else if (tag.contains("Items", Tag.TAG_LIST)) {
            // I82-I91 migration: page inventory slots 1..16 were serialized in Items.
            NonNullList<ItemStack> legacy = NonNullList.withSize(PORT_MIGRATION_SLOT_COUNT, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, legacy, registries);
            if (items.getFirst().isEmpty() && !legacy.getFirst().isEmpty()) items.set(0, legacy.getFirst());
            for (int i = 1; i < legacy.size(); i++) if (!legacy.get(i).isEmpty()) pages.add(legacy.get(i).copyWithCount(1));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        if (!pendingTitle.isEmpty()) tag.putString("title", pendingTitle);
        ListTag list = new ListTag();
        for (ItemStack page : pages) {
            Tag saved = page.saveOptional(registries);
            if (saved instanceof CompoundTag compound) list.add(compound);
        }
        tag.put("pages", list);
    }
}
