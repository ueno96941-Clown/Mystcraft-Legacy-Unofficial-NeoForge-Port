package com.xcompwiz.mystcraft.inventory;

import com.xcompwiz.mystcraft.blockentity.LinkModifierBlockEntity;
import com.xcompwiz.mystcraft.data.InkEffects;
import com.xcompwiz.mystcraft.registry.MystMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 1.21.1 reconstruction of the legacy single-slot Link Modifier container. */
public final class LinkModifierMenu extends AbstractContainerMenu {
    public static final int ACTION_TOGGLE_BASE = 100;
    public static final int ACTION_MARK_DEAD = 200;

    private final Container modifier;
    private final Player player;
    private final BlockPos pos;
    private final List<String> properties;
    private final int[] flags;
    private int dead;
    private int hasSeed;

    public LinkModifierMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, resolveClientContainer(inventory, buffer.readBlockPos()));
    }

    private LinkModifierMenu(int containerId, Inventory inventory, ClientContainer resolved) {
        this(containerId, inventory, resolved.container(), resolved.pos());
    }

    public LinkModifierMenu(int containerId, Inventory inventory, Container modifier, BlockPos pos) {
        super(MystMenus.LINK_MODIFIER.get(), containerId);
        checkContainerSize(modifier, 1);
        this.modifier = modifier;
        this.player = inventory.player;
        this.pos = pos;
        this.properties = InkEffects.getProperties();
        this.flags = new int[properties.size()];

        addSlot(new Slot(modifier, 0, 80, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return LinkModifierBlockEntity.isModifiableBook(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row=0; row<3; row++) for (int col=0; col<9; col++)
            addSlot(new Slot(inventory, col + row*9 + 9, 8 + col*18, 84 + row*18));
        for (int col=0; col<9; col++) addSlot(new Slot(inventory, col, 8 + col*18, 142));

        for (int propertyIndex = 0; propertyIndex < properties.size(); propertyIndex++) {
            final int index = propertyIndex;
            final String property = properties.get(index);
            addDataSlot(new DataSlot() {
                @Override public int get() {
                    if (modifier instanceof LinkModifierBlockEntity be) flags[index] = be.getLinkOption(property) ? 1 : 0;
                    return flags[index];
                }
                @Override public void set(int value) { flags[index] = value != 0 ? 1 : 0; }
            });
        }
        addDataSlot(new DataSlot() {
            @Override public int get() {
                if (modifier instanceof LinkModifierBlockEntity be) dead = be.isLinkDimensionDead(player) ? 1 : 0;
                return dead;
            }
            @Override public void set(int value) { dead = value != 0 ? 1 : 0; }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() {
                if (modifier instanceof LinkModifierBlockEntity be) hasSeed = be.hasItemSeed() ? 1 : 0;
                return hasSeed;
            }
            @Override public void set(int value) { hasSeed = value != 0 ? 1 : 0; }
        });
    }

    private static ClientContainer resolveClientContainer(Inventory inventory, BlockPos pos) {
        var be = inventory.player.level().getBlockEntity(pos);
        return new ClientContainer(be instanceof LinkModifierBlockEntity modifier ? modifier : new SimpleContainer(1), pos);
    }

    public List<String> properties() { return properties; }
    public boolean propertyEnabled(int index) {
        if (index < 0 || index >= properties.size()) return false;
        return flags[index] != 0;
    }
    public boolean isDead() { return dead != 0; }
    public boolean hasSeed() { return hasSeed != 0; }
    public String getBookTitle() {
        ItemStack book = getSlot(0).getItem();
        if (book.getItem() instanceof com.xcompwiz.mystcraft.api.item.IItemRenameable renameable) {
            String value = renameable.getDisplayName(player, book);
            return value == null ? "" : value;
        }
        return "";
    }
    public String getSeedText() {
        ItemStack book = getSlot(0).getItem();
        String seed = com.xcompwiz.mystcraft.linking.LinkOptions.getProperty(book, "Seed");
        return seed == null ? "" : seed;
    }
    public String getDimensionText() {
        ItemStack book = getSlot(0).getItem();
        Integer id = com.xcompwiz.mystcraft.linking.LinkOptions.getLegacyDimensionId(book);
        if (id != null) return Integer.toString(id);
        String key = com.xcompwiz.mystcraft.linking.LinkOptions.getDimensionKey(book);
        return key == null ? "" : key;
    }

    public void setTitleFromNetwork(String text) {
        if (modifier instanceof LinkModifierBlockEntity be && !player.level().isClientSide) {
            be.setBookTitle(player, text == null ? "" : text);
            broadcastChanges();
        }
    }
    public void setSeedFromNetwork(String text) {
        if (modifier instanceof LinkModifierBlockEntity be && !player.level().isClientSide) {
            be.setSeedText(player, text == null ? "" : text);
            broadcastChanges();
        }
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (!(modifier instanceof LinkModifierBlockEntity be) || player.level().isClientSide) return true;
        if (id >= ACTION_TOGGLE_BASE && id < ACTION_TOGGLE_BASE + properties.size()) {
            String property = properties.get(id - ACTION_TOGGLE_BASE);
            be.setLinkOption(property, !be.getLinkOption(property));
            broadcastChanges();
            return true;
        }
        if (id == ACTION_MARK_DEAD) {
            be.recycleDimension(player);
            broadcastChanges();
            return true;
        }
        return false;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = getSlot(index);
        if (!slot.hasItem()) return empty;
        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (index == 0) {
            if (!moveItemStackTo(source, 1, 37, true)) return ItemStack.EMPTY;
        } else {
            if (!LinkModifierBlockEntity.isModifiableBook(source) || !moveItemStackTo(source, 0, 1, false)) return ItemStack.EMPTY;
        }
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }

    @Override public boolean stillValid(Player player) {
        return modifier.stillValid(player);
    }


    private record ClientContainer(Container container, BlockPos pos) {}
}
