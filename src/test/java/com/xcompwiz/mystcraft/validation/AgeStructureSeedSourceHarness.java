package com.xcompwiz.mystcraft.validation;

import java.nio.file.Files;
import java.nio.file.Path;

/** Focused source contract for Age-local structure placement seeding. */
public final class AgeStructureSeedSourceHarness {
    public static void main(String[] args) throws Exception {
        Path root=Path.of(args.length>0?args[0]:".").toAbsolutePath().normalize();
        String text=Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeStructureStateInstaller.java"));
        require(!text.contains("ChunkGeneratorStructureState.createForFlat("), "createForFlat hard-codes concentricRingsSeed=0");
        require(text.contains("new ChunkGeneratorStructureState("), "explicit selected StructureState constructor missing");
        require(text.contains("ageSeed,\n                ageSeed,"), "level/concentric ring seeds must both be Age seed");
        int init=text.indexOf("state.ensureStructuresGenerated()");
        int publish=text.indexOf("chunkSource.chunkMap.chunkGeneratorState = state");
        require(init>=0 && publish>=0 && init<publish, "replacement StructureState must initialize before publication");
        System.out.println("AgeStructureSeedSourceHarness: PASS");
    }

    private static void require(boolean condition,String message) {
        if(!condition) throw new IllegalStateException(message);
    }
}
