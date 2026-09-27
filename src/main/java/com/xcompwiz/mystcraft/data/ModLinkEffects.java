package com.xcompwiz.mystcraft.data;

/** Legacy default policy: every configured Ink Mixer link property is enabled. */
public final class ModLinkEffects {
    private ModLinkEffects() {}

    public static boolean isPropertyAllowed(String property) {
        return property != null && !property.isBlank();
    }
}
