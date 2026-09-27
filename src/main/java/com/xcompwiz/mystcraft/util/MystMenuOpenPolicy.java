package com.xcompwiz.mystcraft.util;

import net.minecraft.world.entity.player.Player;

/** Shared guard for Mystcraft custom menu entry points. */
public final class MystMenuOpenPolicy {
    private MystMenuOpenPolicy() {}

    /** Spectators do not receive custom menu extra-data buffers reliably on 1.21.1. */
    public static boolean canOpen(Player player) {
        return player != null && !player.isSpectator();
    }
}
