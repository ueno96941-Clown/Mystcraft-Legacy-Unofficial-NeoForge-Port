package com.xcompwiz.mystcraft.api.event;
import net.minecraft.core.BlockPos; import net.minecraft.world.entity.Entity; import net.neoforged.bus.api.Event; import java.util.List;
/** Compatibility event family retained for the live CP299 Meteor restoration. */
public abstract class MeteorEvent extends Event { public final Entity meteor; private MeteorEvent(Entity meteor){this.meteor=meteor;} public static class MetorSpawn extends MeteorEvent{public MetorSpawn(Entity e){super(e);}} public static class MetorImpact extends MeteorEvent{public MetorImpact(Entity e){super(e);}} public static class MetorExplosion extends MeteorEvent{public final List<BlockPos> blocks; public MetorExplosion(Entity e,List<BlockPos>b){super(e);blocks=List.copyOf(b);}} }
