package com.xcompwiz.mystcraft.instability;

import java.util.List;

/**
 * Pure 0.13.7.06 Meteor/ExplosionAdvanced contract captured before the dangerous runtime port.
 *
 * <p>This class intentionally contains no Minecraft types and performs no world mutation. CP297 uses
 * it as the auditable specification for a later EntityMeteor/ExplosionAdvanced implementation.</p>
 */
public final class LegacyInstabilityMeteorModel {
    public static final float INSTABILITY_SCALE = 1.0F;
    public static final int INSTABILITY_PENETRATION = 0;
    public static final double INSTABILITY_SPAWN_Y = 500.0D;

    public static final double IMPACT_VERTICAL_DAMPING = 0.90D;
    public static final int DIRECT_CARVE_EXTRA_Y = 5;
    public static final float COLLISION_BORDER = 1.0F;
    public static final boolean ENTITY_RELOAD_KILLS_METEOR = true;
    public static final boolean SPAWN_DATA_SYNCS_SCALE_ONLY = true;

    public static final float PRIMARY_CRATER_POWER = 5.0F;
    public static final int IMPACT_EXPLOSION_COUNT = 8;

    public static final int EXPLOSION_RAY_GRID_SIZE = 16;
    public static final float EXPLOSION_RAY_STEP = 0.3F;
    public static final float EXPLOSION_RAY_DECAY_MULTIPLIER = 0.75F;
    public static final float EXPLOSION_RANDOM_POWER_BASE = 0.7F;
    public static final float EXPLOSION_RANDOM_POWER_SPAN = 0.6F;
    public static final float EXPLOSION_RESISTANCE_BIAS = 0.3F;
    public static final float EXPLOSION_ENTITY_RADIUS_MULTIPLIER = 2.0F;
    public static final float EXPLOSION_DAMAGE_MULTIPLIER = 8.0F;

    public static final int FIRE_CHANCE_DENOMINATOR = 3;
    public static final int ORE_CHANCE_DENOMINATOR = 20;
    public static final float COAL_ORE_WEIGHT = 0.50F;
    public static final float IRON_ORE_WEIGHT = 0.30F;
    public static final float GOLD_ORE_WEIGHT = 0.20F;

    public static final int LEGACY_TRACKING_RANGE = 192;
    public static final int LEGACY_UPDATE_FREQUENCY = 2;
    public static final boolean LEGACY_SENDS_VELOCITY_UPDATES = false;

    private LegacyInstabilityMeteorModel() {}

    public enum OreChoice {
        COAL,
        IRON,
        GOLD
    }

    public enum ExplosionEffectKind {
        BASIC,
        BREAK_BLOCKS_NO_DROP,
        FIRE,
        PLACE_ORES
    }

