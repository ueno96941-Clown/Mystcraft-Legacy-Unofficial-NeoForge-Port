package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.entity.EntityMeteor;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Client-side spawn bridge for the legacy looping Meteor roar. */
@EventBusSubscriber(modid = Mystcraft.MOD_ID, value = Dist.CLIENT)
public final class MeteorClientEvents {
    private MeteorClientEvents() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof EntityMeteor meteor)) return;
        Minecraft.getInstance().getSoundManager().play(new MovingSoundMeteor(meteor));
    }
}
