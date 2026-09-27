package com.xcompwiz.mystcraft.blockentity;

import com.xcompwiz.mystcraft.data.InkEffects;
import com.xcompwiz.mystcraft.data.ModLinkEffects;
import com.xcompwiz.mystcraft.inventory.InkMixerMenu;
import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.registry.MystBlockEntities;
import com.xcompwiz.mystcraft.registry.MystFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/** Faithful one-charge Ink Mixer basin from Mystcraft 0.13.7.06. */
public final class InkMixerBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int SLOT_INK_INPUT = 0;
    public static final int SLOT_PAPER = 1;
    public static final int SLOT_CONTAINER_OUTPUT = 2;
    public static final int SLOT_COUNT = 3;
    public static final int BASIN_VOLUME = FluidType.BUCKET_VOLUME;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final LinkedHashMap<String, Float> inkProbabilities = new LinkedHashMap<>();
    private boolean hasInk;
    private long nextSeed = new Random().nextLong();

    public InkMixerBlockEntity(BlockPos pos, BlockState state) {
        super(MystBlockEntities.INK_MIXER.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mystcraft.ink_mixer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new InkMixerMenu(containerId, playerInventory, this, worldPosition);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, InkMixerBlockEntity mixer) {
        if (!level.isClientSide) mixer.tryFillBasinFromInput();
    }

    public boolean hasInk() { return hasInk; }
    public long getNextSeed() { return nextSeed; }
    public Map<String, Float> getInkProperties() { return Collections.unmodifiableMap(inkProbabilities); }
    public float getInkProbability(String property) { return inkProbabilities.getOrDefault(property, 0.0F); }

    public boolean canBuildItem() {
        return hasInk && items.get(SLOT_PAPER).is(Items.PAPER);
    }

    public ItemStack getCraftedItem() {
        return canBuildItem() ? Page.createLinkPage() : ItemStack.EMPTY;
    }

    /** Applies the current deterministic property roll without consuming the basin. */
    public void decorateResult(ItemStack result) {
        if (!canBuildItem() || result.isEmpty()) return;
        Random random = new Random(nextSeed);
        for (Map.Entry<String, Float> entry : inkProbabilities.entrySet()) {
            float threshold = entry.getValue() * 100.0F;
            if (random.nextInt(100) < threshold) Page.addLinkProperty(result, entry.getKey());
        }
    }

    /** Commits one craft after a decorated result has actually been taken. */
    public void consumeBuiltItem() {
        if (!canBuildItem()) return;
        Random random = new Random(nextSeed);
        // Advance through the exact same random calls used by decorateResult.
        for (int ignored = 0; ignored < inkProbabilities.size(); ignored++) random.nextInt(100);
        nextSeed = random.nextLong();
        hasInk = false;
        inkProbabilities.clear();
        items.get(SLOT_PAPER).shrink(1);
        if (items.get(SLOT_PAPER).isEmpty()) items.set(SLOT_PAPER, ItemStack.EMPTY);
        setChanged();
    }

    public void buildItem(ItemStack result) {
        if (!canBuildItem() || result.isEmpty()) return;
        decorateResult(result);
        consumeBuiltItem();
    }

    /**
     * Consumes up to amount catalysts using the exact 0.13.7.06 compounding formula.
     * Returns how many were consumed.
     */
    public int addCatalysts(ItemStack stack, int amount) {
        if (!hasInk || stack.isEmpty() || amount <= 0) return 0;
        Map<String, Float> effects = InkEffects.getItemEffects(stack);
        if (effects == null) return 0;

        float total = 0.0F;
        for (Map.Entry<String, Float> entry : effects.entrySet()) {
            if (entry.getKey().isEmpty() || !ModLinkEffects.isPropertyAllowed(entry.getKey())) continue;
            total += entry.getValue();
        }
        float inverse = 1.0F - total;
        int count = Math.min(amount, stack.getCount());
        if (count <= 0) return 0;

        for (int n = 0; n < count; n++) {
            stack.shrink(1);
            inkProbabilities.replaceAll((property, probability) -> probability * inverse);
            for (Map.Entry<String, Float> entry : effects.entrySet()) {
                String property = entry.getKey();
                if (property.isEmpty() || !ModLinkEffects.isPropertyAllowed(property)) continue;
                inkProbabilities.merge(property, entry.getValue(), Float::sum);
            }
        }
        setChanged();
        return count;
    }

    private void tryFillBasinFromInput() {
        if (hasInk) return;
        ItemStack input = items.get(SLOT_INK_INPUT);
        if (input.isEmpty()) return;

        ItemStack single = input.copyWithCount(1);
        IFluidHandlerItem handler = single.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) return;

        FluidStack selected = FluidStack.EMPTY;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack contained = handler.getFluidInTank(tank);
            if (contained.isEmpty() || contained.getAmount() != BASIN_VOLUME) continue;
            if (!isValidInk(contained)) continue;
            FluidStack simulated = handler.drain(contained.copy(), IFluidHandler.FluidAction.SIMULATE);
            if (!simulated.isEmpty() && simulated.getAmount() == BASIN_VOLUME) {
                selected = contained.copy();
                break;
            }
        }
        if (selected.isEmpty()) return;

        FluidStack drained = handler.drain(selected, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty() || drained.getAmount() != BASIN_VOLUME) return;
        ItemStack emptyContainer = handler.getContainer().copy();
        if (!canMergeOutput(emptyContainer)) return;

        input.shrink(1);
        if (input.isEmpty()) items.set(SLOT_INK_INPUT, ItemStack.EMPTY);
        mergeOutput(emptyContainer);
        hasInk = true;
        inkProbabilities.clear();
        setChanged();
    }

    private static boolean isValidInk(FluidStack stack) {
        // 0.13.7.06 accepted names registered in Mystcraft.validInks. The port
        // currently restores the canonical black ink entry; compatibility inks
        // can be added here during the API/IMC pass without changing mixer state.
        return stack.is(MystFluids.BLACK_INK.get());
    }

    private boolean canMergeOutput(ItemStack stack) {
        if (stack.isEmpty()) return true;
        ItemStack existing = items.get(SLOT_CONTAINER_OUTPUT);
        if (existing.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(existing, stack)
                && existing.getCount() + stack.getCount() <= existing.getMaxStackSize();
    }

    private void mergeOutput(ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack existing = items.get(SLOT_CONTAINER_OUTPUT);
        if (existing.isEmpty()) items.set(SLOT_CONTAINER_OUTPUT, stack.copy());
        else existing.grow(stack.getCount());
    }

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        if (!canPlaceItem(slot, stack) && !stack.isEmpty()) return;
        items.set(slot, stack);
        if (!stack.isEmpty()) {
            int max = Math.min(getMaxStackSize(), stack.getMaxStackSize());
            if (stack.getCount() > max) stack.setCount(max);
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_PAPER) return stack.is(Items.PAPER);
        if (slot == SLOT_CONTAINER_OUTPUT) return false;
        if (slot != SLOT_INK_INPUT || stack.isEmpty()) return false;
        ItemStack single = stack.copyWithCount(1);
        IFluidHandlerItem handler = single.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) return false;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluid = handler.getFluidInTank(tank);
            // Legacy canAcceptItem only checked whether the contained fluid was a
            // registered ink. A partial/non-bucket container was accepted but would
            // simply remain in the input slot until it contained exactly 1000 mB.
            if (!fluid.isEmpty() && isValidInk(fluid)) return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override public void clearContent() { items.clear(); setChanged(); }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        hasInk = tag.getBoolean("ink");
        inkProbabilities.clear();
        if (tag.contains("probabilities", Tag.TAG_COMPOUND)) {
            CompoundTag probabilities = tag.getCompound("probabilities");
            for (String key : probabilities.getAllKeys()) {
                if (probabilities.contains(key, Tag.TAG_FLOAT)) inkProbabilities.put(key, probabilities.getFloat(key));
            }
        }
        // Legacy did not persist next_seed. Preserve that quirk for existing worlds.
        if (tag.contains("next_seed", Tag.TAG_LONG)) nextSeed = tag.getLong("next_seed");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putBoolean("ink", hasInk);
        CompoundTag probabilities = new CompoundTag();
        inkProbabilities.forEach(probabilities::putFloat);
        tag.put("probabilities", probabilities);
        // Do not write next_seed: 0.13.7.06 intentionally/accidentally regenerated it on load.
    }
}
