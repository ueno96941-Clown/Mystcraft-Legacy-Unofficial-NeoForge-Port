package com.xcompwiz.mystcraft.instability;

import com.xcompwiz.mystcraft.registry.MystBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Modern block-name transcription of EffectCrumble.initMappings() from Mystcraft 0.13.7.06.
 *
 * <p>Only legacy materials, plus modern block IDs that directly split a legacy metadata state into
 * a distinct block, are handled here. Post-1.12 material families such as deepslate/copper/cherry
 * are deliberately not invented in CP296.</p>
 */
public final class LegacyInstabilityCrumbleMappings {
    private LegacyInstabilityCrumbleMappings() {}

    public static BlockState replacement(BlockState state) {
        if (state == null) return null;

        // Ores from the 1.12 mapping. Emerald was not present in EffectCrumble.initMappings().
        if (state.is(Blocks.COAL_ORE)
                || state.is(Blocks.IRON_ORE)
                || state.is(Blocks.REDSTONE_ORE)
                || state.is(Blocks.GOLD_ORE)
                || state.is(Blocks.LAPIS_ORE)) {
            return Blocks.STONE.defaultBlockState();
        }
        if (state.is(Blocks.DIAMOND_ORE)) return Blocks.COAL_ORE.defaultBlockState();

        if (state.is(Blocks.ICE)) return Blocks.WATER.defaultBlockState();
        if (state.is(Blocks.GLOWSTONE)) return Blocks.GLASS.defaultBlockState();
        if (state.is(MystBlocks.CRYSTAL.get())) return Blocks.GLASS.defaultBlockState();

        if (state.is(Blocks.NETHER_BRICKS)) return Blocks.NETHERRACK.defaultBlockState();
        if (state.is(Blocks.NETHER_QUARTZ_ORE)) return Blocks.NETHERRACK.defaultBlockState();
        if (state.is(Blocks.NETHERRACK)) return Blocks.SOUL_SAND.defaultBlockState();
        if (state.is(Blocks.SOUL_SAND)) return Blocks.GRAVEL.defaultBlockState();

        // Blocks.STONEBRICK metadata states became separate modern blocks.
        if (state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.MOSSY_STONE_BRICKS)
                || state.is(Blocks.CRACKED_STONE_BRICKS)
                || state.is(Blocks.CHISELED_STONE_BRICKS)) {
            return Blocks.STONE.defaultBlockState();
        }

        // Blocks.STONE metadata states in 1.12.
        if (state.is(Blocks.STONE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.POLISHED_GRANITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.POLISHED_DIORITE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.POLISHED_ANDESITE)
                || state.is(Blocks.COBBLESTONE)) {
            return Blocks.GRAVEL.defaultBlockState();
        }

        if (state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
                || state.is(Blocks.RED_MUSHROOM_BLOCK)
                || state.is(Blocks.CLAY)) {
            return Blocks.DIRT.defaultBlockState();
        }

        // Blocks.DIRT metadata split into distinct modern block IDs.
        if (state.is(Blocks.GRAVEL)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.GLASS)
                || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.CHISELED_SANDSTONE)
                || state.is(Blocks.SMOOTH_SANDSTONE)) {
            return Blocks.SAND.defaultBlockState();
        }

        // Blocks.LOG/LOG2 covered the six pre-1.13 wood species and always degraded to default oak
        // planks. Stripped/newer wood types did not exist and are intentionally excluded.
        if (state.is(Blocks.OAK_LOG)
                || state.is(Blocks.SPRUCE_LOG)
                || state.is(Blocks.BIRCH_LOG)
                || state.is(Blocks.JUNGLE_LOG)
                || state.is(Blocks.ACACIA_LOG)
                || state.is(Blocks.DARK_OAK_LOG)) {
            return Blocks.OAK_PLANKS.defaultBlockState();
        }
        if (state.is(Blocks.OAK_PLANKS)
                || state.is(Blocks.SPRUCE_PLANKS)
                || state.is(Blocks.BIRCH_PLANKS)
                || state.is(Blocks.JUNGLE_PLANKS)
                || state.is(Blocks.ACACIA_PLANKS)
                || state.is(Blocks.DARK_OAK_PLANKS)) {
            return Blocks.DIRT.defaultBlockState();
        }

        // 1.12 wool was one metadata block: colored -> default white -> cobweb.
        if (state.is(Blocks.WHITE_WOOL)) return Blocks.COBWEB.defaultBlockState();
        if (isLegacyColoredWool(state)) return Blocks.WHITE_WOOL.defaultBlockState();

        if (isLegacySapling(state)
                || state.is(Blocks.COBWEB)
                || isLegacyLeaves(state)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.BROWN_MUSHROOM)
                || state.is(Blocks.RED_MUSHROOM)
                || isLegacyFlower(state)) {
            return Blocks.AIR.defaultBlockState();
        }

        return null;
    }

    private static boolean isLegacyColoredWool(BlockState state) {
        return state.is(Blocks.ORANGE_WOOL)
                || state.is(Blocks.MAGENTA_WOOL)
                || state.is(Blocks.LIGHT_BLUE_WOOL)
                || state.is(Blocks.YELLOW_WOOL)
                || state.is(Blocks.LIME_WOOL)
                || state.is(Blocks.PINK_WOOL)
                || state.is(Blocks.GRAY_WOOL)
                || state.is(Blocks.LIGHT_GRAY_WOOL)
                || state.is(Blocks.CYAN_WOOL)
                || state.is(Blocks.PURPLE_WOOL)
                || state.is(Blocks.BLUE_WOOL)
                || state.is(Blocks.BROWN_WOOL)
                || state.is(Blocks.GREEN_WOOL)
                || state.is(Blocks.RED_WOOL)
                || state.is(Blocks.BLACK_WOOL);
    }

    private static boolean isLegacySapling(BlockState state) {
        return state.is(Blocks.OAK_SAPLING)
                || state.is(Blocks.SPRUCE_SAPLING)
                || state.is(Blocks.BIRCH_SAPLING)
                || state.is(Blocks.JUNGLE_SAPLING)
                || state.is(Blocks.ACACIA_SAPLING)
                || state.is(Blocks.DARK_OAK_SAPLING);
    }

    private static boolean isLegacyLeaves(BlockState state) {
        // EffectCrumble registered Blocks.LEAVES but not Blocks.LEAVES2 in 1.12.
        return state.is(Blocks.OAK_LEAVES)
                || state.is(Blocks.SPRUCE_LEAVES)
                || state.is(Blocks.BIRCH_LEAVES)
                || state.is(Blocks.JUNGLE_LEAVES);
    }

    private static boolean isLegacyFlower(BlockState state) {
        return state.is(Blocks.DANDELION)
                || state.is(Blocks.POPPY)
                || state.is(Blocks.BLUE_ORCHID)
                || state.is(Blocks.ALLIUM)
                || state.is(Blocks.AZURE_BLUET)
                || state.is(Blocks.RED_TULIP)
                || state.is(Blocks.ORANGE_TULIP)
                || state.is(Blocks.WHITE_TULIP)
                || state.is(Blocks.PINK_TULIP)
                || state.is(Blocks.OXEYE_DAISY);
    }
}
