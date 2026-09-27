package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Legacy biome climate/terrain compatibility table used by Mystcraft 0.13.7.06.
 *
 * <p>This table is keyed by the persisted legacy numeric biome id, not by the
 * modern biome key.  Keeping this data separate prevents folded/removed modern
 * biome aliases (for example Desert Hills) from erasing the old base-height and
 * variation values needed by the legacy terrain kernel and Natural colour pages.</p>
 */
public record LegacyBiomeTerrainProfile(
        int legacyNumericId,
        double baseHeight,
        double heightVariation,
        double temperature,
        double rainfall,
        int waterColor) {

    private static final int DEFAULT_WATER = 0xFFFFFF;
    private static final Map<Integer, LegacyBiomeTerrainProfile> BY_ID;

    static {
        LinkedHashMap<Integer, LegacyBiomeTerrainProfile> m = new LinkedHashMap<>();
        add(m, 0, -1.0, .1, .5, .5);
        add(m, 1, .125, .05, .8, .4);
        add(m, 2, .125, .05, 2, 0);
        add(m, 3, 1, .5, .2, .3);
        add(m, 4, .1, .2, .7, .8);
        add(m, 5, .2, .2, .25, .8);
        add(m, 6, -.2, .1, .8, .9, 14745518);
        add(m, 7, -.5, 0, .5, .5);
        add(m, 8, .1, .2, 2, 0);
        add(m, 9, .1, .2, .5, .5);
        add(m, 10, -1, .1, 0, .5);
        add(m, 11, -.5, 0, 0, .5);
        add(m, 12, .125, .05, 0, .5);
        add(m, 13, .45, .3, 0, .5);
        add(m, 14, .2, .3, .9, 1);
        add(m, 15, 0, .025, .9, 1);
        add(m, 16, 0, .025, .8, .4);
        add(m, 17, .45, .3, 2, 0);
        add(m, 18, .45, .3, .7, .8);
        add(m, 19, .45, .3, .25, .8);
        add(m, 20, .8, .3, .2, .3);
        add(m, 21, .1, .2, .95, .9);
        add(m, 22, .45, .3, .95, .9);
        add(m, 23, .1, .2, .95, .8);
        add(m, 24, -1.8, .1, .5, .5);
        add(m, 25, .1, .8, .2, .3);
        add(m, 26, 0, .025, .05, .3);
        add(m, 27, .1, .2, .6, .6);
        add(m, 28, .45, .3, .6, .6);
        add(m, 29, .1, .2, .7, .8);
        add(m, 30, .2, .2, -.5, .4);
        add(m, 31, .45, .3, -.5, .4);
        add(m, 32, .2, .2, .3, .8);
        add(m, 33, .45, .3, .3, .8);
        add(m, 34, 1, .5, .2, .3);
        add(m, 35, .125, .05, 1.2, 0);
        add(m, 36, 1.5, .025, 1, 0);
        add(m, 37, .1, .2, 2, 0);
        add(m, 38, 1.5, .025, 2, 0);
        add(m, 39, 1.5, .025, 2, 0);
        add(m, 127, .1, .2, .5, .5);
        add(m, 129, .125, .05, .8, .4);
        add(m, 130, .225, .25, 2, 0);
        add(m, 131, 1, .5, .2, .3);
        add(m, 132, .1, .4, .7, .8);
        add(m, 133, .3, .4, .25, .8);
        add(m, 134, -.1, .3, .8, .9, 14745518);
        add(m, 140, .425, .45000002, 0, .5);
        add(m, 149, .2, .4, .95, .9);
        add(m, 151, .2, .4, .95, .8);
        add(m, 155, .2, .4, .6, .6);
        add(m, 156, .55, .5, .6, .6);
        add(m, 157, .2, .4, .7, .8);
        add(m, 158, .3, .4, -.5, .4);
        add(m, 160, .2, .2, .25, .8);
        add(m, 161, .2, .2, .25, .8);
        add(m, 162, 1, .5, .2, .3);
        add(m, 163, .3625, 1.225, 1.1, 0);
        add(m, 164, 1.05, 1.2125001, 1, 0);
        add(m, 165, .1, .2, 2, 0);
        add(m, 166, .45, .3, 2, 0);
        add(m, 167, .45, .3, 2, 0);
        BY_ID = Collections.unmodifiableMap(m);
    }

    private static void add(Map<Integer, LegacyBiomeTerrainProfile> m, int id,
                            double base, double variation, double temp, double rain) {
        add(m, id, base, variation, temp, rain, DEFAULT_WATER);
    }

    private static void add(Map<Integer, LegacyBiomeTerrainProfile> m, int id,
                            double base, double variation, double temp, double rain, int water) {
        m.put(id, new LegacyBiomeTerrainProfile(id, base, variation, temp, rain, water));
    }

    public static Optional<LegacyBiomeTerrainProfile> byId(int legacyNumericId) {
        return Optional.ofNullable(BY_ID.get(legacyNumericId));
    }

    public static LegacyBiomeTerrainProfile require(int legacyNumericId) {
        LegacyBiomeTerrainProfile profile = BY_ID.get(legacyNumericId);
        if (profile == null) throw new IllegalArgumentException("Unknown vanilla 1.12 biome id " + legacyNumericId);
        return profile;
    }

    public static Map<Integer, LegacyBiomeTerrainProfile> vanillaProfiles() {
        return BY_ID;
    }
}
