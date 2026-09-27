package com.xcompwiz.mystcraft.registry;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.inventory.BookBinderMenu;
import com.xcompwiz.mystcraft.inventory.LinkBookMenu;
import com.xcompwiz.mystcraft.inventory.LinkModifierMenu;
import com.xcompwiz.mystcraft.inventory.InkMixerMenu;
import com.xcompwiz.mystcraft.inventory.WritingDeskMenu;
import com.xcompwiz.mystcraft.inventory.ArchivistShopMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class MystMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Mystcraft.MOD_ID);

    public static final Supplier<MenuType<LinkBookMenu>> BOOK = MENUS.register(
            "book", () -> IMenuTypeExtension.create(LinkBookMenu::new));

    public static final Supplier<MenuType<BookBinderMenu>> BOOK_BINDER = MENUS.register(
            "book_binder", () -> IMenuTypeExtension.create(BookBinderMenu::new));

    public static final Supplier<MenuType<WritingDeskMenu>> WRITING_DESK = MENUS.register(
            "writing_desk", () -> IMenuTypeExtension.create(WritingDeskMenu::new));

    public static final Supplier<MenuType<InkMixerMenu>> INK_MIXER = MENUS.register(
            "ink_mixer", () -> IMenuTypeExtension.create(InkMixerMenu::new));

    public static final Supplier<MenuType<LinkModifierMenu>> LINK_MODIFIER = MENUS.register(
            "link_modifier", () -> IMenuTypeExtension.create(LinkModifierMenu::new));

    public static final Supplier<MenuType<ArchivistShopMenu>> ARCHIVIST = MENUS.register(
            "archivist", () -> IMenuTypeExtension.create(ArchivistShopMenu::new));

    private MystMenus() {}

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
