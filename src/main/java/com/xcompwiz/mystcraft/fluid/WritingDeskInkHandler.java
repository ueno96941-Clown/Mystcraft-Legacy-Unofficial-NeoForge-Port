package com.xcompwiz.mystcraft.fluid;

import com.xcompwiz.mystcraft.blockentity.WritingDeskBlockEntity;
import com.xcompwiz.mystcraft.registry.MystFluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** External NeoForge fluid capability view for the Writing Desk inkwell. */
public final class WritingDeskInkHandler implements IFluidHandler {
    private final WritingDeskBlockEntity desk;

    public WritingDeskInkHandler(WritingDeskBlockEntity desk) {
        this.desk = desk;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (tank != 0 || desk.inkAmount() <= 0) return FluidStack.EMPTY;
        return new FluidStack(MystFluids.BLACK_INK.get(), desk.inkAmount());
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == 0 ? WritingDeskBlockEntity.CAPACITY : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 && !stack.isEmpty() && stack.is(MystFluids.BLACK_INK.get());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!isFluidValid(0, resource)) return 0;
        return desk.fillInk(resource.getAmount(), action.execute());
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !resource.is(MystFluids.BLACK_INK.get())) return FluidStack.EMPTY;
        int drained = desk.drainInk(resource.getAmount(), action.execute());
        return drained <= 0 ? FluidStack.EMPTY : new FluidStack(MystFluids.BLACK_INK.get(), drained);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        int drained = desk.drainInk(maxDrain, action.execute());
        return drained <= 0 ? FluidStack.EMPTY : new FluidStack(MystFluids.BLACK_INK.get(), drained);
    }
}
