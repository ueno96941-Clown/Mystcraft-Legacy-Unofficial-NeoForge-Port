package com.xcompwiz.mystcraft.inventory;

import com.xcompwiz.mystcraft.blockentity.BookDisplayBlockEntity;
import com.xcompwiz.mystcraft.item.ItemAgebook;
import com.xcompwiz.mystcraft.item.ItemLinking;
import com.xcompwiz.mystcraft.entity.EntityLinkbook;
import com.xcompwiz.mystcraft.linking.LinkController;
import com.xcompwiz.mystcraft.registry.MystMenus;
import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LinkBookMenu extends AbstractContainerMenu {
    private final InteractionHand hand;
    @Nullable private final BlockPos displayPos;
    private final int entityId;
    private final String destinationName;
    private final String dimensionKey;
    private final boolean agebook;
    private final List<String> pageIds;
    private final List<String> authors;

    private record OpenData(InteractionHand hand, @Nullable BlockPos displayPos, int entityId, String destinationName,
                            String dimensionKey, boolean agebook, List<String> pageIds, List<String> authors) {}

    /** Client constructor. */
    public LinkBookMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, readOpenData(buffer));
    }

    private LinkBookMenu(int containerId, Inventory playerInventory, OpenData data) {
        this(containerId, playerInventory, data.hand(), data.displayPos(), data.entityId(), data.destinationName(), data.dimensionKey(),
                data.agebook(), data.pageIds(), data.authors());
    }

    /** Server constructor for a normal held Linking/Descriptive Book. */
    public LinkBookMenu(int containerId, Inventory playerInventory, InteractionHand hand, String destinationName, String dimensionKey,
                        boolean agebook, List<String> pageIds, List<String> authors) {
        this(containerId, playerInventory, hand, null, -1, destinationName, dimensionKey, agebook, pageIds, authors);
    }

    /** Server constructor for a book stored in a Book Stand/Lectern. */
    public LinkBookMenu(int containerId, Inventory playerInventory, BlockPos displayPos, String destinationName, String dimensionKey,
                        boolean agebook, List<String> pageIds, List<String> authors) {
        this(containerId, playerInventory, InteractionHand.MAIN_HAND, displayPos, -1, destinationName, dimensionKey, agebook, pageIds, authors);
    }

    /** Server constructor for a physical legacy EntityLinkbook. */
    public LinkBookMenu(int containerId, Inventory playerInventory, int entityId, String destinationName, String dimensionKey,
                        boolean agebook, List<String> pageIds, List<String> authors) {
        this(containerId, playerInventory, InteractionHand.MAIN_HAND, null, entityId, destinationName, dimensionKey, agebook, pageIds, authors);
    }

    private LinkBookMenu(int containerId, Inventory playerInventory, InteractionHand hand, @Nullable BlockPos displayPos, int entityId,
                         String destinationName, String dimensionKey, boolean agebook, List<String> pageIds, List<String> authors) {
        super(MystMenus.BOOK.get(), containerId);
        this.hand = hand == null ? InteractionHand.MAIN_HAND : hand;
        this.displayPos = displayPos == null ? null : displayPos.immutable();
        this.entityId = entityId;
        this.destinationName = destinationName == null ? "???" : destinationName;
        this.dimensionKey = dimensionKey == null ? "" : dimensionKey;
        this.agebook = agebook;
        this.pageIds = List.copyOf(pageIds == null ? List.of() : pageIds);
        this.authors = List.copyOf(authors == null ? List.of() : authors);
    }

    private static OpenData readOpenData(RegistryFriendlyByteBuf buffer) {
        int sourceType = buffer.readVarInt();
        InteractionHand hand = buffer.readVarInt() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        BlockPos displayPos = sourceType == 1 ? buffer.readBlockPos() : null;
        int entityId = sourceType == 2 ? buffer.readVarInt() : -1;
        String destinationName = buffer.readUtf(64);
        String dimensionKey = buffer.readUtf(256);
        boolean agebook = buffer.readBoolean();
        List<String> pageIds = readStrings(buffer, 512, 256);
        List<String> authors = readStrings(buffer, 32, 64);
        return new OpenData(hand, displayPos, entityId, destinationName, dimensionKey, agebook, pageIds, authors);
    }

    private static List<String> readStrings(RegistryFriendlyByteBuf buffer, int maxEntries, int maxLength) {
        int count = buffer.readVarInt();
        if (count < 0 || count > maxEntries) {
            throw new IllegalArgumentException("Invalid Mystcraft Link Book string count: " + count + " (max " + maxEntries + ")");
        }
        List<String> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) out.add(buffer.readUtf(maxLength));
        return Collections.unmodifiableList(out);
    }

    public InteractionHand getHand() { return hand; }
    public boolean isDisplaySource() { return displayPos != null; }
    public boolean isEntitySource() { return entityId >= 0; }
    public int getEntityId() { return entityId; }
    @Nullable public BlockPos getDisplayPos() { return displayPos; }
    public String getDestinationName() { return destinationName; }
    public String getDimensionKey() { return dimensionKey; }
    public boolean isAgebook() { return agebook; }
    public List<String> getPageIds() { return pageIds; }
    public List<String> getAuthors() { return authors; }
    public int getPageCount() { return pageIds.size(); }

    private ItemStack sourceStack(Player player) {
        if (entityId >= 0) {
            if (player.level().getEntity(entityId) instanceof EntityLinkbook entity) return entity.getBook();
            return ItemStack.EMPTY;
        }
        if (displayPos == null) return player.getItemInHand(hand);
        if (player.level().getBlockEntity(displayPos) instanceof BookDisplayBlockEntity display) return display.getBook();
        return ItemStack.EMPTY;
    }

    public boolean activate(ServerPlayer player) {
        ItemStack stack = sourceStack(player);
        if (!(stack.getItem() instanceof ItemLinking)) return false;

        if (stack.getItem() instanceof ItemAgebook
                && ItemAgebook.isNewAgebook(stack, player.registryAccess())) {
            try {
                AgeManager.establish(player, stack);
                if (displayPos != null && player.level().getBlockEntity(displayPos) instanceof BookDisplayBlockEntity display) {
                    display.setChanged();
                } else if (entityId >= 0 && player.level().getEntity(entityId) instanceof EntityLinkbook entity) {
                    entity.setBook(stack);
                }
            } catch (IllegalStateException ex) {
                player.displayClientMessage(Component.literal(ex.getMessage()), true);
                return false;
            }
        }

        boolean linked = (displayPos != null || entityId >= 0)
                ? LinkController.travelPlayerFromPortal(player, stack)
                : LinkController.travelPlayer(player, hand, stack);
        if (linked) player.closeContainer();
        return linked;
    }

    @Override
    public boolean stillValid(Player player) {
        if (entityId >= 0) {
            if (!(player.level().getEntity(entityId) instanceof EntityLinkbook entity)) return false;
            if (player.distanceToSqr(entity) > 64.0D) return false;
            return entity.getBook().getItem() instanceof ItemLinking;
        }
        if (displayPos == null) return player.getItemInHand(hand).getItem() instanceof ItemLinking;
        if (player.distanceToSqr(displayPos.getX() + 0.5D, displayPos.getY() + 0.5D, displayPos.getZ() + 0.5D) > 64.0D) return false;
        return player.level().getBlockEntity(displayPos) instanceof BookDisplayBlockEntity display
                && BookDisplayBlockEntity.isLinkingBook(display.getBook());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }
}
