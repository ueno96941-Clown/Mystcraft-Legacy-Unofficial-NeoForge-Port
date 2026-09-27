package com.xcompwiz.mystcraft.instability;

import java.util.List;

/**
 * Side-effect-free Instability Meteor spawn/impact plan.
 *
 * <p>The caller supplies the already-advanced private LCG and the three legacy world-random samples
 * in their original order: Gaussian X, float Y, Gaussian Z. No entity is created here.</p>
 */
public final class LegacyInstabilityMeteorPlanService {
    private LegacyInstabilityMeteorPlanService() {}

    public static SpawnPlan buildInstabilitySpawn(
            int chunkMinX,
            int chunkMinZ,
            int advancedLcg,
            double gaussianX,
            float verticalRandom,
            double gaussianZ) {
        if (!(verticalRandom >= 0.0F && verticalRandom < 1.0F)) {
            throw new IllegalArgumentException("verticalRandom");
        }
        var spec = LegacyInstabilityEnvironmentalCatalog.environmental("meteors")
                .orElseThrow(() -> new IllegalStateException("meteors environmental spec missing"));
        int coords = LegacyInstabilityEnvironmentalRuntimeMath.coordsFromLcg(advancedLcg);
        int x = chunkMinX + LegacyInstabilityEnvironmentalRuntimeMath.localX(coords);
        int z = chunkMinZ + LegacyInstabilityEnvironmentalRuntimeMath.localZ(coords);
        double motionX = gaussianX * spec.meteorHorizontalGaussianScale();
        double motionY = verticalRandom * spec.meteorVerticalRandomScale() + spec.meteorVerticalBase();
        double motionZ = gaussianZ * spec.meteorHorizontalGaussianScale();
        float scale = LegacyInstabilityMeteorModel.INSTABILITY_SCALE;
        int penetration = LegacyInstabilityMeteorModel.INSTABILITY_PENETRATION;
        return new SpawnPlan(
                x,
                spec.fixedSpawnY(),
                z,
                motionX,
                motionY,
                motionZ,
                scale,
                penetration,
                LegacyInstabilityMeteorModel.impactExplosions(scale));
    }

    public record SpawnPlan(
            double x,
            double y,
            double z,
            double motionX,
            double motionY,
            double motionZ,
            float scale,
            int penetration,
            List<LegacyInstabilityMeteorModel.ExplosionBurst> impactExplosions) {
        public SpawnPlan {
            impactExplosions = List.copyOf(impactExplosions);
        }

        public boolean explodesOnFirstBlockContact() {
            return LegacyInstabilityMeteorModel.shouldImpactExplode(1, penetration);
        }

        public String compact() {
            return "meteor{pos=(" + x + ',' + y + ',' + z + ")"
                    + ",motion=(" + motionX + ',' + motionY + ',' + motionZ + ")"
                    + ",scale=" + scale
                    + ",penetration=" + penetration
                    + ",impactExplosions=" + impactExplosions.size()
                    + ",firstBlockContactExplodes=" + explodesOnFirstBlockContact()
                    + '}';
        }
    }
}
