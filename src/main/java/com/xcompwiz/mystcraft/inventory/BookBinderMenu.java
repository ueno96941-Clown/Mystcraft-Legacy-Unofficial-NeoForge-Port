package com.xcompwiz.mystcraft.inventory;

import com.xcompwiz.mystcraft.blockentity.BookBinderBlockEntity;
import com.xcompwiz.mystcraft.item.ItemPageContainer;
import com.xcompwiz.mystcraft.network.BinderPageActionPayload;
import com.xcompwiz.mystcraft.network.BinderPagesPayload;
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
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Legacy-style Book Binder: one physical cover slot plus a variable page slider. */
public final class BookBinderMenu extends AbstractContainerMenu {
    public static final int COVER_MENU_SLOT = 0;
    public static final int RESULT_MENU_SLOT = 1;
    public static final int PLAYER_SLOT_START = 2;
    public static final int PLAYER_SLOT_END = PLAYER_SLOT_START + 36;

    private final Container binder;
    private final ResultContainer result = new ResultContainer();
    private final Player player;
    private final BlockPos pos;
    private String pendingTitle;
    private List<String> clientPageDescriptors;

    /** Client constructor used by IMenuTypeExtension. */
    public BookBinderMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId,
                playerInventory,
                new SimpleContainer(BookBinderBlockEntity.SLOT_COUNT),
                buffer.readBlockPos(),
                buffer.readUtf(BookBinderBlockEntity.MAX_TITLE_LENGTH),
                readDescriptors(buffer), true);
    }

    /** Server constructor used by the Book Binder block entity. */
    public BookBinderMenu(int containerId, Inventory playerInventory, Container binder, BlockPos pos,
                          String pendingTitle, List<ItemStack> pages) {
        this(containerId, playerInventory, binder, pos, pendingTitle, describePages(pages), true);
    }

    private BookBinderMenu(int containerId, Inventory playerInventory, Container binder, BlockPos pos,
                           String pendingTitle, List<String> pageDescriptors, boolean descriptorsAlreadyEncoded) {
        super(MystMenus.BOOK_BINDER.get(), containerId);
        checkContainerSize(binder, BookBinderBlockEntity.SLOT_COUNT);
        this.binder = binder;
        this.player = playerInventory.player;
        this.pos = pos;
        this.pendingTitle = pendingTitle == null ? "" : pendingTitle;
        this.clientPageDescriptors = List.copyOf(pageDescriptors);

        binder.startOpen(playerInventory.player);

        this.addSlot(new Slot(binder, BookBinderBlockEntity.COVER_SLOT, 8, 27) {
            @Override public boolean mayPlace(ItemStack stack) { return BookBinderBlockEntity.isValidCover(stack); }
        });

        this.addSlot(new Slot(result, 0, 152, 27) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) {
                return BookBinderMenu.this.binder instanceof BookBinderBlockEntity blockEntity && blockEntity.canBuildItem();
            }
            @Override public void onTake(Player player, ItemStack stack) {
                if (BookBinderMenu.this.binder instanceof BookBinderBlockEntity blockEntity) {
                    // Author the exact stack that AbstractContainerMenu has just put
                    // on the player's cursor, matching Legacy SlotCraftCustom.
                    blockEntity.buildItem(stack, player);
                    BookBinderMenu.this.pendingTitle = blockEntity.getPendingTitle();
                    BookBinderMenu.this.syncPagesToClient();
                    BookBinderMenu.this.updateResult();
                }
                super.onTake(player, stack);
            }
        });

        addPlayerInventory(playerInventory);
        updateResult();
    }

    public static final int MAX_SYNCED_PAGE_DESCRIPTORS = 4096;
    public static final int MAX_PAGE_DESCRIPTOR_LENGTH = 512;

    private static List<String> readDescriptors(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_SYNCED_PAGE_DESCRIPTORS) {
            throw new IllegalArgumentException("Invalid Mystcraft Binder descriptor count: " + count);
        }
        List<String> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) out.add(buffer.readUtf(MAX_PAGE_DESCRIPTOR_LENGTH));
        return List.copyOf(out);
    }

    public static void writeDescriptors(RegistryFriendlyByteBuf buffer, List<ItemStack> pages) {
        List<String> descriptors = describePages(pages);
        int count = Math.min(descriptors.size(), MAX_SYNCED_PAGE_DESCRIPTORS);
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) buffer.writeUtf(descriptors.get(i), MAX_PAGE_DESCRIPTOR_LENGTH);
    }

    public static List<String> describePages(List<ItemStack> pages) {
        List<String> out = new ArrayList<>(pages.size());
        for (ItemStack page : pages) {
            if (Page.isLinkPanel(page)) out.add("$LINK_PANEL$");
            else {
                String id = Page.getSymbolId(page);
                out.add(id == null || id.isBlank() ? "$BLANK$" : id);
            }
        }
        return List.copyOf(out);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 99 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 157));
        }
    }

    public String getPendingTitle() { return pendingTitle; }
    public BlockPos getBlockPos() { return pos; }
    public List<String> getClientPageDescriptors() { return clientPageDescriptors; }
    public void setClientPageDescriptors(List<String> descriptors) { clientPageDescriptors = List.copyOf(descriptors); }

    public void setClientPendingTitle(String title) { this.pendingTitle = title == null ? "" : title; }

    public void setPendingTitleFromNetwork(String title) {
        if (!(binder instanceof BookBinderBlockEntity blockEntity)) return;
        blockEntity.setPendingTitle(title);
        this.pendingTitle = blockEntity.getPendingTitle();
        updateResult();
        broadcastChanges();
    }

    public String getBuildErrorKey() {
        if (binder.getItem(BookBinderBlockEntity.COVER_SLOT).isEmpty()) return "gui.mystcraft.binder.missing_cover";
        if (pendingTitle.isBlank()) return "gui.mystcraft.binder.missing_title";
        if (clientPageDescriptors.isEmpty() || !"$LINK_PANEL$".equals(clientPageDescriptors.getFirst())) {
            return "gui.mystcraft.binder.missing_link_panel";
        }
        for (int i = 1; i < clientPageDescriptors.size(); i++) {
            if ("$LINK_PANEL$".equals(clientPageDescriptors.get(i))) return "gui.mystcraft.binder.extra_link_panel";
        }
        return "";
    }

    public void handlePageAction(ServerPlayer player, int action, int index, boolean single) {
        if (!(binder instanceof BookBinderBlockEntity blockEntity)) return;
        int safeIndex = Math.max(0, Math.min(index, blockEntity.getPageCount()));
        if (action == BinderPageActionPayload.INSERT) {
            ItemStack carried = getCarried();
            if (carried.isEmpty()) return;
            if (carried.getItem() instanceof ItemPageContainer || BookBinderBlockEntity.isValidPageInput(carried)) {
                setCarried(blockEntity.insertPage(carried, safeIndex, single));
            }
        } else if (action == BinderPageActionPayload.REMOVE) {
            if (!getCarried().isEmpty() || safeIndex >= blockEntity.getPageCount()) return;
            setCarried(blockEntity.removePage(safeIndex));
        }
        syncPagesToClient();
        updateResult();
        broadcastChanges();
    }

    private void syncPagesToClient() {
        if (!(binder instanceof BookBinderBlockEntity blockEntity)) return;
        clientPageDescriptors = describePages(blockEntity.getPages());
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new BinderPagesPayload(containerId, clientPageDescriptors));
        }
    }

    private void updateResult() {
        ItemStack output = ItemStack.EMPTY;
        if (binder instanceof BookBinderBlockEntity blockEntity) output = blockEntity.getCraftedItem();
        result.setItem(0, output);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == binder) updateResult();
    }

    @Override public boolean stillValid(Player player) { return binder.stillValid(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack raw = slot.getItem();
        ItemStack copy = raw.copy();

        if (slotIndex == RESULT_MENU_SLOT) {
            if (!(binder instanceof BookBinderBlockEntity blockEntity)) return ItemStack.EMPTY;

            // QUICK_MOVE transfers before Slot#onTake, so never move the plain
            // preview stack. Build one finalized snapshot, transfer it, and only
            // then consume the Binder inputs.
            ItemStack finalized = blockEntity.createBuiltItemSnapshot(player);
            if (finalized.isEmpty()) return ItemStack.EMPTY;
            ItemStack remainder = finalized.copy();
            if (!moveItemStackTo(remainder, PLAYER_SLOT_START, PLAYER_SLOT_END, true) || !remainder.isEmpty()) {
                return ItemStack.EMPTY;
            }
            blockEntity.consumeBuiltItem();
            pendingTitle = blockEntity.getPendingTitle();
            syncPagesToClient();
            updateResult();
            broadcastChanges();
            return finalized;
        } else if (slotIndex == COVER_MENU_SLOT) {
            if (!moveItemStackTo(raw, PLAYER_SLOT_START, PLAYER_SLOT_END, false)) return ItemStack.EMPTY;
        } else if (BookBinderBlockEntity.isValidCover(raw)) {
            if (!moveItemStackTo(raw, COVER_MENU_SLOT, COVER_MENU_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (raw.getItem() instanceof ItemPageContainer pageContainer && !pageContainer.isEmpty(raw)) {
            if (!(binder instanceof BookBinderBlockEntity blockEntity)) return ItemStack.EMPTY;
            int moved = blockEntity.importPageContainer(raw);
            if (moved <= 0) return ItemStack.EMPTY;
            slot.setChanged();
            syncPagesToClient();
            updateResult();
            return copy;
        } else if (raw.is(MystItems.PAGE.get()) || raw.is(Items.PAPER)) {
            if (!(binder instanceof BookBinderBlockEntity blockEntity)) return ItemStack.EMPTY;
            int before = raw.getCount();
            blockEntity.insertPage(raw, blockEntity.getPageCount(), false);
            if (raw.getCount() == before) return ItemStack.EMPTY;
            syncPagesToClient();
            updateResult();
        } else if (slotIndex < PLAYER_SLOT_START + 27) {
            if (!moveItemStackTo(raw, PLAYER_SLOT_START + 27, PLAYER_SLOT_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(raw, PLAYER_SLOT_START, PLAYER_SLOT_START + 27, false)) return ItemStack.EMPTY;

        if (raw.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (raw.getCount() == copy.getCount() && slotIndex != RESULT_MENU_SLOT) return ItemStack.EMPTY;
        slot.onTake(player, raw);
        return copy;
    }

    @Override public void removed(Player player) { super.removed(player); binder.stopOpen(player); }
}
