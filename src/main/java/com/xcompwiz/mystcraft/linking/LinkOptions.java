package com.xcompwiz.mystcraft.linking;

import com.xcompwiz.mystcraft.util.LegacyItemData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Compatibility-first link payload using the 0.13.7.06 key names.
 *
 * <p>Modern Minecraft no longer exposes numeric dimension IDs. Vanilla
 * dimensions still receive their historical values in {@code Dimension};
 * every link additionally carries {@code DimensionKey} as the authoritative
 * 1.21.1 resource-key extension.</p>
 */
public final class LinkOptions {
    public static final String KEY_DISPLAY_NAME = "DisplayName";
    public static final String KEY_DIMENSION = "Dimension";
    public static final String KEY_DIMENSION_KEY = "DimensionKey";
    public static final String KEY_TARGET_UUID = "TargetUUID";
    public static final String KEY_AGE_UID = "AgeUID";
    public static final String KEY_SPAWN_X = "SpawnX";
    public static final String KEY_SPAWN_Y = "SpawnY";
    public static final String KEY_SPAWN_Z = "SpawnZ";
    public static final String KEY_SPAWN_YAW = "SpawnYaw";
    public static final String KEY_FLAGS = "Flags";
    public static final String KEY_PROPS = "Props";

    private LinkOptions() {}

    public static void capturePosition(ItemStack book, Level level, Entity entity) {
        LegacyItemData.update(book, data -> {
            ResourceKey<Level> dimension = level.dimension();
            data.putString(KEY_DIMENSION_KEY, dimension.location().toString());

            Integer legacyId = legacyDimensionId(dimension);
            if (legacyId != null) {
                data.putInt(KEY_DIMENSION, legacyId);
                data.remove(KEY_AGE_UID);
                data.remove(KEY_TARGET_UUID);
            } else if (level instanceof ServerLevel serverLevel) {
                // CP279: a Linking Book created inside a Mystcraft Age must carry
                // the durable Age identity as well as the modern DimensionKey.
                // The Age can be lazily unloaded later, so DimensionKey-only books
                // are not sufficient for the legacy numeric/API portal path.
                var age = AgeManager.resolveByLevel(serverLevel.getServer(), dimension).orElse(null);
                if (age != null) {
                    data.putInt(KEY_DIMENSION, age.ageUid());
                    data.putInt(KEY_AGE_UID, age.ageUid());
                    data.putString(KEY_TARGET_UUID, age.uuid().toString());
                    if (!data.contains(KEY_DISPLAY_NAME, Tag.TAG_STRING)
                            || data.getString(KEY_DISPLAY_NAME).isBlank()
                            || "???".equals(data.getString(KEY_DISPLAY_NAME))) {
                        data.putString(KEY_DISPLAY_NAME, age.ageName());
                    }
                } else {
                    data.remove(KEY_DIMENSION);
                    data.remove(KEY_AGE_UID);
                    data.remove(KEY_TARGET_UUID);
                }
            } else {
                data.remove(KEY_DIMENSION);
            }

            if (entity != null) {
                var pos = entity.blockPosition();
                data.putInt(KEY_SPAWN_X, pos.getX());
                data.putInt(KEY_SPAWN_Y, pos.getY());
                data.putInt(KEY_SPAWN_Z, pos.getZ());
                data.putFloat(KEY_SPAWN_YAW, entity.getYRot());
            }

            if (!data.contains(KEY_DISPLAY_NAME, Tag.TAG_STRING)) {
                data.putString(KEY_DISPLAY_NAME, defaultDimensionName(dimension));
            }
        });
    }

    public static void setDisplayName(ItemStack stack, String name) {
        LegacyItemData.update(stack, data -> data.putString(KEY_DISPLAY_NAME, name));
    }

    public static String getDisplayName(ItemStack stack) {
        CompoundTag data = LegacyItemData.copy(stack);
        if (data.contains(KEY_DISPLAY_NAME, Tag.TAG_STRING)) {
            return data.getString(KEY_DISPLAY_NAME);
        }
        if (data.contains("agename", Tag.TAG_STRING)) {
            return data.getString("agename");
        }
        return "???";
    }

    public static void setFlag(ItemStack stack, String flag, boolean value) {
        LegacyItemData.update(stack, data -> {
            CompoundTag flags = data.contains(KEY_FLAGS, Tag.TAG_COMPOUND)
                    ? data.getCompound(KEY_FLAGS)
                    : new CompoundTag();
            flags.putBoolean(flag, value);
            data.put(KEY_FLAGS, flags);
        });
    }

