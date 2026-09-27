package com.xcompwiz.mystcraft.item;

import com.xcompwiz.mystcraft.inventory.LinkBookMenu;
import com.xcompwiz.mystcraft.api.event.PortalLinkEvent;
import com.xcompwiz.mystcraft.api.impl.LinkInfoAdapter;
import com.xcompwiz.mystcraft.api.item.IItemPortalActivator;
import com.xcompwiz.mystcraft.api.linking.ILinkInfo;
import com.xcompwiz.mystcraft.linking.LinkController;
import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import com.xcompwiz.mystcraft.linking.LinkProperties;
import com.xcompwiz.mystcraft.registry.MystSounds;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import com.xcompwiz.mystcraft.linking.LinkOptions;
import com.xcompwiz.mystcraft.util.LegacyItemData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import com.xcompwiz.mystcraft.entity.EntityLinkbook;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Shared base for Mystcraft's linked books.
 *
 * <p>Stage 1B restores the old interaction boundary: right-click opens a book
 * screen and the actual link is requested from that screen on the server.</p>
 */
public abstract class ItemLinking extends Item implements IItemPortalActivator {
    protected ItemLinking(Properties properties) {
        super(properties);
    }

    protected abstract void initialize(Level level, ItemStack stack, Entity entity);

    protected void validate(Level level, ItemStack stack, Entity entity) {
        if (!LegacyItemData.hasData(stack)) {
            initialize(level, stack, entity);
        }
        if (level instanceof ServerLevel serverLevel) {
            // CP279: upgrade pre-fix DimensionKey-only Linking Books in place.
            // This preserves their recorded spawn/yaw and only fills durable Age
            // identity fields needed after lazy unload.
            AgeManager.repairKnownAgeIdentity(serverLevel.getServer(), stack);
        }
    }

    /** Public compatibility hook for machines such as the legacy Link Modifier. */
    public final void validateLinkData(Level level, ItemStack stack, Entity entity) {
        validate(level, stack, entity);
    }

