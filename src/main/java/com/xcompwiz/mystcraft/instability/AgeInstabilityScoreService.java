package com.xcompwiz.mystcraft.instability;

import com.xcompwiz.mystcraft.world.agedata.AgeRecord;

/** Central score-only service. Runtime application is controlled by InstabilityPolicy. */
public final class AgeInstabilityScoreService {
    private AgeInstabilityScoreService() {}

    public static ScoreBreakdown staticScore(AgeRecord age) {
        return calculate(age, 0);
    }

    /**
     * @param nonOreBlockInstability profiled non-ore block load only. Ore counts are deliberately
     *                               excluded in CP291; Dense Ores uses fixed page-count tiers.
     */
    public static ScoreBreakdown calculate(AgeRecord age, int nonOreBlockInstability) {
        LegacyStaticInstabilityCalculator.StaticBreakdown statics = LegacyStaticInstabilityCalculator.calculate(age);
        int block = Math.max(0, nonOreBlockInstability);
        int raw = statics.rawStaticScore() + block; // bonus subsystem intentionally remains 0 in Phase 1
        int recorded = LegacyInstabilityData.applyDifficulty(raw);
        return new ScoreBreakdown(statics, block, raw, recorded, InstabilityPolicy.effectiveScore(recorded));
    }

    public record ScoreBreakdown(
            LegacyStaticInstabilityCalculator.StaticBreakdown statics,
            int blockInstability,
            int rawBeforeDifficulty,
            int recordedScore,
            int effectiveRuntimeScore) {}
}
