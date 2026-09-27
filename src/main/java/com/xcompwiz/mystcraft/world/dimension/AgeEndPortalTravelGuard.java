package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.Mystcraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

/**
 * Keeps vanilla End Portal blocks inside Mystcraft Ages from becoming an unintended
 * vanilla-progression exit to minecraft:the_end.
 *
 * <p>The guard is intentionally narrow: it only cancels travel to The End when the source
 * level is a Mystcraft Age and the entity's active PortalProcessor entry position is an
 * End Portal block.  Linking Books or other explicit dimension transfers are unaffected.</p>
 */
@EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class AgeEndPortalTravelGuard {
    private AgeEndPortalTravelGuard() {}

    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (!event.getDimension().equals(Level.END)) return;
        var entity = event.getEntity();
        if (!entity.level().dimension().location().getNamespace().equals(Mystcraft.MOD_ID)) return;
        if (entity.portalProcess == null) return;

        var entryPos = entity.portalProcess.getEntryPosition();
        if (!entity.level().getBlockState(entryPos).is(Blocks.END_PORTAL)) return;

        event.setCanceled(true);
    }

}
