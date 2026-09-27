package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.world.worldgen.AgeLegacyStructureSetSpecs;

import java.util.List;

/** Pure-Java regression checks for the Legacy structure-set compatibility contract. */
public final class AgeLegacyStructureSetSpecHarness {
    private AgeLegacyStructureSetSpecHarness() {}

    public static void main(String[] args) {
        if (AgeLegacyStructureSetSpecs.VILLAGE_SPACING != 32) {
            throw new AssertionError("Legacy MapGenVillage distance must remain 32");
        }
        if (AgeLegacyStructureSetSpecs.VILLAGE_SEPARATION != 8) {
            throw new AssertionError("Legacy MapGenVillage separation must remain 8");
        }
        if (AgeLegacyStructureSetSpecs.VILLAGE_SALT != 10387312) {
            throw new AssertionError("Legacy MapGenVillage salt drifted");
        }
        List<String> villages = AgeLegacyStructureSetSpecs.legacyVillageStructureIds();
        if (!villages.equals(List.of(
                "minecraft:village_plains",
                "minecraft:village_desert",
                "minecraft:village_savanna",
                "minecraft:village_taiga"))) {
            throw new AssertionError("Legacy village style allow-list drifted: " + villages);
        }
        if (villages.contains("minecraft:village_snowy")) {
            throw new AssertionError("1.12 MapGenVillage did not include snowy villages");
        }
        if (!AgeLegacyStructureSetSpecs.LEGACY_MINESHAFT_STRUCTURES.equals(List.of(
                "minecraft:mineshaft", "minecraft:mineshaft_mesa"))) {
            throw new AssertionError("Legacy mineshaft compatibility structures drifted: "
                    + AgeLegacyStructureSetSpecs.LEGACY_MINESHAFT_STRUCTURES);
        }
        if (!"minecraft:fortress".equals(AgeLegacyStructureSetSpecs.NETHER_FORTRESS_STRUCTURE)) {
            throw new AssertionError("NetherFort Symbol must resolve to fortress only");
        }
        System.out.println("AgeLegacyStructureSetSpecHarness: PASS");
    }
}