    public static boolean getFlag(ItemStack stack, String flag) {
        CompoundTag data = LegacyItemData.copy(stack);
        if (!data.contains(KEY_FLAGS, Tag.TAG_COMPOUND)) return false;
        CompoundTag flags = data.getCompound(KEY_FLAGS);
        return flags.contains(flag) && flags.getBoolean(flag);
    }

    public static void setProperty(ItemStack stack, String property, String value) {
        LegacyItemData.update(stack, data -> {
            CompoundTag properties = data.contains(KEY_PROPS, Tag.TAG_COMPOUND)
                    ? data.getCompound(KEY_PROPS)
                    : new CompoundTag();
            if (value == null) {
                properties.remove(property);
            } else {
                properties.putString(property, value);
            }
            data.put(KEY_PROPS, properties);
        });
    }

    public static String getProperty(ItemStack stack, String property) {
        CompoundTag data = LegacyItemData.copy(stack);
        if (!data.contains(KEY_PROPS, Tag.TAG_COMPOUND)) return null;
        CompoundTag properties = data.getCompound(KEY_PROPS);
        return properties.contains(property, Tag.TAG_STRING) ? properties.getString(property) : null;
    }


    public static void setDimensionKey(ItemStack stack, String dimensionKey) {
        LegacyItemData.update(stack, data -> {
            if (dimensionKey == null || dimensionKey.isBlank()) data.remove(KEY_DIMENSION_KEY);
            else data.putString(KEY_DIMENSION_KEY, dimensionKey);
        });
    }

    public static void setLegacyDimensionId(ItemStack stack, int dimensionId) {
        LegacyItemData.update(stack, data -> data.putInt(KEY_DIMENSION, dimensionId));
    }

    public static void setAgeUid(ItemStack stack, int ageUid) {
        LegacyItemData.update(stack, data -> data.putInt(KEY_AGE_UID, ageUid));
    }

    public static Integer getAgeUid(ItemStack stack) {
        CompoundTag data = LegacyItemData.copy(stack);
        return data.contains(KEY_AGE_UID, Tag.TAG_INT) ? data.getInt(KEY_AGE_UID) : null;
    }

    public static String getDimensionKey(ItemStack stack) {
        CompoundTag data = LegacyItemData.copy(stack);
        return data.contains(KEY_DIMENSION_KEY, Tag.TAG_STRING) ? data.getString(KEY_DIMENSION_KEY) : null;
    }

    public static Integer getLegacyDimensionId(ItemStack stack) {
        CompoundTag data = LegacyItemData.copy(stack);
        if (data.contains(KEY_DIMENSION, Tag.TAG_INT)) return data.getInt(KEY_DIMENSION);
        if (data.contains("AgeUID", Tag.TAG_INT)) return data.getInt("AgeUID");
        return null;
    }


    public static net.minecraft.core.BlockPos getSpawn(ItemStack stack) {
        CompoundTag data = LegacyItemData.copy(stack);
        if (!data.contains(KEY_SPAWN_X, Tag.TAG_INT) || !data.contains(KEY_SPAWN_Y, Tag.TAG_INT) || !data.contains(KEY_SPAWN_Z, Tag.TAG_INT)) return null;
        return new net.minecraft.core.BlockPos(data.getInt(KEY_SPAWN_X), data.getInt(KEY_SPAWN_Y), data.getInt(KEY_SPAWN_Z));
    }

    public static float getSpawnYaw(ItemStack stack) {
        CompoundTag data = LegacyItemData.copy(stack);
        return data.contains(KEY_SPAWN_YAW, Tag.TAG_FLOAT) ? data.getFloat(KEY_SPAWN_YAW) : 180.0F;
    }

    public static void setTargetUuid(ItemStack stack, UUID uuid) {
        LegacyItemData.update(stack, data -> {
            if (uuid == null) data.remove(KEY_TARGET_UUID);
            else data.putString(KEY_TARGET_UUID, uuid.toString());
        });
    }

    public static UUID getTargetUuid(ItemStack stack) {
        CompoundTag data = LegacyItemData.copy(stack);
        if (!data.contains(KEY_TARGET_UUID, Tag.TAG_STRING)) return null;
        try {
            return UUID.fromString(data.getString(KEY_TARGET_UUID));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Integer legacyDimensionId(ResourceKey<Level> dimension) {
        if (dimension.equals(Level.OVERWORLD)) return 0;
        if (dimension.equals(Level.NETHER)) return -1;
        if (dimension.equals(Level.END)) return 1;
        return null;
    }

    private static String defaultDimensionName(ResourceKey<Level> dimension) {
        if (dimension.equals(Level.OVERWORLD)) return "Overworld";
        if (dimension.equals(Level.NETHER)) return "Nether";
        if (dimension.equals(Level.END)) return "The End";
        return dimension.location().toString();
    }
}
