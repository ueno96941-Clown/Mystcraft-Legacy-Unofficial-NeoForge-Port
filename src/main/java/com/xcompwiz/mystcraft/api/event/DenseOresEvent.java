package com.xcompwiz.mystcraft.api.event;
import net.minecraft.server.level.ServerLevel; import net.neoforged.bus.api.Event; import java.util.Random;
public class DenseOresEvent extends Event { public final ServerLevel worldObj; public final Random random; public final int xPos,zPos; public DenseOresEvent(ServerLevel worldObj,Random random,int xPos,int zPos){this.worldObj=worldObj;this.random=random;this.xPos=xPos;this.zPos=zPos;} }
