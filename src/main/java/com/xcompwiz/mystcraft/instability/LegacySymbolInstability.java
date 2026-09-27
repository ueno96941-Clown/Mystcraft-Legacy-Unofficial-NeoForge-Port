package com.xcompwiz.mystcraft.instability;

/** Fixed/incremental Symbol instability modifiers from Mystcraft 0.13.7.06 plus CP291 Dense Ores tiers. */
public final class LegacySymbolInstability {
    private LegacySymbolInstability() {}

    public static int instabilityModifier(String legacyId, int count) {
        if (legacyId == null) return 0;
        return switch (legacyId) {
            case "mystcraft:DenseOres" -> DenseOresInstabilityPolicy.incrementalForOccurrence(count);
            case "mystcraft:EnvAccel" -> 1000;
            case "mystcraft:LightingBright" -> 500;
            case "mystcraft:EnvLightning" -> count == 1 ? -500 : 0;
            case "mystcraft:EnvMeteor" -> -1000;
            case "mystcraft:EnvExplosions" -> -500;
            case "mystcraft:EnvScorch" -> count == 1 ? -500 : 0;
            case "mystcraft:FeatureLargeDummy" -> 0;
            case "mystcraft:FeatureMediumDummy" -> 1000;
            case "mystcraft:FeatureSmallDummy" -> 2000;
            case "mystcraft:Strongholds", "mystcraft:Mineshafts", "mystcraft:NetherFort", "mystcraft:Villages" -> count > 3 ? 100 : 0;
            default -> 0;
        };
    }
}
