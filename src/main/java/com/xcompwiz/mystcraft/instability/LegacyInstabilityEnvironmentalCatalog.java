package com.xcompwiz.mystcraft.instability;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Side-effect-free transcription of the active environmental Instability providers in
 * Mystcraft 0.13.7.06.
 *
 * <p>CP295 deliberately records the old trigger/coordinate/level semantics without attempting to
 * adapt legacy 0..255 Y choices to the modern build height. That conversion belongs to the future
 * runtime-adapter checkpoint, where each effect can be considered independently.</p>
 */
public final class LegacyInstabilityEnvironmentalCatalog {
    /** LCG used independently by Lightning, Explosions, Crumble and Meteor effect instances. */
    public static final int LEGACY_LCG_MULTIPLIER = 3;
    public static final int LEGACY_LCG_ADDEND = 1013904223;
    public static final int LEGACY_LCG_OUTPUT_SHIFT = 2;
    public static final int LEGACY_X_MASK = 15;
    public static final int LEGACY_Z_SHIFT = 8;
    public static final int LEGACY_Z_MASK = 15;
    public static final int LEGACY_Y_SHIFT = 16;
    public static final int LEGACY_Y_MASK = 255;

    public enum EffectKind {
        LIGHTNING,
        EXPLOSION,
        BURNING,
        CRUMBLE,
        METEOR
    }

    /** How the legacy provider converts provider level into environmental effect objects. */
    public enum LevelSemantics {
        /** Provider registers one independent IEnvironmentalEffect object for every level. */
        INDEPENDENT_EFFECT_PER_LEVEL,
        /** Provider registers one effect object and passes the level into that object's constructor. */
        SINGLE_EFFECT_WITH_LEVEL_PARAMETER
    }

    public enum TriggerRule {
        /** During thunder: 1/N primary roll; if that fails, 1/M fallback. Otherwise fallback only. */
        LIGHTNING_WEATHER_PRIMARY_THEN_FALLBACK,
        /** Exactly one nextInt(N)==0 roll for each effect instance and active chunk tick. */
        SIMPLE_RANDOM_CHANCE,
        /** One attempt every effect-instance/chunk tick, no preliminary random chance gate. */
        EVERY_CHUNK_TICK,
        /** nextInt(N)==0, then legacy random entity-section/entity selection. */
        RANDOM_ENTITY_SAMPLE_CHANCE
    }

    public enum PositionRule {
        /** LCG x/z inside the chunk; y is the precipitation height for that column. */
        LCG_XZ_PRECIPITATION_HEIGHT,
        /** LCG x/z and legacy masked y=(coords>>16)&255, plus the configured y offset. */
        LCG_XYZ_LEGACY_MASK,
        /** Legacy chunk entity-list slot then one arbitrary Entity from that slot. */
        RANDOM_ENTITY_SECTION_THEN_RANDOM_ENTITY,
        /** LCG x/z with a fixed world-space spawn y. */
        LCG_XZ_FIXED_Y
    }

    public record EnvironmentalSpec(
            String providerId,
            EffectKind effectKind,
            LevelSemantics levelSemantics,
            TriggerRule triggerRule,
            PositionRule positionRule,
            int chanceDenominator,
            int thunderChanceDenominator,
            int fallbackChanceDenominator,
            int legacyYOffset,
            int fireSecondsPerLevel,
            float explosionStrength,
            boolean explosionCausesFire,
            boolean explosionDamagesTerrain,
            double fixedSpawnY,
            double meteorHorizontalGaussianScale,
            float meteorVerticalRandomScale,
            float meteorVerticalBase) {
        public EnvironmentalSpec {
            if (providerId == null || providerId.isBlank()) throw new IllegalArgumentException("providerId");
            if (effectKind == null) throw new IllegalArgumentException("effectKind");
            if (levelSemantics == null) throw new IllegalArgumentException("levelSemantics");
            if (triggerRule == null) throw new IllegalArgumentException("triggerRule");
            if (positionRule == null) throw new IllegalArgumentException("positionRule");
            if (chanceDenominator < 0 || thunderChanceDenominator < 0 || fallbackChanceDenominator < 0) {
                throw new IllegalArgumentException("chance denominator");
            }
        }

        public int effectInstancesForProviderLevel(int providerLevel) {
            if (providerLevel <= 0) throw new IllegalArgumentException("providerLevel");
            return levelSemantics == LevelSemantics.INDEPENDENT_EFFECT_PER_LEVEL ? providerLevel : 1;
        }

        public int fireSecondsForProviderLevel(int providerLevel) {
            if (providerLevel <= 0) throw new IllegalArgumentException("providerLevel");
            if (effectKind != EffectKind.BURNING) throw new IllegalStateException("not burning");
            return fireSecondsPerLevel * providerLevel;
        }

        /** Exact legacy explosion Y range for the current spec, inclusive. */
        public int legacyMinY() {
            return positionRule == PositionRule.LCG_XYZ_LEGACY_MASK ? legacyYOffset : Integer.MIN_VALUE;
        }

        public int legacyMaxY() {
            return positionRule == PositionRule.LCG_XYZ_LEGACY_MASK
                    ? LEGACY_Y_MASK + legacyYOffset
                    : Integer.MAX_VALUE;
        }
    }

