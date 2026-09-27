package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.world.worldgen.AgeLegacyStrongholdBiomeEligibility;
import com.xcompwiz.mystcraft.world.worldgen.AgeLegacyStructureSetSpecs;

public final class AgeLegacyStrongholdBiomeEligibilityHarness {
    public static void main(String[] args) {
        require(!AgeLegacyStrongholdBiomeEligibility.eligibleBaseHeight(-1.0), "negative base height excluded");
        require(!AgeLegacyStrongholdBiomeEligibility.eligibleBaseHeight(0.0), "zero base height excluded");
        require(AgeLegacyStrongholdBiomeEligibility.eligibleBaseHeight(0.1), "positive base height included");

        require(AgeLegacyStructureSetSpecs.STRONGHOLD_DISTANCE == 32, "legacy stronghold distance");
        require(AgeLegacyStructureSetSpecs.STRONGHOLD_SPREAD == 3, "legacy stronghold spread");
        require(AgeLegacyStructureSetSpecs.STRONGHOLD_COUNT == 128, "legacy stronghold count");
        System.out.println("AgeLegacyStrongholdBiomeEligibilityHarness: PASS");
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
