package com.xcompwiz.mystcraft.api.event;

import com.xcompwiz.mystcraft.api.linking.ILinkInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Server-side Mystcraft link event family retained from API v1. */
public abstract class LinkEvent extends Event {
    public final Level origin;
    public final Level destination;
    public final Entity entity;
    public final ILinkInfo info;
    protected LinkEvent(Level origin,Level destination,Entity entity,ILinkInfo info){this.origin=origin;this.destination=destination;this.entity=entity;this.info=info;}
    public static class LinkEventAllow extends LinkEvent implements ICancellableEvent { public LinkEventAllow(Level origin,Entity entity,ILinkInfo info){super(origin,null,entity,info);} }
    public static class LinkEventAlter extends LinkEvent { public BlockPos spawn; public Float rotationYaw; public LinkEventAlter(Level origin,Level destination,Entity entity,ILinkInfo info){super(origin,destination,entity,info);} }
    public static class LinkEventStart extends LinkEvent { public LinkEventStart(Level origin,Entity entity,ILinkInfo info){super(origin,null,entity,info);} }
    public static class LinkEventFailed extends LinkEvent { public LinkEventFailed(Level origin,Entity entity,ILinkInfo info){super(origin,null,entity,info);} }
    @Deprecated public static class LinkEventExitWorld extends LinkEvent { public LinkEventExitWorld(Entity entity,ILinkInfo info){super(null,null,entity,info);} }
    @Deprecated public static class LinkEventEnterWorld extends LinkEvent { public LinkEventEnterWorld(Level origin,Level destination,Entity entity,ILinkInfo info){super(origin,destination,entity,info);} }
    public static class LinkEventEnd extends LinkEvent { public LinkEventEnd(Level origin,Level destination,Entity entity,ILinkInfo info){super(origin,destination,entity,info);} }
}
