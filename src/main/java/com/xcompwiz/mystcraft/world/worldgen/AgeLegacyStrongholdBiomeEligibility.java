package com.xcompwiz.mystcraft.world.worldgen;

/** Pure compatibility predicate for Minecraft 1.12 MapGenStronghold#allowedBiomes. */
public final class AgeLegacyStrongholdBiomeEligibility {
    private AgeLegacyStrongholdBiomeEligibility() {}

    /** 1.12 accepted each registered biome iff its baseHeight was strictly greater than zero. */
    public static boolean eligibleBaseHeight(double legacyBaseHeight) {
        return legacyBaseHeight > 0.0D;
    }
}
