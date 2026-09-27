package com.xcompwiz.mystcraft.blockentity;

import com.xcompwiz.mystcraft.item.ItemLinking;
import com.xcompwiz.mystcraft.item.ItemPage;
import com.xcompwiz.mystcraft.registry.MystBlocks;
import com.xcompwiz.mystcraft.registry.MystBlockEntities;
import com.xcompwiz.mystcraft.treasure.MystcraftTreasureGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One-slot storage shared by the legacy Book Stand and Lectern identities.
 *
 * <p>The old blocks primarily acted as persistent book displays.  The modern
 * port keeps that useful behavior without reproducing the old GUI/container
 * stack: one linking/descriptive book is stored directly on the block entity,
 * survives world reloads, and is returned when the display is removed.</p>
 */
public final class BookDisplayBlockEntity extends BlockEntity {
    private ItemStack book = ItemStack.EMPTY;
    /** Set only by legacy Archivist-house structure NBT; consumed exactly once on server load. */
    private boolean treasurePending;

    public BookDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(MystBlockEntities.BOOK_DISPLAY.get(), pos, state);
    }

    public static boolean isLinkingBook(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemLinking;
    }

    /** Legacy lecterns accept Symbol Pages in addition to linking books. */
    public boolean canAccept(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (isLinkingBook(stack)) return true;
        return getBlockState().is(MystBlocks.LECTERN.get())
                && (stack.getItem() instanceof ItemPage || stack.is(Items.FILLED_MAP));
    }

    public boolean hasBook() {
        return !book.isEmpty() && canAccept(book);
    }

    public ItemStack getBook() {
        return book;
    }

    public boolean insertBook(ItemStack stack) {
        if (hasBook() || !canAccept(stack)) return false;
        book = stack.copyWithCount(1);
        setChanged();
        sync();
        return true;
    }

    public ItemStack removeBook() {
        ItemStack result = book.copy();
        book = ItemStack.EMPTY;
        setChanged();
        sync();
        return result;
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide || !treasurePending) return;
        populateLegacyTreasure(level.getRandom(), false);
    }

    /**
     * Populate a generated legacy Lectern without depending on BlockEntity#onLoad timing.
     * Archivist houses use this as a post-generation repair path because Jigsaw-created
     * block entities can become live after the template's custom NBT callback window.
     */
    public boolean populateLegacyTreasure(RandomSource random, boolean allowLinkingItems) {
        treasurePending = false;
        if (!getBlockState().is(MystBlocks.LECTERN.get()) || !book.isEmpty()) {
            setChanged();
            return false;
        }
        ItemStack generated = MystcraftTreasureGenerator.generateLecternItem(random, allowLinkingItems);
        if (generated.isEmpty() || !canAccept(generated)) {
            setChanged();
            return false;
        }
        book = generated.copyWithCount(1);
        setChanged();
        sync();
        return true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) loadAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        book = ItemStack.parseOptional(registries, tag.getCompound("Book"));
        if (!canAccept(book)) book = ItemStack.EMPTY;
        treasurePending = tag.getBoolean("TreasurePending");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!book.isEmpty()) tag.put("Book", book.saveOptional(registries));
        if (treasurePending) tag.putBoolean("TreasurePending", true);
    }
}
