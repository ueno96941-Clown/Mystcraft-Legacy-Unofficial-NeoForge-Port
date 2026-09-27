package com.xcompwiz.mystcraft.instability;

import com.xcompwiz.mystcraft.api.event.MeteorEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Modern executor for the terrain half of 0.13.7.06 ExplosionAdvanced. */
public final class LegacyMeteorExplosionExecutor {
    private LegacyMeteorExplosionExecutor() {}

    public static List<BlockPos> explode(ServerLevel level, Entity meteor,
                                         double x, double y, double z,
                                         LegacyInstabilityMeteorModel.ExplosionBurst burst) {
        // Keep modern damage-source/armor/knockback integration, while terrain mutation below follows
        // the legacy 16^3 boundary-ray algorithm and no-drop effect ordering.
        level.explode(meteor, x, y, z, burst.power(), false, Level.ExplosionInteraction.NONE);

        Set<BlockPos> affected = traceLegacyBlocks(level, meteor, x, y, z, burst.power());
        Random effectRandom = new Random(); // ExplosionAdvanced owned an independent java.util.Random.
        ArrayList<BlockPos> applied = new ArrayList<>(affected.size());

        for (BlockPos pos : affected) {
            if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) continue;
            BlockState before = level.getBlockState(pos);
            if (!before.isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }

            if (burst.flaming()) {
                BlockPos below = pos.below();
                if (level.getBlockState(pos).isAir()
                        && level.getBlockState(below).isSolidRender(level, below)
                        && effectRandom.nextInt(LegacyInstabilityMeteorModel.FIRE_CHANCE_DENOMINATOR) == 0) {
                    level.setBlock(pos, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
                }
            }

            if (burst.addOres()) {
                BlockPos below = pos.below();
                if (effectRandom.nextInt(LegacyInstabilityMeteorModel.ORE_CHANCE_DENOMINATOR) == 0
                        && level.getBlockState(pos).isAir()
                        && level.getBlockState(below).isSolidRender(level, below)) {
                    BlockState ore = switch (LegacyInstabilityMeteorModel.defaultOreChoice(effectRandom.nextFloat())) {
                        case COAL -> Blocks.COAL_ORE.defaultBlockState();
                        case IRON -> Blocks.IRON_ORE.defaultBlockState();
                        case GOLD -> Blocks.GOLD_ORE.defaultBlockState();
                    };
                    level.setBlock(pos, ore, Block.UPDATE_ALL);
                }
            }
            applied.add(pos.immutable());
        }

        NeoForge.EVENT_BUS.post(new MeteorEvent.MetorExplosion(meteor, applied));
        return List.copyOf(applied);
    }

    private static Set<BlockPos> traceLegacyBlocks(ServerLevel level, Entity meteor,
                                                    double originX, double originY, double originZ,
                                                    float explosionSize) {
        HashSet<BlockPos> blocks = new HashSet<>();
        RandomSource random = level.getRandom();
        int n = LegacyInstabilityMeteorModel.EXPLOSION_RAY_GRID_SIZE;
        float step = LegacyInstabilityMeteorModel.EXPLOSION_RAY_STEP;

        for (int gx = 0; gx < n; gx++) {
            for (int gy = 0; gy < n; gy++) {
                for (int gz = 0; gz < n; gz++) {
                    if (gx != 0 && gx != n - 1 && gy != 0 && gy != n - 1 && gz != 0 && gz != n - 1) continue;
                    double dx = gx / (n - 1.0D) * 2.0D - 1.0D;
                    double dy = gy / (n - 1.0D) * 2.0D - 1.0D;
                    double dz = gz / (n - 1.0D) * 2.0D - 1.0D;
                    double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    dx /= length;
                    dy /= length;
                    dz /= length;
                    float power = LegacyInstabilityMeteorModel.initialRayPower(explosionSize, random.nextFloat());
                    double px = originX;
                    double py = originY;
                    double pz = originZ;

                    while (power > 0.0F) {
                        BlockPos pos = BlockPos.containing(px, py, pz);
                        if (pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight()) {
                            BlockState state = level.getBlockState(pos);
                            float resistance = state.isAir() ? 0.0F : state.getBlock().getExplosionResistance();
                            power -= (resistance + LegacyInstabilityMeteorModel.EXPLOSION_RESISTANCE_BIAS) * step;
                            if (power > 0.0F) blocks.add(pos.immutable());
                        }
                        px += dx * step;
                        py += dy * step;
                        pz += dz * step;
                        power -= step * LegacyInstabilityMeteorModel.EXPLOSION_RAY_DECAY_MULTIPLIER;
                    }
                }
            }
        }
        return blocks;
    }
}
