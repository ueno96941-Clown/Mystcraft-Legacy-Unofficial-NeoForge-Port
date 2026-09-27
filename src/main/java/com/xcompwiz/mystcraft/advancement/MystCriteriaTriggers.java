package com.xcompwiz.mystcraft.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xcompwiz.mystcraft.Mystcraft;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The three custom 0.13.7.06 advancement triggers, modernized to the 1.21.1
 * criterion registry.  They intentionally have no extra condition beyond the
 * optional vanilla player predicate, exactly like the legacy trigger classes.
 */
public final class MystCriteriaTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, Mystcraft.MOD_ID);

    public static final Supplier<SimpleMystTrigger> WRITING_DESK_WRITE =
            TRIGGERS.register("writing_desk_write", SimpleMystTrigger::new);
    public static final Supplier<SimpleMystTrigger> ENTER_MYST_DIMENSION_SAFE =
            TRIGGERS.register("enter_myst_dimension_safe", SimpleMystTrigger::new);
    public static final Supplier<SimpleMystTrigger> ENTER_MYST_DIMENSION_QUINN =
            TRIGGERS.register("enter_myst_dimension_quinn", SimpleMystTrigger::new);

    private MystCriteriaTriggers() {}

    public static void register(IEventBus bus) {
        TRIGGERS.register(bus);
    }

    public static final class SimpleMystTrigger extends SimpleCriterionTrigger<TriggerInstance> {
        @Override
        public Codec<TriggerInstance> codec() {
            return TriggerInstance.CODEC;
        }

        public void trigger(ServerPlayer player) {
            trigger(player, instance -> true);
        }
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player)
        ).apply(instance, TriggerInstance::new));
    }
}
