package com.xcompwiz.mystcraft.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Source contract for the completion-first Crystal Portal milestone. */
public final class CrystalPortalSourceHarness {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "." : args[0]);
        List<String> errors = new ArrayList<>();

        String registry = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/registry/MystBlocks.java"));
        String beRegistry = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/registry/MystBlockEntities.java"));
        String receptacle = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/blockentity/BookReceptacleBlockEntity.java"));
        String runtime = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/portal/CrystalPortalRuntime.java"));
        String portal = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/block/BlockLinkPortal.java"));
        String link = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/linking/LinkController.java"));
        String creative = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/registry/MystCreativeTabs.java"));

        if (!registry.contains("DeferredBlock<BlockBookReceptacle>")
                || !registry.contains("DeferredBlock<BlockCrystal>")
                || !registry.contains("DeferredBlock<BlockLinkPortal>")) {
            errors.add("Crystal Portal blocks regressed to placeholder Block registrations");
        }
        if (!beRegistry.contains("BlockEntityType<BookReceptacleBlockEntity>")) {
            errors.add("Book Receptacle block entity registration missing");
        }
        if (!receptacle.contains("PortalBlocks") || !receptacle.contains("FrameBlocks")) {
            errors.add("portal/frame geometry must be persisted by the Book Receptacle");
        }
        if (!receptacle.contains("portalMutation") || !receptacle.contains("beginPortalMutation()")
                || !runtime.contains("receptacle.isPortalMutation() || isStillValid")) {
            errors.add("portal mutation recursion guard missing");
        }
        if (!runtime.contains("MAX_OUTER_WIDTH = 21") || !runtime.contains("MAX_OUTER_HEIGHT = 21")) {
            errors.add("bounded modern Crystal Portal geometry contract missing");
        }
        if (!runtime.contains("width < 3 || height < 4") || !runtime.contains("detectVerticalRectangle")) {
            errors.add("vertical rectangular Crystal frame validation missing");
        }
        if (!runtime.contains("state.isAir() && !state.is(MystBlocks.LINK_PORTAL.get())")) {
            errors.add("portal interior replacement safety check missing");
        }
        if (!portal.contains("player.isOnPortalCooldown()") || !portal.contains("player.setPortalCooldown()")) {
            errors.add("portal collision cooldown missing");
        }
        if (!portal.contains("LinkController.travelPlayerFromPortal")) {
            errors.add("Link Portal is not connected to the server-authoritative linking path");
        }
        if (!link.contains("travelPlayerFromPortal") || !link.contains("!portalTravel && !following")) {
            errors.add("portal travel must preserve the receptacle book instead of dropping the player's hand item");
        }
        if (creative.contains("output.accept(MystItems.LINK_PORTAL_BLOCK.get())")) {
            errors.add("internal Link Portal field block should not be exposed in the creative tab");
        }
        if (!Files.isRegularFile(root.resolve("src/main/resources/data/mystcraft/recipe/book_receptacle.json"))) {
            errors.add("Book Receptacle crafting recipe missing");
        }
        for (String loot : List.of("blockcrystal.json", "blockbookreceptacle.json")) {
            if (!Files.isRegularFile(root.resolve("src/main/resources/data/mystcraft/loot_table/blocks").resolve(loot))) {
                errors.add("missing portal survival loot table: " + loot);
            }
        }

        if (!errors.isEmpty()) {
            errors.forEach(System.err::println);
            throw new IllegalStateException("CrystalPortalSourceHarness failed: " + errors.size());
        }
        System.out.println("CrystalPortalSourceHarness: PASS");
    }
}
