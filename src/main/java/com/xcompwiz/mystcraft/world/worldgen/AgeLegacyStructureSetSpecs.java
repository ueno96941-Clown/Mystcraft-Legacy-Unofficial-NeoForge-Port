package com.xcompwiz.mystcraft.world.worldgen;

import java.util.List;

/**
 * Small pure-Java contract for the Legacy 1.12 structure-set adapters.
 *
 * <p>These constants deliberately describe the old MapGen selection surface rather than
 * blindly inheriting the 1.21.1 built-in StructureSets. The modern structure piece
 * implementations remain compatibility substitutes, but their candidate set/spacing must
 * not silently add structures that the Mystcraft Symbol never requested.</p>
 */
public final class AgeLegacyStructureSetSpecs {
    public static final int VILLAGE_SPACING = 32;
    public static final int VILLAGE_SEPARATION = 8;
    public static final int VILLAGE_SALT = 10387312;

    public static final int STRONGHOLD_DISTANCE = 32;
    public static final int STRONGHOLD_SPREAD = 3;
    public static final int STRONGHOLD_COUNT = 128;
    public static final String STRONGHOLD_STRUCTURE = "minecraft:stronghold";

    private static final List<String> LEGACY_VILLAGE_STRUCTURES = List.of(
            "minecraft:village_plains",
            "minecraft:village_desert",
            "minecraft:village_savanna",
            "minecraft:village_taiga");

    public static final List<String> LEGACY_MINESHAFT_STRUCTURES = List.of(
            "minecraft:mineshaft",
            "minecraft:mineshaft_mesa");

    public static final String NETHER_FORTRESS_STRUCTURE = "minecraft:fortress";

    private AgeLegacyStructureSetSpecs() {}

    public static List<String> legacyVillageStructureIds() {
        return LEGACY_VILLAGE_STRUCTURES;
    }
}
