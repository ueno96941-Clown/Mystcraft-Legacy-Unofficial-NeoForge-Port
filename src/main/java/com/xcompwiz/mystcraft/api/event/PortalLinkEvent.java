package com.xcompwiz.mystcraft.api.event;
import com.xcompwiz.mystcraft.api.linking.ILinkInfo; import net.minecraft.world.entity.Entity; import net.minecraft.world.level.Level; import net.neoforged.bus.api.Event;
public class PortalLinkEvent extends Event { public final Level worldObj; public final Entity entity; public final ILinkInfo info; public PortalLinkEvent(Level worldObj,Entity entity,ILinkInfo info){this.worldObj=worldObj;this.entity=entity;this.info=info;} }
