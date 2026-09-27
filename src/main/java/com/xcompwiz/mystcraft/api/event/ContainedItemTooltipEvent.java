package com.xcompwiz.mystcraft.api.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.Event;
import java.util.List;

/** Compatibility event used when Mystcraft renders a tooltip for an item contained by another item. */
public class ContainedItemTooltipEvent extends Event {
    private final ItemStack itemStack;
    private final ItemStack container;
    private final Player entityPlayer;
    private final List<Component> toolTip;
    private final TooltipFlag flags;
    public ContainedItemTooltipEvent(ItemStack itemStack, ItemStack container, Player entityPlayer, List<Component> toolTip, TooltipFlag flags) {
        this.itemStack=itemStack; this.container=container; this.entityPlayer=entityPlayer; this.toolTip=toolTip; this.flags=flags;
    }
    public ItemStack getItemStack(){return itemStack;}
    public ItemStack getContainer(){return container;}
    public Player getEntityPlayer(){return entityPlayer;}
    public List<Component> getToolTip(){return toolTip;}
    public TooltipFlag getFlags(){return flags;}
}
