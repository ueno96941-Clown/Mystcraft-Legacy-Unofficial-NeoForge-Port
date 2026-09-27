package com.xcompwiz.mystcraft.api.world.storage;
/** Small persistent key/value view retained for legacy weather/effect integrations. */
public interface StorageObject {
    boolean getBoolean(String key); void setBoolean(String key, boolean value);
    int getInteger(String key); void setInteger(String key, int value);
}
