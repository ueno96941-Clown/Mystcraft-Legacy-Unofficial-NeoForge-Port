package com.xcompwiz.mystcraft.item;

import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.registry.MystItems;
import com.xcompwiz.mystcraft.symbol.SymbolRemappings;
import com.xcompwiz.mystcraft.util.LegacyItemData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern 1.21.1 page-container base used by Folder and Portfolio.
 *
 * <p>The port intentionally uses the vanilla {@link DataComponents#CONTAINER}
 * component rather than recreating the old ad-hoc numbered NBT inventory. This
 * keeps nested ItemStack data registry-aware and lets vanilla copy/network the
 * contents safely.</p>
 *
 * <p>Folder and Portfolio expose their legacy page-management semantics through
 * the dedicated Writing Desk and Book Binder surfaces. Direct use remains as a
 * lightweight convenience path for inserting and retrieving pages.</p>
 */
public abstract class ItemPageContainer extends Item {
    private final int pageCapacity;

    protected ItemPageContainer(Properties properties, int pageCapacity) {
        super(properties);
        if (pageCapacity <= 0 || pageCapacity > 256) {
            throw new IllegalArgumentException("pageCapacity must be in 1..256");
        }
        this.pageCapacity = pageCapacity;
    }

    public final int pageCapacity() {
        return pageCapacity;
    }

    /** Whether this container keeps explicit slot holes like the legacy Folder. */
    protected boolean preserveSparseSlots() { return false; }

    public final List<ItemStack> getPages(ItemStack container) {
        if (container.isEmpty()) return List.of();
        List<ItemStack> pages = new ArrayList<>();
        ItemContainerContents contents = container.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (preserveSparseSlots()) {
            contents.stream().limit(pageCapacity).forEach(stack ->
                    pages.add(stack.isEmpty() || !isPageInput(stack) ? ItemStack.EMPTY : normalizePage(stack)));
            List<ItemStack> remapped = remapSparsePages(pages);
            if (!samePages(pages, remapped)) setPagesRaw(container, remapped);
            return remapped;
        }

        contents.nonEmptyStream()
                .filter(ItemPageContainer::isPageInput)
                .limit(pageCapacity)
                .forEach(stack -> pages.add(stack.copyWithCount(1)));
        List<ItemStack> remapped = SymbolRemappings.remapPages(pages);
        if (remapped.size() > pageCapacity) remapped = new ArrayList<>(remapped.subList(0, pageCapacity));
        if (!samePages(pages, remapped)) setPagesRaw(container, remapped);
        return remapped;
    }

    private List<ItemStack> remapSparsePages(List<ItemStack> source) {
        List<ItemStack> result = new ArrayList<>(source);
        for (int slot = 0; slot < source.size(); slot++) {
            ItemStack page = source.get(slot);
            if (page.isEmpty()) continue;
            List<ItemStack> mapped = SymbolRemappings.remapPage(page);
            result.set(slot, mapped.isEmpty() ? ItemStack.EMPTY : mapped.getFirst());
            for (int i = 1; i < mapped.size(); i++) {
                int target = firstEmptySlot(result);
                if (target < 0 || target >= pageCapacity) break;
                while (result.size() <= target) result.add(ItemStack.EMPTY);
                result.set(target, mapped.get(i));
            }
        }
        trimToCapacity(result);
        return result;
    }

    private static int firstEmptySlot(List<ItemStack> pages) {
        for (int i = 0; i < pages.size(); i++) if (pages.get(i).isEmpty()) return i;
        return pages.size();
    }

    private void trimToCapacity(List<ItemStack> pages) {
        if (pages.size() > pageCapacity) pages.subList(pageCapacity, pages.size()).clear();
    }

    public final int getPageCount(ItemStack container) {
        if (!preserveSparseSlots()) return getPages(container).size();
        int count = 0;
        for (ItemStack page : getPages(container)) if (!page.isEmpty()) count++;
        return count;
    }

    /** Legacy Folder/Portfolio display name stored under the original "Name" key. */
    public final String getLegacyName(ItemStack container) {
        if (container.isEmpty()) return "";
        return LegacyItemData.copy(container).getString("Name");
    }

    /** Restores IItemRenameable semantics used by the Writing Desk. */
    public final void setLegacyName(ItemStack container, String name) {
        if (container.isEmpty()) return;
        String normalized = name == null ? "" : name;
        LegacyItemData.update(container, data -> {
            if (normalized.isEmpty()) data.remove("Name");
            else data.putString("Name", normalized);
        });
    }

    public final boolean isEmpty(ItemStack container) {
        return getPageCount(container) == 0;
    }

    /** Replaces the content with valid one-count pages, preserving their order. */
    public final void setPages(ItemStack container, List<ItemStack> pages) {
        if (container.isEmpty()) return;
        List<ItemStack> normalized = new ArrayList<>(Math.min(pageCapacity, pages.size()));
        for (ItemStack page : pages) {
            if (normalized.size() >= pageCapacity) break;
            if (preserveSparseSlots()) {
                normalized.add(page == null || page.isEmpty() || !isPageInput(page) ? ItemStack.EMPTY : normalizePage(page));
            } else if (isPageInput(page)) {
                normalized.add(normalizePage(page));
            }
        }
        setPagesRaw(container, normalized);
    }

    private void setPagesRaw(ItemStack container, List<ItemStack> pages) {
        container.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(pages));
    }

    private static boolean samePages(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.isSameItemSameComponents(a.get(i), b.get(i))) return false;
        }
        return true;
    }

    /**
     * Consumes up to the remaining capacity from {@code source} and returns the
     * number of pages inserted. Paper is converted to a blank Mystcraft page.
     */
    public final int insert(ItemStack container, ItemStack source) {
        if (container.isEmpty() || source.isEmpty() || !isPageInput(source)) return 0;
        List<ItemStack> pages = new ArrayList<>(getPages(container));
        if (preserveSparseSlots()) {
            int moved = 0;
            while (!source.isEmpty() && moved < pageCapacity) {
                int slot = firstEmptySlot(pages);
                if (slot >= pageCapacity) break;
                while (pages.size() <= slot) pages.add(ItemStack.EMPTY);
                pages.set(slot, normalizePage(source));
                source.shrink(1);
                moved++;
            }
            if (moved > 0) setPages(container, pages);
            return moved;
        }
        int available = pageCapacity - pages.size();
        if (available <= 0) return 0;
        int moved = Math.min(available, source.getCount());
        for (int i = 0; i < moved; i++) pages.add(normalizePage(source));
        source.shrink(moved);
        setPages(container, pages);
        return moved;
    }


    /** Removes one page at an exact ordered index. Used by the Writing Desk surface. */
    public final ItemStack removeAt(ItemStack container, int index) {
        List<ItemStack> pages = new ArrayList<>(getPages(container));
        if (index < 0 || index >= pages.size()) return ItemStack.EMPTY;
        ItemStack out = pages.get(index);
        if (preserveSparseSlots()) pages.set(index, ItemStack.EMPTY); else pages.remove(index);
        setPages(container, pages);
        return out;
    }

    /**
     * Legacy Folder setPage semantics used by the Writing Desk slider/surface.
     * One page is taken from {@code source}; replacing an occupied slot returns
     * the previous page only when the carried stack is otherwise exhausted.
     */
    public final ItemStack setAtLegacy(ItemStack container, ItemStack source, int index) {
        if (container.isEmpty() || source.isEmpty() || !isPageInput(source) || index < 0 || index >= pageCapacity) return source;
        List<ItemStack> pages = new ArrayList<>(getPages(container));
        if (preserveSparseSlots()) {
            while (pages.size() <= index) pages.add(ItemStack.EMPTY);
            ItemStack previous = pages.get(index);
            // Exact legacy InventoryFolder#setItem rule: an occupied slot cannot
            // be replaced while the incoming stack still has another item left.
            if (!previous.isEmpty() && source.getCount() > 1) return source;
            ItemStack inserted = normalizePage(source);
            source.shrink(1);
            pages.set(index, inserted);
            setPages(container, pages);
            return source.isEmpty() ? previous : source;
        }
        if (index > pages.size()) return source;
        ItemStack inserted = normalizePage(source);
        ItemStack previous = index < pages.size() ? pages.get(index) : ItemStack.EMPTY;
        if (!previous.isEmpty() && source.getCount() > 1) return source;
        source.shrink(1);
        if (index < pages.size()) pages.set(index, inserted); else pages.add(inserted);
        setPages(container, pages);
        return !previous.isEmpty() && source.isEmpty() ? previous : source;
    }

    /**
     * Inserts pages at an exact ordered index and returns the amount moved.
     * Portfolio callers intentionally ignore the supplied index and append instead,
     * matching the legacy unordered collection semantics.
     */
    public final int insertAt(ItemStack container, ItemStack source, int index, int limit) {
        if (container.isEmpty() || source.isEmpty() || !isPageInput(source) || limit <= 0) return 0;
        List<ItemStack> pages = new ArrayList<>(getPages(container));
        int available = pageCapacity - pages.size();
        if (available <= 0) return 0;
        int moved = Math.min(Math.min(available, source.getCount()), limit);
        int at = Math.max(0, Math.min(index, pages.size()));
        for (int i = 0; i < moved; i++) {
            pages.add(at + i, normalizePage(source));
        }
        source.shrink(moved);
        setPages(container, pages);
        return moved;
    }

    /** Removes up to {@code limit} pages matching the page at {@code index}. */
    public final ItemStack removeMatching(ItemStack container, int index, int limit) {
        List<ItemStack> pages = new ArrayList<>(getPages(container));
        if (index < 0 || index >= pages.size() || limit <= 0) return ItemStack.EMPTY;
        ItemStack sample = pages.get(index);
        ItemStack out = sample.copyWithCount(0);
        int removed = 0;
        for (int i = pages.size() - 1; i >= 0 && removed < limit; i--) {
            if (ItemStack.isSameItemSameComponents(sample, pages.get(i))) {
                pages.remove(i);
                removed++;
            }
        }
        if (removed <= 0) return ItemStack.EMPTY;
        out.setCount(removed);
        setPages(container, pages);
        return out;
    }

    /** Removes the oldest page, used by Book Binder ordered import. */
    public final ItemStack removeFirst(ItemStack container) {
        List<ItemStack> pages = new ArrayList<>(getPages(container));
        if (preserveSparseSlots()) {
            for (int i = 0; i < pages.size(); i++) if (!pages.get(i).isEmpty()) return removeAt(container, i);
            return ItemStack.EMPTY;
        }
        if (pages.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = pages.removeFirst();
        setPages(container, pages);
        return out;
    }

    /** Removes the newest page for simple hand interaction. */
    public final ItemStack removeLast(ItemStack container) {
        List<ItemStack> pages = new ArrayList<>(getPages(container));
        if (preserveSparseSlots()) {
            for (int i = pages.size() - 1; i >= 0; i--) if (!pages.get(i).isEmpty()) return removeAt(container, i);
            return ItemStack.EMPTY;
        }
        if (pages.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = pages.removeLast();
        setPages(container, pages);
        return out;
    }

    public static boolean isPageContainer(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemPageContainer;
    }

    public static boolean isEmptyContainer(ItemStack stack) {
        return stack.getItem() instanceof ItemPageContainer container && container.isEmpty(stack);
    }

    private static boolean isPageInput(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(MystItems.PAGE.get()) || stack.is(Items.PAPER));
    }

    private static ItemStack normalizePage(ItemStack source) {
        if (source.is(Items.PAPER)) return Page.createPage();
        return source.copyWithCount(1);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack container = player.getItemInHand(hand);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack other = player.getItemInHand(otherHand);

        if (isPageInput(other)) {
            if (!level.isClientSide) {
                int moved = insert(container, other);
                if (other.isEmpty()) player.setItemInHand(otherHand, ItemStack.EMPTY);
                if (moved > 0) {
                    player.displayClientMessage(Component.translatable(
                            "message.mystcraft.page_container_inserted", moved, getPageCount(container), pageCapacity), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.mystcraft.page_container_full"), true);
                }
            }
            return InteractionResultHolder.sidedSuccess(container, level.isClientSide);
        }

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                ItemStack extracted = removeLast(container);
                if (!extracted.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(extracted);
                } else {
                    player.displayClientMessage(Component.translatable("message.mystcraft.page_container_empty"), true);
                }
            }
            return InteractionResultHolder.sidedSuccess(container, level.isClientSide);
        }

        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(
                    "message.mystcraft.page_container_count", getPageCount(container), pageCapacity), true);
        }
        return InteractionResultHolder.sidedSuccess(container, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        String legacyName = getLegacyName(stack);
        if (!legacyName.isEmpty()) {
            tooltipComponents.add(Component.literal(legacyName).withStyle(ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.translatable(
                "tooltip.mystcraft.page_container_count", getPageCount(stack), pageCapacity).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.mystcraft.page_container_controls").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
