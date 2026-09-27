package com.xcompwiz.mystcraft.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Source-only guard for the modern Book Stand/Lectern/Link Modifier milestone. */
public final class FunctionalBookFixturesSourceHarness {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "." : args[0]);
        List<String> errors = new ArrayList<>();
        String blocks = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/registry/MystBlocks.java"));
        String entities = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/registry/MystBlockEntities.java"));
        String display = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/block/BlockBookDisplay.java"));
        String displayBe = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/blockentity/BookDisplayBlockEntity.java"));
        String modifier = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/block/BlockLinkModifier.java"));
        String modifierBe = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/blockentity/LinkModifierBlockEntity.java"));
        String props = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/linking/LinkProperties.java"));

        if (blocks.contains("BOOK_STAND = simple(") || blocks.contains("LECTERN = simple(") || blocks.contains("LINK_MODIFIER = simple("))
            errors.add("Book fixtures must not remain placeholder simple blocks");
        if (!blocks.contains("BlockBookDisplay::new") || !blocks.contains("BlockLinkModifier::new"))
            errors.add("Functional book fixture block classes are not registered");
        if (!entities.contains("BOOK_DISPLAY") || !entities.contains("LINK_MODIFIER"))
            errors.add("Book fixture block entities are not registered");
        if (!display.contains("LinkController.travelPlayerFromPortal") || !display.contains("player.isShiftKeyDown()"))
            errors.add("Book display must link persistently and support sneak retrieval");
        if (!displayBe.contains("ItemStack.parseOptional") || !displayBe.contains("saveOptional"))
            errors.add("Book display must persist its stored book");
        if (!modifier.contains("Page.isLinkPanel") || !modifier.contains("LinkOptions.setFlag") || !modifier.contains("Items.PAPER"))
            errors.add("Link Modifier must support property-panel editing and paper selection");
        if (!modifier.contains("player.isShiftKeyDown()") || !modifier.contains("Page.addLinkProperty"))
            errors.add("Link Modifier must support disabling panels and imprinting blank panels");
        if (!modifierBe.contains("SelectedProperty") || !modifierBe.contains("saveOptional"))
            errors.add("Link Modifier selector/book state must persist");
        if (!props.contains("MODIFIER_EDITABLE") || !props.contains("MAINTAIN_MOMENTUM") || !props.contains("GENERATE_PLATFORM"))
            errors.add("Editable property list missing implemented LinkController flags");
        for (String id : List.of("book_stand.json", "lectern.json")) {
            if (!Files.exists(root.resolve("src/main/resources/data/mystcraft/recipe").resolve(id)))
                errors.add("Missing recipe: " + id);
        }
        if (Files.exists(root.resolve("src/main/resources/data/mystcraft/recipe/link_modifier.json")))
            errors.add("Link Modifier must not regain a non-legacy convenience recipe");
        for (String id : List.of("blockbookstand.json", "blocklectern.json", "blocklinkmodifier.json")) {
            if (!Files.exists(root.resolve("src/main/resources/data/mystcraft/loot_table/blocks").resolve(id)))
                errors.add("Missing loot table: " + id);
        }

        if (!errors.isEmpty()) throw new AssertionError(String.join("\n", errors));
        System.out.println("FunctionalBookFixturesSourceHarness: PASS");
    }
}
