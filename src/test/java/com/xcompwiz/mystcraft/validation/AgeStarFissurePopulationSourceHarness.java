package com.xcompwiz.mystcraft.validation;

import java.nio.file.Files;
import java.nio.file.Path;

/** Source-level contract for pre-FEATURES spawn resolution and ordered Star Fissure short-circuiting. */
public final class AgeStarFissurePopulationSourceHarness {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        String spawn = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeSpawnResolver.java"));
        String placed = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyPopulationPlacedFeatures.java"));
        String batch = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyPopulationBatchFeature.java"));
        String bridge = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeStarFissurePopulationBridge.java"));
        String runtime = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeStarFissureRuntime.java"));
        String record = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/agedata/AgeRecord.java"));

        require(spawn.contains("ChunkStatus.CARVERS"), "spawn search must stop before FEATURES");
        require(!spawn.contains("getHeightmapPos("), "spawn search must not force FULL heightmap lookup");
        require(spawn.contains("level.getSeaLevel()"), "Legacy spawn Y starts at sea level");
        require(spawn.contains("state.blocksMotion()"), "Legacy canCoordinateBeSpawn movement-blocking check");

        require(placed.contains("case STAR_FISSURE"), "Star Fissure ordering sentinel must be installed");
        require(placed.contains("starFissureSentinel()"), "Star Fissure sentinel factory");
        require(placed.contains("case MINESHAFTS, STRONGHOLDS, NETHER_FORTRESS"),
                "Legacy structure population steps must remain explicit in Symbol order");
        require(batch.contains("AgeStarFissurePopulationBridge.stageIfSpawnChunk"),
                "spawn-chunk Star Fissure plan staging");
        require(batch.contains("containsStarFissureAfter(index)"),
                "Village short-circuit must know whether it suppresses Star Fissure");
        require(batch.contains("AgeStarFissurePopulationBridge.suppressIfSpawnChunk"),
                "Village-before-Star-Fissure suppression bridge");

        require(bridge.contains("ConcurrentHashMap"), "async worldgen/server-thread handoff must be concurrent");
        require(bridge.contains("AgeStarFissureLegacyPlan.create"), "FEATURES pass must consume Star Fissure RNG");
        require(runtime.contains("level.getChunk(spawnChunkX, spawnChunkZ)"),
                "runtime must force spawn chunk FULL only after spawn is installed");
        require(runtime.contains("AgeStarFissurePopulationBridge.take"),
                "server thread must consume the staged decision");
        require(runtime.contains("populationBoundarySeed"),
                "pre-I66 already-FULL chunk fallback must preserve real Legacy boundary seed");

        require(record.contains("StarFissureProcessed"), "suppressed-vs-generated one-shot state must be durable");
        require(record.contains(": record.starFissureGenerated"),
                "old records must migrate generated=true to processed=true");

        System.out.println("AgeStarFissurePopulationSourceHarness: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
