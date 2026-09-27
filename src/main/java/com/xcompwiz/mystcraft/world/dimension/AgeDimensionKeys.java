package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.Mystcraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * Canonical resource-key mapping for reserved Mystcraft Ages.
 *
 * <p>Legacy Mystcraft used one DimensionType/provider family and allocated a
 * numeric dimension id per Age. The 1.21.1 port mirrors that split with one
 * shared {@code mystcraft:age} DimensionType key and one
 * {@code mystcraft:age_<UID>} Level key per Age.</p>
 */
public final class AgeDimensionKeys {
    private AgeDimensionKeys() {}

    public static final ResourceLocation AGE_DIMENSION_TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "age");
    public static final ResourceKey<DimensionType> AGE_DIMENSION_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, AGE_DIMENSION_TYPE_ID);

    public static ResourceLocation levelLocation(int ageUid) {
        requireValidUid(ageUid);
        return ResourceLocation.fromNamespaceAndPath(Mystcraft.MOD_ID, "age_" + ageUid);
    }

    public static ResourceKey<Level> levelKey(int ageUid) {
        return ResourceKey.create(Registries.DIMENSION, levelLocation(ageUid));
    }

    public static ResourceKey<LevelStem> levelStemKey(int ageUid) {
        return ResourceKey.create(Registries.LEVEL_STEM, levelLocation(ageUid));
    }

    public static String levelKeyString(int ageUid) {
        return levelLocation(ageUid).toString();
    }

    public static boolean matches(int ageUid, String dimensionKey) {
        if (dimensionKey == null || dimensionKey.isBlank()) return false;
        return levelKeyString(ageUid).equals(dimensionKey);
    }

    private static void requireValidUid(int ageUid) {
        if (ageUid < 2) {
            throw new IllegalArgumentException("Mystcraft AgeUID must be >= 2: " + ageUid);
        }
    }
}
