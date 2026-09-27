package com.xcompwiz.mystcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Blocks;

/**
 * Installs the legacy Mystcraft block-colour wrappers after Minecraft has created its vanilla
 * BlockColors table.
 *
 * <p>Mystcraft 0.13.7.06 did not rely solely on Forge's registration event. Its client proxy
 * obtained Minecraft's live BlockColors instance and replaced the handlers for vanilla blocks.
 * On 1.21.1 the registration-event path is kept as the normal declaration, while this live-table
 * install reproduces the old ordering guarantee.  CP232 deliberately re-applies the wrappers when
 * a synchronized Age visual snapshot changes: CP231 runtime proved foliage survived but grass was
 * still being resolved by a later handler/path.</p>
 */
public final class AgeLegacyBlockTintInstaller {
    private static boolean installed;

    private AgeLegacyBlockTintInstaller() {}

    @SuppressWarnings("deprecation")
    public static boolean installIfReady() {
        if (installed) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        installNow(mc);
        installed = true;
        return true;
    }

    /** Re-assert the Legacy wrappers after a new Age visual payload becomes active. */
    @SuppressWarnings("deprecation")
    public static boolean reinstallForSnapshot() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        installNow(mc);
        installed = true;
        return true;
    }

    private static void installNow(Minecraft mc) {
        var colors = mc.getBlockColors();
        colors.register(AgeBlockColorHandlers::grass,
                Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.TALL_GRASS,
                Blocks.FERN, Blocks.LARGE_FERN, Blocks.POTTED_FERN, Blocks.SUGAR_CANE);
        colors.register(AgeBlockColorHandlers::foliage,
                Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES,
                Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
                Blocks.MANGROVE_LEAVES, Blocks.CHERRY_LEAVES, Blocks.VINE,
                Blocks.LILY_PAD, Blocks.MELON_STEM);
    }

    public static void reset() {
        installed = false;
    }
}
