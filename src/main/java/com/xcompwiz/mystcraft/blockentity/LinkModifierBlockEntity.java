package com.xcompwiz.mystcraft.blockentity;

import com.xcompwiz.mystcraft.api.item.IItemRenameable;
import com.xcompwiz.mystcraft.inventory.LinkModifierMenu;
import com.xcompwiz.mystcraft.item.ItemAgebook;
import com.xcompwiz.mystcraft.item.ItemLinking;
import com.xcompwiz.mystcraft.linking.LinkOptions;
import com.xcompwiz.mystcraft.registry.MystBlockEntities;
import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import com.xcompwiz.mystcraft.world.agedata.AgeRegistryData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Legacy single-book Link Modifier state and server-authoritative edit operations. */
public final class LinkModifierBlockEntity extends BlockEntity implements Container, MenuProvider {
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    public LinkModifierBlockEntity(BlockPos pos, BlockState state) {
        super(MystBlockEntities.LINK_MODIFIER.get(), pos, state);
    }

    public static boolean isModifiableBook(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemLinking;
    }

    public boolean hasBook() { return isModifiableBook(items.getFirst()); }
    public ItemStack getBook() {
        ItemStack book = items.getFirst();
        if (level != null && book.getItem() instanceof ItemLinking linking) {
            linking.validateLinkData(level, book, null);
        }
        return book;
    }

    public void setBookTitle(Player player, String text) {
        ItemStack book = getBook();
        if (!book.isEmpty() && book.getItem() instanceof IItemRenameable renameable) {
            renameable.setDisplayName(player, book, text == null ? "" : text);
            changedAndSync();
        }
    }

    public String getBookTitle(Player player) {
        ItemStack book = getBook();
        if (book.isEmpty()) return "";
        if (book.getItem() instanceof IItemRenameable renameable) {
            String value = renameable.getDisplayName(player, book);
            return value == null ? "" : value;
        }
        return "";
    }

    public boolean getLinkOption(String name) { return hasBook() && LinkOptions.getFlag(getBook(), name); }
    public void setLinkOption(String name, boolean value) {
        if (!hasBook()) return;
        LinkOptions.setFlag(getBook(), name, value);
        changedAndSync();
    }

    public String getLinkProperty(String name) { return hasBook() ? LinkOptions.getProperty(getBook(), name) : null; }
    public void setLinkProperty(String name, String value) {
        if (!hasBook()) return;
        LinkOptions.setProperty(getBook(), name, value);
        changedAndSync();
    }

    public String getLinkDimensionUID() {
        if (!hasBook()) return "";
        Integer id = LinkOptions.getLegacyDimensionId(getBook());
        if (id != null) return Integer.toString(id);
        String key = LinkOptions.getDimensionKey(getBook());
        return key == null ? "" : key;
    }

    public boolean hasItemSeed() { return getBook().getItem() instanceof ItemAgebook; }

    public void setSeedText(Player player, String seedText) {
        if (!hasItemSeed()) return;
        if (seedText == null || seedText.isBlank()) {
            LinkOptions.setProperty(getBook(), "Seed", null);
            changedAndSync();
            return;
        }
        final long seed;
        try { seed = Long.parseLong(seedText); }
        catch (NumberFormatException ignored) { return; }

        if (player instanceof ServerPlayer serverPlayer && serverPlayer.getServer() != null) {
            var server = serverPlayer.getServer();
            var age = AgeManager.resolveReservation(server, getBook());
            if (age.isPresent()) {
                age.get().setSeed(seed);
                LinkOptions.setProperty(getBook(), "Seed", Long.toString(age.get().seed()));
                AgeRegistryData.get(server).changed();
                AgeRegistryData.flush(server);
            } else {
                LinkOptions.setProperty(getBook(), "Seed", Long.toString(seed));
            }
        } else {
            LinkOptions.setProperty(getBook(), "Seed", Long.toString(seed));
        }
        changedAndSync();
    }

    public boolean isLinkDimensionDead(Player player) {
        if (!hasBook() || !(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) return false;
        return AgeManager.resolveExisting(serverPlayer.getServer(), getBook()).map(a -> a.dead()).orElse(false);
    }

    public void recycleDimension(Player player) {
        if (!hasBook() || !(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) return;
        var server = serverPlayer.getServer();
        AgeManager.resolveExisting(server, getBook()).ifPresent(age -> {
            age.setDead(true);
            AgeRegistryData.get(server).changed();
            AgeRegistryData.flush(server);
        });
        changedAndSync();
    }

    private void changedAndSync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override public Component getDisplayName() { return Component.translatable("container.mystcraft.link_modifier"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new LinkModifierMenu(id, inventory, this, worldPosition);
    }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return items.getFirst().isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 ? items.getFirst() : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack result = slot == 0 ? ContainerHelper.removeItem(items, 0, amount) : ItemStack.EMPTY;
        if (!result.isEmpty()) changedAndSync();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return slot == 0 ? ContainerHelper.takeItem(items, 0) : ItemStack.EMPTY; }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot != 0) return;
        items.set(0, isModifiableBook(stack) ? stack.copyWithCount(1) : ItemStack.EMPTY);
        changedAndSync();
    }
    @Override public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5) <= 64.0;
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && isModifiableBook(stack); }
    @Override public void clearContent() { items.set(0, ItemStack.EMPTY); changedAndSync(); }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.set(0, ItemStack.parseOptional(registries, tag.getCompound("Book")));
        if (!isModifiableBook(items.getFirst())) items.set(0, ItemStack.EMPTY);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!items.getFirst().isEmpty()) tag.put("Book", items.getFirst().saveOptional(registries));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
