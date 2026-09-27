package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.Mystcraft;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Server-thread fallback for deferred Legacy population writes whose target chunk is already FULL. */
@EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class AgeLegacyDeferredBlockWriteEvents {
    private AgeLegacyDeferredBlockWriteEvents() {}

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().location().getNamespace().equals(Mystcraft.MOD_ID)) return;
        // Bound work per tick; newly generating chunks normally drain their own queue earlier.
        AgeLegacyDeferredBlockWrites.flushFullChunks(level, 8);
    }
}
