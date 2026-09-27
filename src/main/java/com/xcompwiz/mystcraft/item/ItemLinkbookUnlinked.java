package com.xcompwiz.mystcraft.item;

import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.registry.MystItems;
import com.xcompwiz.mystcraft.util.LegacyItemData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;

public final class ItemLinkbookUnlinked extends Item {
    public ItemLinkbookUnlinked(Properties properties) {
        super(properties);
    }

    public static ItemStack createItem(ItemStack linkPanel) {
        ItemStack linkbook = new ItemStack(MystItems.UNLINKED_BOOK.get());
        LegacyItemData.set(linkbook, LegacyItemData.copy(linkPanel));
        return linkbook;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        Collection<String> properties = Page.getLinkProperties(stack);
        if (properties != null) {
            for (String property : properties) {
                tooltipComponents.add(Component.translatable("tooltip.mystcraft.link_property", property)
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack inHand = player.getItemInHand(hand);

        // Legacy behavior: stacked unlinked books cannot be linked in-place.
        if (level.isClientSide || inHand.getCount() > 1) {
            return InteractionResultHolder.pass(inHand);
        }

        ItemStack linked = new ItemStack(MystItems.LINKBOOK.get());
        ((ItemLinkbook) MystItems.LINKBOOK.get()).initializeAt(level, linked, player);
        Page.applyLinkPanel(inHand, linked);

        player.setItemInHand(hand, linked);
        inHand.setCount(0);

        // 0.13.7.06 returned PASS after replacing the held stack.
        return InteractionResultHolder.pass(linked);
    }
}
