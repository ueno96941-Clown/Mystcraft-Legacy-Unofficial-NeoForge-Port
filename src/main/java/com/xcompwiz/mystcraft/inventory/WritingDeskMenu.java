package com.xcompwiz.mystcraft.inventory;

import com.xcompwiz.mystcraft.blockentity.WritingDeskBlockEntity;
import com.xcompwiz.mystcraft.item.ItemAgebook;
import com.xcompwiz.mystcraft.item.ItemFolder;
import com.xcompwiz.mystcraft.item.ItemLinking;
import com.xcompwiz.mystcraft.item.ItemPageContainer;
import com.xcompwiz.mystcraft.linking.LinkOptions;
import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.registry.MystItems;
import com.xcompwiz.mystcraft.registry.MystMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Server-authoritative 1.21.1 bridge for the 0.13.7.06 Writing Desk UI.
 *
 * <p>I89 restores the remaining target-side Writing Desk contract on top of the
 * I88 page surface: renameable targets, linked-book activation, book/page preview
 * navigation and the legacy ordered Folder page slider.</p>
 */
public final class WritingDeskMenu extends AbstractContainerMenu {
    public static final int VISIBLE_TAB_SLOTS = 4;
    public static final int TAB_MENU_START = 0;
    public static final int WORK_MENU_START = VISIBLE_TAB_SLOTS;
    public static final int PLAYER_MENU_START = WORK_MENU_START + WritingDeskBlockEntity.WORK_SLOT_COUNT;
    public static final int PLAYER_MENU_END = PLAYER_MENU_START + 36;

    // Small fixed GUI actions.
    public static final int ACTION_TAB_UP = 0;
    public static final int ACTION_TAB_DOWN = 1;
    public static final int ACTION_TARGET_PREVIOUS = 2;
    public static final int ACTION_TARGET_NEXT = 3;
    public static final int ACTION_TARGET_LINK = 4;
    public static final int ACTION_TARGET_TAKE_PAGE = 5;
    public static final int ACTION_TARGET_SET_PAGE = 6;
    public static final int ACTION_TARGET_SET_PAGE_SINGLE = 7;
    public static final int ACTION_SELECT_TAB = 10;

    // Standard inventory-button packet action ranges used by the page surface.
    public static final int ACTION_SURFACE_REMOVE_ONE = 1000;
    public static final int ACTION_SURFACE_REMOVE_STACK = 2000;
    public static final int ACTION_SURFACE_INSERT_ALL = 3000;
    public static final int ACTION_SURFACE_INSERT_ONE = 4000;
    public static final int ACTION_SURFACE_COPY_SYMBOL = 5000;
    public static final int ACTION_RANGE = 1000;

    private final Container desk;
    private final Player player;
    private final BlockPos pos;
    private int activeTab;
    private int firstTab;
    private int inkAmount;
    private int currentTargetPage;
    private int linkPermitted;

    private final DataSlot activeTabData = new DataSlot() {
        @Override public int get() { return activeTab; }
        @Override public void set(int value) { activeTab = clampTab(value); }
    };
    private final DataSlot firstTabData = new DataSlot() {
        @Override public int get() { return firstTab; }
        @Override public void set(int value) { firstTab = clampFirst(value); }
    };
    private final DataSlot inkData = new DataSlot() {
        @Override public int get() {
            return desk instanceof WritingDeskBlockEntity blockEntity ? blockEntity.inkAmount() : inkAmount;
        }
        @Override public void set(int value) {
            inkAmount = Math.max(0, Math.min(WritingDeskBlockEntity.CAPACITY, value));
        }
    };
    private final DataSlot currentPageData = new DataSlot() {
        @Override public int get() {
            currentTargetPage = clampTargetPage(currentTargetPage);
            return currentTargetPage;
        }
        @Override public void set(int value) { currentTargetPage = clampTargetPage(value); }
    };
    private final DataSlot linkPermittedData = new DataSlot() {
        @Override public int get() {
            if (desk instanceof WritingDeskBlockEntity blockEntity) {
                linkPermitted = blockEntity.canLinkTarget(player) ? 1 : 0;
            }
            return linkPermitted;
        }
        @Override public void set(int value) { linkPermitted = value != 0 ? 1 : 0; }
    };