    public record CarveBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}

    /** One ExplosionAdvanced invocation in the exact EntityMeteor.onImpact sequence. */
    public record ExplosionBurst(
            double offsetX,
            double offsetY,
            double offsetZ,
            float power,
            boolean flaming,
            boolean addOres,
            boolean breakBlocksWithoutDrops) {}

    /** Exact eight-explosion crater sequence for the supplied meteor scale. */
    public static List<ExplosionBurst> impactExplosions(float scale) {
        if (!(scale > 0.0F)) throw new IllegalArgumentException("scale");
        double tenth = scale / 10.0D;
        double fifth = scale / 5.0D;
        double twoFifths = scale * 2.0D / 5.0D;
        double fourFifths = scale * 4.0D / 5.0D;
        return List.of(
                new ExplosionBurst(0, 0, 0, PRIMARY_CRATER_POWER, false, true, true),
                new ExplosionBurst(0, -tenth, 0, scale, false, true, true),
                new ExplosionBurst(0, -fifth, 0, scale * 2.0F, false, true, true),
                new ExplosionBurst(0, -twoFifths, 0, scale, false, true, true),
                new ExplosionBurst(fourFifths, 0, 0, scale, true, false, true),
                new ExplosionBurst(-fourFifths, 0, 0, scale, true, false, true),
                new ExplosionBurst(0, 0, fourFifths, scale, true, false, true),
                new ExplosionBurst(0, 0, -fourFifths, scale, true, false, true));
    }

    public static int nextInGroundTime(int current, boolean blockRayHit) {
        return blockRayHit ? current + 1 : Math.max(0, current - 1);
    }

    public static boolean shouldImpactExplode(int inGroundTime, int penetration) {
        return inGroundTime >= penetration;
    }

    public static double dampedVerticalMotion(double motionY) {
        return motionY * IMPACT_VERTICAL_DAMPING;
    }

    /** Exact integer loop bounds used by EntityMeteor.breakBlocksInAABB after adding current motion. */
    public static CarveBounds directCarveBounds(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ,
            double motionX, double motionY, double motionZ) {
        return new CarveBounds(
                floor(minX + motionX),
                floor(minY + motionY),
                floor(minZ + motionZ),
                floor(maxX + motionX),
                floor(maxY + DIRECT_CARVE_EXTRA_Y + motionY),
                floor(maxZ + motionZ));
    }

    /** ExplosionAdvanced applies effects per affected block in this exact order. */
    public static List<ExplosionEffectKind> effectOrder(ExplosionBurst burst) {
        if (burst == null) throw new IllegalArgumentException("burst");
        if (burst.flaming() && burst.addOres()) {
            return List.of(ExplosionEffectKind.BASIC, ExplosionEffectKind.BREAK_BLOCKS_NO_DROP,
                    ExplosionEffectKind.FIRE, ExplosionEffectKind.PLACE_ORES);
        }
        if (burst.flaming()) {
            return List.of(ExplosionEffectKind.BASIC, ExplosionEffectKind.BREAK_BLOCKS_NO_DROP,
                    ExplosionEffectKind.FIRE);
        }
        if (burst.addOres()) {
            return List.of(ExplosionEffectKind.BASIC, ExplosionEffectKind.BREAK_BLOCKS_NO_DROP,
                    ExplosionEffectKind.PLACE_ORES);
        }
        return List.of(ExplosionEffectKind.BASIC, ExplosionEffectKind.BREAK_BLOCKS_NO_DROP);
    }

    /** Number of boundary rays emitted by ExplosionAdvanced's 16x16x16 shell. */
    public static int explosionBoundaryRayCount() {
        int n = EXPLOSION_RAY_GRID_SIZE;
        return n * n * n - (n - 2) * (n - 2) * (n - 2);
    }

    public static float initialRayPower(float explosionSize, float randomFloat) {
        validateUnitFloat(randomFloat);
        return explosionSize * (EXPLOSION_RANDOM_POWER_BASE + randomFloat * EXPLOSION_RANDOM_POWER_SPAN);
    }

    /** Total power loss from one occupied ray step, including the for-loop's 0.3*0.75 decay. */
    public static float occupiedRayStepLoss(float explosionResistance) {
        return (explosionResistance + EXPLOSION_RESISTANCE_BIAS) * EXPLOSION_RAY_STEP
                + EXPLOSION_RAY_STEP * EXPLOSION_RAY_DECAY_MULTIPLIER;
    }

    public static double entityEffectRadius(float explosionSize) {
        return explosionSize * EXPLOSION_ENTITY_RADIUS_MULTIPLIER;
    }

    /** Original ExplosionAdvanced integer damage formula after distance/block-density force is known. */
    public static int entityDamage(float explosionSize, double force) {
        if (force < 0.0D) throw new IllegalArgumentException("force");
        double doubledSize = entityEffectRadius(explosionSize);
        return (int) (((force * force + force) / 2.0D)
                * EXPLOSION_DAMAGE_MULTIPLIER * doubledSize + 1.0D);
    }

    public static boolean fireRollSucceeds(int nextInt3) {
        if (nextInt3 < 0 || nextInt3 >= FIRE_CHANCE_DENOMINATOR) throw new IllegalArgumentException("nextInt3");
        return nextInt3 == 0;
    }

    public static boolean oreRollSucceeds(int nextInt20) {
        if (nextInt20 < 0 || nextInt20 >= ORE_CHANCE_DENOMINATOR) throw new IllegalArgumentException("nextInt20");
        return nextInt20 == 0;
    }

    /** Default 0.13.7.06 ore weights: coal .50, iron .30, gold .20. */
    public static OreChoice defaultOreChoice(float randomFloat) {
        validateUnitFloat(randomFloat);
        if (randomFloat < COAL_ORE_WEIGHT) return OreChoice.COAL;
        if (randomFloat < COAL_ORE_WEIGHT + IRON_ORE_WEIGHT) return OreChoice.IRON;
        return OreChoice.GOLD;
    }

    private static int floor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private static void validateUnitFloat(float value) {
        if (!(value >= 0.0F && value < 1.0F)) throw new IllegalArgumentException("randomFloat");
    }
}
