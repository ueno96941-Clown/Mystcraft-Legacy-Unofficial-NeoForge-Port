package com.xcompwiz.mystcraft.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Source contract checks for the modern Folder/Portfolio page-container milestone. */
public final class PageContainerSourceHarness {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "." : args[0]);
        List<String> errors = new ArrayList<>();

        String base = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/item/ItemPageContainer.java"));
        String registry = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/registry/MystItems.java"));
        String binder = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/blockentity/BookBinderBlockEntity.java"));
        String menu = Files.readString(root.resolve("src/main/java/com/xcompwiz/mystcraft/inventory/BookBinderMenu.java"));

        if (!base.contains("DataComponents.CONTAINER") || !base.contains("ItemContainerContents.fromItems")) {
            errors.add("page containers must use the vanilla 1.21.1 CONTAINER component");
        }
        if (!base.contains("source.is(Items.PAPER)") || !base.contains("Page.createPage()")) {
            errors.add("paper insertion must normalize to a blank Mystcraft page");
        }
        if (!base.contains("removeFirst(ItemStack container)") || !base.contains("removeLast(ItemStack container)")) {
            errors.add("page containers lost ordered Binder drain / hand extraction behavior");
        }
        if (!registry.contains("DeferredItem<ItemFolder>") || !registry.contains("DeferredItem<ItemPortfolio>")) {
            errors.add("Folder/Portfolio regressed to placeholder Items");
        }
        if (!registry.contains("component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)")) {
            errors.add("Folder/Portfolio default CONTAINER component missing");
        }
        if (!binder.contains("container.isEmpty(stack) && container.getLegacyName(stack).isEmpty()")) {
            errors.add("Book Binder must accept only unnamed, content-empty Folder covers");
        }
        if (!binder.contains("importPageContainer(ItemStack source)")) {
            errors.add("Book Binder page-container import path missing");
        }
        if (!menu.contains("raw.getItem() instanceof ItemPageContainer") || !menu.contains("blockEntity.importPageContainer(raw)")) {
            errors.add("Book Binder shift-click no longer drains portable page containers");
        }
        int importCall = menu.indexOf("blockEntity.importPageContainer(raw)");
        int importReturn = menu.indexOf("return copy;", importCall);
        if (importCall < 0 || importReturn < importCall) {
            errors.add("Book Binder page-container shift-click must report success when only components change");
        }

        for (String recipe : List.of("folder.json", "portfolio.json")) {
            Path path = root.resolve("src/main/resources/data/mystcraft/recipe").resolve(recipe);
            if (!Files.isRegularFile(path)) errors.add("missing crafting recipe: " + recipe);
        }

        if (!errors.isEmpty()) {
            errors.forEach(System.err::println);
            throw new IllegalStateException("PageContainerSourceHarness failed: " + errors.size());
        }
        System.out.println("PageContainerSourceHarness: PASS");
    }
}
