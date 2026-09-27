package com.xcompwiz.mystcraft.api.linking;
import net.minecraft.core.BlockPos; import net.minecraft.nbt.CompoundTag; import java.util.UUID;
public interface ILinkInfo extends Cloneable {
    String getDisplayName(); void setDisplayName(String name);
    Integer getDimensionUID(); void setDimensionUID(int uid);
    UUID getTargetUUID(); void setTargetUUID(UUID uuid);
    BlockPos getSpawn(); void setSpawn(BlockPos spawn);
    float getSpawnYaw(); void setSpawnYaw(float yaw);
    boolean getFlag(String flag); void setFlag(String flag,boolean value);
    String getProperty(String flag); void setProperty(String flag,String value);
    CompoundTag getTagCompound(); ILinkInfo clone();
}
