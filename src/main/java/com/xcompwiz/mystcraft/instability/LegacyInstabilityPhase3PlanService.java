package com.xcompwiz.mystcraft.instability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure bridge from Phase-2 provider levels to Potion execution descriptors.
 *
 * <p>This service intentionally contains no Minecraft/NeoForge types and performs no side effects.
 * CP299 consumes these descriptors in the live runtime adapters while retaining the same auditable
 * Provider-level-to-effect translation proven during CP293.</p>
 */
public final class LegacyInstabilityPhase3PlanService {
    private LegacyInstabilityPhase3PlanService() {}

    public static Plan build(Map<String, Integer> providerLevels) {
        ArrayList<PotionPlan> potionPlans = new ArrayList<>();
        LinkedHashMap<String, DeferredProvider> deferred = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> unknown = new LinkedHashMap<>();

        if (providerLevels != null) {
            for (Map.Entry<String, Integer> entry : providerLevels.entrySet()) {
                String providerId = entry.getKey();
                Integer levelValue = entry.getValue();
                if (providerId == null || providerId.isBlank() || levelValue == null || levelValue <= 0) continue;
                int providerLevel = levelValue;

                var potion = LegacyInstabilityProviderCatalog.potion(providerId);
                if (potion.isPresent()) {
                    var spec = potion.get();
                    potionPlans.add(new PotionPlan(
                            providerId,
                            providerLevel,
                            spec.effectId(),
                            spec.durationTicks(),
                            spec.amplifierForProviderLevel(providerLevel),
                            spec.targetRule(),
                            spec.samplingRule(),
                            InstabilityPolicy.allowProvider(providerId)));
                    continue;
                }

                var family = LegacyInstabilityProviderCatalog.family(providerId);
                if (family.isPresent()) {
                    String reason = switch (family.get()) {
                        case DECAY -> InstabilityPolicy.DECAY_ENABLED ? "handled-by-cp299-decay-runtime" : "decay-phase-gated";
                        case ENVIRONMENTAL -> "handled-by-environmental-runtime";
                        default -> "handled-by-runtime";
                    };
                    deferred.put(providerId, new DeferredProvider(providerId, providerLevel, family.get(), reason));
                } else {
                    unknown.put(providerId, providerLevel);
                }
            }
        }

        return new Plan(
                List.copyOf(potionPlans),
                Collections.unmodifiableMap(deferred),
                Collections.unmodifiableMap(unknown),
                InstabilityPolicy.PENALTIES_ENABLED,
                InstabilityPolicy.DECAY_ENABLED);
    }

    public record PotionPlan(
            String providerId,
            int providerLevel,
            String effectId,
            int durationTicks,
            int amplifier,
            LegacyInstabilityProviderCatalog.TargetRule targetRule,
            LegacyInstabilityProviderCatalog.SamplingRule samplingRule,
            boolean executionAllowed) {
        public String compact() {
            return providerId + "{lvl=" + providerLevel
                    + ",effect=" + effectId
                    + ",amp=" + amplifier
                    + ",duration=" + durationTicks
                    + ",target=" + targetRule
                    + ",sample=" + samplingRule
                    + ",execute=" + executionAllowed + "}";
        }
    }

    public record DeferredProvider(
            String providerId,
            int providerLevel,
            LegacyInstabilityProviderCatalog.ProviderFamily family,
            String reason) {}

    public record Plan(
            List<PotionPlan> potionPlans,
            Map<String, DeferredProvider> deferredProviders,
            Map<String, Integer> unknownProviders,
            boolean penaltiesEnabled,
            boolean decayEnabled) {
        public List<String> compactPotionPlans() {
            ArrayList<String> out = new ArrayList<>(potionPlans.size());
            for (PotionPlan plan : potionPlans) out.add(plan.compact());
            return List.copyOf(out);
        }

        public boolean hasExecutableEffects() {
            for (PotionPlan plan : potionPlans) if (plan.executionAllowed()) return true;
            return false;
        }
    }
}
