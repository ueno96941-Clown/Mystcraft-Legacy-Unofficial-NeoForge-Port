package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.inventory.ArchivistShopMenu;
import com.xcompwiz.mystcraft.item.ItemPage;
import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.registry.MystItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * CP245 modernized Archivist shop.
 *
 * <p>The 0.13.7.06 shop was not a vanilla villager-trade screen: it used three
 * custom shop cards with a symbol name, emerald price and Buy button, plus a
 * separate Booster offer.  The first port only copied the old 176x181 backing
 * texture and placed modern buttons/slots over it, which caused the overlap seen
 * in runtime testing.  This screen preserves the original shop semantics while
 * rebuilding the presentation for 1.21.1.</p>
 */
public final class ArchivistShopScreen extends AbstractContainerScreen<ArchivistShopMenu> {
    private static final int BG = 0xFFC6C6C6;
    private static final int BORDER_DARK = 0xFF373737;
    private static final int BORDER_LIGHT = 0xFFFFFFFF;
    private static final int CARD = 0xFF9E9E9E;
    private static final int CARD_INNER = 0xFFB8B8B8;
    private static final int TEXT = 0xFF303030;
    private static final int EMERALD_TEXT = 0xFF178A32;
    private static final int SLOT_DARK = 0xFF555555;
    private static final int SLOT_LIGHT = 0xFFE6E6E6;

    private static final int[] CARD_X = {9, 65, 121, 177};
    /** Preserve the original visual order: Booster on the left, then three Symbol Page offers. */
    private static final int[] OFFER_ORDER = {ArchivistShopMenu.BOOSTER_INDEX, 0, 1, 2};
    private static final int CARD_Y = 28;
    private static final int CARD_W = 50;
    private static final int CARD_H = 82;
    private static final int OFFER_SLOT_Y = 36;

    private final Button[] buyButtons = new Button[4];

    public ArchivistShopScreen(ArchivistShopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 236;
        imageHeight = 214;
        inventoryLabelX = 37;
        inventoryLabelY = 116;
        titleLabelY = -1000; // title is rendered explicitly in the modern header
    }

    @Override
    protected void init() {
        super.init();
        for (int column = 0; column < OFFER_ORDER.length; ++column) {
            final int index = OFFER_ORDER[column];
            int x = leftPos + CARD_X[column] + 3;
            int y = topPos + 86;
            buyButtons[index] = addRenderableWidget(Button.builder(
                    Component.empty(), b -> buy(index))
                    .bounds(x, y, 44, 18).build());
        }
        syncButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        syncButtons();
    }

    private void syncButtons() {
        for (int i = 0; i < buyButtons.length; ++i) {
            Button button = buyButtons[i];
            if (button == null) continue;
            ItemStack stock = menu.getDisplayItem(i);
            button.active = !stock.isEmpty() && menu.getEmeraldValue() >= menu.getPrice(i);
        }
    }

    private void buy(int index) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos;
        int y0 = topPos;

        // Clean 1.21-style container frame. The old texture remains in resources for provenance,
        // but is intentionally not stretched underneath modern widgets anymore.
        graphics.fill(x0, y0, x0 + imageWidth, y0 + imageHeight, BORDER_DARK);
        graphics.fill(x0 + 1, y0 + 1, x0 + imageWidth - 1, y0 + imageHeight - 1, BORDER_LIGHT);
        graphics.fill(x0 + 2, y0 + 2, x0 + imageWidth - 2, y0 + imageHeight - 2, BG);

        // Four offer cards: Booster + the three original ranked Symbol Page offers.
        for (int i = 0; i < CARD_X.length; ++i) {
            int cx = x0 + CARD_X[i];
            int cy = y0 + CARD_Y;
            graphics.fill(cx, cy, cx + CARD_W, cy + CARD_H, BORDER_DARK);
            graphics.fill(cx + 1, cy + 1, cx + CARD_W - 1, cy + CARD_H - 1, CARD_INNER);
            graphics.fill(cx + 3, cy + 3, cx + CARD_W - 3, cy + 31, CARD);
            drawSlotFrame(graphics, x0 + 25 + i * 56, y0 + OFFER_SLOT_Y);
        }

        // Player inventory / hotbar slot wells.
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                drawSlotFrame(graphics, x0 + 37 + col * 18, y0 + 128 + row * 18);
            }
        }
        for (int col = 0; col < 9; ++col) {
            drawSlotFrame(graphics, x0 + 37 + col * 18, y0 + 188);
        }
    }

    private static void drawSlotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_DARK);
        graphics.fill(x, y, x + 16, y + 16, SLOT_LIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Header. The shop's emerald-block-as-change behavior is unchanged; this is the
        // synchronized emerald-equivalent value used by the original custom shop.
        graphics.drawString(font, title, 9, 8, TEXT, false);
        Component emeralds = Component.translatable("gui.mystcraft.archivist.emeralds", menu.getEmeraldValue());
        graphics.drawString(font, emeralds, imageWidth - 9 - font.width(emeralds), 8, EMERALD_TEXT, false);

        for (int column = 0; column < OFFER_ORDER.length; ++column) {
            int index = OFFER_ORDER[column];
            int cx = CARD_X[column];
            ItemStack stack = menu.getDisplayItem(index);
            String name = getOfferName(index, stack);
            name = trim(name, CARD_W - 6);
            graphics.drawString(font, name, cx + (CARD_W - font.width(name)) / 2, 58, TEXT, false);

            // Match the original visual language: emerald icon immediately beside the price.
            graphics.renderItem(new ItemStack(Items.EMERALD), cx + 6, 68);
            graphics.drawString(font, Integer.toString(menu.getPrice(index)), cx + 24, 72, EMERALD_TEXT, false);
        }

        graphics.drawString(font, Component.translatable("container.inventory"), inventoryLabelX, inventoryLabelY, TEXT, false);
    }

    private String getOfferName(int index, ItemStack stack) {
        if (index == ArchivistShopMenu.BOOSTER_INDEX) {
            return MystItems.BOOSTER.get().getDefaultInstance().getHoverName().getString();
        }
        if (stack.isEmpty()) return Component.translatable("gui.mystcraft.archivist.sold_out").getString();
        if (stack.getItem() instanceof ItemPage) {
            String symbol = Page.getSymbolId(stack);
            if (symbol != null) return LegacySymbolName.component(symbol).getString();
        }
        return stack.getHoverName().getString();
    }

    private String trim(String text, int width) {
        if (font.width(text) <= width) return text;
        String clipped = font.plainSubstrByWidth(text, Math.max(1, width - font.width("…")));
        return clipped + "…";
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        Component buy = Component.translatable("gui.mystcraft.archivist.buy");
        for (Button button : buyButtons) {
            if (button == null || !button.visible) continue;
            int x = button.getX() + (button.getWidth() - font.width(buy)) / 2;
            int y = button.getY() + (button.getHeight() - 8) / 2;
            graphics.drawString(font, buy, x, y, button.active ? TEXT : 0x707070, false);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }
}
