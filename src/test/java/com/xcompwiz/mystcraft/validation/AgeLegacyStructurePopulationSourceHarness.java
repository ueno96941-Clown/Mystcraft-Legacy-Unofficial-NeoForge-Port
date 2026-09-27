package com.xcompwiz.mystcraft.validation;

import java.nio.file.Files;
import java.nio.file.Path;

/** Source contract for moving Legacy MapGenStructure piece placement into IPopulate order. */
public final class AgeLegacyStructurePopulationSourceHarness {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        String runtime = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyStructurePlacementRuntime.java"));
        String bridge = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyStructurePopulationBridge.java"));
        String batch = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyPopulationBatchFeature.java"));
        String placed = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyPopulationPlacedFeatures.java"));
        String installer = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeRuntimeServerLevelInstaller.java"));
        String at = Files.readString(root.resolve("src/main/resources/META-INF/accesstransformer.cfg"));

        require(runtime.contains("super(level, new WorldOptions(ageSeed, true, false), structureCheck)"),
                "top-level Age manager must continue reporting structures enabled");
        require(runtime.contains("this.suppressedWorldgenOptions = new WorldOptions(ageSeed, false, false)"),
                "worldgen-region decoration suppression options");
        require(runtime.contains("level.structureManager = new PopulationAwareStructureManager("),
                "replace only Age StructureManager while retaining StructureCheck");
        require(runtime.contains("public StructureManager forWorldGenRegion(WorldGenRegion region)"),
                "worldgen-only suppression override");
        require(at.contains("public-f net.minecraft.server.level.ServerLevel structureManager"), "StructureManager AT");
        require(installer.contains("AgeStructureStateInstaller.install(")
                        && installer.indexOf("AgeStructureStateInstaller.install(")
                        < installer.indexOf("AgeLegacyStructurePlacementRuntime.install(level, age.seed())"),
                "StructureState before placement suppression");

        require(bridge.contains("int minX = chunkBlockX + 8"), "legacy shifted population X");
        require(bridge.contains("int minZ = chunkBlockZ + 8"), "legacy shifted population Z");
        require(bridge.contains("int maxX = minX + 15"), "legacy 16-wide box");
        require(bridge.contains("structures.startsForStructure"), "query prepared modern StructureStarts");
        require(bridge.contains("start.placeInChunk("), "manual structure piece placement");
        require(bridge.contains("structureBox.intersects(populationBox)"), "full-box intersection gate");
        require(bridge.contains("case MINESHAFTS ->") && bridge.contains("mineshaft_mesa"), "Mineshaft styles");
        require(bridge.contains("case STRONGHOLDS -> \"stronghold\".equals(path)"), "Stronghold only");
        require(bridge.contains("case NETHER_FORTRESS -> \"fortress\".equals(path)"), "Fortress only, no Bastion");
        require(bridge.contains("case VILLAGES -> path.startsWith(\"village_\")"), "Village family");

        require(placed.contains("case MINESHAFTS, STRONGHOLDS, NETHER_FORTRESS")
                        && placed.contains("legacyStructure(entry.kind())"),
                "non-village Structure Symbols join ordered batch");
        require(batch.contains("AgeLegacyStructurePopulationBridge.place"), "batch performs structure placement");
        require(batch.contains("step.kind() == AgeFeatureKind.VILLAGES && placed"),
                "only successful Village controls Legacy short-circuit");
        require(batch.contains("Only SymbolVillages returned MapGenStructure.generateStructure's boolean"),
                "false-return contract for other Legacy structures documented");

        System.out.println("AgeLegacyStructurePopulationSourceHarness: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
