package com.xcompwiz.mystcraft.instability;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Side-effect-free transcription of the active 0.13.7.06 Instability provider semantics.
 *
 * <p>CP293 uses this catalog only to build dry-run execution plans. Nothing in this class applies
 * an effect to a world/entity. Potion metadata is copied from InstabilityData.initialize(),
 * EffectPotion and EffectPotionEnemy in the 0.13.7.06 source/final jar.</p>
 */
public final class LegacyInstabilityProviderCatalog {
    public enum ProviderFamily {
        POTION,
        ENVIRONMENTAL,
        DECAY
    }

    /** Exact target predicate used by the old EffectPotion implementations. */
    public enum TargetRule {
        /** Any living entity, but only if World.canSeeSky(entity position) is true. */
        LIVING_CAN_SEE_SKY,
        /** Any living entity regardless of sky visibility. */
        ALL_LIVING,
        /** Any non-player living entity regardless of hostility/passivity. */
        NON_PLAYER_LIVING
    }

    /**
     * 0.13.7.06 did not scan all entities in a chunk. Each effect invocation selected one random
     * vertical entity section, then one random entity from that section, and attempted that one
     * target only.
     */
    public enum SamplingRule {
        RANDOM_ENTITY_SECTION_THEN_RANDOM_ENTITY_PER_CHUNK_TICK
    }

    public record PotionSpec(
            String providerId,
            String effectId,
            int durationTicks,
            TargetRule targetRule,
            SamplingRule samplingRule) {
        public PotionSpec {
            if (providerId == null || providerId.isBlank()) throw new IllegalArgumentException("providerId");
            if (effectId == null || effectId.isBlank()) throw new IllegalArgumentException("effectId");
            if (durationTicks <= 0) throw new IllegalArgumentException("durationTicks");
            if (targetRule == null) throw new IllegalArgumentException("targetRule");
            if (samplingRule == null) throw new IllegalArgumentException("samplingRule");
        }

        /** EffectPotion stores provider level - 1 as the Minecraft potion amplifier. */
        public int amplifierForProviderLevel(int providerLevel) {
            if (providerLevel <= 0) throw new IllegalArgumentException("providerLevel");
            return providerLevel - 1;
        }
    }

    private static final SamplingRule LEGACY_SAMPLING =
            SamplingRule.RANDOM_ENTITY_SECTION_THEN_RANDOM_ENTITY_PER_CHUNK_TICK;

    private static final Map<String, ProviderFamily> FAMILIES;
    private static final Map<String, PotionSpec> POTIONS;

    static {
        LinkedHashMap<String, ProviderFamily> families = new LinkedHashMap<>();
        LinkedHashMap<String, PotionSpec> potions = new LinkedHashMap<>();

        // Registration order matches InstabilityData.initialize() in 0.13.7.06.
        potion(families, potions, "blindness", "minecraft:blindness", 60, TargetRule.LIVING_CAN_SEE_SKY);
        potion(families, potions, "blindness,g", "minecraft:blindness", 60, TargetRule.ALL_LIVING);
        potion(families, potions, "enemyregen,g", "minecraft:regeneration", 200, TargetRule.NON_PLAYER_LIVING);
        potion(families, potions, "enemyresist,g", "minecraft:resistance", 200, TargetRule.NON_PLAYER_LIVING);
        potion(families, potions, "fatigue", "minecraft:mining_fatigue", 80, TargetRule.LIVING_CAN_SEE_SKY);
        potion(families, potions, "fatigue,g", "minecraft:mining_fatigue", 80, TargetRule.ALL_LIVING);
        potion(families, potions, "hunger", "minecraft:hunger", 80, TargetRule.LIVING_CAN_SEE_SKY);
        potion(families, potions, "hunger,g", "minecraft:hunger", 80, TargetRule.ALL_LIVING);
        potion(families, potions, "nausea", "minecraft:nausea", 60, TargetRule.LIVING_CAN_SEE_SKY);
        potion(families, potions, "nausea,g", "minecraft:nausea", 60, TargetRule.ALL_LIVING);
        potion(families, potions, "poison", "minecraft:poison", 80, TargetRule.LIVING_CAN_SEE_SKY);
        potion(families, potions, "poison,g", "minecraft:poison", 80, TargetRule.ALL_LIVING);
        potion(families, potions, "slow", "minecraft:slowness", 80, TargetRule.LIVING_CAN_SEE_SKY);
        potion(families, potions, "slow,g", "minecraft:slowness", 80, TargetRule.ALL_LIVING);
        potion(families, potions, "weakness", "minecraft:weakness", 80, TargetRule.LIVING_CAN_SEE_SKY);
        potion(families, potions, "weakness,g", "minecraft:weakness", 80, TargetRule.ALL_LIVING);
        potion(families, potions, "wither", "minecraft:wither", 30, TargetRule.LIVING_CAN_SEE_SKY);
        potion(families, potions, "wither,g", "minecraft:wither", 30, TargetRule.ALL_LIVING);

        environmental(families, "burning");
        environmental(families, "crumble");
        decay(families, "decayblue");
        decay(families, "decaypurple");
        decay(families, "decayred");
        decay(families, "decaywhite");
        environmental(families, "explosions");
        environmental(families, "lightning");
        environmental(families, "meteors");

        FAMILIES = Collections.unmodifiableMap(families);
        POTIONS = Collections.unmodifiableMap(potions);
    }

    private LegacyInstabilityProviderCatalog() {}

    public static Optional<ProviderFamily> family(String providerId) {
        return Optional.ofNullable(FAMILIES.get(providerId));
    }

    public static Optional<PotionSpec> potion(String providerId) {
        return Optional.ofNullable(POTIONS.get(providerId));
    }

    public static Map<String, PotionSpec> potionProviders() {
        return POTIONS;
    }

    public static Map<String, ProviderFamily> activeProviderFamilies() {
        return FAMILIES;
    }

    private static void potion(
            Map<String, ProviderFamily> families,
            Map<String, PotionSpec> potions,
            String providerId,
            String effectId,
            int durationTicks,
            TargetRule targetRule) {
        if (families.put(providerId, ProviderFamily.POTION) != null) {
            throw new IllegalStateException("Duplicate provider: " + providerId);
        }
        PotionSpec spec = new PotionSpec(providerId, effectId, durationTicks, targetRule, LEGACY_SAMPLING);
        potions.put(providerId, spec);
    }

    private static void environmental(Map<String, ProviderFamily> families, String providerId) {
        if (families.put(providerId, ProviderFamily.ENVIRONMENTAL) != null) {
            throw new IllegalStateException("Duplicate provider: " + providerId);
        }
    }

    private static void decay(Map<String, ProviderFamily> families, String providerId) {
        if (families.put(providerId, ProviderFamily.DECAY) != null) {
            throw new IllegalStateException("Duplicate provider: " + providerId);
        }
    }
}
