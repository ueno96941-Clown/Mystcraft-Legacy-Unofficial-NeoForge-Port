package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.blockentity.WritingDeskBlockEntity;
import com.xcompwiz.mystcraft.inventory.WritingDeskMenu;
import com.xcompwiz.mystcraft.linking.LinkOptions;
import com.xcompwiz.mystcraft.network.WritingDeskTitlePayload;
import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.symbol.LegacyBiomeSymbolRegistry;
import com.xcompwiz.mystcraft.symbol.LegacyMaterialSymbolRegistry;
import com.xcompwiz.mystcraft.symbol.SymbolRegistry;
import com.xcompwiz.mystcraft.symbol.SymbolAvailability;
import com.xcompwiz.mystcraft.symbol.LegacySymbolCatalogOrder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Legacy Writing Desk screen bridge with I89 target/book controls restored. */
public final class WritingDeskScreen extends AbstractContainerScreen<WritingDeskMenu> {
    private static final ResourceLocation DESK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Mystcraft.MOD_ID, "gui/writingdesk.png");
    private static final int LEFT_SURFACE_WIDTH = 228;
    private static final int RIGHT_PANEL_X = 233;
    private static final int MAIN_TOP = 20;
    private static final int RIGHT_PANEL_WIDTH = 176;
    private static final int RIGHT_PANEL_HEIGHT = 166;

    private static final int SURFACE_X = 58;
    private static final int SURFACE_Y = MAIN_TOP;
    private static final int SURFACE_W = LEFT_SURFACE_WIDTH - 53;
    private static final int SURFACE_H = RIGHT_PANEL_HEIGHT;
    private static final int CARD_W = 30;
    private static final int CARD_H = 40;
    private static final int CARD_GAP = 2;

    private EditBox searchBox;
    private EditBox titleBox;
    private Button azButton;
    private Button allButton;
    private Button targetPreviousButton;
    private Button targetNextButton;
    private Button targetTakeButton;
    private Button targetSetButton;
    private Button linkButton;
    private boolean alphabetical = true;
    private boolean showAll;
    private int surfaceScrollRow;
    private boolean syncingTitle;

    private record SurfaceEntry(ItemStack page, int sourceIndex, int count, boolean ghost) {}

    public WritingDeskScreen(WritingDeskMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = LEFT_SURFACE_WIDTH + RIGHT_PANEL_WIDTH + 5;
        this.imageHeight = 185;
        // Legacy GuiWritingDesk did not draw vanilla container title/inventory labels.
        // Leaving AbstractContainerScreen defaults here causes the exact overlaps
        // seen during I103 runtime testing.
        this.inventoryLabelX = -1000;
        this.inventoryLabelY = -1000;
        this.titleLabelX = -1000;
        this.titleLabelY = -1000;
    }

    @Override
    protected void init() {
        super.init();

        // Dedicated clients do not share the server JVM's dynamic registries. Refresh
        // client-side ghost/Page discovery from the connection's registry view so ALL
        // exposes modded biomes and mod fluids just like the original post-mod-load scan.
        LegacyMaterialSymbolRegistry.bootstrapRuntimeFluids();
        if (minecraft != null && minecraft.level != null) {
            LegacyBiomeSymbolRegistry.bootstrapRuntime(minecraft.level.registryAccess());
        }

        int x = leftPos;
        int y = topPos;

        azButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            alphabetical = !alphabetical;
            surfaceScrollRow = 0;
        }).bounds(x + SURFACE_X, y + 1, 18, 18).build());
        allButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            showAll = !showAll;
            surfaceScrollRow = 0;
        }).bounds(x + SURFACE_X + 18, y + 1, 18, 18).build());

        searchBox = new EditBox(font, x + SURFACE_X + 40, y + 2, SURFACE_W - 40, 16,
                Component.translatable("gui.mystcraft.writing_desk.search"));
        searchBox.setMaxLength(64);
        searchBox.setBordered(true);
        addRenderableWidget(searchBox);

        targetPreviousButton = addRenderableWidget(Button.builder(Component.literal("<"),
                b -> clickMenuButton(WritingDeskMenu.ACTION_TARGET_PREVIOUS))
                .bounds(x + RIGHT_PANEL_X + 28, y + MAIN_TOP + 7, 18, 18).build());
        targetNextButton = addRenderableWidget(Button.builder(Component.literal(">"),
                b -> clickMenuButton(WritingDeskMenu.ACTION_TARGET_NEXT))
                .bounds(x + RIGHT_PANEL_X + 112, y + MAIN_TOP + 7, 18, 18).build());
        targetTakeButton = addRenderableWidget(Button.builder(Component.translatable("gui.mystcraft.take"),
                b -> clickMenuButton(WritingDeskMenu.ACTION_TARGET_TAKE_PAGE))
                .bounds(x + RIGHT_PANEL_X + 28, y + MAIN_TOP + 35, 36, 18).build());
        targetSetButton = addRenderableWidget(Button.builder(Component.translatable("gui.mystcraft.put"),
                b -> clickMenuButton(WritingDeskMenu.ACTION_TARGET_SET_PAGE))
                .bounds(x + RIGHT_PANEL_X + 66, y + MAIN_TOP + 35, 32, 18).build());
        linkButton = addRenderableWidget(Button.builder(Component.translatable("gui.mystcraft.link"),
                b -> clickMenuButton(WritingDeskMenu.ACTION_TARGET_LINK))
                .bounds(x + RIGHT_PANEL_X + 100, y + MAIN_TOP + 35, 31, 18).build());

        titleBox = new EditBox(font, x + RIGHT_PANEL_X + 28, y + MAIN_TOP + 61, 99, 14,
                Component.translatable("gui.mystcraft.book_title"));
        titleBox.setMaxLength(WritingDeskBlockEntity.MAX_TITLE_LENGTH);
        titleBox.setBordered(true);
        syncingTitle = true;
        titleBox.setValue(menu.getTargetTitle());
        syncingTitle = false;
        titleBox.setResponder(text -> {
            if (!syncingTitle) PacketDistributor.sendToServer(new WritingDeskTitlePayload(menu.containerId, text));
        });
        addRenderableWidget(titleBox);
        syncTargetWidgets();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        syncTargetWidgets();
    }

    private void syncTargetWidgets() {
        if (titleBox == null) return;
        boolean renameable = menu.isTargetRenameable();
        titleBox.setEditable(renameable);
        if (!titleBox.isFocused()) {
            String title = menu.getTargetTitle();
            if (!titleBox.getValue().equals(title)) {
                syncingTitle = true;
                titleBox.setValue(title);
                syncingTitle = false;
            }
        }

        int count = menu.getTargetPageCount();
        int index = menu.getCurrentTargetPageIndex();
        targetPreviousButton.active = index > 0;
        targetNextButton.active = index < count;
        targetTakeButton.visible = menu.targetOrderedEditable();
        targetSetButton.visible = menu.targetOrderedEditable();
        targetTakeButton.active = menu.targetOrderedEditable() && index < count && menu.getCarried().isEmpty();
        targetSetButton.active = menu.targetOrderedEditable() && !menu.getCarried().isEmpty();
        linkButton.visible = menu.isLinkTarget();
        linkButton.active = linkButton.visible && menu.isLinkPermitted();
    }

    private void clickMenuButton(int id) {
        if (minecraft == null || minecraft.gameMode == null || minecraft.player == null) return;
        // Navigation-only actions are mirrored client-side for immediate feedback.
        if (id == WritingDeskMenu.ACTION_TAB_UP || id == WritingDeskMenu.ACTION_TAB_DOWN
                || id == WritingDeskMenu.ACTION_TARGET_PREVIOUS || id == WritingDeskMenu.ACTION_TARGET_NEXT
                || (id >= WritingDeskMenu.ACTION_SELECT_TAB
                    && id < WritingDeskMenu.ACTION_SELECT_TAB + WritingDeskMenu.VISIBLE_TAB_SLOTS)) {
            menu.clickMenuButton(minecraft.player, id);
        }
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos + SURFACE_X, topPos + SURFACE_Y, leftPos + LEFT_SURFACE_WIDTH,
                topPos + SURFACE_Y + SURFACE_H, 0xFF3B3026);
        graphics.fill(leftPos + SURFACE_X + 2, topPos + SURFACE_Y + 2, leftPos + LEFT_SURFACE_WIDTH - 2,
                topPos + SURFACE_Y + SURFACE_H - 2, 0xFF5C4936);

        graphics.blit(DESK_TEXTURE, leftPos + RIGHT_PANEL_X, topPos + MAIN_TOP,
                0, 0, RIGHT_PANEL_WIDTH, RIGHT_PANEL_HEIGHT, 256, 256);

        renderTabRail(graphics);
        renderInkGauge(graphics);
        renderTargetPreview(graphics);
        renderSurface(graphics, mouseX, mouseY);
    }

    private void renderTabRail(GuiGraphics graphics) {
        int top = topPos + MAIN_TOP;
        int first = menu.getFirstTab();
        int active = menu.getActiveTab();

        // Exact 0.13.7.06 writingdesk.png slices used by GuiElementSurfaceTabs.
        graphics.setColor(first == 0 ? 0.4F : (active < first ? 0.5F : 1.0F),
                first == 0 ? 0.4F : (active < first ? 0.5F : 1.0F),
                first == 0 ? 0.4F : 1.0F, 1.0F);
        graphics.blit(DESK_TEXTURE, leftPos, top, 0, 203, 58, 9, 256, 256);
        graphics.setColor(1F, 1F, 1F, 1F);

        int tabY = top + 9;
        for (int view = 0; view < WritingDeskMenu.VISIBLE_TAB_SLOTS; view++) {
            int tab = first + view;
            if (tab == active) graphics.setColor(0.5F, 0.5F, 1.0F, 1.0F);
            graphics.blit(DESK_TEXTURE, leftPos, tabY, 0, 166, 58, 37, 256, 256);
            graphics.setColor(1F, 1F, 1F, 1F);

            ItemStack stack = menu.getTabStack(tab);
            graphics.drawString(font, Integer.toString(tab), leftPos + 8, tabY + 5, 0x404040, false);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, leftPos + 37, tabY + 3);
                String name = stack.getHoverName().getString();
                int maxWidth = 50;
                if (font.width(name) > maxWidth) name = font.plainSubstrByWidth(name, maxWidth);
                graphics.drawString(font, name, leftPos + 4, tabY + 25, 0x404040, false);
            }
            tabY += 37;
        }

        boolean bottomDisabled = first + WritingDeskMenu.VISIBLE_TAB_SLOTS >= WritingDeskBlockEntity.TAB_SLOT_COUNT;
        boolean activeBelow = active >= first + WritingDeskMenu.VISIBLE_TAB_SLOTS;
        graphics.setColor(bottomDisabled ? 0.4F : (activeBelow ? 0.5F : 1.0F),
                bottomDisabled ? 0.4F : (activeBelow ? 0.5F : 1.0F),
                bottomDisabled ? 0.4F : 1.0F, 1.0F);
        graphics.blit(DESK_TEXTURE, leftPos, tabY, 0, 212, 58, 9, 256, 256);
        graphics.setColor(1F, 1F, 1F, 1F);
    }

    private void renderInkGauge(GuiGraphics graphics) {
        int amount = menu.getInkAmount();
        int height = Math.round((amount / (float) WritingDeskBlockEntity.CAPACITY) * 70.0F);
        int x = leftPos + RIGHT_PANEL_X + RIGHT_PANEL_WIDTH - 44;
        int y = topPos + MAIN_TOP + 7;
        graphics.fill(x - 1, y - 1, x + 17, y + 71, 0xFF44392E);
        graphics.fill(x, y, x + 16, y + 70, 0xFFDDD3C0);
        if (height > 0) graphics.fill(x + 1, y + 69 - height, x + 15, y + 69, 0xFF191919);
    }

    private void renderTargetPreview(GuiGraphics graphics) {
        int x = leftPos + RIGHT_PANEL_X + 48;
        int y = topPos + MAIN_TOP + 7;
        int w = 64;
        int h = 46;
        graphics.fill(x, y, x + w, y + h, 0xFF6C573D);
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFFEBDDBD);

        ItemStack page = menu.getCurrentTargetPage();
        int count = menu.getTargetPageCount();
        int index = menu.getCurrentTargetPageIndex();
        if (!page.isEmpty()) {
            String descriptor = Page.isLinkPanel(page) ? "$LINK_PANEL$" : Page.getSymbolId(page);
            if (descriptor != null) {
                LegacySymbolRenderer.drawPage(graphics, descriptor, x + 20, y + 3, 25, 34, false);
            } else {
                graphics.renderItem(page, x + 24, y + 5);
            }
        } else if (!menu.getTarget().isEmpty()) {
            graphics.renderItem(menu.getTarget(), x + 24, y + 5);
        }
        graphics.drawCenteredString(font, Component.literal((Math.min(index, count) + 1) + " / " + Math.max(1, count)),
                x + w / 2, y + 36, 0x6B5841);

        if (menu.isLinkTarget()) {
            String dimension = LinkOptions.getDimensionKey(menu.getTarget());
            if (dimension != null && !dimension.isBlank()) {
                String shortDim = dimension.length() > 20 ? dimension.substring(0, 20) : dimension;
                graphics.drawString(font, shortDim, leftPos + RIGHT_PANEL_X + 28,
                        topPos + MAIN_TOP + 78, 0x5B4933, false);
            }
        }
    }

    private void renderSurface(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean collection = menu.surfaceIsCollection();
        azButton.active = collection;
        allButton.active = collection;
        searchBox.setEditable(collection);

        List<SurfaceEntry> entries = surfaceEntries();
        int areaX = leftPos + SURFACE_X + 4;
        int areaY = topPos + SURFACE_Y + 24;
        int usableW = SURFACE_W - 8;
        int cols = Math.max(1, usableW / (CARD_W + CARD_GAP));

        int index = 0;
        int firstVisible = surfaceScrollRow * cols;
        for (SurfaceEntry entry : entries) {
            if (index++ < firstVisible) continue;
            int visible = index - 1 - firstVisible;
            int col = visible % cols;
            int row = visible / cols;
            int sx = areaX + col * (CARD_W + CARD_GAP);
            int sy = areaY + row * (CARD_H + CARD_GAP);
            if (sy + CARD_H > topPos + SURFACE_Y + SURFACE_H - 2) break;

            boolean hover = mouseX >= sx && mouseX < sx + CARD_W && mouseY >= sy && mouseY < sy + CARD_H;
            int border = hover ? 0xFFE0C38E : 0xFFB69A69;
            graphics.fill(sx, sy, sx + CARD_W, sy + CARD_H, border);
            graphics.fill(sx + 2, sy + 2, sx + CARD_W - 2, sy + CARD_H - 2,
                    entry.ghost ? 0xFF8A7F6A : 0xFFEADDBE);
            if (!entry.page.isEmpty()) {
                String descriptor = Page.isLinkPanel(entry.page) ? "$LINK_PANEL$" : Page.getSymbolId(entry.page);
                if (descriptor != null) {
                    LegacySymbolRenderer.drawPage(graphics, descriptor, sx + 2, sy + 2, CARD_W - 4, CARD_H - 4, entry.ghost);
                    if (entry.count > 1) {
                        graphics.drawString(font, Integer.toString(entry.count), sx + CARD_W - 9, sy + CARD_H - 10, 0xFFFFFFFF, true);
                    }
                } else {
                    graphics.renderItem(entry.page, sx + 7, sy + 5);
                    String label = entry.count > 1 ? Integer.toString(entry.count) : "";
                    graphics.renderItemDecorations(font, entry.page, sx + 7, sy + 5, label);
                }
            }
        }
    }

    private List<SurfaceEntry> surfaceEntries() {
        List<ItemStack> pages = menu.getSurfacePages();
        if (!menu.surfaceIsCollection()) {
            List<SurfaceEntry> ordered = new ArrayList<>();
            for (int i = 0; i < pages.size(); i++) ordered.add(new SurfaceEntry(pages.get(i), i, 1, false));
            return ordered;
        }

        Map<String, SurfaceEntry> grouped = new LinkedHashMap<>();
        for (int i = 0; i < pages.size(); i++) {
            ItemStack page = pages.get(i);
            String key = pageKey(page);
            SurfaceEntry old = grouped.get(key);
            if (old == null) grouped.put(key, new SurfaceEntry(page, i, 1, false));
            else grouped.put(key, new SurfaceEntry(old.page, old.sourceIndex, old.count + 1, false));
        }

        if (showAll) {
            for (var symbol : SymbolRegistry.values()) if (SymbolAvailability.isSelectable(symbol.legacyId())) addGhost(grouped, symbol.legacyId());
            for (var symbol : LegacyMaterialSymbolRegistry.values()) if (SymbolAvailability.isSelectable(symbol.legacyId())) addGhost(grouped, symbol.legacyId());
            for (var symbol : LegacyBiomeSymbolRegistry.values()) if (SymbolAvailability.isSelectable(symbol.legacyId())) addGhost(grouped, symbol.legacyId());
        }

        String filter = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<SurfaceEntry> result = new ArrayList<>();
        for (SurfaceEntry entry : grouped.values()) {
            String displayName = surfaceDisplayName(entry.page);
            if (!filter.isEmpty() && !surfaceSearchText(entry.page).contains(filter)) continue;
            result.add(entry);
        }
        if (alphabetical) {
            result.sort((a, b) -> surfaceDisplayName(a.page).compareToIgnoreCase(surfaceDisplayName(b.page)));
        } else {
            // CP264A: the legacy-registry insertion order was useful to developers but opaque to
            // players.  Group every symbol exactly once in a grammar-oriented catalogue order.
            result.sort((a, b) -> {
                String aId = Page.getSymbolId(a.page);
                String bId = Page.getSymbolId(b.page);
                int ag = aId == null ? LegacySymbolCatalogOrder.OTHER : LegacySymbolCatalogOrder.group(aId);
                int bg = bId == null ? LegacySymbolCatalogOrder.OTHER : LegacySymbolCatalogOrder.group(bId);
                int group = Integer.compare(ag, bg);
                if (group != 0) return group;
                return surfaceDisplayName(a.page).compareToIgnoreCase(surfaceDisplayName(b.page));
            });
        }
        return result;
    }

    private static void addGhost(Map<String, SurfaceEntry> grouped, String legacyId) {
        ItemStack page = Page.createSymbolPage(legacyId);
        grouped.putIfAbsent(pageKey(page), new SurfaceEntry(page, -1, 0, true));
    }

    private static String pageKey(ItemStack page) {
        String symbol = Page.getSymbolId(page);
        if (symbol != null) return symbol;
        return page.getDescriptionId() + ":" + page.getComponentsPatch();
    }


    private static String surfaceSearchText(ItemStack page) {
        String symbol = Page.getSymbolId(page);
        if (symbol == null) return page.getHoverName().getString().toLowerCase(Locale.ROOT);
        return LegacySymbolName.searchText(symbol);
    }

    private static String surfaceDisplayName(ItemStack page) {
        String symbol = Page.getSymbolId(page);
        if (symbol != null) return LegacySymbolName.component(symbol).getString();
        return page.getHoverName().getString();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= leftPos && mouseX < leftPos + 58) {
            int y = topPos + MAIN_TOP;
            if (mouseY >= y && mouseY < y + 9) {
                clickMenuButton(WritingDeskMenu.ACTION_TAB_UP);
                return true;
            }
            y += 9;
            for (int view = 0; view < WritingDeskMenu.VISIBLE_TAB_SLOTS; view++) {
                // Keep the 18x18 actual inventory slot at x=37..54 available to normal slot handling.
                if (mouseY >= y + 1 && mouseY < y + 36
                        && !(mouseX >= leftPos + 35 && mouseX < leftPos + 54 && mouseY >= y + 2 && mouseY < y + 21)) {
                    clickMenuButton(WritingDeskMenu.ACTION_SELECT_TAB + view);
                    return true;
                }
                y += 37;
            }
            if (mouseY >= y && mouseY < y + 9) {
                clickMenuButton(WritingDeskMenu.ACTION_TAB_DOWN);
                return true;
            }
        }

        SurfaceEntry hit = hitSurface(mouseX, mouseY);
        if (minecraft != null && minecraft.player != null && minecraft.gameMode != null) {
            if (!menu.getCarried().isEmpty() && isInsideSurfacePageArea(mouseX, mouseY)) {
                int action = button == 1 ? WritingDeskMenu.ACTION_SURFACE_INSERT_ONE : WritingDeskMenu.ACTION_SURFACE_INSERT_ALL;
                int index = hit != null && hit.sourceIndex >= 0 ? hit.sourceIndex : menu.getSurfacePages().size();
                clickMenuButton(action + Math.min(index, WritingDeskMenu.ACTION_RANGE - 1));
                return true;
            }
            if (hit != null && !hit.ghost && hit.sourceIndex >= 0) {
                int action;
                if (button == 1) action = WritingDeskMenu.ACTION_SURFACE_COPY_SYMBOL;
                else if (hasShiftDown() && menu.surfaceIsCollection()) action = WritingDeskMenu.ACTION_SURFACE_REMOVE_STACK;
                else action = WritingDeskMenu.ACTION_SURFACE_REMOVE_ONE;
                clickMenuButton(action + Math.min(hit.sourceIndex, WritingDeskMenu.ACTION_RANGE - 1));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isInsideSurfacePageArea(double mouseX, double mouseY) {
        int x0 = leftPos + SURFACE_X + 2;
        int y0 = topPos + SURFACE_Y + 22;
        return mouseX >= x0 && mouseX < leftPos + LEFT_SURFACE_WIDTH - 2
                && mouseY >= y0 && mouseY < topPos + SURFACE_Y + SURFACE_H - 2;
    }

    private SurfaceEntry hitSurface(double mouseX, double mouseY) {
        List<SurfaceEntry> entries = surfaceEntries();
        int areaX = leftPos + SURFACE_X + 4;
        int areaY = topPos + SURFACE_Y + 24;
        int usableW = SURFACE_W - 8;
        int cols = Math.max(1, usableW / (CARD_W + CARD_GAP));
        int firstVisible = surfaceScrollRow * cols;
        for (int i = firstVisible; i < entries.size(); i++) {
            int visible = i - firstVisible;
            int col = visible % cols;
            int row = visible / cols;
            int x = areaX + col * (CARD_W + CARD_GAP);
            int y = areaY + row * (CARD_H + CARD_GAP);
            if (y + CARD_H > topPos + SURFACE_Y + SURFACE_H - 2) break;
            if (mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H) return entries.get(i);
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int x0 = leftPos + SURFACE_X;
        int y0 = topPos + SURFACE_Y + 20;
        if (mouseX >= x0 && mouseX < leftPos + LEFT_SURFACE_WIDTH
                && mouseY >= y0 && mouseY < topPos + SURFACE_Y + SURFACE_H) {
            int cols = Math.max(1, (SURFACE_W - 8) / (CARD_W + CARD_GAP));
            int visibleRows = Math.max(1, (SURFACE_H - 28) / (CARD_H + CARD_GAP));
            int totalRows = (surfaceEntries().size() + cols - 1) / cols;
            int max = Math.max(0, totalRows - visibleRows);
            if (scrollY > 0) surfaceScrollRow = Math.max(0, surfaceScrollRow - 1);
            else if (scrollY < 0) surfaceScrollRow = Math.min(max, surfaceScrollRow + 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 0.13.7.06 GuiWritingDesk draws neither vanilla container labels nor a
        // permanent textual mB counter.  Keeping this empty prevents the modern
        // labels from overlapping the legacy page/book controls.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        // The legacy desk used compact AZ/ALL toggle labels.  Keep the controls
        // tiny, readable and shadow-free; the normal Button renderer is left
        // empty so translated text cannot overflow the 18px buttons.
        Component sortLabel = Component.translatable("gui.mystcraft.writing_desk.sort_short");
        Component allLabel = Component.translatable("gui.mystcraft.writing_desk.all_short");
        drawToggleState(graphics, azButton, alphabetical);
        drawToggleState(graphics, allButton, showAll);
        drawCenteredNoShadow(graphics, sortLabel, azButton);
        drawCenteredNoShadow(graphics, allLabel, allButton);
        if (azButton != null && azButton.isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable(
                    alphabetical ? "gui.mystcraft.writing_desk.sort_on" : "gui.mystcraft.writing_desk.sort_off"), mouseX, mouseY);
        } else if (allButton != null && allButton.isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable(
                    showAll ? "gui.mystcraft.writing_desk.all_on" : "gui.mystcraft.writing_desk.all_off"), mouseX, mouseY);
        }

        SurfaceEntry hit = hitSurface(mouseX, mouseY);
        if (hit != null && !hit.page.isEmpty()) graphics.renderTooltip(font, hit.page, mouseX, mouseY);
        else renderTooltip(graphics, mouseX, mouseY);
    }
    private void drawToggleState(GuiGraphics graphics, Button button, boolean selected) {
        if (button == null || !button.visible || !selected) return;
        int x0 = button.getX() + 1;
        int y0 = button.getY() + 1;
        int x1 = button.getX() + button.getWidth() - 1;
        int y1 = button.getY() + button.getHeight() - 1;
        // Independent one-pixel inset selection frame.  Because it is drawn for each
        // toggle separately, ABC+ALL can visibly remain selected at the same time.
        graphics.fill(x0, y0, x1, y0 + 1, 0xFFFFFFFF);
        graphics.fill(x0, y1 - 1, x1, y1, 0xFFFFFFFF);
        graphics.fill(x0, y0, x0 + 1, y1, 0xFFFFFFFF);
        graphics.fill(x1 - 1, y0, x1, y1, 0xFFFFFFFF);
    }

    private void drawCenteredNoShadow(GuiGraphics graphics, Component text, Button button) {
        if (button == null || !button.visible) return;
        int x = button.getX() + (button.getWidth() - font.width(text)) / 2;
        int y = button.getY() + (button.getHeight() - 8) / 2;
        graphics.drawString(font, text, x, y, button.active ? 0xFFFFFF : 0xA0A0A0, false);
    }

}
