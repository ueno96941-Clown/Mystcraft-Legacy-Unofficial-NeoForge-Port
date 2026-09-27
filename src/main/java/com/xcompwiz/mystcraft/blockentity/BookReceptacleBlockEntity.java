package com.xcompwiz.mystcraft.blockentity;

import com.xcompwiz.mystcraft.api.item.IItemPortalActivator;
import com.xcompwiz.mystcraft.portal.CrystalPortalRuntime;
import com.xcompwiz.mystcraft.registry.MystBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern, deliberately small Book Receptacle implementation.
 *
 * <p>The receptacle owns one linked Mystcraft book and the set of Crystal Portal
 * blocks created from the adjacent frame.  Geometry is persisted so a frame
 * break, book removal, block removal, or world reload can cleanly tear the
 * portal down without depending on transient runtime maps.</p>
 */
public final class BookReceptacleBlockEntity extends BlockEntity {
    private ItemStack book = ItemStack.EMPTY;
    private final List<BlockPos> portalBlocks = new ArrayList<>();
    private final List<BlockPos> frameBlocks = new ArrayList<>();
    private boolean portalMutation;
    private boolean portalRefreshBusy;
    private int portalColor = 0xFFFFFF;

    public BookReceptacleBlockEntity(BlockPos pos, BlockState state) {
        super(MystBlockEntities.BOOK_RECEPTACLE.get(), pos, state);
    }

    public ItemStack getBook() {
        return book;
    }

    public boolean hasBook() {
        return !book.isEmpty() && book.getItem() instanceof IItemPortalActivator;
    }

    /** Replace the controller book. Portal teardown always happens before the swap. */
    public void setBook(ItemStack stack) {
        ItemStack next = isPortalBook(stack) ? stack.copyWithCount(1) : ItemStack.EMPTY;
        if (level instanceof ServerLevel serverLevel) rerollPortalColor(serverLevel);
        this.book = ItemStack.EMPTY;
        if (level instanceof ServerLevel serverLevel) {
            deactivatePortal(serverLevel);
        }
        this.book = next;
        setChanged();
        if (level instanceof ServerLevel serverLevel && hasBook()) {
            tryActivatePortal(serverLevel);
        }
        // Sync only after the activation attempt.  CP262 synced the receptacle first;
        // that block update could wake the surrounding Crystal graph and recursively
        // enter refreshPortal before the explicit activation had finished.
        sync();
    }

    public ItemStack removeBook() {
        ItemStack result = this.book.copy();
        setBook(ItemStack.EMPTY);
        return result;
    }

    public boolean tryActivatePortal(ServerLevel level) {
        if (!hasBook()) return false;
        return CrystalPortalRuntime.activate(level, worldPosition, this);
    }

    public void refreshPortal(ServerLevel level) {
        if (portalMutation || portalRefreshBusy) return;
        portalRefreshBusy = true;
        try {
            if (!hasBook()) {
                deactivatePortal(level);
                return;
            }
            if (!portalBlocks.isEmpty() && CrystalPortalRuntime.isStillValid(level, this)) return;
            deactivatePortal(level);
            tryActivatePortal(level);
        } finally {
            portalRefreshBusy = false;
        }
    }

    public void deactivatePortal(ServerLevel level) {
        if (portalMutation) return;
        portalMutation = true;
        List<BlockPos> removing = List.copyOf(portalBlocks);
        portalBlocks.clear();
        frameBlocks.clear();
        try {
            for (BlockPos pos : removing) {
                if (level.getBlockState(pos).is(com.xcompwiz.mystcraft.registry.MystBlocks.LINK_PORTAL.get())) {
                    level.removeBlock(pos, false);
                }
            }
        } finally {
            portalMutation = false;
        }
        setChanged();
    }

    public void beginPortalMutation() {
        this.portalMutation = true;
    }

    public void endPortalMutation() {
        this.portalMutation = false;
    }

    public boolean isPortalMutation() {
        return portalMutation;
    }

    public void setPortalGeometry(List<BlockPos> frame, List<BlockPos> interior) {
        frameBlocks.clear();
        frameBlocks.addAll(frame);
        portalBlocks.clear();
        portalBlocks.addAll(interior);
        setChanged();
    }

    public List<BlockPos> getPortalBlocks() {
        return List.copyOf(portalBlocks);
    }

    public List<BlockPos> getFrameBlocks() {
        return List.copyOf(frameBlocks);
    }

    public boolean controls(BlockPos portalPos) {
        return portalBlocks.contains(portalPos);
    }

    public int getPortalColor() {
        return portalColor & 0xFFFFFF;
    }

    /**
     * CP264B user-facing color cycle: every physical book insertion/removal selects
     * a fresh visible portal tint.  It is persisted on the receptacle so chunk rebuilds,
     * reconnects and every portal cell in the frame share one stable color until the
     * next book change.
     */
    private void rerollPortalColor(ServerLevel level) {
        int previous = portalColor & 0xFFFFFF;
        int next;
        do {
            int r = 64 + level.getRandom().nextInt(192);
            int g = 64 + level.getRandom().nextInt(192);
            int b = 64 + level.getRandom().nextInt(192);
            next = (r << 16) | (g << 8) | b;
        } while (next == previous);
        portalColor = next;
    }

    public static boolean isPortalBook(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IItemPortalActivator;
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
            // Portal tint is baked into block-model meshes.  Re-send every active cell
            // after the receptacle BE payload so the client rebuilds them with the new color.
            for (BlockPos portal : portalBlocks) {
                BlockState portalState = level.getBlockState(portal);
                level.sendBlockUpdated(portal, portalState, portalState, 3);
            }
        }
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
        this.book = ItemStack.parseOptional(registries, tag.getCompound("Book"));
        portalBlocks.clear();
        for (long packed : tag.getLongArray("PortalBlocks")) {
            portalBlocks.add(BlockPos.of(packed));
        }
        frameBlocks.clear();
        for (long packed : tag.getLongArray("FrameBlocks")) {
            frameBlocks.add(BlockPos.of(packed));
        }
        if (tag.contains("PortalColor")) portalColor = tag.getInt("PortalColor") & 0xFFFFFF;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!book.isEmpty()) {
            tag.put("Book", book.saveOptional(registries));
        }
        tag.putLongArray("PortalBlocks", portalBlocks.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putLongArray("FrameBlocks", frameBlocks.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putInt("PortalColor", portalColor & 0xFFFFFF);
    }
}
