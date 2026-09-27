package com.xcompwiz.mystcraft.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.xcompwiz.mystcraft.symbol.LegacyBiomeSymbolDefinition;
import com.xcompwiz.mystcraft.symbol.LegacyBiomeSymbolRegistry;
import com.xcompwiz.mystcraft.symbol.LegacyMaterialSymbolRegistry;
import com.xcompwiz.mystcraft.symbol.LegacySymbolId;
import com.xcompwiz.mystcraft.world.worldgen.LegacyModernBiomeIdResolver;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Central user-visible Symbol naming without mutating persisted legacy IDs.
 *
 * <p>The old Page ID remains byte-for-byte compatible, but every UI surface uses this
 * resolver so raw translation keys such as {@code biome.minecraft.jungle_hills}, legacy
 * aliases which collapse onto the same modern biome, and internal camel-case IDs never
 * leak into normal item names/tooltips.</p>
 */
public final class LegacySymbolName {
    private static final Map<String, String> ENGLISH_NAMES = loadEnglishNames();

    private LegacySymbolName() {}

    public static Component component(String legacyId) {
        String id = LegacySymbolId.qualify(legacyId);

        var material = LegacyMaterialSymbolRegistry.resolveLegacy(id);
        if (material.isPresent()) {
            ResourceLocation blockId = ResourceLocation.tryParse(material.get().modernBlockId());
            if (blockId != null && BuiltInRegistries.BLOCK.containsKey(blockId)) {
                var block = BuiltInRegistries.BLOCK.get(blockId);
                return Component.translatableWithFallback(block.getDescriptionId(), humanize(blockId.getPath()));
            }
            if (blockId != null) return Component.literal(humanize(blockId.getPath()));
        }

        var biome = LegacyBiomeSymbolRegistry.resolveLegacy(id);
        if (biome.isPresent()) return biomeComponent(biome.get());

        String rawPath = path(id);
        String keyPath = rawPath.toLowerCase(Locale.ROOT);
        return Component.translatableWithFallback("myst.symbol." + keyPath + ".name", humanize(rawPath));
    }

    private static Component biomeComponent(LegacyBiomeSymbolDefinition definition) {
        // Legacy numeric biome pages keep their original 1.12 identity in the visible
        // name.  CP262 exposed compatibility mappings as "Old -> Modern", which
        // produced very long tooltips in both English and Japanese.  The modern target
        // remains an implementation detail and a search alias, not display text.
        String legacyName = LegacyBiomeSymbolRegistry.legacyDisplayName(definition).orElse(null);
        if (legacyName != null) {
            String key = "myst.symbol.biome.legacy." + definition.legacyNumericId();
            return Component.translatableWithFallback(key, legacyName);
        }

        String resolvedModernId = LegacyModernBiomeIdResolver.resolveModernId(definition.modernBiomeId());
        ResourceLocation modernId = ResourceLocation.tryParse(resolvedModernId);
        return modernId == null
                ? Component.literal(humanize(resolvedModernId))
                : Component.translatableWithFallback(
                        "biome." + modernId.getNamespace() + "." + modernId.getPath(),
                        humanize(modernId.getPath()));
    }

    private static boolean equivalentName(String left, String right) {
        return normalizeName(left).equals(normalizeName(right));
    }

    private static String normalizeName(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    public static String humanize(String value) {
        if (value == null || value.isBlank()) return "Unknown";
        String path = value;
        int colon = path.indexOf(':');
        if (colon >= 0 && colon + 1 < path.length()) path = path.substring(colon + 1);
        path = path.replace('_', ' ').replace('-', ' ')
                .replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ")
                .replaceAll("\\s+", " ").trim();
        if (path.isEmpty()) return "Unknown";

        StringBuilder out = new StringBuilder(path.length());
        boolean capitalize = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == ' ') {
                out.append(c);
                capitalize = true;
            } else if (capitalize) {
                out.append(Character.toUpperCase(c));
                capitalize = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** Search aliases: localized visible name, canonical English name, and stable legacy ID. */
    public static String searchText(String legacyId) {
        String id = LegacySymbolId.qualify(legacyId);
        String rawPath = path(id);
        String english = englishName(id);
        return (component(id).getString() + " " + english + " " + humanize(rawPath) + " " + rawPath + " " + id)
                .toLowerCase(Locale.ROOT);
    }

    /** Canonical en_us name used only as a search alias; it is not rendered in Japanese UI. */
    public static String englishName(String legacyId) {
        String id = LegacySymbolId.qualify(legacyId);
        var biome = LegacyBiomeSymbolRegistry.resolveLegacy(id);
        if (biome.isPresent()) {
            LegacyBiomeSymbolDefinition definition = biome.get();
            String legacyName = LegacyBiomeSymbolRegistry.legacyDisplayName(definition).orElse(null);
            if (legacyName != null) return legacyName;
            String modern = LegacyModernBiomeIdResolver.resolveModernId(definition.modernBiomeId());
            ResourceLocation modernId = ResourceLocation.tryParse(modern);
            return humanize(modernId == null ? modern : modernId.getPath());
        }
        String rawPath = path(id);
        return ENGLISH_NAMES.getOrDefault(rawPath.toLowerCase(Locale.ROOT), humanize(rawPath));
    }

    private static Map<String, String> loadEnglishNames() {
        Map<String, String> out = new HashMap<>();
        try (var stream = LegacySymbolName.class.getClassLoader().getResourceAsStream("assets/mystcraft/lang/en_us.json")) {
            if (stream == null) return out;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : root.entrySet()) {
                String key = entry.getKey();
                if (!key.startsWith("myst.symbol.") || !key.endsWith(".name")) continue;
                String path = key.substring("myst.symbol.".length(), key.length() - ".name".length());
                if (entry.getValue().isJsonPrimitive()) out.put(path, entry.getValue().getAsString());
            }
        } catch (Exception ignored) {
        }
        return Map.copyOf(out);
    }

    private static String path(String id) {
        int split = id.indexOf(':');
        return split >= 0 && split + 1 < id.length() ? id.substring(split + 1) : id;
    }
}
