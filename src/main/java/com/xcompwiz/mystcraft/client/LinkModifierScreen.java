package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.inventory.LinkModifierMenu;
import com.xcompwiz.mystcraft.linking.LinkProperties;
import com.xcompwiz.mystcraft.network.LinkModifierTextPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** Legacy 176x166 Link Modifier editor: flags, Age seed, title and dead-Age control. */
public final class LinkModifierScreen extends AbstractContainerScreen<LinkModifierMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "gui/single_slot.png");
    private EditBox seedBox;
    private EditBox titleBox;
    private boolean syncing;
    private boolean deadArmed;
    private String lastBookSignature = "";

    public LinkModifierScreen(LinkModifierMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelX = 8;
        inventoryLabelY = 72;
        titleLabelY = -1000; // legacy GUI had no conventional container title label
    }

    @Override protected void init() {
        super.init();
        seedBox = new EditBox(font, leftPos + 80, topPos + 15, 87, 14, Component.translatable("gui.mystcraft.link_modifier.seed"));
        seedBox.setMaxLength(21);
        seedBox.setBordered(true);
        seedBox.setResponder(value -> {
            if (!syncing) PacketDistributor.sendToServer(new LinkModifierTextPayload(menu.containerId, LinkModifierTextPayload.FIELD_SEED, value));
        });
        addRenderableWidget(seedBox);

        titleBox = new EditBox(font, leftPos + 80, topPos + 56, 87, 14, Component.translatable("gui.mystcraft.book_title"));
        titleBox.setMaxLength(21);
        titleBox.setBordered(true);
        titleBox.setResponder(value -> {
            if (!syncing) PacketDistributor.sendToServer(new LinkModifierTextPayload(menu.containerId, LinkModifierTextPayload.FIELD_TITLE, value));
        });
        addRenderableWidget(titleBox);
        syncFields(true);
    }

    @Override protected void containerTick() {
        super.containerTick();
        syncFields(false);
        boolean seedVisible = menu.hasSeed();
        seedBox.visible = seedVisible;
        seedBox.setEditable(seedVisible);
        if (!seedVisible) deadArmed = false;
    }

    private void syncFields(boolean force) {
        if (seedBox == null || titleBox == null) return;
        String signature = menu.getSlot(0).getItem().isEmpty() ? "" : menu.getSlot(0).getItem().getHoverName().getString() + menu.getDimensionText();
        if (!force && signature.equals(lastBookSignature)) return;
        lastBookSignature = signature;
        syncing = true;
        seedBox.setValue(menu.getSeedText());
        titleBox.setValue(menu.getBookTitle());
        syncing = false;
        deadArmed = false;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        for (int i=0; i<menu.properties().size(); i++) {
            int col = i / 3;
            int row = i % 3;
            int x = leftPos + 5 + col*20;
            int y = topPos + 10 + row*20;
            drawLegacyToggleSquare(graphics, x, y, menu.propertyEnabled(i), false);
        }

        if (menu.hasSeed()) {
            int x = leftPos + (deadArmed || menu.isDead() ? 140 : 120);
            int y = topPos + 32;
            drawLegacyToggleSquare(graphics, x, y, menu.isDead(), deadArmed || menu.isDead());
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("container.inventory"), inventoryLabelX, inventoryLabelY, 0x404040, false);
        String dim = menu.getDimensionText();
        if (!dim.isBlank()) graphics.drawString(font, dim, 100, 40, 0xFFFFFF, true);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i=0; i<menu.properties().size(); i++) {
            int col=i/3, row=i%3;
            int x=leftPos+5+col*20, y=topPos+10+row*20;
            if (mouseX>=x && mouseX<x+18 && mouseY>=y && mouseY<y+18) {
                clickMenuButton(LinkModifierMenu.ACTION_TOGGLE_BASE+i);
                return true;
            }
        }
        if (menu.hasSeed() && !menu.isDead()) {
            int x=leftPos+(deadArmed?140:120), y=topPos+32;
            if (mouseX>=x && mouseX<x+18 && mouseY>=y && mouseY<y+18) {
                if (deadArmed) clickMenuButton(LinkModifierMenu.ACTION_MARK_DEAD);
                else deadArmed=true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickMenuButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        for (int i=0; i<menu.properties().size(); i++) {
            int col=i/3,row=i%3,x=leftPos+5+col*20,y=topPos+10+row*20;
            if (mouseX>=x && mouseX<x+18 && mouseY>=y && mouseY<y+18) {
                graphics.renderTooltip(font, propertyName(menu.properties().get(i)), mouseX, mouseY);
                return;
            }
        }
        if (menu.hasSeed()) {
            int x=leftPos+(deadArmed||menu.isDead()?140:120),y=topPos+32;
            if (mouseX>=x && mouseX<x+18 && mouseY>=y && mouseY<y+18) {
                graphics.renderTooltip(font, Component.translatable(menu.isDead()
                        ? "gui.mystcraft.link_modifier.dead_already"
                        : deadArmed ? "gui.mystcraft.link_modifier.dead_confirm" : "gui.mystcraft.link_modifier.dead_arm"), mouseX, mouseY);
            }
        }
    }

    /** Matches the plain square GuiElementButtonToggle used by 0.13.7.06. */
    private static void drawLegacyToggleSquare(GuiGraphics graphics, int x, int y, boolean depressed, boolean redTint) {
        int inner = redTint ? 0xFF8B4040 : 0xFF8B8B8B;
        int light = redTint ? 0xFFFF8080 : 0xFFFFFFFF;
        int dark = 0xFF373737;
        int topLeft = depressed ? dark : light;
        int bottomRight = depressed ? light : dark;
        graphics.fill(x, y, x + 18, y + 18, inner);
        graphics.fill(x, y, x + 18, y + 1, topLeft);
        graphics.fill(x, y, x + 1, y + 18, topLeft);
        graphics.fill(x, y + 17, x + 18, y + 18, bottomRight);
        graphics.fill(x + 17, y, x + 18, y + 18, bottomRight);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, inner);
    }

    private static Component propertyName(String property) {
        return switch (property) {
            case LinkProperties.INTRA_LINKING -> Component.translatable("linkeffect.intralinking.name");
            case LinkProperties.INTRA_LINKING_ONLY -> Component.translatable("linkeffect.intralinkingonly.name");
            case LinkProperties.GENERATE_PLATFORM -> Component.translatable("linkeffect.generateplatform.name");
            case LinkProperties.MAINTAIN_MOMENTUM -> Component.translatable("linkeffect.maintainmomentum.name");
            case LinkProperties.DISARM -> Component.translatable("linkeffect.disarm.name");
            case LinkProperties.RELATIVE -> Component.translatable("linkeffect.relative.name");
            default -> Component.literal(property);
        };
    }
}
