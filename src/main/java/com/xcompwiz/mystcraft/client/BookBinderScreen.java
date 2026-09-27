package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.blockentity.BookBinderBlockEntity;
import com.xcompwiz.mystcraft.inventory.BookBinderMenu;
import com.xcompwiz.mystcraft.network.BinderPageActionPayload;
import com.xcompwiz.mystcraft.network.BinderTitlePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** 0.13.7.06-style Book Binder UI with a variable-length horizontal page slider. */
public final class BookBinderScreen extends AbstractContainerScreen<BookBinderMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "gui/pagebinder.png");
    private static final int SLIDER_X = 7;
    private static final int SLIDER_Y = 45;
    private static final int SLIDER_W = 162;
    private static final int SLIDER_H = 40;
    private static final int ARROW_W = 8;
    private static final int PAGE_W = 25;
    private static final int PAGE_H = 34;
    private static final int PAGE_STEP = PAGE_W + 2;

    private EditBox titleBox;
    private int firstElement;
    private int hoveredPage = -1;

    public BookBinderScreen(BookBinderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 181;
        titleLabelX = -1000;
        inventoryLabelX = -1000;
        inventoryLabelY = -1000;
    }

    @Override
    protected void init() {
        super.init();
        titleBox = new EditBox(font, leftPos + 7, topPos + 9, 116, 14, Component.translatable("gui.mystcraft.book_title"));
        titleBox.setMaxLength(BookBinderBlockEntity.MAX_TITLE_LENGTH);
        titleBox.setValue(menu.getPendingTitle());
        titleBox.setBordered(true);
        titleBox.setResponder(text -> {
            menu.setClientPendingTitle(text);
            PacketDistributor.sendToServer(new BinderTitlePayload(menu.containerId, text));
        });
        addRenderableWidget(titleBox);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        renderPageSlider(graphics, mouseX, mouseY);

        // Legacy binder marks an empty title as an error and overlays a warning icon
        // whenever the first page is not a Link Panel.
        if (titleBox != null && titleBox.getValue().isEmpty()) {
            graphics.renderOutline(leftPos + 6, topPos + 8, 118, 16, 0xFFFF0000);
        }
        if (missingLinkPanel()) {
            float pulse = 0.3F + 0.7F * (0.5F + 0.5F * (float)Math.sin(System.currentTimeMillis() / 636.62));
            graphics.setColor(1F, 0.5F, 0.5F, pulse);
            graphics.blit(TEXTURE, leftPos + 27, topPos + 26, 18, 18, 176F, 0F, 30, 40, 256, 256);
            graphics.setColor(1F, 1F, 1F, 1F);
        }
    }

    private void renderPageSlider(GuiGraphics graphics, int mouseX, int mouseY) {
        int x0 = leftPos + SLIDER_X;
        int y0 = topPos + SLIDER_Y;
        graphics.fill(x0, y0, x0 + SLIDER_W, y0 + SLIDER_H, 0xAA000000);

        List<String> pages = menu.getClientPageDescriptors();
        int maxFirst = Math.max(0, pages.size() - 1);
        if (firstElement > maxFirst) firstElement = maxFirst;
        hoveredPage = -1;

        int localMouseX = mouseX - x0;
        int localMouseY = mouseY - y0;
        int x = 2;
        for (int i = firstElement; i < pages.size(); i++) {
            if (x + PAGE_W > SLIDER_W - ARROW_W) break;
            int px = x0 + x;
            int py = y0 + 3;
            drawPageCard(graphics, pages.get(i), px, py, i == 0);
            if (localMouseX >= x && localMouseX < x + PAGE_W && localMouseY >= 3 && localMouseY < 3 + PAGE_H) {
                hoveredPage = i;
                drawBorder(graphics, px - 1, py - 1, PAGE_W + 2, PAGE_H + 2, 0xFFFFFFFF);
            }
            x += PAGE_STEP;
        }

        int leftColor = firstElement == 0 ? 0x33000000 : 0xAA000000;
        int rightColor = pages.isEmpty() || firstElement >= pages.size() - 1 ? 0x33000000 : 0xAA000000;
        graphics.fill(x0, y0, x0 + ARROW_W, y0 + SLIDER_H, leftColor);
        graphics.fill(x0 + SLIDER_W - ARROW_W, y0, x0 + SLIDER_W, y0 + SLIDER_H, rightColor);
    }


    private boolean missingLinkPanel() {
        List<String> pages = menu.getClientPageDescriptors();
        return pages.isEmpty() || !"$LINK_PANEL$".equals(pages.getFirst());
    }

    private void drawPageCard(GuiGraphics graphics, String descriptor, int x, int y, boolean first) {
        LegacySymbolRenderer.drawPage(graphics, descriptor, x, y, PAGE_W, PAGE_H, false);
        if (first && !"$LINK_PANEL$".equals(descriptor)) {
            drawBorder(graphics, x - 1, y - 1, PAGE_W + 2, PAGE_H + 2, 0xFFFF4040);
        }
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("container.inventory"), 8, imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = mouseX - leftPos;
        double y = mouseY - topPos;
        if (x >= SLIDER_X && x < SLIDER_X + SLIDER_W && y >= SLIDER_Y && y < SLIDER_Y + SLIDER_H) {
            if (x < SLIDER_X + ARROW_W) {
                cycleLeft();
                return true;
            }
            if (x >= SLIDER_X + SLIDER_W - ARROW_W) {
                cycleRight();
                return true;
            }

            if (!menu.getCarried().isEmpty()) {
                int index = hoveredPage >= 0 ? hoveredPage : menu.getClientPageDescriptors().size();
                PacketDistributor.sendToServer(new BinderPageActionPayload(menu.containerId,
                        BinderPageActionPayload.INSERT, index, button == 1));
                return true;
            }
            if (hoveredPage >= 0) {
                PacketDistributor.sendToServer(new BinderPageActionPayload(menu.containerId,
                        BinderPageActionPayload.REMOVE, hoveredPage, false));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double x = mouseX - leftPos;
        double y = mouseY - topPos;
        if (x >= SLIDER_X && x < SLIDER_X + SLIDER_W && y >= SLIDER_Y && y < SLIDER_Y + SLIDER_H) {
            if (scrollY > 0) cycleLeft();
            else if (scrollY < 0) cycleRight();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void cycleLeft() { firstElement = Math.max(0, firstElement - 1); }
    private void cycleRight() {
        int size = menu.getClientPageDescriptors().size();
        if (size > 0) firstElement = Math.min(size - 1, firstElement + 1);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (missingLinkPanel() && mouseX >= leftPos + 27 && mouseX < leftPos + 45
                && mouseY >= topPos + 26 && mouseY < topPos + 44) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.mystcraft.book_binder.missing_panel"),
                    Component.translatable("gui.mystcraft.book_binder.missing_panel_hint")), mouseX, mouseY);
        } else if (hoveredPage >= 0 && hoveredPage < menu.getClientPageDescriptors().size()) {
            String descriptor = menu.getClientPageDescriptors().get(hoveredPage);
            Component tooltip;
            if ("$LINK_PANEL$".equals(descriptor)) tooltip = Component.translatable("item.mystcraft.page.link_panel");
            else if ("$BLANK$".equals(descriptor)) tooltip = Component.translatable("item.mystcraft.page.blank");
            else tooltip = LegacySymbolName.component(descriptor);
            tooltip = Component.literal((hoveredPage + 1) + ". ").append(tooltip);
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }
}
