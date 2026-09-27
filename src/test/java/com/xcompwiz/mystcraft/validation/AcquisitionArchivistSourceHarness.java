package com.xcompwiz.mystcraft.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Static contract for Legacy page acquisition, Small Libraries and Archivist trade restoration. */
public final class AcquisitionArchivistSourceHarness {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "." : args[0]);
        List<String> errors = new ArrayList<>();
        String treasure = read(root, "src/main/java/com/xcompwiz/mystcraft/treasure/MystcraftTreasureGenerator.java");
        String library = read(root, "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacySmallLibraryFeature.java");
        String placementMath = read(root, "src/main/java/com/xcompwiz/mystcraft/world/worldgen/AgeLegacySmallLibraryPlacementMath.java");
        String placed = read(root, "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyPopulationPlacedFeatures.java");
        String batch = read(root, "src/main/java/com/xcompwiz/mystcraft/world/dimension/AgeLegacyPopulationBatchFeature.java");
        String display = read(root, "src/main/java/com/xcompwiz/mystcraft/blockentity/BookDisplayBlockEntity.java");
        String villagers = read(root, "src/main/java/com/xcompwiz/mystcraft/registry/MystVillagers.java");
        String events = read(root, "src/main/java/com/xcompwiz/mystcraft/villager/ArchivistEvents.java");
        String shop = read(root, "src/main/java/com/xcompwiz/mystcraft/villager/ArchivistShopData.java");

        if (!treasure.contains("VIAL_WEIGHT = 50") || !treasure.contains("BOOSTER_WEIGHT = 1000")
                || !treasure.contains("LEATHER_WEIGHT = 50") || !treasure.contains("PAPER_WEIGHT = 50")
                || !treasure.contains("int rolls = 4 + random.nextInt(5)"))
            errors.add("mystcraft_treasure base weights/4..8 rolls drifted");
        if (!treasure.contains("1 + random.nextInt(Math.max(1, maxStack - 1))")
                || !treasure.contains("SymbolItemEconomy.cardRank(symbol) < 3"))
            errors.add("legacy symbol treasure count / rank>=3 lectern filter missing");
        if (treasure.contains("SIMPLE_DUNGEON") || treasure.contains("DESERT_PYRAMID") || treasure.contains("STRONGHOLD_LIBRARY"))
            errors.add("empty legacy TreasureGenWrapper must not be 'fixed' into vanilla chest injection");

        if (!placementMath.contains("MAX_DISTANCE = 32") || !placementMath.contains("MIN_DISTANCE = 8")
                || !placementMath.contains("SALT = 14357617L") || count(library, "new Placement") != 695)
            errors.add("Small Library spacing/salt/694-placement blueprint drifted");
        if (!placementMath.contains("new Random(regionX * 341873128712L + regionZ * 132897987541L + worldSeed + SALT)")
                || !placementMath.contains("random.nextInt(MAX_DISTANCE - MIN_DISTANCE)"))
            errors.add("Small Library legacy scattered-feature candidate math missing");
        if (!library.contains("MystcraftTreasureGenerator.generate(random)")
                || !library.contains("MystcraftTreasureGenerator.generateLecternItem")
                || !library.contains("private static final int[][] LECTERNS"))
            errors.add("Small Library treasure chest / five lecterns missing");
        if (!placed.contains("Small Library") || !batch.contains("smallLibrary.place(context)"))
            errors.add("Small Library is not unconditional in every Age population batch");

        if (!display.contains("getBlockState().is(MystBlocks.LECTERN.get())") || !display.contains("stack.getItem() instanceof ItemPage"))
            errors.add("legacy Lectern no longer accepts Symbol Pages");
        if (!villagers.contains("ImmutableSet.of()") || !villagers.contains("WRITING_DESK"))
            errors.add("Archivist profession POI/1.21 VillagerProfession constructor bridge incomplete");
        if (!events.contains("isShiftKeyDown()") || !events.contains("new ItemStack(Items.EMERALD, 25)"))
            errors.add("Archivist sneak vanilla-trade fallback / 25-emerald Booster path missing");
        if (!shop.contains("STEP_SIZE = 12000L") || !shop.contains("page.setCount(3)")
                || !shop.contains("boosterCount = 5") || !shop.contains("getBoosterCost() { return 20; }")
                || !shop.contains("chooseAtLeastRank(villager.getRandom(), index + 1)"))
            errors.add("Archivist custom shop stock/restock/rank/price contract drifted");

        if (!errors.isEmpty()) {
            errors.forEach(System.err::println);
            throw new IllegalStateException("AcquisitionArchivistSourceHarness failed: " + errors.size());
        }
        System.out.println("AcquisitionArchivistSourceHarness: PASS");
    }

    private static int count(String text, String needle) {
        int n = 0, at = 0;
        while ((at = text.indexOf(needle, at)) >= 0) { ++n; at += needle.length(); }
        return n;
    }

    private static String read(Path root, String relative) throws Exception {
        return Files.readString(root.resolve(relative));
    }
}
