package com.xcompwiz.mystcraft.validation;

import java.nio.file.Files;
import java.nio.file.Path;

/** Lightweight source invariant gate for CP253's two targeted QA repairs. */
public final class Checkpoint253SourceHarness {
    private Checkpoint253SourceHarness() {}

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "." : args[0]);
        String accel = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeAcceleratedTickRuntimeEvents.java"));
        String locate = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyStructureLocateBridge.java"));
        String noise = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeNoiseBasedChunkGenerator.java"));
        String flat = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeFlatNoiseBasedChunkGenerator.java"));
        String voidGen = Files.readString(root.resolve(
                "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeVoidFlatLevelSource.java"));

        require(accel.contains("EXTRA_ATTEMPTS_PER_RANDOM_TICKING_SECTION = 6"),
                "EnvAccel extra attempts are not doubled to 6");
        require(accel.contains("CP253 EnvAccel active"), "CP253 EnvAccel diagnostic missing");
        require(locate.contains("ChunkStatus.STRUCTURE_STARTS"),
                "Fortress locate fallback must force only structure-start generation");
        require(locate.contains("candidate.enabled()"),
                "Fortress locate fallback must preserve the legacy one-in-three regional gate");
        require(locate.contains("CP253 legacy Nether Fortress locate"),
                "Fortress locate diagnostic missing");
        require(noise.contains("AgeLegacyStructureLocateBridge.findNearestFortress"),
                "Noise generator locate fallback missing");
        require(flat.contains("AgeLegacyStructureLocateBridge.findNearestFortress"),
                "Flat generator locate fallback missing");
        require(voidGen.contains("AgeLegacyStructureLocateBridge.findNearestFortress"),
                "Void generator locate fallback missing");

        System.out.println("CP253 source invariants: PASS");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
