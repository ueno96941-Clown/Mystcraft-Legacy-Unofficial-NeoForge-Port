package com.xcompwiz.mystcraft.validation;

import java.nio.file.Files;
import java.nio.file.Path;

/** Focused source contract for the direct Mystcraft 0.13.7.06 Dungeons population port. */
public final class AgeDungeonPopulationSourceHarness {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        String feature = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyPopulationFeature.java"));
        String placed = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyPopulationPlacedFeatures.java"));
        String bridge = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/worldgen/AgeVanillaWorldgenBridgeResolver.java"));
        String batch = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyPopulationBatchFeature.java"));

        require(feature.contains("for (int attempt = 0; attempt < 8; ++attempt)"), "8 attempts");
        require(feature.contains("chunkX + random.nextInt(16) + 8"), "legacy X +8 window");
        require(feature.contains("chunkZ + random.nextInt(16) + 8"), "legacy Z +8 window");
        require(feature.contains("int y = random.nextInt(256)"), "legacy fixed Y range");
        require(feature.contains("openings < 1 || openings > 5"), "opening guard");
        require(feature.contains("dy == -1 && random.nextInt(4) != 0"), "mossy floor 3/4 rule");
        require(feature.contains("for (int chest = 0; chest < 2; ++chest)"), "two chest attempts");
        require(feature.contains("for (int tryIndex = 0; tryIndex < 3; ++tryIndex)"), "three chest candidates");
        require(feature.contains("BuiltInLootTables.SIMPLE_DUNGEON"), "simple dungeon loot");
        require(feature.contains("EntityType.SKELETON, EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.SPIDER"), "legacy mob pool");
        require(!feature.contains("Feature.MONSTER_ROOM.place"), "must not delegate room shape to modern feature");
        require(!bridge.contains("minecraft:monster_room"), "no modern monster-room injection");
        require(bridge.contains("case DUNGEONS ->"), "dungeon custom bridge case");
        require(placed.contains("new AgeLegacyPopulationBatchFeature(ordered)"), "single ordered batch");
        require(batch.contains("any |= step.feature().place(context)"), "non-short-circuit shared context execution");

        System.out.println("AgeDungeonPopulationSourceHarness: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
