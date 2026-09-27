package com.xcompwiz.mystcraft.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Pure-source guard for the CP252 runtime/worldgen repair bundle. */
public final class Checkpoint252SourceHarness {
    private Checkpoint252SourceHarness() {}

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "." : args[0]);
        List<String> errors = new ArrayList<>();

        checkSpectator(root, "BlockBookBinder.java", "serverPlayer.openMenu(binder");
        checkSpectator(root, "BlockInkMixer.java", "serverPlayer.openMenu(mixer");
        checkSpectator(root, "BlockLinkModifier.java", "serverPlayer.openMenu(modifier");
        checkSpectator(root, "BlockWritingDesk.java", "serverPlayer.openMenu(desk");

        String deep = read(root, "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyPopulationFeature.java");
        require(deep, "random.nextInt(8) != 0", "Deep Lakes lost legacy 1/8 attempt gate", errors);
        require(deep, "level.getMaxBuildHeight() - minY", "Deep Lakes is no longer adapted to the full modern build height", errors);
        require(deep, "y >= level.getSeaLevel() && random.nextInt(10) != 0", "Deep Lakes lost legacy above-sea 1/10 branch", errors);
        require(deep, "CP252 Deep Lake generated", "Deep Lakes lost CP252 runtime evidence log", errors);

        String accel = read(root, "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeAcceleratedTickRuntimeEvents.java");
        require(accel, "for (int i = 0; i < 3; i++)", "EnvAccel no longer performs the original fixed three attempts", errors);
        require(accel, "fixedExtraAttemptsPerRandomTickingSection=3", "EnvAccel lost fixed-attempt runtime evidence", errors);

        String noise = read(root, "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeNoiseRouterIsolation.java");
        require(noise, "overworld/sloped_cheese", "Normal cave isolation lost sloped-cheese base terrain", errors);
        require(noise, "overworld_amplified/sloped_cheese", "Amplified cave isolation lost sloped-cheese base terrain", errors);
        require(noise, "DensityFunctions.interpolated(DensityFunctions.blendDensity(slid))", "Cave-free final density no longer follows vanilla post-processing", errors);
        if (noise.contains("ENTRANCES") || noise.contains("NOODLE") || noise.contains("SPAGHETTI")) {
            errors.add("Cave-free final density accidentally reintroduced a modern cave density component");
        }

        String levelStem = read(root, "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLevelStemFactory.java");
        require(levelStem, "AgeNoiseRouterIsolation.withoutImplicitOverworldCaves", "Normal/Amplified terrain no longer routes through cave isolation", errors);
        require(levelStem, "new AgeNoiseBasedChunkGenerator", "Noise terrain lost the CP252 structure bridge generator", errors);

        String fortressPlacement = read(root, "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyNetherFortressPlacement.java");
        require(fortressPlacement, "FrequencyReductionMethod.DEFAULT", "Nether Fortress still double-applies a modern frequency reducer", errors);
        require(fortressPlacement, "AgeLegacyNetherFortressPlacementMath.isPlacementChunk", "Nether Fortress lost exact 1.12 candidate test", errors);

        String selection = read(root, "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeStructureSetSelection.java");
        require(selection, "new StructureSet(fortress, new AgeLegacyNetherFortressPlacement())", "Nether Fortress no longer uses fortress-only legacy placement", errors);

        String bridge = read(root, "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyStructureGenerationBridge.java");
        require(bridge, "biome -> true", "Nether Fortress biome-independence bridge is missing", errors);
        require(bridge, "CP252 legacy Nether Fortress start", "Nether Fortress lost runtime evidence log", errors);

        String portal = read(root, "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeEndPortalTravelGuard.java");
        require(portal, "event.getDimension().equals(Level.END)", "End Portal guard is not destination-scoped to The End", errors);
        require(portal, "getNamespace().equals(Mystcraft.MOD_ID)", "End Portal guard is not source-scoped to Mystcraft Ages", errors);
        require(portal, "is(Blocks.END_PORTAL)", "End Portal guard is not restricted to actual End Portal travel", errors);
        require(portal, "event.setCanceled(true)", "End Portal travel is not canceled", errors);

        if (!errors.isEmpty()) throw new AssertionError(String.join("\n", errors));
        System.out.println("Checkpoint252SourceHarness: PASS");
    }

    private static void checkSpectator(Path root, String file, String normalOpen) throws Exception {
        String text = read(root, "src/main/java/com/xcompwiz/mystcraft/block/" + file);
        if (!text.contains("public MenuProvider getMenuProvider") || !text.contains("return null;")) {
            throw new AssertionError(file + " still exposes a spectator MenuProvider");
        }
        if (!text.contains(normalOpen)) {
            throw new AssertionError(file + " lost its explicit normal-player menu payload path");
        }
    }

    private static String read(Path root, String relative) throws Exception {
        return Files.readString(root.resolve(relative));
    }

    private static void require(String text, String token, String error, List<String> errors) {
        if (!text.contains(token)) errors.add(error);
    }
}
