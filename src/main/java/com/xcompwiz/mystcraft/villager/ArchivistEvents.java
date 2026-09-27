package com.xcompwiz.mystcraft.villager;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.registry.MystItems;
import com.xcompwiz.mystcraft.registry.MystVillagers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/** Legacy Archivist interaction and fallback vanilla trade bridge. */
@EventBusSubscriber(modid = Mystcraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class ArchivistEvents {
    private ArchivistEvents() {}

    @SubscribeEvent
    public static void onVillagerInteraction(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getTarget() instanceof Villager villager)) return;
        if (villager.getVillagerData().getProfession() != MystVillagers.ARCHIVIST.get()) return;
        if (!com.xcompwiz.mystcraft.util.MystMenuOpenPolicy.canOpen(event.getEntity())) return;
        // Legacy sneaking deliberately bypassed the custom shop, allowing the
        // normal Merchant UI (which contained the 25-emerald Booster trade).
        if (event.getEntity().isShiftKeyDown()) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            player.openMenu(
                    new net.minecraft.world.MenuProvider() {
                        @Override public Component getDisplayName() { return Component.translatable("container.mystcraft.archivist"); }
                        @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.entity.player.Player ignored) {
                            return new com.xcompwiz.mystcraft.inventory.ArchivistShopMenu(id, inv, villager);
                        }
                    },
                    buf -> buf.writeVarInt(villager.getId()));
        }
    }

    @SubscribeEvent
    public static void addLegacyFallbackTrade(VillagerTradesEvent event) {
        if (event.getType() != MystVillagers.ARCHIVIST.get()) return;
        // Mystcraft 0.13.7.06 registered this normal trade in addition to its
        // custom non-sneak shop: 25 Emeralds -> one Booster Pack.
        event.getTrades().get(1).add(new BasicItemListing(
                new ItemStack(Items.EMERALD, 25),
                new ItemStack(MystItems.BOOSTER.get()),
                7, 1, 0.05F));
    }
}
