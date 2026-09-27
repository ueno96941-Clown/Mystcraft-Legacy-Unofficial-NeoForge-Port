package com.xcompwiz.mystcraft.api.hook;
import com.xcompwiz.mystcraft.api.linking.ILinkInfo; import net.minecraft.nbt.CompoundTag; import net.minecraft.world.entity.Entity; import net.minecraft.world.level.Level;
public interface LinkingAPI { boolean isLinkAllowed(Entity entity,ILinkInfo info); void linkEntity(Entity entity,ILinkInfo info); ILinkInfo createLinkInfoFromPosition(Level level,Entity location); ILinkInfo createLinkInfo(CompoundTag tag); }
