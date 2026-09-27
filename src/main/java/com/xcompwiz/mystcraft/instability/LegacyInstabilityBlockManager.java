package com.xcompwiz.mystcraft.instability;

import com.xcompwiz.mystcraft.symbol.LegacyMaterialSymbolRegistry;
import com.xcompwiz.mystcraft.world.dimension.DenseOreResourceClassifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Legacy watched-block factors used by the persisted block-instability profiler.
 *
 * <p>CP291 deliberately does <strong>not</strong> use ore abundance to score Dense Ores.
 * Ore entries remain available for compatibility diagnostics but do not affect Dense Ores scoring. Non-ore
 * watched blocks (crystal/glowstone/runtime-fluid material symbols) may still contribute
 * their original block-instability load.</p>
 */
public final class LegacyInstabilityBlockManager {
    public record Factor(String logicalKey, float accessibilityFactor, float flatFactor, boolean oreResource) {}

    private static volatile Map<Block, Factor> cache;

    private LegacyInstabilityBlockManager() {}

    public static Map<Block, Factor> watchedBlocks() {
        Map<Block, Factor> current = cache;
        if (current != null) return current;
        synchronized (LegacyInstabilityBlockManager.class) {
            if (cache != null) return cache;
            LinkedHashMap<Block, Factor> out = new LinkedHashMap<>();

            // Mystcraft 0.13.7.06 factors. Normal/deepslate variants share one logical key.
            putOre(out, Blocks.COAL_ORE, "ore:coal", 5F, 1F); putOre(out, Blocks.DEEPSLATE_COAL_ORE, "ore:coal", 5F, 1F);
            putOre(out, Blocks.LAPIS_ORE, "ore:lapis", 5F, 1F); putOre(out, Blocks.DEEPSLATE_LAPIS_ORE, "ore:lapis", 5F, 1F);
            putOre(out, Blocks.IRON_ORE, "ore:iron", 60F, 1F); putOre(out, Blocks.DEEPSLATE_IRON_ORE, "ore:iron", 60F, 1F);
            putOre(out, Blocks.EMERALD_ORE, "ore:emerald", 200F, 2F); putOre(out, Blocks.DEEPSLATE_EMERALD_ORE, "ore:emerald", 200F, 2F);
            putOre(out, Blocks.REDSTONE_ORE, "ore:redstone", 250F, 2F); putOre(out, Blocks.DEEPSLATE_REDSTONE_ORE, "ore:redstone", 250F, 2F);
            putOre(out, Blocks.GOLD_ORE, "ore:gold", 750F, 4F); putOre(out, Blocks.DEEPSLATE_GOLD_ORE, "ore:gold", 750F, 4F);
            putOre(out, Blocks.DIAMOND_ORE, "ore:diamond", 4000F, 20F); putOre(out, Blocks.DEEPSLATE_DIAMOND_ORE, "ore:diamond", 4000F, 20F);
            putOre(out, Blocks.NETHER_QUARTZ_ORE, "ore:quartz", 20F, 4F);

            // Modern/foreign ores with no 1.12 factor are grouped for diagnostics. The
            // factor is intentionally conservative (iron-class) and never affects Dense score.
            for (Block block : BuiltInRegistries.BLOCK) {
                if (out.containsKey(block)) continue;
                if (DenseOreResourceClassifier.isOre(block.defaultBlockState())) {
                    out.put(block, new Factor("ore:dynamic_modded", 60F, 1F, true));
                }
            }

            Block crystal = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mystcraft", "blockcrystal"));
            if (crystal != Blocks.AIR) out.put(crystal, new Factor("mystcraft:crystal", 20F, 4F, false));
            out.put(Blocks.GLOWSTONE, new Factor("minecraft:glowstone", 50F, 4F, false));

            // Legacy fluid-symbol defaults are factor1=1.0 / factor2=0.25. Runtime
            // material symbols are non-ore and therefore retain their original score role.
            for (ResourceLocation id : LegacyMaterialSymbolRegistry.runtimeRegisteredFluidBlockIds()) {
                Block block = BuiltInRegistries.BLOCK.get(id);
                if (block != Blocks.AIR && !out.containsKey(block)) {
                    float[] factors = LegacyMaterialSymbolRegistry.runtimeFluidInstabilityFactors(id);
                    out.put(block, new Factor("fluid:" + id, factors[0], factors[1], false));
                }
            }

            cache = Map.copyOf(out);
            return cache;
        }
    }

    public static void invalidateCache() { cache = null; }

    public static Factor factor(BlockState state) { return watchedBlocks().get(state.getBlock()); }

    private static void putOre(Map<Block, Factor> out, Block block, String key, float f1, float f2) {
        out.put(block, new Factor(key, f1, f2, true));
    }
}