    private static final Map<String, EnvironmentalSpec> SPECS;

    static {
        LinkedHashMap<String, EnvironmentalSpec> specs = new LinkedHashMap<>();

        put(specs, new EnvironmentalSpec(
                "burning",
                EffectKind.BURNING,
                LevelSemantics.SINGLE_EFFECT_WITH_LEVEL_PARAMETER,
                TriggerRule.RANDOM_ENTITY_SAMPLE_CHANCE,
                PositionRule.RANDOM_ENTITY_SECTION_THEN_RANDOM_ENTITY,
                10,
                0,
                0,
                0,
                4,
                0.0F,
                false,
                false,
                Double.NaN,
                0.0,
                0.0F,
                0.0F));

        put(specs, new EnvironmentalSpec(
                "crumble",
                EffectKind.CRUMBLE,
                LevelSemantics.INDEPENDENT_EFFECT_PER_LEVEL,
                TriggerRule.EVERY_CHUNK_TICK,
                PositionRule.LCG_XYZ_LEGACY_MASK,
                1,
                0,
                0,
                0,
                0,
                0.0F,
                false,
                false,
                Double.NaN,
                0.0,
                0.0F,
                0.0F));

        put(specs, new EnvironmentalSpec(
                "explosions",
                EffectKind.EXPLOSION,
                LevelSemantics.INDEPENDENT_EFFECT_PER_LEVEL,
                TriggerRule.SIMPLE_RANDOM_CHANCE,
                PositionRule.LCG_XYZ_LEGACY_MASK,
                1000,
                0,
                0,
                1,
                0,
                3.0F,
                true,
                true,
                Double.NaN,
                0.0,
                0.0F,
                0.0F));

        put(specs, new EnvironmentalSpec(
                "lightning",
                EffectKind.LIGHTNING,
                LevelSemantics.INDEPENDENT_EFFECT_PER_LEVEL,
                TriggerRule.LIGHTNING_WEATHER_PRIMARY_THEN_FALLBACK,
                PositionRule.LCG_XZ_PRECIPITATION_HEIGHT,
                0,
                5000,
                100000,
                0,
                0,
                0.0F,
                false,
                false,
                Double.NaN,
                0.0,
                0.0F,
                0.0F));

        put(specs, new EnvironmentalSpec(
                "meteors",
                EffectKind.METEOR,
                LevelSemantics.INDEPENDENT_EFFECT_PER_LEVEL,
                TriggerRule.SIMPLE_RANDOM_CHANCE,
                PositionRule.LCG_XZ_FIXED_Y,
                50000,
                0,
                0,
                0,
                0,
                0.0F,
                false,
                false,
                500.0,
                0.25,
                -2.0F,
                -2.0F));

        SPECS = Collections.unmodifiableMap(specs);
    }

    private LegacyInstabilityEnvironmentalCatalog() {}

    public static Optional<EnvironmentalSpec> environmental(String providerId) {
        return Optional.ofNullable(SPECS.get(providerId));
    }

    public static Map<String, EnvironmentalSpec> environmentalProviders() {
        return SPECS;
    }

    private static void put(Map<String, EnvironmentalSpec> specs, EnvironmentalSpec spec) {
        if (specs.put(spec.providerId(), spec) != null) {
            throw new IllegalStateException("Duplicate environmental provider: " + spec.providerId());
        }
    }
}
