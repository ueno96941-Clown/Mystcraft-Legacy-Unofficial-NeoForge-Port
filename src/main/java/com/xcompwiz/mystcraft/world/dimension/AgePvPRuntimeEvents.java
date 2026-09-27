package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Implements legacy PvPOff per Age without changing global server PvP state. */
@EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class AgePvPRuntimeEvents {
    private AgePvPRuntimeEvents() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(victim.level() instanceof ServerLevel level)) return;
        if (!level.dimension().location().getNamespace().equals(Mystcraft.MOD_ID)) return;

        // DamageSource#getEntity resolves the responsible owner for indirect attacks
        // (for example an arrow's shooter), matching legacy PvP intent better than only
        // checking the direct projectile entity.
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker == victim) return;

        var age = AgeManager.resolveByLevel(level.getServer(), level.dimension()).orElse(null);
        if (age == null) return;
        var prepared = AgeRuntimeDimensionLoader.getPrepared(age.ageUid()).orElse(null);
        if (prepared == null) return;

        if (!prepared.worldgenPlan().environmentPlan().pvpEnabled()) {
            event.setCanceled(true);
        }
    }
}