    /** Legacy API portal-activator path used by Crystal Portals and third-party portal frames. */
    @Override
    public void onPortalCollision(ItemStack stack, Level level, Entity entity, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || entity == null || stack.isEmpty()) return;
        ItemStack transientBook = stack.copyWithCount(1);
        // CP279: stored legacy Linking Books may predate durable AgeUID/Dimension
        // metadata.  Repair the transient portal payload so an already-installed
        // Receptacle works immediately after update, even before reinsertion.
        AgeManager.repairKnownAgeIdentity(serverLevel.getServer(), transientBook);
        ILinkInfo info = LinkInfoAdapter.forStack(transientBook);
        info.setFlag(LinkProperties.MAINTAIN_MOMENTUM, true);
        info.setFlag(LinkProperties.GENERATE_PLATFORM, false);
        info.setFlag(LinkProperties.EXTERNAL, true);
        info.setProperty(LinkProperties.SOUND, "mystcraft:linking.link-portal");
        NeoForge.EVENT_BUS.post(new PortalLinkEvent(serverLevel, entity, info));
        LinkController.travelPortalGroup(serverLevel, entity, info, MystSounds.LINK_PORTAL.get());
    }

    /** 0.13.7.06 portal colour: deterministic RGB derived from the link display name. */
    @Override
    public int getPortalColor(ItemStack stack, Level level) {
        String name = LinkOptions.getDisplayName(stack);
        java.util.Random random = new java.util.Random(name == null ? 0 : name.hashCode());
        return random.nextInt(256) | (random.nextInt(256) << 8) | (random.nextInt(256) << 16);
    }

    /** Legacy linked books replace ordinary dropped ItemEntity instances with EntityLinkbook. */
    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return !stack.isEmpty();
    }

    @Override
    public Entity createEntity(Level level, Entity location, ItemStack stack) {
        if (!(location instanceof ItemEntity)) return null;
        return new EntityLinkbook(level, location, stack);
    }

    /** Open the normal two-page GUI for a physical world-book entity. */
    public static void openEntityBookMenu(ServerPlayer player, int entityId, ItemStack stack) {
        if (player == null || stack.isEmpty() || !(stack.getItem() instanceof ItemLinking linking)) return;
        linking.validate(player.level(), stack, player);
        openBookMenu(player, InteractionHand.MAIN_HAND, null, entityId, stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int inventorySlot, boolean isCurrentItem) {
        if (!level.isClientSide) {
            validate(level, stack, entity);
        }
        super.inventoryTick(stack, level, entity, inventorySlot, isCurrentItem);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!com.xcompwiz.mystcraft.util.MystMenuOpenPolicy.canOpen(player)) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            validate(level, stack, player);
            openBookMenu(serverPlayer, hand, null, -1, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Open the normal two-page Mystcraft GUI for a Book Stand/Lectern book. */
    public static void openStoredBookMenu(ServerPlayer player, BlockPos displayPos, ItemStack stack) {
        if (player == null || displayPos == null || stack.isEmpty() || !(stack.getItem() instanceof ItemLinking linking)) return;
        linking.validate(player.level(), stack, player);
        openBookMenu(player, InteractionHand.MAIN_HAND, displayPos, -1, stack);
    }

    private static void openBookMenu(ServerPlayer serverPlayer, InteractionHand hand, BlockPos displayPos, int entityId, ItemStack stack) {
        String destinationName = LinkOptions.getDisplayName(stack);
        String dimensionKey = LinkOptions.getDimensionKey(stack);
        String syncedDimension = dimensionKey == null ? "" : dimensionKey;
        List<String> pageIds = new ArrayList<>();
        List<String> authors = new ArrayList<>();
        if (stack.getItem() instanceof ItemAgebook agebook) {
            for (ItemStack page : agebook.getPageList(stack, serverPlayer.registryAccess())) {
                if (com.xcompwiz.mystcraft.page.Page.isLinkPanel(page)) pageIds.add("$LINK_PANEL$");
                else if (com.xcompwiz.mystcraft.page.Page.isBlank(page)) pageIds.add("$BLANK$");
                else {
                    String symbol = com.xcompwiz.mystcraft.page.Page.getSymbolId(page);
                    pageIds.add(symbol == null ? "$PAGE$" : symbol);
                }
            }
            authors.addAll(agebook.getAuthors(stack));
        } else {
            pageIds.add("$LINK_PANEL$");
        }

        final BlockPos storedPos = displayPos == null ? null : displayPos.immutable();
        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (containerId, inventory, openingPlayer) -> entityId >= 0
                                ? new LinkBookMenu(containerId, inventory, entityId, destinationName, syncedDimension, stack.getItem() instanceof ItemAgebook, pageIds, authors)
                                : storedPos == null
                                ? new LinkBookMenu(containerId, inventory, hand, destinationName, syncedDimension, stack.getItem() instanceof ItemAgebook, pageIds, authors)
                                : new LinkBookMenu(containerId, inventory, storedPos, destinationName, syncedDimension, stack.getItem() instanceof ItemAgebook, pageIds, authors),
                        Component.translatable("container.mystcraft.book")),
                buffer -> {
                    int sourceType = entityId >= 0 ? 2 : (storedPos != null ? 1 : 0);
                    buffer.writeVarInt(sourceType);
                    buffer.writeVarInt(hand == InteractionHand.OFF_HAND ? 1 : 0);
                    if (storedPos != null) buffer.writeBlockPos(storedPos);
                    if (entityId >= 0) buffer.writeVarInt(entityId);
                    buffer.writeUtf(destinationName, 64);
                    buffer.writeUtf(syncedDimension, 256);
                    buffer.writeBoolean(stack.getItem() instanceof ItemAgebook);
                    int syncedPageCount = Math.min(pageIds.size(), 512);
                    buffer.writeVarInt(syncedPageCount);
                    for (int i = 0; i < syncedPageCount; i++) buffer.writeUtf(pageIds.get(i), 256);
                    int syncedAuthorCount = Math.min(authors.size(), 32);
                    buffer.writeVarInt(syncedAuthorCount);
                    for (int i = 0; i < syncedAuthorCount; i++) buffer.writeUtf(authors.get(i), 64);
                });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (LegacyItemData.hasData(stack)) {
            String name = LinkOptions.getDisplayName(stack);
            if (!name.isEmpty()) {
                tooltipComponents.add(Component.literal(name).withStyle(ChatFormatting.GRAY));
            }
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }
}
