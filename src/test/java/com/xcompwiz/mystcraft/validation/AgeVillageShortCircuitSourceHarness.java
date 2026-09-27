package com.xcompwiz.mystcraft.validation;

import java.nio.file.Files;
import java.nio.file.Path;

/** Source contract for Legacy Village IPopulate short-circuit behavior. */
public final class AgeVillageShortCircuitSourceHarness {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        String placed = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyPopulationPlacedFeatures.java"));
        String batch = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyPopulationBatchFeature.java"));
        String village = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyStructurePopulationBridge.java"));

        require(placed.contains("case VILLAGES") && placed.contains("legacyStructure(entry.kind())"), "Village structure population step");
        require(batch.contains("step.kind() == AgeFeatureKind.VILLAGES && placed"), "Village placement-result dispatch");
        require(batch.contains("AgeLegacyStructurePopulationBridge.place"), "Village structure placement bridge");
        require(batch.contains("return any;"), "Legacy Java short-circuit must stop later populators");
        require(batch.contains("any |= step.feature().place(context)"), "non-village children stay non-short-circuit");
        require(!batch.contains("any ||="), "must not accidentally short-circuit ordinary children");
        require(village.contains("int minX = chunkBlockX + 8"), "Legacy +8 population X");
        require(village.contains("int maxX = minX + 15"), "Legacy 16-wide population X");
        require(village.contains("int minZ = chunkBlockZ + 8"), "Legacy +8 population Z");
        require(village.contains("path.startsWith(\"village_\")"), "vanilla village structures only");
        require(village.contains("structureBox.intersects(populationBox)"),
                "StructureStart full bounding-box intersection");
        require(village.contains("start.placeInChunk("), "Village structure pieces placed from Legacy population step");

        System.out.println("AgeVillageShortCircuitSourceHarness: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
