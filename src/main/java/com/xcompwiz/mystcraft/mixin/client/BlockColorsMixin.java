package com.xcompwiz.mystcraft.mixin.client;

import com.xcompwiz.mystcraft.client.AgeBlockColorHandlers;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Final-result hook for legacy ColorGrass / ColorFoliage.
 *
 * Registration order and model tint-source internals changed substantially after 1.12. Rather
 * than competing with vanilla registrations, alter only the colour that BlockColors has already
 * resolved. Non-target blocks and non-Mystcraft dimensions are returned untouched.
 */
@Mixin(BlockColors.class)
public abstract class BlockColorsMixin {
    @Inject(
            method = "getColor(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;I)I",
            at = @At("RETURN"),
            cancellable = true
    )
    private void mystcraft$finalAgeBlockTint(
            BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex,
            CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValue();
        int resolved = AgeBlockColorHandlers.finalModelTint(state, level, pos, tintIndex, original);
        if (resolved != original) cir.setReturnValue(resolved);
    }
}
