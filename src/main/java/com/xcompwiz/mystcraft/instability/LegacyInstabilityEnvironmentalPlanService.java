package com.xcompwiz.mystcraft.instability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CP295 side-effect-free plan builder for the five active environmental Instability providers.
 *
 * <p>No Minecraft/NeoForge types appear here. In particular this class does not reinterpret the
 * old 0..255 Y masks for modern dimensions. It only records the verified 0.13.7.06 semantics so a
 * future runtime adapter has an auditable contract to implement.</p>
 */
public final class LegacyInstabilityEnvironmentalPlanService {
    private LegacyInstabilityEnvironmentalPlanService() {}

    public static Plan build(Map<String, Integer> providerLevels) {
        ArrayList<EnvironmentalPlan> plans = new ArrayList<>();
        if (providerLevels != null) {
            for (Map.Entry<String, Integer> entry : providerLevels.entrySet()) {
                String providerId = entry.getKey();
                Integer levelValue = entry.getValue();
                if (providerId == null || providerId.isBlank() || levelValue == null || levelValue <= 0) continue;

                var spec = LegacyInstabilityEnvironmentalCatalog.environmental(providerId);
                if (spec.isEmpty()) continue;
                int providerLevel = levelValue;
                var environmental = spec.get();
                plans.add(new EnvironmentalPlan(
                        providerId,
                        providerLevel,
                        environmental.effectInstancesForProviderLevel(providerLevel),
                        environmental,
                        InstabilityPolicy.allowProvider(providerId)));
            }
        }
        return new Plan(List.copyOf(plans), InstabilityPolicy.PENALTIES_ENABLED);
    }

    public record EnvironmentalPlan(
            String providerId,
            int providerLevel,
            int effectInstances,
            LegacyInstabilityEnvironmentalCatalog.EnvironmentalSpec spec,
            boolean executionAllowed) {
        public String compact() {
            StringBuilder out = new StringBuilder();
            out.append(providerId)
                    .append("{lvl=").append(providerLevel)
                    .append(",instances=").append(effectInstances)
                    .append(",kind=").append(spec.effectKind())
                    .append(",trigger=").append(spec.triggerRule())
                    .append(",position=").append(spec.positionRule());
            switch (spec.effectKind()) {
                case LIGHTNING -> out.append(",thunder=1/").append(spec.thunderChanceDenominator())
                        .append(",fallback=1/").append(spec.fallbackChanceDenominator());
                case EXPLOSION -> out.append(",chance=1/").append(spec.chanceDenominator())
                        .append(",legacyY=").append(spec.legacyMinY()).append("..").append(spec.legacyMaxY())
                        .append(",power=").append(spec.explosionStrength())
                        .append(",fire=").append(spec.explosionCausesFire())
                        .append(",terrain=").append(spec.explosionDamagesTerrain());
                case BURNING -> out.append(",chance=1/").append(spec.chanceDenominator())
                        .append(",fireSeconds=").append(spec.fireSecondsForProviderLevel(providerLevel));
                case CRUMBLE -> out.append(",chance=everyTick")
                        .append(",legacyY=").append(spec.legacyMinY()).append("..").append(spec.legacyMaxY());
                case METEOR -> out.append(",chance=1/").append(spec.chanceDenominator())
                        .append(",spawnY=").append(spec.fixedSpawnY())
                        .append(",hGaussScale=").append(spec.meteorHorizontalGaussianScale())
                        .append(",vRandScale=").append(spec.meteorVerticalRandomScale())
                        .append(",vBase=").append(spec.meteorVerticalBase());
            }
            return out.append(",execute=").append(executionAllowed).append('}').toString();
        }
    }

    public record Plan(List<EnvironmentalPlan> environmentalPlans, boolean penaltiesEnabled) {
        public List<String> compactEnvironmentalPlans() {
            ArrayList<String> out = new ArrayList<>(environmentalPlans.size());
            for (EnvironmentalPlan plan : environmentalPlans) out.add(plan.compact());
            return List.copyOf(out);
        }

        public boolean hasExecutableEffects() {
            for (EnvironmentalPlan plan : environmentalPlans) if (plan.executionAllowed()) return true;
            return false;
        }
    }
}
