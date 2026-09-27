package com.xcompwiz.mystcraft.linking;

import java.util.List;

/**
 * String identifiers retained from Mystcraft 0.13.7.06 LinkPropertyAPI.
 *
 * <p>Keeping these exact values matters because they are persisted in Link
 * Panel and book data.</p>
 */
public final class LinkProperties {
    public static final String INTRA_LINKING = "Intra Linking";
    public static final String INTRA_LINKING_ONLY = "Intra Linking Only";
    public static final String RELATIVE = "Relative";
    public static final String DISARM = "Disarm";
    public static final String MAINTAIN_MOMENTUM = "Maintain Momentum";
    public static final String GENERATE_PLATFORM = "Generate Platform";
    public static final String NATURAL = "Natural";
    public static final String EXTERNAL = "External";
    public static final String OFFENSIVE = "Offensive";
    public static final String TP_COMMAND = "Op-TP";
    public static final String FOLLOWING = "Following";
    public static final String SOUND = "Sound";


    /** Link properties registered by 0.13.7.06 InkEffects.init(). */
    public static final List<String> LEGACY_INK_PROPERTIES = List.of(
            DISARM, GENERATE_PLATFORM, INTRA_LINKING, INTRA_LINKING_ONLY,
            MAINTAIN_MOMENTUM, RELATIVE);

    /** 1.12 ItemPage creative subitems omitted Relative even though InkEffects registered it. */
    public static final List<String> LEGACY_CREATIVE_LINK_PANELS = List.of(
            DISARM, GENERATE_PLATFORM, INTRA_LINKING, INTRA_LINKING_ONLY,
            MAINTAIN_MOMENTUM);

    /** Flags whose behavior is implemented by the 1.21.1 LinkController and exposed by the modern Link Modifier. */
    public static final List<String> MODIFIER_EDITABLE = List.of(
            INTRA_LINKING, INTRA_LINKING_ONLY, RELATIVE, DISARM,
            MAINTAIN_MOMENTUM, GENERATE_PLATFORM, FOLLOWING);

    private LinkProperties() {}
}
