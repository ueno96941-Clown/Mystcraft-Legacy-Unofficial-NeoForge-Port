package com.xcompwiz.mystcraft.world.dimension;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Shared Dense Ores resource classifier, including mod ores. */
public final class DenseOreResourceClassifier {
    private static final TagKey<Block> COMMON_ORES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));
    private static final TagKey<Block> FORGE_ORES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("forge", "ores"));

    private DenseOreResourceClassifier() {}

    public static boolean isOre(BlockState state) {
        if (state == null) return false;
        if (state.is(COMMON_ORES) || state.is(FORGE_ORES)) return true;
        Block block = state.getBlock();
        if (isVanillaOre(block)) return true;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || "minecraft".equals(id.getNamespace())) return false;
        String path = id.getPath();
        return path.endsWith("_ore") || path.contains("_ore_") || path.startsWith("ore_");
    }

    private static boolean isVanillaOre(Block block) {
        return block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE
                || block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE
                || block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE
                || block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE
                || block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE
                || block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE
                || block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE
                || block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE
                || block == Blocks.NETHER_QUARTZ_ORE || block == Blocks.NETHER_GOLD_ORE;
    }
}
