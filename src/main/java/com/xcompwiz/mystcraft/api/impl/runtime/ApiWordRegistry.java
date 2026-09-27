package com.xcompwiz.mystcraft.api.impl.runtime;

import com.xcompwiz.mystcraft.api.word.DrawableWord;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Runtime word extensions supplied through WordAPI; legacy lookup is case-insensitive. */
public final class ApiWordRegistry {
    private static final Map<String, DrawableWord> WORDS = new LinkedHashMap<>();
    private ApiWordRegistry() {}

    public static synchronized void register(String name, DrawableWord word) {
        if (name == null || name.isBlank() || word == null) return;
        // 0.13.7.06 DrawableWordManager lower-cased both registration and lookup.
        WORDS.putIfAbsent(name.toLowerCase(Locale.ROOT), word);
    }

    public static synchronized DrawableWord get(String name) {
        return name == null ? null : WORDS.get(name.toLowerCase(Locale.ROOT));
    }

    public static synchronized Map<String, DrawableWord> all() { return Map.copyOf(WORDS); }
}
