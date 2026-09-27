package com.xcompwiz.mystcraft.inventory;

import com.xcompwiz.mystcraft.blockentity.InkMixerBlockEntity;
import com.xcompwiz.mystcraft.data.InkEffects;
import com.xcompwiz.mystcraft.linking.LinkProperties;
import com.xcompwiz.mystcraft.registry.MystMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Server-authoritative 0.13.7.06 Ink Mixer container bridge. */
public final class InkMixerMenu extends AbstractContainerMenu {
    public static final int ACTION_CATALYST_ALL = 0;
    public static final int ACTION_CATALYST_SINGLE = 1;
    private static final int INTERNAL_COUNT = 3;
    private static final int RESULT_SLOT = 3;
    private static final int PLAYER_START = 4;
    private static final int PLAYER_END = PLAYER_START + 36;
    private static final int PROB_SCALE = 10000;

    private final Container mixer;
    private final ResultContainer result = new ResultContainer();
    private final BlockPos pos;
    private int hasInk;
    private final int[] probabilities = new int[LinkProperties.LEGACY_INK_PROPERTIES.size()];

    public InkMixerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, new SimpleContainer(InkMixerBlockEntity.SLOT_COUNT), buffer.readBlockPos());
    }

    public InkMixerMenu(int containerId, Inventory inventory, Container mixer, BlockPos pos) {
        super(MystMenus.INK_MIXER.get(), containerId);
        checkContainerSize(mixer, InkMixerBlockEntity.SLOT_COUNT);
        this.mixer = mixer;
        this.pos = pos;
        mixer.startOpen(inventory.player);

        this.addSlot(new Slot(mixer, InkMixerBlockEntity.SLOT_INK_INPUT, 8, 27) {
            @Override public boolean mayPlace(ItemStack stack) { return mixer.canPlaceItem(getContainerSlot(), stack); }
        });
        this.addSlot(new Slot(mixer, InkMixerBlockEntity.SLOT_PAPER, 8, 48) {
            @Override public boolean mayPlace(ItemStack stack) { return mixer.canPlaceItem(getContainerSlot(), stack); }
        });
        this.addSlot(new Slot(mixer, InkMixerBlockEntity.SLOT_CONTAINER_OUTPUT, 152, 27) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });

        this.addSlot(new Slot(result, 0, 152, 48) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) {
                return InkMixerMenu.this.mixer instanceof InkMixerBlockEntity be && be.canBuildItem();
            }
            @Override public void onTake(Player player, ItemStack stack) {
                if (InkMixerMenu.this.mixer instanceof InkMixerBlockEntity be) {
                    be.buildItem(stack);
                    updateCraftResult();
                }
                super.onTake(player, stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 99 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) this.addSlot(new Slot(inventory, col, 8 + col * 18, 157));

        addDataSlot(new DataSlot() {
            @Override public int get() {
                if (InkMixerMenu.this.mixer instanceof InkMixerBlockEntity be) hasInk = be.hasInk() ? 1 : 0;
                return hasInk;
            }
            @Override public void set(int value) { hasInk = value != 0 ? 1 : 0; }
        });
        for (int i = 0; i < probabilities.length; i++) {
            final int index = i;
            addDataSlot(new DataSlot() {
                @Override public int get() {
                    if (InkMixerMenu.this.mixer instanceof InkMixerBlockEntity be) {
                        String property = LinkProperties.LEGACY_INK_PROPERTIES.get(index);
                        probabilities[index] = Math.round(be.getInkProbability(property) * PROB_SCALE);
                    }
                    return probabilities[index];
                }
                @Override public void set(int value) { probabilities[index] = Math.max(0, value); }
            });
        }
        updateCraftResult();
    }

    public BlockPos getBlockPos() { return pos; }
    public boolean hasInk() { return hasInk != 0; }
    public float getPropertyProbability(String property) {
        int index = LinkProperties.LEGACY_INK_PROPERTIES.indexOf(property);
        return index < 0 ? 0.0F : probabilities[index] / (float) PROB_SCALE;
    }

    /**
     * Evaluates the legacy Ink Mixer property ColorGradient at the supplied point.
     * This mirrors LinkingAPIDelegate#getPropertiesGradient + ColorGradient#getColor:
     * each probability contributes its registered color, long intervals are split at
     * 0.3, unused probability mass is black, and the gradient loops continuously.
     */
    public int getLegacyGradientColor(float point) {
        java.util.ArrayList<Integer> colors = new java.util.ArrayList<>();
        java.util.ArrayList<Float> intervals = new java.util.ArrayList<>();
        float total = 0.0F;

        for (String property : LinkProperties.LEGACY_INK_PROPERTIES) {
            float probability = getPropertyProbability(property);
            if (probability < 0.001F) continue;
            int color = InkEffects.getPropertyColor(property);
            float interval = probability;
            total += interval;
            if (interval > 0.3F) {
                colors.add(color);
                intervals.add(interval - 0.3F);
                interval = 0.3F;
            }
            colors.add(color);
            intervals.add(interval);
        }

        if (total < 0.99F) {
            float interval = Math.max(0.0F, 1.0F - total);
            if (interval > 0.3F) {
                colors.add(0x000000);
                intervals.add(interval - 0.3F);
                interval = 0.3F;
            }
            if (interval > 0.0F) {
                colors.add(0x000000);
                intervals.add(interval);
            }
        }

        if (colors.isEmpty()) return 0x000000;
        if (colors.size() == 1) return colors.getFirst();

        float length = 0.0F;
        for (float interval : intervals) length += interval;
        if (length <= 0.0F) return colors.getFirst();
        float cursor = point % length;
        if (cursor < 0.0F) cursor += length;

        int index = 0;
        while (index < intervals.size() - 1 && cursor >= intervals.get(index)) {
            cursor -= intervals.get(index);
            index++;
        }
        int next = (index + 1) % colors.size();
        float span = intervals.get(index);
        float t = span <= 0.0F ? 0.0F : cursor / span;
        int a = colors.get(index);
        int b = colors.get(next);
        int r = Math.round(((a >> 16) & 0xFF) * (1.0F - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1.0F - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1.0F - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    public int getBlendedInkColor() {
        float total = 0.0F;
        float r = 0.0F, g = 0.0F, b = 0.0F;
        for (String property : LinkProperties.LEGACY_INK_PROPERTIES) {
            float p = getPropertyProbability(property);
            if (p <= 0.0F) continue;
            int color = InkEffects.getPropertyColor(property);
            r += ((color >> 16) & 0xFF) * p;
            g += ((color >> 8) & 0xFF) * p;
            b += (color & 0xFF) * p;
            total += p;
        }
        if (total <= 0.0F) return 0x191919;
        int ri = Math.min(255, Math.round(r / total));
        int gi = Math.min(255, Math.round(g / total));
        int bi = Math.min(255, Math.round(b / total));
        return (ri << 16) | (gi << 8) | bi;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != ACTION_CATALYST_ALL && id != ACTION_CATALYST_SINGLE) return super.clickMenuButton(player, id);
        if (!(mixer instanceof InkMixerBlockEntity be) || player.level().isClientSide) return true;
        ItemStack carried = getCarried();
        if (carried.isEmpty() || !be.hasInk()) return true;
        int amount = id == ACTION_CATALYST_SINGLE ? 1 : carried.getCount();
        int consumed = be.addCatalysts(carried, amount);
        if (consumed > 0) {
            if (carried.isEmpty()) setCarried(ItemStack.EMPTY);
            updateCraftResult();
            broadcastChanges();
        }
        return true;
    }

    private void updateCraftResult() {
        if (mixer instanceof InkMixerBlockEntity be) result.setItem(0, be.getCraftedItem());
        else result.setItem(0, ItemStack.EMPTY);
    }

    @Override
    public void broadcastChanges() {
        updateCraftResult();
        super.broadcastChanges();
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == mixer) updateCraftResult();
    }

    @Override
    public boolean stillValid(Player player) { return mixer.stillValid(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack original = source.copy();

        if (index == RESULT_SLOT) {
            if (!(mixer instanceof InkMixerBlockEntity be) || !be.canBuildItem()) return ItemStack.EMPTY;
            ItemStack crafted = be.getCraftedItem();
            be.decorateResult(crafted);
            if (!moveItemStackTo(crafted, PLAYER_START, PLAYER_END, true) || !crafted.isEmpty()) return ItemStack.EMPTY;
            be.consumeBuiltItem();
            updateCraftResult();
            return original;
        }

        if (index < PLAYER_START) {
            if (!moveItemStackTo(source, PLAYER_START, PLAYER_END, false)) return ItemStack.EMPTY;
        } else if (mixer.canPlaceItem(InkMixerBlockEntity.SLOT_INK_INPUT, source)) {
            if (!moveItemStackTo(source, 0, 1, false)) return ItemStack.EMPTY;
        } else if (mixer.canPlaceItem(InkMixerBlockEntity.SLOT_PAPER, source)) {
            if (!moveItemStackTo(source, 1, 2, false)) return ItemStack.EMPTY;
        } else if (index < PLAYER_START + 27) {
            if (!moveItemStackTo(source, PLAYER_START + 27, PLAYER_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(source, PLAYER_START, PLAYER_START + 27, false)) return ItemStack.EMPTY;

        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (source.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, source);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        mixer.stopOpen(player);
    }
}
