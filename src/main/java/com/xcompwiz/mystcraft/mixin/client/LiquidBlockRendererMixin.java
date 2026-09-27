package com.xcompwiz.mystcraft.mixin.client;

import com.xcompwiz.mystcraft.client.AgeBlockColorHandlers;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Result-fidelity hook for legacy ColorWater.
 *
 * <p>NeoForge owns the client extension for its built-in WATER_TYPE and rejects duplicate
 * registrations. CP233 attempted to replace that extension and therefore crashed during client
 * initialization. CP234 leaves NeoForge's extension untouched and intercepts only the tint value
 * consumed by LiquidBlockRenderer. Non-water fluids are delegated verbatim; water outside a
 * synchronized Mystcraft Age is also returned verbatim by AgeBlockColorHandlers.</p>
 */
@Mixin(LiquidBlockRenderer.class)
public abstract class LiquidBlockRendererMixin {
    @Redirect(
            method = "tesselate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;getTintColor(Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I",
                    remap = false
            )
    )
    private int mystcraft$ageWaterTint(
            IClientFluidTypeExtensions extension,
            FluidState state,
            BlockAndTintGetter level,
            BlockPos pos) {
        int original = extension.getTintColor(state, level, pos);
        if (!state.is(FluidTags.WATER)) return original;
        return AgeBlockColorHandlers.waterFluidTint(original, level, pos);
    }
}
