package com.xcompwiz.mystcraft.fluid;

import com.xcompwiz.mystcraft.registry.MystFluids;
import com.xcompwiz.mystcraft.registry.MystItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * Fixed one-bucket black-ink capability for the legacy Ink Vial.
 *
 * <p>This intentionally mirrors the 1.12 ItemInkVial behavior rather than turning
 * the vial into a general-purpose tank.  A filled vial can only be drained, and a
 * successful drain yields a vanilla glass bottle.</p>
 */
public final class InkVialFluidHandler implements IFluidHandlerItem {
    public static final int CAPACITY = FluidType.BUCKET_VOLUME;

    private ItemStack container;
    private boolean filled;

    public InkVialFluidHandler(ItemStack stack) {
        this.container = stack;
        this.filled = !stack.isEmpty() && stack.is(MystItems.INK_VIAL.get());
    }

    @Override
    public ItemStack getContainer() {
        return container;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (tank != 0 || !filled) return FluidStack.EMPTY;
        return new FluidStack(MystFluids.BLACK_INK.get(), CAPACITY);
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == 0 ? CAPACITY : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 && stack.is(MystFluids.BLACK_INK.get());
    }

    @Override
    public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
        // The legacy filled vial itself was not refillable.  Recipes create a filled
        // vial from a glass bottle; this capability exists for fluid-aware machines.
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
        if (resource.isEmpty() || resource.getAmount() < CAPACITY || !resource.is(MystFluids.BLACK_INK.get())) {
            return FluidStack.EMPTY;
        }
        return drain(CAPACITY, action);
    }

    @Override
    public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
        if (!filled || maxDrain < CAPACITY) return FluidStack.EMPTY;
        FluidStack drained = new FluidStack(MystFluids.BLACK_INK.get(), CAPACITY);
        if (action.execute()) {
            filled = false;
            container = new ItemStack(Items.GLASS_BOTTLE);
        }
        return drained;
    }
}