    /** Client constructor used by IMenuTypeExtension. */
    public WritingDeskMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, new SimpleContainer(WritingDeskBlockEntity.SLOT_COUNT), buffer.readBlockPos());
    }

    /** Server constructor used by WritingDeskBlockEntity. */
    public WritingDeskMenu(int containerId, Inventory inventory, Container desk, BlockPos pos) {
        super(MystMenus.WRITING_DESK.get(), containerId);
        checkContainerSize(desk, WritingDeskBlockEntity.SLOT_COUNT);
        this.desk = desk;
        this.player = inventory.player;
        this.pos = pos;

        desk.startOpen(inventory.player);

        // Legacy UI only exposed four notebook tabs at a time although 25 were stored.
        for (int view = 0; view < VISIBLE_TAB_SLOTS; view++) {
            this.addSlot(new SurfaceTabSlot(new TabProxyContainer(view), 0, 37, 34 + view * 37));
        }

        // Work slots: target, paper, fluid container, output.
        this.addSlot(new DeskSlot(desk, WritingDeskBlockEntity.TARGET_SLOT, 241, 80, 1));
        this.addSlot(new DeskSlot(desk, WritingDeskBlockEntity.PAPER_SLOT, 241, 28, 64));
        this.addSlot(new DeskSlot(desk, WritingDeskBlockEntity.CONTAINER_SLOT, 385, 28, 1));
        this.addSlot(new DeskSlot(desk, WritingDeskBlockEntity.OUTPUT_SLOT, 385, 80, 64));

        // Legacy player inventory coordinates (xShift=233, yShift=20).
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 241 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 241 + col * 18, 162));
        }

        addDataSlot(activeTabData);
        addDataSlot(firstTabData);
        addDataSlot(inkData);
        addDataSlot(currentPageData);
        addDataSlot(linkPermittedData);
    }

    public BlockPos getBlockPos() { return pos; }
    public int getActiveTab() { return activeTab; }
    public int getFirstTab() { return firstTab; }
    public int getInkAmount() { return desk instanceof WritingDeskBlockEntity be ? be.inkAmount() : inkAmount; }
    public ItemStack getTarget() { return desk.getItem(WritingDeskBlockEntity.TARGET_SLOT); }
    public ItemStack getActiveTabStack() { return desk.getItem(WritingDeskBlockEntity.FIRST_TAB_SLOT + activeTab); }
    public ItemStack getTabStack(int tab) {
        int clamped = clampTab(tab);
        return desk.getItem(WritingDeskBlockEntity.FIRST_TAB_SLOT + clamped);
    }
    public int getCurrentTargetPageIndex() { return clampTargetPage(currentTargetPage); }
    public boolean isLinkPermitted() { return linkPermitted != 0; }
    public boolean isLinkTarget() { return getTarget().getItem() instanceof ItemLinking; }

    public boolean isTargetRenameable() {
        ItemStack target = getTarget();
        return !target.isEmpty() && (target.getItem() instanceof ItemLinking || target.getItem() instanceof ItemPageContainer);
    }

    public String getTargetTitle() {
        ItemStack target = getTarget();
        if (target.isEmpty()) return "";
        if (target.getItem() instanceof ItemLinking) {
            String title = LinkOptions.getDisplayName(target);
            return title == null ? "" : title;
        }
        if (target.getItem() instanceof ItemPageContainer container) return container.getLegacyName(target);
        return "";
    }

    public void setTargetTitleFromNetwork(String title) {
        if (!(desk instanceof WritingDeskBlockEntity blockEntity) || player.level().isClientSide) return;
        blockEntity.setTargetTitle(player, title);
        broadcastChanges();
    }

    /** Snapshot used by the client surface renderer. */
    public List<ItemStack> getSurfacePages() {
        ItemStack tab = getActiveTabStack();
        if (tab.isEmpty()) return List.of();
        if (tab.getItem() instanceof ItemPageContainer container) return container.getPages(tab);
        if (tab.getItem() instanceof ItemAgebook agebook && player.level() != null) {
            return agebook.getPageList(tab, player.level().registryAccess());
        }
        if (tab.is(MystItems.PAGE.get())) return List.of(tab.copyWithCount(1));
        return List.of();
    }

    /** Page list shown on the right-side target preview/book widget. */
    public List<ItemStack> getTargetPages() {
        ItemStack target = getTarget();
        if (target.isEmpty()) return List.of();
        if (target.getItem() instanceof ItemAgebook agebook && player.level() != null) {
            return agebook.getPageList(target, player.level().registryAccess());
        }
        if (target.is(MystItems.LINKBOOK.get())) return List.of(Page.createLinkPage());
        if (target.is(MystItems.FOLDER.get()) && target.getItem() instanceof ItemPageContainer container) {
            return container.getPages(target);
        }
        if (target.is(MystItems.PAGE.get())) return List.of(target.copyWithCount(1));
        return List.of();
    }

    public ItemStack getCurrentTargetPage() {
        List<ItemStack> pages = getTargetPages();
        int index = getCurrentTargetPageIndex();
        return index >= 0 && index < pages.size() ? pages.get(index) : ItemStack.EMPTY;
    }

    public int getTargetPageCount() { return getTargetPages().size(); }
    public boolean targetOrderedEditable() { return getTarget().is(MystItems.FOLDER.get()); }

    public boolean surfaceIsCollection() { return getActiveTabStack().is(MystItems.PORTFOLIO.get()); }
    public boolean surfaceIsOrderedEditable() { return getActiveTabStack().is(MystItems.FOLDER.get()); }

    private static int clampTab(int value) {
        return Math.max(0, Math.min(WritingDeskBlockEntity.TAB_SLOT_COUNT - 1, value));
    }

    private static int clampFirst(int value) {
        return Math.max(0, Math.min(WritingDeskBlockEntity.TAB_SLOT_COUNT - VISIBLE_TAB_SLOTS, value));
    }

    private int clampTargetPage(int value) {
        return Math.max(0, Math.min(getTargetPageCount(), value));
    }

    private void setActiveTab(int tab) {
        activeTab = clampTab(tab);
        if (activeTab < firstTab) firstTab = activeTab;
        if (activeTab >= firstTab + VISIBLE_TAB_SLOTS) firstTab = clampFirst(activeTab - VISIBLE_TAB_SLOTS + 1);
        broadcastChanges();
    }

    private void setFirstTab(int first) {
        firstTab = clampFirst(first);
        if (activeTab < firstTab) activeTab = firstTab;
        if (activeTab >= firstTab + VISIBLE_TAB_SLOTS) activeTab = firstTab + VISIBLE_TAB_SLOTS - 1;
        broadcastChanges();
    }

    private void setCurrentTargetPage(int page) {
        currentTargetPage = clampTargetPage(page);
        broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == ACTION_TAB_UP) {
            setFirstTab(firstTab - 1);
            return true;
        }
        if (id == ACTION_TAB_DOWN) {
            setFirstTab(firstTab + 1);
            return true;
        }
        if (id == ACTION_TARGET_PREVIOUS) {
            setCurrentTargetPage(currentTargetPage - 1);
            return true;
        }
        if (id == ACTION_TARGET_NEXT) {
            setCurrentTargetPage(currentTargetPage + 1);
            return true;
        }
        if (id == ACTION_TARGET_LINK) {
            return activateLink(player);
        }
        if (id == ACTION_TARGET_TAKE_PAGE) {
            return takeTargetPage();
        }
        if (id == ACTION_TARGET_SET_PAGE || id == ACTION_TARGET_SET_PAGE_SINGLE) {
            return setTargetPage(id == ACTION_TARGET_SET_PAGE_SINGLE);
        }
        if (id >= ACTION_SELECT_TAB && id < ACTION_SELECT_TAB + VISIBLE_TAB_SLOTS) {
            setActiveTab(firstTab + (id - ACTION_SELECT_TAB));
            return true;
        }

        if (id >= ACTION_SURFACE_REMOVE_ONE && id < ACTION_SURFACE_REMOVE_ONE + ACTION_RANGE) {
            return removeSurfacePage(id - ACTION_SURFACE_REMOVE_ONE, false);
        }
        if (id >= ACTION_SURFACE_REMOVE_STACK && id < ACTION_SURFACE_REMOVE_STACK + ACTION_RANGE) {
            return removeSurfacePage(id - ACTION_SURFACE_REMOVE_STACK, true);
        }
        if (id >= ACTION_SURFACE_INSERT_ALL && id < ACTION_SURFACE_INSERT_ALL + ACTION_RANGE) {
            return insertSurfacePage(id - ACTION_SURFACE_INSERT_ALL, false);
        }
        if (id >= ACTION_SURFACE_INSERT_ONE && id < ACTION_SURFACE_INSERT_ONE + ACTION_RANGE) {
            return insertSurfacePage(id - ACTION_SURFACE_INSERT_ONE, true);
        }
        if (id >= ACTION_SURFACE_COPY_SYMBOL && id < ACTION_SURFACE_COPY_SYMBOL + ACTION_RANGE) {
            return copySurfaceSymbol(player, id - ACTION_SURFACE_COPY_SYMBOL);
        }
        return false;
    }

    private boolean activateLink(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(desk instanceof WritingDeskBlockEntity blockEntity)) return false;
        if (!blockEntity.canLinkTarget(player)) return false;
        boolean linked = blockEntity.activateLink(serverPlayer);
        if (linked) serverPlayer.closeContainer();
        return linked;
    }

    private boolean takeTargetPage() {
        if (!getCarried().isEmpty()) return false;
        ItemStack target = getTarget();
        if (!target.is(MystItems.FOLDER.get()) || !(target.getItem() instanceof ItemPageContainer container)) return false;
        int index = getCurrentTargetPageIndex();
        List<ItemStack> slots = container.getPages(target);
        if (index >= slots.size() || slots.get(index).isEmpty()) return false;
        ItemStack removed = container.removeAt(target, index);
        if (removed.isEmpty()) return false;
        setCarried(removed);
        currentTargetPage = clampTargetPage(index);
        desk.setChanged();
        broadcastChanges();
        return true;
    }

    private boolean setTargetPage(boolean single) {
        ItemStack carried = getCarried();
        if (carried.isEmpty()) return false;
        ItemStack target = getTarget();
        if (!target.is(MystItems.FOLDER.get()) || !(target.getItem() instanceof ItemPageContainer container)) return false;
        if (!carried.is(MystItems.PAGE.get()) && !carried.is(Items.PAPER)) return false;

        int index = getCurrentTargetPageIndex();
        List<ItemStack> targetSlots = container.getPages(target);
        if (single && carried.getCount() > 1) {
            // A split stack may fill an empty hole or the first slot after the
            // current sparse span, but cannot replace an occupied page because
            // that page would need to be returned onto a non-empty cursor.
            if (index < targetSlots.size() && !targetSlots.get(index).isEmpty()) return false;
            ItemStack one = carried.copyWithCount(1);
            ItemStack returned = container.setAtLegacy(target, one, index);
            if (!one.isEmpty() || !returned.isEmpty()) return false;
            carried.shrink(1);
            if (carried.isEmpty()) setCarried(ItemStack.EMPTY);
        } else {
            ItemStack returned = container.setAtLegacy(target, carried, index);
            setCarried(returned);
        }

        currentTargetPage = clampTargetPage(index + 1);
        desk.setChanged();
        broadcastChanges();
        return true;
    }

    private boolean removeSurfacePage(int index, boolean matchingStack) {
        if (!getCarried().isEmpty() || index < 0) return false;
        ItemStack tab = getActiveTabStack();
        if (!(tab.getItem() instanceof ItemPageContainer container)) return false;

        ItemStack out;
        if (tab.is(MystItems.PORTFOLIO.get())) {
            out = container.removeMatching(tab, index, matchingStack ? 64 : 1);
        } else if (tab.is(MystItems.FOLDER.get())) {
            out = container.removeAt(tab, index);
        } else {
            return false;
        }
        if (out.isEmpty()) return false;
        setCarried(out);
        desk.setChanged();
        broadcastChanges();
        return true;
    }

    private boolean insertSurfacePage(int index, boolean single) {
        ItemStack carried = getCarried();
        if (carried.isEmpty() || index < 0) return false;
        ItemStack tab = getActiveTabStack();
        if (!(tab.getItem() instanceof ItemPageContainer container)) return false;
        if (!carried.is(MystItems.PAGE.get()) && !carried.is(Items.PAPER)) return false;

        if (tab.is(MystItems.FOLDER.get())) {
            // Folder surface uses the old IItemOrderablePageProvider.setPage contract.
            if (single && carried.getCount() > 1) {
                List<ItemStack> slots = container.getPages(tab);
                int targetIndex = Math.min(index, ItemFolder.PAGE_CAPACITY - 1);
                if (targetIndex < slots.size() && !slots.get(targetIndex).isEmpty()) return false;
                ItemStack one = carried.copyWithCount(1);
                ItemStack returned = container.setAtLegacy(tab, one, targetIndex);
                if (!one.isEmpty() || !returned.isEmpty()) return false;
                carried.shrink(1);
                if (carried.isEmpty()) setCarried(ItemStack.EMPTY);
            } else {
                ItemStack returned = container.setAtLegacy(tab, carried, Math.min(index, ItemFolder.PAGE_CAPACITY - 1));
                setCarried(returned);
            }
        } else if (tab.is(MystItems.PORTFOLIO.get())) {
            int before = carried.getCount();
            int moved = container.insertAt(tab, carried, container.getPageCount(tab), single ? 1 : carried.getCount());
            if (moved <= 0 || carried.getCount() == before) return false;
            if (carried.isEmpty()) setCarried(ItemStack.EMPTY);
        } else return false;

        desk.setChanged();
        broadcastChanges();
        return true;
    }

    private boolean copySurfaceSymbol(Player player, int index) {
        if (index < 0) return false;
        List<ItemStack> pages = getSurfacePages();
        if (index >= pages.size()) return false;
        String symbol = Page.getSymbolId(pages.get(index));
        if (symbol == null || !(desk instanceof WritingDeskBlockEntity blockEntity)) return false;
        boolean written = blockEntity.writeSymbol(player, symbol);
        if (written) broadcastChanges();
        return written;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= TAB_MENU_START && slotId < TAB_MENU_START + VISIBLE_TAB_SLOTS
                && getCarried().isEmpty() && clickType == ClickType.PICKUP) {
            setActiveTab(firstTab + (slotId - TAB_MENU_START));
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == desk) currentTargetPage = clampTargetPage(currentTargetPage);
        // Fluid-container processing lives on WritingDeskBlockEntity's server tick,
        // matching 0.13.7.06 even when no player has the GUI open.
    }

    @Override
    public boolean stillValid(Player player) { return desk.stillValid(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack raw = slot.getItem();
        ItemStack copy = raw.copy();

        if (slotIndex < PLAYER_MENU_START) {
            if (!moveItemStackTo(raw, PLAYER_MENU_START, PLAYER_MENU_END, true)) return ItemStack.EMPTY;
        } else if (WritingDeskBlockEntity.isValidTarget(raw)) {
            if (!moveItemStackTo(raw, WORK_MENU_START, WORK_MENU_START + 1, false)) return ItemStack.EMPTY;
        } else if (desk.canPlaceItem(WritingDeskBlockEntity.PAPER_SLOT, raw)) {
            if (!moveItemStackTo(raw, WORK_MENU_START + 1, WORK_MENU_START + 2, false)) return ItemStack.EMPTY;
        } else if (desk.canPlaceItem(WritingDeskBlockEntity.CONTAINER_SLOT, raw)) {
            if (!moveItemStackTo(raw, WORK_MENU_START + 2, WORK_MENU_START + 3, false)) return ItemStack.EMPTY;
        } else if (WritingDeskBlockEntity.isValidTab(raw)) {
            if (!moveItemStackTo(raw, TAB_MENU_START, TAB_MENU_START + VISIBLE_TAB_SLOTS, false)) return ItemStack.EMPTY;
        } else if (slotIndex < PLAYER_MENU_START + 27) {
            if (!moveItemStackTo(raw, PLAYER_MENU_START + 27, PLAYER_MENU_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(raw, PLAYER_MENU_START, PLAYER_MENU_START + 27, false)) {
            return ItemStack.EMPTY;
        }

        if (raw.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (raw.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, raw);
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        desk.stopOpen(player);
    }

    private final class TabProxyContainer implements Container {
        private final int viewIndex;
        private TabProxyContainer(int viewIndex) { this.viewIndex = viewIndex; }
        private int actual() { return WritingDeskBlockEntity.FIRST_TAB_SLOT + clampTab(firstTab + viewIndex); }
        @Override public int getContainerSize() { return 1; }
        @Override public boolean isEmpty() { return getItem(0).isEmpty(); }
        @Override public ItemStack getItem(int slot) { return desk.getItem(actual()); }
        @Override public ItemStack removeItem(int slot, int amount) { return desk.removeItem(actual(), amount); }
        @Override public ItemStack removeItemNoUpdate(int slot) { return desk.removeItemNoUpdate(actual()); }
        @Override public void setItem(int slot, ItemStack stack) { desk.setItem(actual(), stack); }
        @Override public void setChanged() { desk.setChanged(); }
        @Override public boolean stillValid(Player player) { return desk.stillValid(player); }
        @Override public boolean canPlaceItem(int slot, ItemStack stack) { return WritingDeskBlockEntity.isValidTab(stack); }
        @Override public void clearContent() { setItem(0, ItemStack.EMPTY); }
    }

    private static final class SurfaceTabSlot extends Slot {
        private SurfaceTabSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public int getMaxStackSize() { return 1; }
        @Override public boolean mayPlace(ItemStack stack) { return WritingDeskBlockEntity.isValidTab(stack); }
    }

    private static final class DeskSlot extends Slot {
        private final int max;
        private DeskSlot(Container container, int slot, int x, int y, int max) {
            super(container, slot, x, y);
            this.max = max;
        }
        @Override public int getMaxStackSize() { return max; }
        @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(getContainerSlot(), stack); }
    }
}
