package com.xcompwiz.mystcraft.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Source-only guard for CP221's Legacy Book Binder transaction boundary. */
public final class BookBinderTransactionSourceHarness {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "." : args[0]);
        List<String> errors = new ArrayList<>();

        String block = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/blockentity/BookBinderBlockEntity.java"));
        String menu = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/inventory/BookBinderMenu.java"));
        String client = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/network/BinderPagesPayloadHandler.java"));
        String ages = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/world/agedata/AgeManager.java"));

        if (!block.contains("public ItemStack getCraftedItem()")) errors.add("Missing plain Legacy result preview");
        if (!block.contains("new ItemStack(MystItems.AGEBOOK.get())") || block.contains("previewBuiltItem"))
            errors.add("Stale pre-authored preview path still exists");
        if (!menu.contains("blockEntity.buildItem(stack, player)"))
            errors.add("Normal pickup does not author the exact taken stack");
        if (!block.contains("ItemAgebook.create(result, player, snapshot, titleSnapshot)"))
            errors.add("Build does not author from an immutable page/title snapshot");
        if (!block.contains("createBuiltItemSnapshot(Player player)"))
            errors.add("Missing finalized QUICK_MOVE snapshot path");

        int quickEncode = menu.indexOf("ItemStack finalized = blockEntity.createBuiltItemSnapshot(player)");
        int quickMove = menu.indexOf("moveItemStackTo(remainder, PLAYER_SLOT_START, PLAYER_SLOT_END, true)");
        int quickConsume = menu.indexOf("blockEntity.consumeBuiltItem()");
        if (!(quickEncode >= 0 && quickEncode < quickMove && quickMove < quickConsume))
            errors.add("QUICK_MOVE must encode, transfer, then consume in that order");

        if (!block.contains("if (container.isEmpty(source))") || !block.contains("container.setPages(source, transfer)"))
            errors.add("Legacy empty-Folder Binder export is missing");
        if (!block.contains("container.getLegacyName(stack).isEmpty()"))
            errors.add("Named empty Folder can still become a Binder cover");

        if (!block.contains("CP221 Book Binder build snapshot")
                || !block.contains("CP221 Descriptive Book Pages after encode"))
            errors.add("Missing Binder/book server trace points");
        if (!client.contains("CP221 Book Binder client mirror menu"))
            errors.add("Missing client Binder mirror trace");
        if (!ages.contains("CP221 AgeData input Pages for Age"))
            errors.add("Missing AgeData input trace");

        if (!errors.isEmpty()) throw new AssertionError(String.join("\n", errors));
        System.out.println("BookBinderTransactionSourceHarness: PASS");
    }
}
