package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.api.impl.runtime.ApiWordRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.Random;

/** 1.21.1 renderer for the legacy Mystcraft/Narayan four-word symbol glyphs. */
public final class LegacySymbolRenderer {
    private static final ResourceLocation COMPONENTS = ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "textures/symbolcomponents.png");
    private static final ResourceLocation PAGE_LEFT = ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "gui/bookui_pagel.png");
    /*
     * 0.13.7.06 did NOT ship a fixed table for ordinary Narayan words.
     * DrawableWordManager.getDrawableWord() lower-cased the word and generated
     * its component list from new Random(word.hashCode()).  Only decimal tab
     * numbers 0..25 were explicitly registered by ModWords.
     */


    private LegacySymbolRenderer() {}

    /** Draws the same 30x40 parchment crop used by 0.13.7.06, scaled to the requested size. */
    public static void drawPage(GuiGraphics graphics, String descriptor, int x, int y, int width, int height, boolean dimmed) {
        graphics.blit(PAGE_LEFT, x, y, width, height, 156, 0, 30, 40, 256, 256);
        if (dimmed) graphics.fill(x, y, x + width, y + height, 0x99000000);
        if (descriptor == null || descriptor.isBlank() || "$BLANK$".equals(descriptor)) return;
        if ("$LINK_PANEL$".equals(descriptor)) {
            int x1 = x + Math.round(width * 0.15F);
            int x2 = x + Math.round(width * 0.85F);
            int y1 = y + Math.round(height * 0.15F);
            int y2 = y + Math.round(height * 0.50F);
            graphics.fill(x1, y1, x2, y2, 0xFF000000);
            return;
        }
        int glyphSize = Math.max(4, width - 1);
        int glyphY = y + Math.round((height + 1 - width) / 2.0F);
        drawSymbol(graphics, descriptor, x, glyphY, glyphSize);
    }

    public static void drawSymbol(GuiGraphics graphics, String legacyId, int x, int y, int scale) {
        String[] poem = LegacySymbolVisuals.poem(legacyId);
        int half = Math.max(1, scale / 2);
        float sf = half / 2.41421356237F;
        int s = Math.max(1, Math.round(sf));
        int o = Math.round(sf * 1.41421356237F);
        int wordSize = s * 2;
        if (poem.length > 0) drawWord(graphics, poem[0], x + o, y, wordSize);
        if (poem.length > 1) drawWord(graphics, poem[1], x + o * 2, y + o, wordSize);
        if (poem.length > 2) drawWord(graphics, poem[2], x + o, y + o * 2, wordSize);
        if (poem.length > 3) drawWord(graphics, poem[3], x, y + o, wordSize);
    }

    public static void drawWord(GuiGraphics graphics, String word, int x, int y, int size) {
        var custom = word == null ? null : ApiWordRegistry.get(word);
        ResourceLocation source = custom == null ? COMPONENTS : custom.imageSource();
        int[] components = componentsFor(word);
        for (int i = 0; i < components.length; i++) {
            int color = 0;
            if (custom != null && !custom.colors().isEmpty()) {
                color = custom.colors().get(Math.min(i, custom.colors().size() - 1));
            }
            graphics.setColor(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F,
                    (color & 0xFF) / 255F, 1F);
            int component = components[i];
            int iconX = (component % 8) * 64;
            int iconY = (component / 8) * 64;
            graphics.blit(source, x, y, size, size, iconX, iconY, 64, 64, 512, 512);
        }
        graphics.setColor(1F, 1F, 1F, 1F);
    }

    /**
     * Exact 0.13.7.06 explicitly registered word set.  Ordinary words return
     * null and therefore follow DrawableWordManager's seeded fallback below.
     */
    public static int[] componentsForItem(String word) {
        if (word == null) return new int[]{0};
        String key = word.toLowerCase(java.util.Locale.ROOT);
        try {
            int value = Integer.parseInt(key);
            if (value >= 0 && value <= 25) return legacyNumber(value);
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private static int[] legacyNumber(int num) {
        if (num == 0) return new int[]{1};
        if (num >= 25) return new int[]{2};
        int first = 0;
        if (num >= 20) first = 63;
        else if (num >= 15) first = 62;
        else if (num >= 10) first = 61;
        else if (num >= 5) first = 60;
        int second = num % 5 > 0 ? num % 5 + 55 : 0;
        if (first > 0 && second > 0) return new int[]{first, second};
        if (first > 0) return new int[]{first};
        return new int[]{second};
    }

    private static int[] componentsFor(String word) {
        if (word == null) return new int[]{0};
        var custom = ApiWordRegistry.get(word);
        if (custom != null && !custom.components().isEmpty()) {
            return custom.components().stream().mapToInt(Integer::intValue).toArray();
        }
        int[] known = componentsForItem(word);
        if (known != null) return known;
        Random random = new Random(word.toLowerCase().hashCode());
        int maxComponent = 20;
        int count = random.nextInt(10) + 3;
        if (word.toLowerCase().startsWith("easter")) { count = 4; maxComponent = 8; }
        int[] generated = new int[count];
        for (int i = 0; i < count; i++) generated[i] = random.nextInt(maxComponent) + 4;
        return generated;
    }
}
