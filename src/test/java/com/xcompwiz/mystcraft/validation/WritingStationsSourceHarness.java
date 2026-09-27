package com.xcompwiz.mystcraft.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Source contract for the legacy Writing Desk / Ink Mixer restoration pass. */
public final class WritingStationsSourceHarness {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "." : args[0]);
        List<String> errors = new ArrayList<>();

        String blocks = read(root, "src/main/java/com/xcompwiz/mystcraft/registry/MystBlocks.java");
        String blockEntities = read(root, "src/main/java/com/xcompwiz/mystcraft/registry/MystBlockEntities.java");
        String capabilities = read(root, "src/main/java/com/xcompwiz/mystcraft/fluid/MystFluidCapabilities.java");
        String deskBlock = read(root, "src/main/java/com/xcompwiz/mystcraft/block/BlockWritingDesk.java");
        String desk = read(root, "src/main/java/com/xcompwiz/mystcraft/blockentity/WritingDeskBlockEntity.java");
        String deskMenu = read(root, "src/main/java/com/xcompwiz/mystcraft/inventory/WritingDeskMenu.java");
        String deskItems = read(root, "src/main/java/com/xcompwiz/mystcraft/inventory/WritingDeskItemHandler.java");
        String mixerBlock = read(root, "src/main/java/com/xcompwiz/mystcraft/block/BlockInkMixer.java");
        String mixer = read(root, "src/main/java/com/xcompwiz/mystcraft/blockentity/InkMixerBlockEntity.java");
        String mixerMenu = read(root, "src/main/java/com/xcompwiz/mystcraft/inventory/InkMixerMenu.java");
        String effects = read(root, "src/main/java/com/xcompwiz/mystcraft/data/InkEffects.java");

        if (!blocks.contains("DeferredBlock<BlockWritingDesk>") || !blocks.contains("DeferredBlock<BlockInkMixer>"))
            errors.add("Writing Desk / Ink Mixer regressed to placeholder blocks");
        if (!blocks.contains("sound(SoundType.WOOD)"))
            errors.add("legacy wood SoundType not restored for writing furniture");
        if (!blockEntities.contains("WritingDeskBlockEntity::new") || !blockEntities.contains("InkMixerBlockEntity::new"))
            errors.add("writing station BlockEntity registration missing");

        if (!desk.contains("CAPACITY = 1000") || !desk.contains("WRITE_COST = 50"))
            errors.add("Writing Desk 1000mB/50mB legacy ink contract changed");
        if (!desk.contains("TAB_SLOT_COUNT = 25") || !desk.contains("WORK_SLOT_COUNT = 4"))
            errors.add("Writing Desk 4 work + 25 tab inventory shape changed");
        if (!deskBlock.contains("IS_TOP") || !deskBlock.contains("IS_FOOT") || !deskBlock.contains("WritingDeskBlockEntity::serverTick"))
            errors.add("Writing Desk four-part furniture/tick bridge incomplete");
        if (!desk.contains("processFluidContainer()") || !desk.contains("Capabilities.FluidHandler.ITEM"))
            errors.add("Writing Desk no longer processes fluid containers server-side");
        if (!deskItems.contains("slot == WritingDeskBlockEntity.PAPER_SLOT") || !deskItems.contains("return ItemStack.EMPTY;"))
            errors.add("Writing Desk item automation must remain legacy paper-input/no-output");
        if (!capabilities.contains("new WritingDeskInkHandler(desk)") || !capabilities.contains("new WritingDeskItemHandler(desk)"))
            errors.add("Writing Desk capability views incomplete");
        if (!deskMenu.contains("ACTION_SURFACE_COPY_SYMBOL") || !deskMenu.contains("ACTION_TARGET_LINK") || !deskMenu.contains("ACTION_TARGET_SET_PAGE"))
            errors.add("Writing Desk page surface/target controls missing");

        if (!mixerBlock.contains("InkMixerBlockEntity::serverTick"))
            errors.add("Ink Mixer server tick missing");
        if (!mixer.contains("BASIN_VOLUME = FluidType.BUCKET_VOLUME") || !mixer.contains("boolean hasInk"))
            errors.add("Ink Mixer must retain one-bucket boolean basin");
        if (!mixer.contains("inkProbabilities.replaceAll") || !mixer.contains("float inverse = 1.0F - total"))
            errors.add("Ink Mixer catalyst compounding formula changed");
        if (!mixer.contains("new Random(nextSeed)") || !mixer.contains("nextSeed = random.nextLong()"))
            errors.add("Ink Mixer deterministic craft RNG stream changed");
        if (mixer.contains("tag.putLong(\"next_seed\""))
            errors.add("Ink Mixer next_seed must not be persisted (legacy quirk)");
        if (!mixerMenu.contains("getLegacyGradientColor") || !mixerMenu.contains("interval > 0.3F"))
            errors.add("Ink Mixer legacy property gradient math missing");
        if (!effects.contains("Items.ENDER_PEARL") || !effects.contains("dusts/diamond") || !effects.contains("dyes/black"))
            errors.add("Ink Mixer catalyst table/common-tag bridge incomplete");

        for (String recipe : List.of("writingdesk.json", "writingdesk_back.json", "ink_mixer.json", "ink_vial.json")) {
            if (!Files.isRegularFile(root.resolve("src/main/resources/data/mystcraft/recipe").resolve(recipe)))
                errors.add("missing crafting recipe: " + recipe);
        }
        if (!Files.isRegularFile(root.resolve("src/main/resources/data/mystcraft/loot_table/blocks/blockinkmixer.json")))
            errors.add("missing survival loot table: blockinkmixer.json");
        if (!deskBlock.contains("getDrops(BlockState state") || !deskBlock.contains("ItemWritingDesk.topStack()"))
            errors.add("Writing Desk custom base/top drops missing");

        if (!errors.isEmpty()) {
            errors.forEach(System.err::println);
            throw new IllegalStateException("WritingStationsSourceHarness failed: " + errors.size());
        }
        System.out.println("WritingStationsSourceHarness: PASS");
    }

    private static String read(Path root, String relative) throws Exception {
        return Files.readString(root.resolve(relative));
    }
}
