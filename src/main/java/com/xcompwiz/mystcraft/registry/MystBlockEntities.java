package com.xcompwiz.mystcraft.registry;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.blockentity.BookBinderBlockEntity;
import com.xcompwiz.mystcraft.blockentity.BookReceptacleBlockEntity;
import com.xcompwiz.mystcraft.blockentity.BookDisplayBlockEntity;
import com.xcompwiz.mystcraft.blockentity.LinkModifierBlockEntity;
import com.xcompwiz.mystcraft.blockentity.InkMixerBlockEntity;
import com.xcompwiz.mystcraft.blockentity.WritingDeskBlockEntity;
import com.xcompwiz.mystcraft.blockentity.StarFissureBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class MystBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Mystcraft.MOD_ID);

    public static final Supplier<BlockEntityType<BookBinderBlockEntity>> BOOK_BINDER = BLOCK_ENTITY_TYPES.register(
            "blockbookbinder",
            () -> BlockEntityType.Builder.of(BookBinderBlockEntity::new, MystBlocks.BOOK_BINDER.get()).build(null));

    public static final Supplier<BlockEntityType<BookReceptacleBlockEntity>> BOOK_RECEPTACLE = BLOCK_ENTITY_TYPES.register(
            "blockbookreceptacle",
            () -> BlockEntityType.Builder.of(BookReceptacleBlockEntity::new, MystBlocks.BOOK_RECEPTACLE.get()).build(null));

    public static final Supplier<BlockEntityType<BookDisplayBlockEntity>> BOOK_DISPLAY = BLOCK_ENTITY_TYPES.register(
            "book_display",
            () -> BlockEntityType.Builder.of(BookDisplayBlockEntity::new,
                    MystBlocks.BOOK_STAND.get(), MystBlocks.LECTERN.get()).build(null));

    public static final Supplier<BlockEntityType<LinkModifierBlockEntity>> LINK_MODIFIER = BLOCK_ENTITY_TYPES.register(
            "blocklinkmodifier",
            () -> BlockEntityType.Builder.of(LinkModifierBlockEntity::new, MystBlocks.LINK_MODIFIER.get()).build(null));

    public static final Supplier<BlockEntityType<WritingDeskBlockEntity>> WRITING_DESK = BLOCK_ENTITY_TYPES.register(
            "writingdesk",
            () -> BlockEntityType.Builder.of(WritingDeskBlockEntity::new, MystBlocks.WRITING_DESK.get()).build(null));

    public static final Supplier<BlockEntityType<InkMixerBlockEntity>> INK_MIXER = BLOCK_ENTITY_TYPES.register(
            "blockinkmixer",
            () -> BlockEntityType.Builder.of(InkMixerBlockEntity::new, MystBlocks.INK_MIXER.get()).build(null));

    public static final Supplier<BlockEntityType<StarFissureBlockEntity>> STAR_FISSURE = BLOCK_ENTITY_TYPES.register(
            "starfissure",
            () -> BlockEntityType.Builder.of(StarFissureBlockEntity::new, MystBlocks.STAR_FISSURE.get()).build(null));

    private MystBlockEntities() {}

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }
}
