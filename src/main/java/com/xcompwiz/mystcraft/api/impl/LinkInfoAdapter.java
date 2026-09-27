package com.xcompwiz.mystcraft.api.impl;

import com.xcompwiz.mystcraft.api.linking.ILinkInfo;
import com.xcompwiz.mystcraft.linking.LinkOptions;
import com.xcompwiz.mystcraft.util.LegacyItemData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Bridges the public API link descriptor to either an ItemStack or standalone legacy NBT. */
public final class LinkInfoAdapter implements ILinkInfo {
    private final ItemStack stack;
    private CompoundTag detached;

    private LinkInfoAdapter(ItemStack stack, CompoundTag detached) { this.stack=stack; this.detached=detached; }
    public static LinkInfoAdapter forStack(ItemStack stack){return new LinkInfoAdapter(stack,null);}
    public static LinkInfoAdapter detached(CompoundTag tag){return new LinkInfoAdapter(null,tag==null?new CompoundTag():tag.copy());}

    private CompoundTag read(){return stack!=null? LegacyItemData.copy(stack):detached.copy();}
    private void write(CompoundTag tag){if(stack!=null) LegacyItemData.set(stack,tag); else detached=tag.copy();}
    private void mutate(java.util.function.Consumer<CompoundTag> mutator){CompoundTag t=read();mutator.accept(t);write(t);}

    @Override public String getDisplayName(){CompoundTag t=read();return t.contains(LinkOptions.KEY_DISPLAY_NAME,Tag.TAG_STRING)?t.getString(LinkOptions.KEY_DISPLAY_NAME):"???";}
    @Override public void setDisplayName(String name){mutate(t->t.putString(LinkOptions.KEY_DISPLAY_NAME,name==null?"":name));}
    @Override public Integer getDimensionUID(){CompoundTag t=read();return t.contains(LinkOptions.KEY_DIMENSION,Tag.TAG_INT)?t.getInt(LinkOptions.KEY_DIMENSION):null;}
    @Override public void setDimensionUID(int uid){mutate(t->t.putInt(LinkOptions.KEY_DIMENSION,uid));}
    @Override public UUID getTargetUUID(){CompoundTag t=read();if(!t.contains(LinkOptions.KEY_TARGET_UUID,Tag.TAG_STRING))return null;try{return UUID.fromString(t.getString(LinkOptions.KEY_TARGET_UUID));}catch(IllegalArgumentException e){return null;}}
    @Override public void setTargetUUID(UUID uuid){mutate(t->{if(uuid==null)t.remove(LinkOptions.KEY_TARGET_UUID);else t.putString(LinkOptions.KEY_TARGET_UUID,uuid.toString());});}
    @Override public BlockPos getSpawn(){CompoundTag t=read();if(!t.contains(LinkOptions.KEY_SPAWN_X,Tag.TAG_INT)||!t.contains(LinkOptions.KEY_SPAWN_Y,Tag.TAG_INT)||!t.contains(LinkOptions.KEY_SPAWN_Z,Tag.TAG_INT))return null;return new BlockPos(t.getInt(LinkOptions.KEY_SPAWN_X),t.getInt(LinkOptions.KEY_SPAWN_Y),t.getInt(LinkOptions.KEY_SPAWN_Z));}
    @Override public void setSpawn(BlockPos pos){mutate(t->{if(pos==null){t.remove(LinkOptions.KEY_SPAWN_X);t.remove(LinkOptions.KEY_SPAWN_Y);t.remove(LinkOptions.KEY_SPAWN_Z);}else{t.putInt(LinkOptions.KEY_SPAWN_X,pos.getX());t.putInt(LinkOptions.KEY_SPAWN_Y,pos.getY());t.putInt(LinkOptions.KEY_SPAWN_Z,pos.getZ());}});}
    @Override public float getSpawnYaw(){CompoundTag t=read();return t.contains(LinkOptions.KEY_SPAWN_YAW,Tag.TAG_FLOAT)?t.getFloat(LinkOptions.KEY_SPAWN_YAW):180f;}
    @Override public void setSpawnYaw(float yaw){mutate(t->t.putFloat(LinkOptions.KEY_SPAWN_YAW,yaw));}
    @Override public boolean getFlag(String flag){CompoundTag t=read();return t.contains(LinkOptions.KEY_FLAGS,Tag.TAG_COMPOUND)&&t.getCompound(LinkOptions.KEY_FLAGS).getBoolean(flag);}
    @Override public void setFlag(String flag,boolean value){mutate(t->{CompoundTag f=t.contains(LinkOptions.KEY_FLAGS,Tag.TAG_COMPOUND)?t.getCompound(LinkOptions.KEY_FLAGS):new CompoundTag();f.putBoolean(flag,value);t.put(LinkOptions.KEY_FLAGS,f);});}
    @Override public String getProperty(String flag){CompoundTag t=read();if(!t.contains(LinkOptions.KEY_PROPS,Tag.TAG_COMPOUND))return null;CompoundTag p=t.getCompound(LinkOptions.KEY_PROPS);return p.contains(flag,Tag.TAG_STRING)?p.getString(flag):null;}
    @Override public void setProperty(String flag,String value){mutate(t->{CompoundTag p=t.contains(LinkOptions.KEY_PROPS,Tag.TAG_COMPOUND)?t.getCompound(LinkOptions.KEY_PROPS):new CompoundTag();if(value==null)p.remove(flag);else p.putString(flag,value);t.put(LinkOptions.KEY_PROPS,p);});}
    @Override public CompoundTag getTagCompound(){return read();}
    @Override public ILinkInfo clone(){return detached(read());}
}
