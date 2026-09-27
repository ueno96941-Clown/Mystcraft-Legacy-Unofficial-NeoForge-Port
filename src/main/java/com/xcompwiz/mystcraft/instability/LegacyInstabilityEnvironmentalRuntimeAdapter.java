package com.xcompwiz.mystcraft.instability;

import com.xcompwiz.mystcraft.entity.EntityMeteor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * CP299 Minecraft-facing adapter for the five active environmental Instability providers.
 *
 * <p>Every path reproduces trigger/selection/coordinate work and re-checks
 * {@link InstabilityPolicy#allowProvider(String)} immediately before mutation.</p>
 */
public final class LegacyInstabilityEnvironmentalRuntimeAdapter {
    private LegacyInstabilityEnvironmentalRuntimeAdapter() {}

    public static AttemptResult tick(
            ServerLevel level,
            LevelChunk chunk,
            LegacyInstabilityEnvironmentalPlanService.EnvironmentalPlan plan,
            int effectIndex) {
        if (level == null || chunk == null || plan == null) return AttemptResult.INVALID_INPUT;
        if (effectIndex < 0 || effectIndex >= plan.effectInstances()) return AttemptResult.INVALID_EFFECT_INDEX;

        return switch (plan.spec().effectKind()) {
            case BURNING -> tickBurning(level, chunk, plan);
            case LIGHTNING -> tickLightning(level, chunk, plan, effectIndex);
            case EXPLOSION -> tickExplosion(level, chunk, plan, effectIndex);
            case CRUMBLE -> tickCrumble(level, chunk, plan, effectIndex);
            case METEOR -> tickMeteor(level, chunk, plan, effectIndex);
        };
    }

    private static AttemptResult tickBurning(
            ServerLevel level,
            LevelChunk chunk,
            LegacyInstabilityEnvironmentalPlanService.EnvironmentalPlan plan) {
        if (LegacyInstabilityRuntimeRandom.nextInt(level, plan.spec().chanceDenominator()) != 0) {
            return AttemptResult.CHANCE_MISSED;
        }

        int slot = LegacyInstabilityRuntimeRandom.nextInt(level, LegacyInstabilityPotionRuntimeMath.LEGACY_VERTICAL_SLOTS);
        var band = LegacyInstabilityPotionRuntimeMath.band(level.getMinBuildHeight(), level.getHeight(), slot);
        ChunkPos chunkPos = chunk.getPos();
        AABB bounds = new AABB(
                chunkPos.getMinBlockX(), band.minY(), chunkPos.getMinBlockZ(),
                chunkPos.getMinBlockX() + 16, band.maxExclusiveY(), chunkPos.getMinBlockZ() + 16);
        List<Entity> candidates = level.getEntitiesOfClass(Entity.class, bounds, entity -> {
            if (entity == null || entity.isRemoved()) return false;
            ChunkPos entityChunk = entity.chunkPosition();
            return entityChunk.x == chunkPos.x
                    && entityChunk.z == chunkPos.z
                    && LegacyInstabilityPotionRuntimeMath.containsY(band, entity.getBlockY());
        });
        if (candidates.isEmpty()) return AttemptResult.EMPTY_VERTICAL_BAND;

        Entity selected = candidates.get(LegacyInstabilityRuntimeRandom.nextInt(level, candidates.size()));
        if (!level.canSeeSky(selected.blockPosition())) return AttemptResult.TARGET_REJECTED;

        if (!plan.executionAllowed() || !InstabilityPolicy.allowProvider(plan.providerId())) {
            return AttemptResult.BLOCKED_AT_FINAL_GATE;
        }

        selected.igniteForSeconds(plan.spec().fireSecondsForProviderLevel(plan.providerLevel()));
        return AttemptResult.APPLIED_BURNING;
    }

    private static AttemptResult tickLightning(
            ServerLevel level,
            LevelChunk chunk,
            LegacyInstabilityEnvironmentalPlanService.EnvironmentalPlan plan,
            int effectIndex) {
        boolean trigger;
        if (level.isRaining() && level.isThundering()) {
            trigger = LegacyInstabilityRuntimeRandom.nextInt(level, plan.spec().thunderChanceDenominator()) == 0;
            if (!trigger) trigger = LegacyInstabilityRuntimeRandom.nextInt(level, plan.spec().fallbackChanceDenominator()) == 0;
        } else {
            trigger = LegacyInstabilityRuntimeRandom.nextInt(level, plan.spec().fallbackChanceDenominator()) == 0;
        }
        if (!trigger) return AttemptResult.CHANCE_MISSED;

        int lcg = LegacyInstabilityEnvironmentalRuntimeState.advance(level, plan.providerId(), effectIndex);
        int coords = LegacyInstabilityEnvironmentalRuntimeMath.coordsFromLcg(lcg);
        int x = chunk.getPos().getMinBlockX() + LegacyInstabilityEnvironmentalRuntimeMath.localX(coords);
        int z = chunk.getPos().getMinBlockZ() + LegacyInstabilityEnvironmentalRuntimeMath.localZ(coords);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos pos = new BlockPos(x, y, z);

        if (!plan.executionAllowed() || !InstabilityPolicy.allowProvider(plan.providerId())) {
            return AttemptResult.BLOCKED_AT_FINAL_GATE;
        }

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return AttemptResult.ENTITY_CREATE_FAILED;
        bolt.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        level.addFreshEntity(bolt);
        return AttemptResult.APPLIED_LIGHTNING;
    }

    private static AttemptResult tickExplosion(
            ServerLevel level,
            LevelChunk chunk,
            LegacyInstabilityEnvironmentalPlanService.EnvironmentalPlan plan,
            int effectIndex) {
        if (LegacyInstabilityRuntimeRandom.nextInt(level, plan.spec().chanceDenominator()) != 0) {
            return AttemptResult.CHANCE_MISSED;
        }

        int lcg = LegacyInstabilityEnvironmentalRuntimeState.advance(level, plan.providerId(), effectIndex);
        int coords = LegacyInstabilityEnvironmentalRuntimeMath.coordsFromLcg(lcg);
        int x = chunk.getPos().getMinBlockX() + LegacyInstabilityEnvironmentalRuntimeMath.localX(coords);
        int z = chunk.getPos().getMinBlockZ() + LegacyInstabilityEnvironmentalRuntimeMath.localZ(coords);
        int rawY = LegacyInstabilityEnvironmentalRuntimeMath.legacyRawY(coords);
        int y = LegacyInstabilityEnvironmentalRuntimeMath.mapExplosionY(
                rawY, level.getMinBuildHeight(), level.getHeight());

        if (!plan.executionAllowed() || !InstabilityPolicy.allowProvider(plan.providerId())) {
            return AttemptResult.BLOCKED_AT_FINAL_GATE;
        }

        level.explode(
                null,
                x,
                y,
                z,
                plan.spec().explosionStrength(),
                plan.spec().explosionCausesFire(),
                Level.ExplosionInteraction.BLOCK);
        return AttemptResult.APPLIED_EXPLOSION;
    }

    private static AttemptResult tickCrumble(
            ServerLevel level,
            LevelChunk chunk,
            LegacyInstabilityEnvironmentalPlanService.EnvironmentalPlan plan,
            int effectIndex) {
        int lcg = LegacyInstabilityEnvironmentalRuntimeState.advance(level, plan.providerId(), effectIndex);
        int coords = LegacyInstabilityEnvironmentalRuntimeMath.coordsFromLcg(lcg);
        int x = chunk.getPos().getMinBlockX() + LegacyInstabilityEnvironmentalRuntimeMath.localX(coords);
        int z = chunk.getPos().getMinBlockZ() + LegacyInstabilityEnvironmentalRuntimeMath.localZ(coords);
        int rawY = LegacyInstabilityEnvironmentalRuntimeMath.legacyRawY(coords);
        int y = LegacyInstabilityEnvironmentalRuntimeMath.mapCrumbleY(
                rawY, level.getMinBuildHeight(), level.getHeight());
        BlockPos pos = new BlockPos(x, y, z);
        var replacement = LegacyInstabilityCrumbleMappings.replacement(
                LegacyInstabilityBlockMutationBatch.effectiveState(level, pos));
        if (replacement == null) return AttemptResult.CRUMBLE_UNMAPPED_BLOCK;

        if (!plan.executionAllowed() || !InstabilityPolicy.allowProvider(plan.providerId())) {
            return AttemptResult.BLOCKED_AT_FINAL_GATE;
        }

        LegacyInstabilityBlockMutationBatch.queue(level, pos, replacement);
        return AttemptResult.APPLIED_CRUMBLE;
    }

    private static AttemptResult tickMeteor(
            ServerLevel level,
            LevelChunk chunk,
            LegacyInstabilityEnvironmentalPlanService.EnvironmentalPlan plan,
            int effectIndex) {
        if (LegacyInstabilityRuntimeRandom.nextInt(level, plan.spec().chanceDenominator()) != 0) {
            return AttemptResult.CHANCE_MISSED;
        }

        int lcg = LegacyInstabilityEnvironmentalRuntimeState.advance(level, plan.providerId(), effectIndex);
        double gaussianX = LegacyInstabilityRuntimeRandom.nextGaussian(level);
        float verticalRandom = LegacyInstabilityRuntimeRandom.nextFloat(level);
        double gaussianZ = LegacyInstabilityRuntimeRandom.nextGaussian(level);
        var spawn = LegacyInstabilityMeteorPlanService.buildInstabilitySpawn(
                chunk.getPos().getMinBlockX(), chunk.getPos().getMinBlockZ(), lcg,
                gaussianX, verticalRandom, gaussianZ);
        if (!plan.executionAllowed() || !InstabilityPolicy.allowProvider(plan.providerId())) {
            return AttemptResult.BLOCKED_AT_FINAL_GATE;
        }
        EntityMeteor meteor = EntityMeteor.fromPlan(level, spawn);
        if (!level.addFreshEntity(meteor)) return AttemptResult.ENTITY_CREATE_FAILED;
        return AttemptResult.APPLIED_METEOR;
    }

    public enum AttemptResult {
        INVALID_INPUT,
        INVALID_EFFECT_INDEX,
        CHANCE_MISSED,
        EMPTY_VERTICAL_BAND,
        TARGET_REJECTED,
        CRUMBLE_UNMAPPED_BLOCK,
        METEOR_DEFERRED,
        METEOR_SPAWN_PLAN_READY,
        BLOCKED_AT_FINAL_GATE,
        ENTITY_CREATE_FAILED,
        APPLIED_BURNING,
        APPLIED_LIGHTNING,
        APPLIED_EXPLOSION,
        APPLIED_CRUMBLE,
        APPLIED_METEOR
    }
}
