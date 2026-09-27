package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.network.AgeClientVisualState;
import com.xcompwiz.mystcraft.world.worldgen.AgeBiomeTintBridge;
import com.xcompwiz.mystcraft.world.worldgen.AgeVisualSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Legacy Mystcraft static biome-colour bridge for block-model tint callbacks.
 *
 * <p>Do not compare the supplied BlockAndTintGetter with ClientLevel by object identity: chunk
 * meshing normally supplies a RenderChunkRegion view. The current Age is authoritative; the
 * supplied getter is only used for vanilla fallback sampling.</p>
 */
public final class AgeBlockColorHandlers {
    private AgeBlockColorHandlers() {}

    private static AgeVisualSnapshot snapshot() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        var entry = AgeClientVisualState.get(mc.level.dimension().location().toString());
        return entry == null ? null : entry.snapshot();
    }

    public static int grass(BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex) {
        int vanilla = level == null || pos == null ? 0x91BD59 : BiomeColors.getAverageGrassColor(level, pos);
        return AgeBiomeTintBridge.resolve(snapshot(), AgeBiomeTintBridge.Channel.GRASS, vanilla);
    }

    public static int foliage(BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex) {
        int vanilla;
        if (state != null && state.is(Blocks.SPRUCE_LEAVES)) vanilla = FoliageColor.getEvergreenColor();
        else if (state != null && state.is(Blocks.BIRCH_LEAVES)) vanilla = FoliageColor.getBirchColor();
        else vanilla = level == null || pos == null ? 0x48B518 : BiomeColors.getAverageFoliageColor(level, pos);
        return AgeBiomeTintBridge.resolve(snapshot(), AgeBiomeTintBridge.Channel.FOLIAGE, vanilla);
    }

    /**
     * Retained for model/block callers and regression harnesses. Vanilla 1.21.1 liquid surfaces
     * are rendered through the fluid renderer, so ColorWater needs a separate fluid-path repair.
     */
    public static int water(BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex) {
        int vanilla = level == null || pos == null ? 0x3F76E4 : BiomeColors.getAverageWaterColor(level, pos);
        return AgeBiomeTintBridge.resolve(snapshot(), AgeBiomeTintBridge.Channel.WATER, vanilla);
    }
    /**
     * CP234 liquid-render bridge. The vanilla/NeoForge Water FluidType extension remains the
     * owner of textures and the baseline tint. We only replace that already-computed tint while
     * a synchronized Mystcraft Age snapshot exists, avoiding duplicate FluidType registration.
     */
    public static int waterFluidTint(int originalArgb, BlockAndTintGetter level, BlockPos pos) {
        AgeVisualSnapshot snap = snapshot();
        if (snap == null) return originalArgb;
        int rgb = AgeBiomeTintBridge.resolve(snap, AgeBiomeTintBridge.Channel.WATER, originalArgb & 0x00FFFFFF);
        int alpha = originalArgb & 0xFF000000;
        if (alpha == 0) alpha = 0xFF000000;
        return alpha | (rgb & 0x00FFFFFF);
    }

    public static int finalModelTint(BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex, int original) {
        if (state == null) return original;
        if (isGrassTintBlock(state)) {
            int vanilla = original >= 0 ? original & 0x00FFFFFF
                    : (level == null || pos == null ? 0x91BD59 : BiomeColors.getAverageGrassColor(level, pos));
            return AgeBiomeTintBridge.resolve(snapshot(), AgeBiomeTintBridge.Channel.GRASS, vanilla);
        }
        if (isFoliageTintBlock(state)) {
            int vanilla = original >= 0 ? original & 0x00FFFFFF : foliage(state, level, pos, tintIndex);
            return AgeBiomeTintBridge.resolve(snapshot(), AgeBiomeTintBridge.Channel.FOLIAGE, vanilla);
        }
        return original;
    }

    public static boolean isGrassTintBlock(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.POTTED_FERN)
                || state.is(Blocks.SUGAR_CANE);
    }

    public static boolean isFoliageTintBlock(BlockState state) {
        return state.is(Blocks.OAK_LEAVES) || state.is(Blocks.SPRUCE_LEAVES) || state.is(Blocks.BIRCH_LEAVES)
                || state.is(Blocks.JUNGLE_LEAVES) || state.is(Blocks.ACACIA_LEAVES) || state.is(Blocks.DARK_OAK_LEAVES)
                || state.is(Blocks.MANGROVE_LEAVES) || state.is(Blocks.CHERRY_LEAVES) || state.is(Blocks.VINE)
                || state.is(Blocks.LILY_PAD) || state.is(Blocks.MELON_STEM);
    }

}
