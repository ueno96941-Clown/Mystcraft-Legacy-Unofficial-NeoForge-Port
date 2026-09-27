package com.xcompwiz.mystcraft.instability;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;

/**
 * Minecraft-facing adapter for the verified legacy Potion-provider plan.
 *
 * <p>The sampling/targeting path follows the source-verified legacy implementation, while
 * {@link InstabilityPolicy#allowProvider(String)} remains the final mutation gate.</p>
 */
public final class LegacyInstabilityPotionRuntimeAdapter {
    private LegacyInstabilityPotionRuntimeAdapter() {}

    public static AttemptResult tick(
            ServerLevel level,
            LevelChunk chunk,
            LegacyInstabilityPhase3PlanService.PotionPlan plan) {
        if (level == null || chunk == null || plan == null) return AttemptResult.INVALID_INPUT;

        // Legacy EffectPotion.tick(): world.rand.nextInt(entityLists.length)
        int slot = LegacyInstabilityRuntimeRandom.nextInt(level, LegacyInstabilityPotionRuntimeMath.LEGACY_VERTICAL_SLOTS);
        var band = LegacyInstabilityPotionRuntimeMath.band(level.getMinBuildHeight(), level.getHeight(), slot);

        // 0.13.7.06 read the chosen vertical entity list directly from the chunk. CP323 caches a
        // modern equivalent once per chunk/game tick so multiple Potion/Burning providers do not
        // repeat expensive spatial queries while preserving the same candidate set and order.
        List<Entity> candidates = LegacyInstabilityEntitySlotCache.entitiesForSlot(level, chunk, slot);
        if (candidates.isEmpty()) return AttemptResult.EMPTY_VERTICAL_BAND;

        // Legacy EffectPotion.tick(): world.rand.nextInt(list.size())
        Entity selected = candidates.get(LegacyInstabilityRuntimeRandom.nextInt(level, candidates.size()));
        if (!(selected instanceof LivingEntity living)) return AttemptResult.NON_LIVING_SELECTED;
        if (!targetValid(level, living, plan.targetRule())) return AttemptResult.TARGET_REJECTED;

        Holder<MobEffect> effect = resolveEffect(plan.effectId());
        if (effect == null) return AttemptResult.UNKNOWN_EFFECT;
        MobEffectInstance instance = new MobEffectInstance(effect, plan.durationTicks(), plan.amplifier());

        // Deliberately the final check before the side effect. Re-check the live policy as well as
        // the immutable plan flag so a stale plan can never bypass a later gate change.
        if (!plan.executionAllowed() || !InstabilityPolicy.allowProvider(plan.providerId())) {
            return AttemptResult.BLOCKED_AT_FINAL_GATE;
        }

        boolean changed = living.addEffect(instance);
        if (!changed) return AttemptResult.NO_EFFECT_CHANGE;
        if (living instanceof net.minecraft.server.level.ServerPlayer) {
            return AttemptResult.APPLIED_PLAYER;
        }
        return AttemptResult.APPLIED;
    }

    static boolean targetValid(
            ServerLevel level,
            LivingEntity entity,
            LegacyInstabilityProviderCatalog.TargetRule rule) {
        return switch (rule) {
            case LIVING_CAN_SEE_SKY -> level.canSeeSky(entity.blockPosition());
            case ALL_LIVING -> true;
            case NON_PLAYER_LIVING -> !(entity instanceof Player);
        };
    }

    static Holder<MobEffect> resolveEffect(String effectId) {
        return switch (effectId) {
            case "minecraft:blindness" -> MobEffects.BLINDNESS;
            case "minecraft:regeneration" -> MobEffects.REGENERATION;
            case "minecraft:resistance" -> MobEffects.DAMAGE_RESISTANCE;
            case "minecraft:mining_fatigue" -> MobEffects.DIG_SLOWDOWN;
            case "minecraft:hunger" -> MobEffects.HUNGER;
            case "minecraft:nausea" -> MobEffects.CONFUSION;
            case "minecraft:poison" -> MobEffects.POISON;
            case "minecraft:slowness" -> MobEffects.MOVEMENT_SLOWDOWN;
            case "minecraft:weakness" -> MobEffects.WEAKNESS;
            case "minecraft:wither" -> MobEffects.WITHER;
            default -> null;
        };
    }

    public enum AttemptResult {
        INVALID_INPUT,
        EMPTY_VERTICAL_BAND,
        NON_LIVING_SELECTED,
        TARGET_REJECTED,
        UNKNOWN_EFFECT,
        BLOCKED_AT_FINAL_GATE,
        NO_EFFECT_CHANGE,
        APPLIED,
        APPLIED_PLAYER
    }
}
