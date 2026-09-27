package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.block.BlockBookDisplay;
import com.xcompwiz.mystcraft.blockentity.BookDisplayBlockEntity;
import com.xcompwiz.mystcraft.registry.MystBlocks;
import com.xcompwiz.mystcraft.treasure.MystcraftTreasureGenerator;
import com.xcompwiz.mystcraft.world.worldgen.AgeLegacySmallLibraryPlacementMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Direct port of the unconditional 0.13.7.06 Mystcraft Small Library scattered feature.
 *
 * <p>The 694 authored block placements below are mechanically converted from
 * ComponentScatteredFeatureSmallLibrary. Location spacing is the legacy 32/8 scattered
 * feature algorithm with salt 14357617 and no biome restriction. In 1.21 this runs at
 * the head of Mystcraft's direct post-decoration population batch; that is later than
 * the old MapGenStructure placement boundary, but preserves library-before-Mystcraft-
 * symbol-populators ordering inside the bridge.</p>
 */
public final class AgeLegacySmallLibraryFeature {
    private static final int SIZE = 11;
    private static final int[][] LECTERNS = {{6,2,2},{8,2,4},{8,2,5},{8,2,6},{6,2,8}};
    // 0.13.7.06 ComponentScatteredFeatureSmallLibrary#lecternTargs.
    // Each X/Z pair is the point the corresponding lectern was authored to face.
    private static final int[][] LECTERN_TARGETS = {{6,3},{7,4},{7,5},{7,6},{6,7}};

    private record Placement(byte x, byte y, byte z, char state) {}
    private static final Placement[] BLUEPRINT = new Placement[] {
        new Placement((byte)0, (byte)1, (byte)3, 'A'),
        new Placement((byte)0, (byte)1, (byte)4, 'A'),
        new Placement((byte)0, (byte)1, (byte)5, 'A'),
        new Placement((byte)0, (byte)1, (byte)6, 'A'),
        new Placement((byte)0, (byte)1, (byte)7, 'A'),
        new Placement((byte)0, (byte)2, (byte)3, 'A'),
        new Placement((byte)0, (byte)2, (byte)4, 'A'),
        new Placement((byte)0, (byte)2, (byte)5, 'A'),
        new Placement((byte)0, (byte)2, (byte)6, 'A'),
        new Placement((byte)0, (byte)2, (byte)7, 'A'),
        new Placement((byte)0, (byte)3, (byte)3, 'A'),
        new Placement((byte)0, (byte)3, (byte)4, 'A'),
        new Placement((byte)0, (byte)3, (byte)5, 'A'),
        new Placement((byte)0, (byte)3, (byte)6, 'A'),
        new Placement((byte)0, (byte)3, (byte)7, 'A'),
        new Placement((byte)1, (byte)1, (byte)3, 'A'),
        new Placement((byte)1, (byte)1, (byte)5, 'A'),
        new Placement((byte)1, (byte)1, (byte)7, 'A'),
        new Placement((byte)1, (byte)2, (byte)3, 'A'),
        new Placement((byte)1, (byte)2, (byte)5, 'A'),
        new Placement((byte)1, (byte)2, (byte)7, 'A'),
        new Placement((byte)1, (byte)3, (byte)3, 'A'),
        new Placement((byte)1, (byte)3, (byte)5, 'A'),
        new Placement((byte)1, (byte)3, (byte)7, 'A'),
        new Placement((byte)2, (byte)1, (byte)3, 'A'),
        new Placement((byte)2, (byte)1, (byte)4, 'A'),
        new Placement((byte)2, (byte)1, (byte)5, 'A'),
        new Placement((byte)2, (byte)1, (byte)6, 'A'),
        new Placement((byte)2, (byte)1, (byte)7, 'A'),
        new Placement((byte)2, (byte)2, (byte)3, 'A'),
        new Placement((byte)2, (byte)2, (byte)4, 'A'),
        new Placement((byte)2, (byte)2, (byte)5, 'A'),
        new Placement((byte)2, (byte)2, (byte)6, 'A'),
        new Placement((byte)2, (byte)2, (byte)7, 'A'),
        new Placement((byte)2, (byte)3, (byte)3, 'A'),
        new Placement((byte)2, (byte)3, (byte)4, 'A'),
        new Placement((byte)2, (byte)3, (byte)5, 'A'),
        new Placement((byte)2, (byte)3, (byte)6, 'A'),
        new Placement((byte)2, (byte)3, (byte)7, 'A'),
        new Placement((byte)3, (byte)1, (byte)5, 'A'),
        new Placement((byte)3, (byte)2, (byte)5, 'A'),
        new Placement((byte)4, (byte)1, (byte)4, 'A'),
        new Placement((byte)4, (byte)1, (byte)5, 'A'),
        new Placement((byte)4, (byte)1, (byte)6, 'A'),
        new Placement((byte)4, (byte)2, (byte)4, 'A'),
        new Placement((byte)4, (byte)2, (byte)5, 'A'),
        new Placement((byte)4, (byte)2, (byte)6, 'A'),
        new Placement((byte)4, (byte)3, (byte)4, 'A'),
        new Placement((byte)4, (byte)3, (byte)5, 'A'),
        new Placement((byte)4, (byte)3, (byte)6, 'A'),
        new Placement((byte)4, (byte)4, (byte)4, 'A'),
        new Placement((byte)4, (byte)4, (byte)5, 'A'),
        new Placement((byte)4, (byte)4, (byte)6, 'A'),
        new Placement((byte)4, (byte)5, (byte)3, 'A'),
        new Placement((byte)4, (byte)5, (byte)4, 'A'),
        new Placement((byte)4, (byte)5, (byte)5, 'A'),
        new Placement((byte)4, (byte)5, (byte)6, 'A'),
        new Placement((byte)4, (byte)5, (byte)7, 'A'),
        new Placement((byte)4, (byte)5, (byte)8, 'A'),
        new Placement((byte)4, (byte)6, (byte)7, 'A'),
        new Placement((byte)4, (byte)6, (byte)8, 'A'),
        new Placement((byte)5, (byte)1, (byte)3, 'A'),
        new Placement((byte)5, (byte)1, (byte)4, 'A'),
        new Placement((byte)5, (byte)1, (byte)5, 'A'),
        new Placement((byte)5, (byte)1, (byte)6, 'A'),
        new Placement((byte)5, (byte)1, (byte)7, 'A'),
        new Placement((byte)5, (byte)2, (byte)3, 'A'),
        new Placement((byte)5, (byte)2, (byte)4, 'A'),
        new Placement((byte)5, (byte)2, (byte)5, 'A'),
        new Placement((byte)5, (byte)2, (byte)6, 'A'),
        new Placement((byte)5, (byte)2, (byte)7, 'A'),
        new Placement((byte)5, (byte)3, (byte)3, 'A'),
        new Placement((byte)5, (byte)3, (byte)4, 'A'),
        new Placement((byte)5, (byte)3, (byte)5, 'A'),
        new Placement((byte)5, (byte)3, (byte)6, 'A'),
        new Placement((byte)5, (byte)3, (byte)7, 'A'),
        new Placement((byte)5, (byte)4, (byte)3, 'A'),
        new Placement((byte)5, (byte)4, (byte)4, 'A'),
        new Placement((byte)5, (byte)4, (byte)5, 'A'),
        new Placement((byte)5, (byte)4, (byte)6, 'A'),
        new Placement((byte)5, (byte)4, (byte)7, 'A'),
        new Placement((byte)5, (byte)5, (byte)3, 'A'),
        new Placement((byte)5, (byte)5, (byte)4, 'A'),
        new Placement((byte)5, (byte)5, (byte)5, 'A'),
        new Placement((byte)5, (byte)5, (byte)6, 'A'),
        new Placement((byte)5, (byte)5, (byte)7, 'A'),
        new Placement((byte)5, (byte)6, (byte)5, 'A'),
        new Placement((byte)5, (byte)6, (byte)6, 'A'),
        new Placement((byte)5, (byte)6, (byte)7, 'A'),
        new Placement((byte)5, (byte)6, (byte)8, 'A'),
        new Placement((byte)6, (byte)1, (byte)3, 'A'),
        new Placement((byte)6, (byte)1, (byte)4, 'A'),
        new Placement((byte)6, (byte)1, (byte)5, 'A'),
        new Placement((byte)6, (byte)1, (byte)6, 'A'),
        new Placement((byte)6, (byte)1, (byte)7, 'A'),
        new Placement((byte)6, (byte)2, (byte)3, 'A'),
        new Placement((byte)6, (byte)2, (byte)4, 'A'),
        new Placement((byte)6, (byte)2, (byte)5, 'A'),
        new Placement((byte)6, (byte)2, (byte)6, 'A'),
        new Placement((byte)6, (byte)2, (byte)7, 'A'),
        new Placement((byte)6, (byte)3, (byte)3, 'A'),
        new Placement((byte)6, (byte)3, (byte)4, 'A'),
        new Placement((byte)6, (byte)3, (byte)5, 'A'),
        new Placement((byte)6, (byte)3, (byte)6, 'A'),
        new Placement((byte)6, (byte)3, (byte)7, 'A'),
        new Placement((byte)6, (byte)4, (byte)3, 'A'),
        new Placement((byte)6, (byte)4, (byte)4, 'A'),
        new Placement((byte)6, (byte)4, (byte)5, 'A'),
        new Placement((byte)6, (byte)4, (byte)6, 'A'),
        new Placement((byte)6, (byte)4, (byte)7, 'A'),
        new Placement((byte)6, (byte)5, (byte)2, 'A'),
        new Placement((byte)6, (byte)5, (byte)3, 'A'),
        new Placement((byte)6, (byte)5, (byte)4, 'A'),
        new Placement((byte)6, (byte)5, (byte)5, 'A'),
        new Placement((byte)6, (byte)5, (byte)6, 'A'),
        new Placement((byte)6, (byte)5, (byte)7, 'A'),
        new Placement((byte)6, (byte)6, (byte)4, 'A'),
        new Placement((byte)6, (byte)6, (byte)5, 'A'),
        new Placement((byte)6, (byte)6, (byte)6, 'A'),
        new Placement((byte)7, (byte)1, (byte)3, 'A'),
        new Placement((byte)7, (byte)1, (byte)4, 'A'),
        new Placement((byte)7, (byte)1, (byte)5, 'A'),
        new Placement((byte)7, (byte)1, (byte)6, 'A'),
        new Placement((byte)7, (byte)1, (byte)7, 'A'),
        new Placement((byte)7, (byte)2, (byte)3, 'A'),
        new Placement((byte)7, (byte)2, (byte)4, 'A'),
        new Placement((byte)7, (byte)2, (byte)5, 'A'),
        new Placement((byte)7, (byte)2, (byte)6, 'A'),
        new Placement((byte)7, (byte)2, (byte)7, 'A'),
        new Placement((byte)7, (byte)3, (byte)3, 'A'),
        new Placement((byte)7, (byte)3, (byte)4, 'A'),
        new Placement((byte)7, (byte)3, (byte)5, 'A'),
        new Placement((byte)7, (byte)3, (byte)6, 'A'),
        new Placement((byte)7, (byte)3, (byte)7, 'A'),
        new Placement((byte)7, (byte)4, (byte)3, 'A'),
        new Placement((byte)7, (byte)4, (byte)4, 'A'),
        new Placement((byte)7, (byte)4, (byte)5, 'A'),
        new Placement((byte)7, (byte)4, (byte)6, 'A'),
        new Placement((byte)7, (byte)4, (byte)7, 'A'),
        new Placement((byte)7, (byte)5, (byte)2, 'A'),
        new Placement((byte)7, (byte)5, (byte)3, 'A'),
        new Placement((byte)7, (byte)5, (byte)4, 'A'),
        new Placement((byte)7, (byte)5, (byte)5, 'A'),
        new Placement((byte)7, (byte)5, (byte)6, 'A'),
        new Placement((byte)7, (byte)5, (byte)7, 'A'),
        new Placement((byte)7, (byte)6, (byte)3, 'A'),
        new Placement((byte)7, (byte)6, (byte)4, 'A'),
        new Placement((byte)7, (byte)6, (byte)5, 'A'),
        new Placement((byte)7, (byte)6, (byte)6, 'A'),
        new Placement((byte)7, (byte)6, (byte)7, 'A'),
        new Placement((byte)8, (byte)5, (byte)7, 'A'),
        new Placement((byte)8, (byte)6, (byte)2, 'A'),
        new Placement((byte)8, (byte)6, (byte)4, 'A'),
        new Placement((byte)8, (byte)6, (byte)5, 'A'),
        new Placement((byte)8, (byte)6, (byte)7, 'A'),
        new Placement((byte)8, (byte)6, (byte)8, 'A'),
        new Placement((byte)4, (byte)3, (byte)7, 'B'),
        new Placement((byte)4, (byte)1, (byte)3, 'B'),
        new Placement((byte)4, (byte)1, (byte)7, 'B'),
        new Placement((byte)4, (byte)2, (byte)3, 'B'),
        new Placement((byte)4, (byte)2, (byte)7, 'B'),
        new Placement((byte)4, (byte)3, (byte)3, 'B'),
        new Placement((byte)4, (byte)4, (byte)3, 'B'),
        new Placement((byte)4, (byte)4, (byte)7, 'B'),
        new Placement((byte)5, (byte)1, (byte)2, 'B'),
        new Placement((byte)5, (byte)1, (byte)8, 'B'),
        new Placement((byte)5, (byte)2, (byte)2, 'B'),
        new Placement((byte)5, (byte)2, (byte)8, 'B'),
        new Placement((byte)5, (byte)3, (byte)2, 'B'),
        new Placement((byte)5, (byte)3, (byte)8, 'B'),
        new Placement((byte)5, (byte)4, (byte)2, 'B'),
        new Placement((byte)5, (byte)4, (byte)8, 'B'),
        new Placement((byte)6, (byte)1, (byte)2, 'B'),
        new Placement((byte)6, (byte)1, (byte)8, 'B'),
        new Placement((byte)6, (byte)3, (byte)2, 'B'),
        new Placement((byte)6, (byte)3, (byte)8, 'B'),
        new Placement((byte)6, (byte)4, (byte)2, 'B'),
        new Placement((byte)6, (byte)4, (byte)8, 'B'),
        new Placement((byte)7, (byte)1, (byte)2, 'B'),
        new Placement((byte)7, (byte)1, (byte)8, 'B'),
        new Placement((byte)7, (byte)2, (byte)2, 'B'),
        new Placement((byte)7, (byte)2, (byte)8, 'B'),
        new Placement((byte)7, (byte)3, (byte)2, 'B'),
        new Placement((byte)7, (byte)3, (byte)8, 'B'),
        new Placement((byte)7, (byte)4, (byte)2, 'B'),
        new Placement((byte)7, (byte)4, (byte)8, 'B'),
        new Placement((byte)8, (byte)1, (byte)3, 'B'),
        new Placement((byte)8, (byte)1, (byte)4, 'B'),
        new Placement((byte)8, (byte)1, (byte)5, 'B'),
        new Placement((byte)8, (byte)1, (byte)6, 'B'),
        new Placement((byte)8, (byte)1, (byte)7, 'B'),
        new Placement((byte)8, (byte)2, (byte)3, 'B'),
        new Placement((byte)8, (byte)2, (byte)7, 'B'),
        new Placement((byte)8, (byte)3, (byte)3, 'B'),
        new Placement((byte)8, (byte)3, (byte)4, 'B'),
        new Placement((byte)8, (byte)3, (byte)5, 'B'),
        new Placement((byte)8, (byte)3, (byte)6, 'B'),
        new Placement((byte)8, (byte)3, (byte)7, 'B'),
        new Placement((byte)8, (byte)4, (byte)3, 'B'),
        new Placement((byte)8, (byte)4, (byte)4, 'B'),
        new Placement((byte)8, (byte)4, (byte)5, 'B'),
        new Placement((byte)8, (byte)4, (byte)6, 'B'),
        new Placement((byte)8, (byte)4, (byte)7, 'B'),
        new Placement((byte)1, (byte)0, (byte)4, 'C'),
        new Placement((byte)1, (byte)0, (byte)5, 'C'),
        new Placement((byte)1, (byte)0, (byte)6, 'C'),
        new Placement((byte)2, (byte)0, (byte)4, 'C'),
        new Placement((byte)2, (byte)0, (byte)5, 'C'),
        new Placement((byte)2, (byte)0, (byte)6, 'C'),
        new Placement((byte)2, (byte)4, (byte)4, 'C'),
        new Placement((byte)2, (byte)4, (byte)5, 'C'),
        new Placement((byte)2, (byte)4, (byte)6, 'C'),
        new Placement((byte)2, (byte)5, (byte)5, 'C'),
        new Placement((byte)3, (byte)0, (byte)1, 'C'),
        new Placement((byte)3, (byte)0, (byte)2, 'C'),
        new Placement((byte)3, (byte)0, (byte)3, 'C'),
        new Placement((byte)3, (byte)0, (byte)4, 'C'),
        new Placement((byte)3, (byte)0, (byte)5, 'C'),
        new Placement((byte)3, (byte)0, (byte)6, 'C'),
        new Placement((byte)3, (byte)0, (byte)7, 'C'),
        new Placement((byte)3, (byte)0, (byte)8, 'C'),
        new Placement((byte)3, (byte)0, (byte)9, 'C'),
        new Placement((byte)3, (byte)1, (byte)1, 'C'),
        new Placement((byte)3, (byte)1, (byte)2, 'C'),
        new Placement((byte)3, (byte)1, (byte)3, 'C'),
        new Placement((byte)3, (byte)1, (byte)4, 'C'),
        new Placement((byte)3, (byte)1, (byte)6, 'C'),
        new Placement((byte)3, (byte)1, (byte)7, 'C'),
        new Placement((byte)3, (byte)1, (byte)8, 'C'),
        new Placement((byte)3, (byte)1, (byte)9, 'C'),
        new Placement((byte)3, (byte)2, (byte)1, 'C'),
        new Placement((byte)3, (byte)2, (byte)2, 'C'),
        new Placement((byte)3, (byte)2, (byte)3, 'C'),
        new Placement((byte)3, (byte)2, (byte)4, 'C'),
        new Placement((byte)3, (byte)2, (byte)6, 'C'),
        new Placement((byte)3, (byte)2, (byte)7, 'C'),
        new Placement((byte)3, (byte)2, (byte)8, 'C'),
        new Placement((byte)3, (byte)2, (byte)9, 'C'),
        new Placement((byte)3, (byte)3, (byte)1, 'C'),
        new Placement((byte)3, (byte)3, (byte)2, 'C'),
        new Placement((byte)3, (byte)3, (byte)3, 'C'),
        new Placement((byte)3, (byte)3, (byte)4, 'C'),
        new Placement((byte)3, (byte)3, (byte)6, 'C'),
        new Placement((byte)3, (byte)3, (byte)7, 'C'),
        new Placement((byte)3, (byte)3, (byte)8, 'C'),
        new Placement((byte)3, (byte)3, (byte)9, 'C'),
        new Placement((byte)3, (byte)4, (byte)1, 'C'),
        new Placement((byte)3, (byte)4, (byte)2, 'C'),
        new Placement((byte)3, (byte)4, (byte)3, 'C'),
        new Placement((byte)3, (byte)4, (byte)4, 'C'),
        new Placement((byte)3, (byte)4, (byte)5, 'C'),
        new Placement((byte)3, (byte)4, (byte)6, 'C'),
        new Placement((byte)3, (byte)4, (byte)7, 'C'),
        new Placement((byte)3, (byte)4, (byte)8, 'C'),
        new Placement((byte)3, (byte)4, (byte)9, 'C'),
        new Placement((byte)3, (byte)5, (byte)1, 'C'),
        new Placement((byte)3, (byte)5, (byte)2, 'C'),
        new Placement((byte)3, (byte)5, (byte)3, 'C'),
        new Placement((byte)3, (byte)5, (byte)4, 'C'),
        new Placement((byte)3, (byte)5, (byte)5, 'C'),
        new Placement((byte)3, (byte)5, (byte)6, 'C'),
        new Placement((byte)3, (byte)5, (byte)7, 'C'),
        new Placement((byte)3, (byte)5, (byte)8, 'C'),
        new Placement((byte)3, (byte)5, (byte)9, 'C'),
        new Placement((byte)3, (byte)6, (byte)1, 'C'),
        new Placement((byte)3, (byte)6, (byte)2, 'C'),
        new Placement((byte)3, (byte)6, (byte)3, 'C'),
        new Placement((byte)3, (byte)6, (byte)4, 'C'),
        new Placement((byte)3, (byte)6, (byte)5, 'C'),
        new Placement((byte)3, (byte)6, (byte)6, 'C'),
        new Placement((byte)3, (byte)6, (byte)7, 'C'),
        new Placement((byte)3, (byte)6, (byte)8, 'C'),
        new Placement((byte)3, (byte)6, (byte)9, 'C'),
        new Placement((byte)3, (byte)7, (byte)1, 'C'),
        new Placement((byte)3, (byte)7, (byte)2, 'C'),
        new Placement((byte)3, (byte)7, (byte)3, 'C'),
        new Placement((byte)3, (byte)7, (byte)4, 'C'),
        new Placement((byte)3, (byte)7, (byte)5, 'C'),
        new Placement((byte)3, (byte)7, (byte)6, 'C'),
        new Placement((byte)3, (byte)7, (byte)7, 'C'),
        new Placement((byte)3, (byte)7, (byte)8, 'C'),
        new Placement((byte)3, (byte)7, (byte)9, 'C'),
        new Placement((byte)4, (byte)0, (byte)1, 'C'),
        new Placement((byte)4, (byte)0, (byte)2, 'C'),
        new Placement((byte)4, (byte)0, (byte)3, 'C'),
        new Placement((byte)4, (byte)0, (byte)4, 'C'),
        new Placement((byte)4, (byte)0, (byte)5, 'C'),
        new Placement((byte)4, (byte)0, (byte)6, 'C'),
        new Placement((byte)4, (byte)0, (byte)7, 'C'),
        new Placement((byte)4, (byte)0, (byte)8, 'C'),
        new Placement((byte)4, (byte)0, (byte)9, 'C'),
        new Placement((byte)4, (byte)1, (byte)1, 'C'),
        new Placement((byte)4, (byte)1, (byte)9, 'C'),
        new Placement((byte)4, (byte)2, (byte)1, 'C'),
        new Placement((byte)4, (byte)2, (byte)9, 'C'),
        new Placement((byte)4, (byte)3, (byte)1, 'C'),
        new Placement((byte)4, (byte)3, (byte)9, 'C'),
        new Placement((byte)4, (byte)4, (byte)1, 'C'),
        new Placement((byte)4, (byte)4, (byte)9, 'C'),
        new Placement((byte)4, (byte)5, (byte)1, 'C'),
        new Placement((byte)4, (byte)5, (byte)9, 'C'),
        new Placement((byte)4, (byte)6, (byte)1, 'C'),
        new Placement((byte)4, (byte)6, (byte)9, 'C'),
        new Placement((byte)4, (byte)7, (byte)1, 'C'),
        new Placement((byte)4, (byte)7, (byte)2, 'C'),
        new Placement((byte)4, (byte)7, (byte)3, 'C'),
        new Placement((byte)4, (byte)7, (byte)4, 'C'),
        new Placement((byte)4, (byte)7, (byte)5, 'C'),
        new Placement((byte)4, (byte)7, (byte)6, 'C'),
        new Placement((byte)4, (byte)7, (byte)7, 'C'),
        new Placement((byte)4, (byte)7, (byte)8, 'C'),
        new Placement((byte)4, (byte)7, (byte)9, 'C'),
        new Placement((byte)4, (byte)8, (byte)4, 'C'),
        new Placement((byte)4, (byte)8, (byte)5, 'C'),
        new Placement((byte)4, (byte)8, (byte)6, 'C'),
        new Placement((byte)5, (byte)0, (byte)1, 'C'),
        new Placement((byte)5, (byte)0, (byte)2, 'C'),
        new Placement((byte)5, (byte)0, (byte)3, 'C'),
        new Placement((byte)5, (byte)0, (byte)4, 'C'),
        new Placement((byte)5, (byte)0, (byte)5, 'C'),
        new Placement((byte)5, (byte)0, (byte)6, 'C'),
        new Placement((byte)5, (byte)0, (byte)7, 'C'),
        new Placement((byte)5, (byte)0, (byte)8, 'C'),
        new Placement((byte)5, (byte)0, (byte)9, 'C'),
        new Placement((byte)5, (byte)1, (byte)1, 'C'),
        new Placement((byte)5, (byte)1, (byte)9, 'C'),
        new Placement((byte)5, (byte)2, (byte)1, 'C'),
        new Placement((byte)5, (byte)2, (byte)9, 'C'),
        new Placement((byte)5, (byte)3, (byte)1, 'C'),
        new Placement((byte)5, (byte)3, (byte)9, 'C'),
        new Placement((byte)5, (byte)4, (byte)1, 'C'),
        new Placement((byte)5, (byte)4, (byte)9, 'C'),
        new Placement((byte)5, (byte)5, (byte)1, 'C'),
        new Placement((byte)5, (byte)5, (byte)9, 'C'),
        new Placement((byte)5, (byte)6, (byte)1, 'C'),
        new Placement((byte)5, (byte)6, (byte)9, 'C'),
        new Placement((byte)5, (byte)7, (byte)1, 'C'),
        new Placement((byte)5, (byte)7, (byte)2, 'C'),
        new Placement((byte)5, (byte)7, (byte)3, 'C'),
        new Placement((byte)5, (byte)7, (byte)4, 'C'),
        new Placement((byte)5, (byte)7, (byte)5, 'C'),
        new Placement((byte)5, (byte)7, (byte)6, 'C'),
        new Placement((byte)5, (byte)7, (byte)7, 'C'),
        new Placement((byte)5, (byte)7, (byte)8, 'C'),
        new Placement((byte)5, (byte)7, (byte)9, 'C'),
        new Placement((byte)5, (byte)8, (byte)4, 'C'),
        new Placement((byte)5, (byte)8, (byte)5, 'C'),
        new Placement((byte)5, (byte)8, (byte)6, 'C'),
        new Placement((byte)5, (byte)9, (byte)5, 'C'),
        new Placement((byte)6, (byte)0, (byte)1, 'C'),
        new Placement((byte)6, (byte)0, (byte)2, 'C'),
        new Placement((byte)6, (byte)0, (byte)3, 'C'),
        new Placement((byte)6, (byte)0, (byte)4, 'C'),
        new Placement((byte)6, (byte)0, (byte)5, 'C'),
        new Placement((byte)6, (byte)0, (byte)6, 'C'),
        new Placement((byte)6, (byte)0, (byte)7, 'C'),
        new Placement((byte)6, (byte)0, (byte)8, 'C'),
        new Placement((byte)6, (byte)0, (byte)9, 'C'),
        new Placement((byte)6, (byte)1, (byte)1, 'C'),
        new Placement((byte)6, (byte)1, (byte)9, 'C'),
        new Placement((byte)6, (byte)2, (byte)1, 'C'),
        new Placement((byte)6, (byte)2, (byte)9, 'C'),
        new Placement((byte)6, (byte)3, (byte)1, 'C'),
        new Placement((byte)6, (byte)3, (byte)9, 'C'),
        new Placement((byte)6, (byte)4, (byte)1, 'C'),
        new Placement((byte)6, (byte)4, (byte)9, 'C'),
        new Placement((byte)6, (byte)5, (byte)1, 'C'),
        new Placement((byte)6, (byte)5, (byte)9, 'C'),
        new Placement((byte)6, (byte)6, (byte)1, 'C'),
        new Placement((byte)6, (byte)6, (byte)9, 'C'),
        new Placement((byte)6, (byte)7, (byte)1, 'C'),
        new Placement((byte)6, (byte)7, (byte)2, 'C'),
        new Placement((byte)6, (byte)7, (byte)3, 'C'),
        new Placement((byte)6, (byte)7, (byte)4, 'C'),
        new Placement((byte)6, (byte)7, (byte)5, 'C'),
        new Placement((byte)6, (byte)7, (byte)6, 'C'),
        new Placement((byte)6, (byte)7, (byte)7, 'C'),
        new Placement((byte)6, (byte)7, (byte)8, 'C'),
        new Placement((byte)6, (byte)7, (byte)9, 'C'),
        new Placement((byte)6, (byte)8, (byte)4, 'C'),
        new Placement((byte)6, (byte)8, (byte)5, 'C'),
        new Placement((byte)6, (byte)8, (byte)6, 'C'),
        new Placement((byte)6, (byte)9, (byte)5, 'C'),
        new Placement((byte)7, (byte)0, (byte)1, 'C'),
        new Placement((byte)7, (byte)0, (byte)2, 'C'),
        new Placement((byte)7, (byte)0, (byte)3, 'C'),
        new Placement((byte)7, (byte)0, (byte)4, 'C'),
        new Placement((byte)7, (byte)0, (byte)5, 'C'),
        new Placement((byte)7, (byte)0, (byte)6, 'C'),
        new Placement((byte)7, (byte)0, (byte)7, 'C'),
        new Placement((byte)7, (byte)0, (byte)8, 'C'),
        new Placement((byte)7, (byte)0, (byte)9, 'C'),
        new Placement((byte)7, (byte)1, (byte)1, 'C'),
        new Placement((byte)7, (byte)1, (byte)9, 'C'),
        new Placement((byte)7, (byte)2, (byte)1, 'C'),
        new Placement((byte)7, (byte)2, (byte)9, 'C'),
        new Placement((byte)7, (byte)3, (byte)1, 'C'),
        new Placement((byte)7, (byte)3, (byte)9, 'C'),
        new Placement((byte)7, (byte)4, (byte)1, 'C'),
        new Placement((byte)7, (byte)4, (byte)9, 'C'),
        new Placement((byte)7, (byte)5, (byte)1, 'C'),
        new Placement((byte)7, (byte)5, (byte)9, 'C'),
        new Placement((byte)7, (byte)6, (byte)1, 'C'),
        new Placement((byte)7, (byte)7, (byte)1, 'C'),
        new Placement((byte)7, (byte)7, (byte)2, 'C'),
        new Placement((byte)7, (byte)7, (byte)3, 'C'),
        new Placement((byte)7, (byte)7, (byte)4, 'C'),
        new Placement((byte)7, (byte)7, (byte)5, 'C'),
        new Placement((byte)7, (byte)7, (byte)6, 'C'),
        new Placement((byte)7, (byte)7, (byte)7, 'C'),
        new Placement((byte)7, (byte)7, (byte)8, 'C'),
        new Placement((byte)7, (byte)7, (byte)9, 'C'),
        new Placement((byte)7, (byte)8, (byte)4, 'C'),
        new Placement((byte)7, (byte)8, (byte)5, 'C'),
        new Placement((byte)7, (byte)8, (byte)6, 'C'),
        new Placement((byte)7, (byte)9, (byte)5, 'C'),
        new Placement((byte)8, (byte)0, (byte)1, 'C'),
        new Placement((byte)8, (byte)0, (byte)2, 'C'),
        new Placement((byte)8, (byte)0, (byte)3, 'C'),
        new Placement((byte)8, (byte)0, (byte)4, 'C'),
        new Placement((byte)8, (byte)0, (byte)5, 'C'),
        new Placement((byte)8, (byte)0, (byte)6, 'C'),
        new Placement((byte)8, (byte)0, (byte)7, 'C'),
        new Placement((byte)8, (byte)0, (byte)8, 'C'),
        new Placement((byte)8, (byte)0, (byte)9, 'C'),
        new Placement((byte)8, (byte)1, (byte)1, 'C'),
        new Placement((byte)8, (byte)1, (byte)9, 'C'),
        new Placement((byte)8, (byte)2, (byte)1, 'C'),
        new Placement((byte)8, (byte)2, (byte)9, 'C'),
        new Placement((byte)8, (byte)3, (byte)1, 'C'),
        new Placement((byte)8, (byte)3, (byte)9, 'C'),
        new Placement((byte)8, (byte)4, (byte)1, 'C'),
        new Placement((byte)8, (byte)4, (byte)9, 'C'),
        new Placement((byte)8, (byte)5, (byte)1, 'C'),
        new Placement((byte)8, (byte)5, (byte)9, 'C'),
        new Placement((byte)8, (byte)6, (byte)1, 'C'),
        new Placement((byte)8, (byte)6, (byte)9, 'C'),
        new Placement((byte)8, (byte)7, (byte)1, 'C'),
        new Placement((byte)8, (byte)7, (byte)2, 'C'),
        new Placement((byte)8, (byte)7, (byte)3, 'C'),
        new Placement((byte)8, (byte)7, (byte)4, 'C'),
        new Placement((byte)8, (byte)7, (byte)5, 'C'),
        new Placement((byte)8, (byte)7, (byte)6, 'C'),
        new Placement((byte)8, (byte)7, (byte)7, 'C'),
        new Placement((byte)8, (byte)7, (byte)8, 'C'),
        new Placement((byte)8, (byte)7, (byte)9, 'C'),
        new Placement((byte)8, (byte)8, (byte)4, 'C'),
        new Placement((byte)8, (byte)8, (byte)5, 'C'),
        new Placement((byte)8, (byte)8, (byte)6, 'C'),
        new Placement((byte)9, (byte)0, (byte)1, 'C'),
        new Placement((byte)9, (byte)0, (byte)2, 'C'),
        new Placement((byte)9, (byte)0, (byte)3, 'C'),
        new Placement((byte)9, (byte)0, (byte)4, 'C'),
        new Placement((byte)9, (byte)0, (byte)5, 'C'),
        new Placement((byte)9, (byte)0, (byte)6, 'C'),
        new Placement((byte)9, (byte)0, (byte)7, 'C'),
        new Placement((byte)9, (byte)0, (byte)8, 'C'),
        new Placement((byte)9, (byte)0, (byte)9, 'C'),
        new Placement((byte)9, (byte)1, (byte)1, 'C'),
        new Placement((byte)9, (byte)1, (byte)2, 'C'),
        new Placement((byte)9, (byte)1, (byte)3, 'C'),
        new Placement((byte)9, (byte)1, (byte)4, 'C'),
        new Placement((byte)9, (byte)1, (byte)5, 'C'),
        new Placement((byte)9, (byte)1, (byte)6, 'C'),
        new Placement((byte)9, (byte)1, (byte)7, 'C'),
        new Placement((byte)9, (byte)1, (byte)8, 'C'),
        new Placement((byte)9, (byte)1, (byte)9, 'C'),
        new Placement((byte)9, (byte)2, (byte)1, 'C'),
        new Placement((byte)9, (byte)2, (byte)2, 'C'),
        new Placement((byte)9, (byte)2, (byte)3, 'C'),
        new Placement((byte)9, (byte)2, (byte)4, 'C'),
        new Placement((byte)9, (byte)2, (byte)5, 'C'),
        new Placement((byte)9, (byte)2, (byte)6, 'C'),
        new Placement((byte)9, (byte)2, (byte)7, 'C'),
        new Placement((byte)9, (byte)2, (byte)8, 'C'),
        new Placement((byte)9, (byte)2, (byte)9, 'C'),
        new Placement((byte)9, (byte)3, (byte)1, 'C'),
        new Placement((byte)9, (byte)3, (byte)2, 'C'),
        new Placement((byte)9, (byte)3, (byte)3, 'C'),
        new Placement((byte)9, (byte)3, (byte)4, 'C'),
        new Placement((byte)9, (byte)3, (byte)5, 'C'),
        new Placement((byte)9, (byte)3, (byte)6, 'C'),
        new Placement((byte)9, (byte)3, (byte)7, 'C'),
        new Placement((byte)9, (byte)3, (byte)8, 'C'),
        new Placement((byte)9, (byte)3, (byte)9, 'C'),
        new Placement((byte)9, (byte)4, (byte)1, 'C'),
        new Placement((byte)9, (byte)4, (byte)2, 'C'),
        new Placement((byte)9, (byte)4, (byte)3, 'C'),
        new Placement((byte)9, (byte)4, (byte)4, 'C'),
        new Placement((byte)9, (byte)4, (byte)5, 'C'),
        new Placement((byte)9, (byte)4, (byte)6, 'C'),
        new Placement((byte)9, (byte)4, (byte)7, 'C'),
        new Placement((byte)9, (byte)4, (byte)8, 'C'),
        new Placement((byte)9, (byte)4, (byte)9, 'C'),
        new Placement((byte)9, (byte)5, (byte)1, 'C'),
        new Placement((byte)9, (byte)5, (byte)2, 'C'),
        new Placement((byte)9, (byte)5, (byte)3, 'C'),
        new Placement((byte)9, (byte)5, (byte)4, 'C'),
        new Placement((byte)9, (byte)5, (byte)5, 'C'),
        new Placement((byte)9, (byte)5, (byte)6, 'C'),
        new Placement((byte)9, (byte)5, (byte)7, 'C'),
        new Placement((byte)9, (byte)5, (byte)8, 'C'),
        new Placement((byte)9, (byte)5, (byte)9, 'C'),
        new Placement((byte)9, (byte)6, (byte)1, 'C'),
        new Placement((byte)9, (byte)6, (byte)2, 'C'),
        new Placement((byte)9, (byte)6, (byte)3, 'C'),
        new Placement((byte)9, (byte)6, (byte)4, 'C'),
        new Placement((byte)9, (byte)6, (byte)5, 'C'),
        new Placement((byte)9, (byte)6, (byte)6, 'C'),
        new Placement((byte)9, (byte)6, (byte)7, 'C'),
        new Placement((byte)9, (byte)6, (byte)8, 'C'),
        new Placement((byte)9, (byte)6, (byte)9, 'C'),
        new Placement((byte)9, (byte)7, (byte)1, 'C'),
        new Placement((byte)9, (byte)7, (byte)2, 'C'),
        new Placement((byte)9, (byte)7, (byte)3, 'C'),
        new Placement((byte)9, (byte)7, (byte)4, 'C'),
        new Placement((byte)9, (byte)7, (byte)5, 'C'),
        new Placement((byte)9, (byte)7, (byte)6, 'C'),
        new Placement((byte)9, (byte)7, (byte)7, 'C'),
        new Placement((byte)9, (byte)7, (byte)8, 'C'),
        new Placement((byte)9, (byte)7, (byte)9, 'C'),
        new Placement((byte)7, (byte)6, (byte)9, 'C'),
        new Placement((byte)4, (byte)1, (byte)8, 'P'),
        new Placement((byte)4, (byte)2, (byte)2, 'P'),
        new Placement((byte)4, (byte)2, (byte)8, 'P'),
        new Placement((byte)4, (byte)3, (byte)2, 'P'),
        new Placement((byte)4, (byte)3, (byte)8, 'P'),
        new Placement((byte)4, (byte)4, (byte)2, 'P'),
        new Placement((byte)4, (byte)4, (byte)8, 'P'),
        new Placement((byte)8, (byte)1, (byte)2, 'P'),
        new Placement((byte)8, (byte)1, (byte)8, 'P'),
        new Placement((byte)8, (byte)2, (byte)2, 'P'),
        new Placement((byte)8, (byte)2, (byte)8, 'P'),
        new Placement((byte)8, (byte)3, (byte)2, 'P'),
        new Placement((byte)8, (byte)3, (byte)8, 'P'),
        new Placement((byte)8, (byte)4, (byte)2, 'P'),
        new Placement((byte)8, (byte)4, (byte)8, 'P'),
        new Placement((byte)0, (byte)0, (byte)4, 'E'),
        new Placement((byte)0, (byte)0, (byte)5, 'E'),
        new Placement((byte)0, (byte)0, (byte)6, 'E'),
        new Placement((byte)2, (byte)0, (byte)0, 'E'),
        new Placement((byte)2, (byte)0, (byte)1, 'E'),
        new Placement((byte)2, (byte)0, (byte)2, 'E'),
        new Placement((byte)2, (byte)0, (byte)3, 'E'),
        new Placement((byte)2, (byte)0, (byte)8, 'E'),
        new Placement((byte)2, (byte)0, (byte)9, 'E'),
        new Placement((byte)3, (byte)8, (byte)4, 'E'),
        new Placement((byte)3, (byte)8, (byte)5, 'E'),
        new Placement((byte)3, (byte)8, (byte)6, 'E'),
        new Placement((byte)4, (byte)9, (byte)4, 'E'),
        new Placement((byte)4, (byte)9, (byte)5, 'E'),
        new Placement((byte)4, (byte)9, (byte)6, 'E'),
        new Placement((byte)8, (byte)9, (byte)4, 'Q'),
        new Placement((byte)8, (byte)9, (byte)5, 'Q'),
        new Placement((byte)9, (byte)8, (byte)4, 'Q'),
        new Placement((byte)9, (byte)8, (byte)5, 'Q'),
        new Placement((byte)9, (byte)8, (byte)6, 'Q'),
        new Placement((byte)9, (byte)8, (byte)7, 'Q'),
        new Placement((byte)10, (byte)0, (byte)1, 'Q'),
        new Placement((byte)10, (byte)0, (byte)2, 'Q'),
        new Placement((byte)10, (byte)0, (byte)3, 'Q'),
        new Placement((byte)10, (byte)0, (byte)4, 'Q'),
        new Placement((byte)10, (byte)0, (byte)5, 'Q'),
        new Placement((byte)10, (byte)0, (byte)6, 'Q'),
        new Placement((byte)10, (byte)0, (byte)7, 'Q'),
        new Placement((byte)10, (byte)0, (byte)8, 'Q'),
        new Placement((byte)10, (byte)0, (byte)9, 'Q'),
        new Placement((byte)10, (byte)0, (byte)10, 'Q'),
        new Placement((byte)0, (byte)0, (byte)3, 'N'),
        new Placement((byte)1, (byte)0, (byte)3, 'N'),
        new Placement((byte)1, (byte)4, (byte)3, 'N'),
        new Placement((byte)1, (byte)5, (byte)4, 'N'),
        new Placement((byte)2, (byte)4, (byte)3, 'N'),
        new Placement((byte)2, (byte)5, (byte)4, 'N'),
        new Placement((byte)3, (byte)0, (byte)0, 'N'),
        new Placement((byte)3, (byte)8, (byte)3, 'N'),
        new Placement((byte)4, (byte)0, (byte)0, 'N'),
        new Placement((byte)4, (byte)8, (byte)3, 'N'),
        new Placement((byte)5, (byte)0, (byte)0, 'N'),
        new Placement((byte)5, (byte)8, (byte)3, 'N'),
        new Placement((byte)5, (byte)9, (byte)4, 'N'),
        new Placement((byte)6, (byte)0, (byte)0, 'N'),
        new Placement((byte)6, (byte)8, (byte)3, 'N'),
        new Placement((byte)6, (byte)9, (byte)4, 'N'),
        new Placement((byte)7, (byte)0, (byte)0, 'N'),
        new Placement((byte)7, (byte)8, (byte)3, 'N'),
        new Placement((byte)7, (byte)9, (byte)4, 'N'),
        new Placement((byte)8, (byte)0, (byte)0, 'N'),
        new Placement((byte)8, (byte)8, (byte)3, 'N'),
        new Placement((byte)9, (byte)0, (byte)0, 'N'),
        new Placement((byte)9, (byte)8, (byte)3, 'N'),
        new Placement((byte)10, (byte)0, (byte)0, 'N'),
        new Placement((byte)0, (byte)0, (byte)7, 'S'),
        new Placement((byte)1, (byte)0, (byte)7, 'S'),
        new Placement((byte)1, (byte)4, (byte)7, 'S'),
        new Placement((byte)1, (byte)5, (byte)6, 'S'),
        new Placement((byte)2, (byte)0, (byte)7, 'S'),
        new Placement((byte)2, (byte)0, (byte)10, 'S'),
        new Placement((byte)2, (byte)4, (byte)7, 'S'),
        new Placement((byte)2, (byte)5, (byte)6, 'S'),
        new Placement((byte)3, (byte)0, (byte)10, 'S'),
        new Placement((byte)3, (byte)8, (byte)7, 'S'),
        new Placement((byte)4, (byte)0, (byte)10, 'S'),
        new Placement((byte)4, (byte)8, (byte)7, 'S'),
        new Placement((byte)5, (byte)0, (byte)10, 'S'),
        new Placement((byte)5, (byte)8, (byte)7, 'S'),
        new Placement((byte)5, (byte)9, (byte)6, 'S'),
        new Placement((byte)6, (byte)0, (byte)10, 'S'),
        new Placement((byte)6, (byte)8, (byte)7, 'S'),
        new Placement((byte)6, (byte)9, (byte)6, 'S'),
        new Placement((byte)7, (byte)0, (byte)10, 'S'),
        new Placement((byte)7, (byte)8, (byte)7, 'S'),
        new Placement((byte)7, (byte)9, (byte)6, 'S'),
        new Placement((byte)8, (byte)0, (byte)10, 'S'),
        new Placement((byte)8, (byte)8, (byte)7, 'S'),
        new Placement((byte)8, (byte)9, (byte)6, 'S'),
        new Placement((byte)9, (byte)0, (byte)10, 'S'),
        new Placement((byte)2, (byte)7, (byte)1, 'e'),
        new Placement((byte)2, (byte)7, (byte)2, 'e'),
        new Placement((byte)2, (byte)7, (byte)3, 'e'),
        new Placement((byte)2, (byte)7, (byte)4, 'e'),
        new Placement((byte)2, (byte)7, (byte)5, 'e'),
        new Placement((byte)2, (byte)7, (byte)6, 'e'),
        new Placement((byte)2, (byte)7, (byte)7, 'e'),
        new Placement((byte)2, (byte)7, (byte)8, 'e'),
        new Placement((byte)2, (byte)7, (byte)9, 'e'),
        new Placement((byte)2, (byte)7, (byte)10, 'e'),
        new Placement((byte)10, (byte)7, (byte)0, 'q'),
        new Placement((byte)10, (byte)7, (byte)1, 'q'),
        new Placement((byte)10, (byte)7, (byte)2, 'q'),
        new Placement((byte)10, (byte)7, (byte)3, 'q'),
        new Placement((byte)10, (byte)7, (byte)4, 'q'),
        new Placement((byte)10, (byte)7, (byte)5, 'q'),
        new Placement((byte)10, (byte)7, (byte)6, 'q'),
        new Placement((byte)10, (byte)7, (byte)7, 'q'),
        new Placement((byte)10, (byte)7, (byte)8, 'q'),
        new Placement((byte)10, (byte)7, (byte)9, 'q'),
        new Placement((byte)2, (byte)7, (byte)0, 'n'),
        new Placement((byte)3, (byte)7, (byte)0, 'n'),
        new Placement((byte)4, (byte)7, (byte)0, 'n'),
        new Placement((byte)5, (byte)7, (byte)0, 'n'),
        new Placement((byte)6, (byte)7, (byte)0, 'n'),
        new Placement((byte)7, (byte)7, (byte)0, 'n'),
        new Placement((byte)8, (byte)7, (byte)0, 'n'),
        new Placement((byte)9, (byte)7, (byte)0, 'n'),
        new Placement((byte)3, (byte)7, (byte)10, 's'),
        new Placement((byte)4, (byte)7, (byte)10, 's'),
        new Placement((byte)5, (byte)7, (byte)10, 's'),
        new Placement((byte)6, (byte)7, (byte)10, 's'),
        new Placement((byte)7, (byte)7, (byte)10, 's'),
        new Placement((byte)8, (byte)7, (byte)10, 's'),
        new Placement((byte)9, (byte)7, (byte)10, 's'),
        new Placement((byte)10, (byte)7, (byte)10, 's'),
        new Placement((byte)1, (byte)4, (byte)4, 'L'),
        new Placement((byte)1, (byte)4, (byte)5, 'L'),
        new Placement((byte)1, (byte)4, (byte)6, 'L'),
        new Placement((byte)1, (byte)5, (byte)5, 'U'),
        new Placement((byte)3, (byte)3, (byte)5, 'U'),
        new Placement((byte)5, (byte)10, (byte)5, 'L'),
        new Placement((byte)6, (byte)10, (byte)5, 'L'),
        new Placement((byte)7, (byte)10, (byte)5, 'L'),
        new Placement((byte)4, (byte)5, (byte)2, 'X'),
        new Placement((byte)4, (byte)6, (byte)2, 'X'),
        new Placement((byte)4, (byte)6, (byte)3, 'X'),
        new Placement((byte)4, (byte)6, (byte)4, 'X'),
        new Placement((byte)4, (byte)6, (byte)5, 'X'),
        new Placement((byte)4, (byte)6, (byte)6, 'X'),
        new Placement((byte)5, (byte)5, (byte)2, 'X'),
        new Placement((byte)5, (byte)5, (byte)8, 'X'),
        new Placement((byte)5, (byte)6, (byte)2, 'X'),
        new Placement((byte)5, (byte)6, (byte)3, 'X'),
        new Placement((byte)5, (byte)6, (byte)4, 'X'),
        new Placement((byte)6, (byte)5, (byte)8, 'X'),
        new Placement((byte)6, (byte)6, (byte)2, 'X'),
        new Placement((byte)6, (byte)6, (byte)3, 'X'),
        new Placement((byte)6, (byte)6, (byte)7, 'X'),
        new Placement((byte)6, (byte)6, (byte)8, 'X'),
        new Placement((byte)7, (byte)5, (byte)8, 'X'),
        new Placement((byte)7, (byte)6, (byte)2, 'X'),
        new Placement((byte)7, (byte)6, (byte)8, 'X'),
        new Placement((byte)8, (byte)5, (byte)2, 'X'),
        new Placement((byte)8, (byte)5, (byte)3, 'X'),
        new Placement((byte)8, (byte)5, (byte)4, 'X'),
        new Placement((byte)8, (byte)5, (byte)5, 'X'),
        new Placement((byte)8, (byte)5, (byte)6, 'X'),
        new Placement((byte)8, (byte)5, (byte)8, 'X'),
        new Placement((byte)8, (byte)6, (byte)3, 'X'),
        new Placement((byte)8, (byte)6, (byte)6, 'X'),
        new Placement((byte)1, (byte)1, (byte)4, 'W'),
        new Placement((byte)1, (byte)1, (byte)6, 'W'),
        new Placement((byte)1, (byte)2, (byte)4, 'W'),
        new Placement((byte)1, (byte)2, (byte)6, 'W'),
        new Placement((byte)1, (byte)3, (byte)4, 'W'),
        new Placement((byte)1, (byte)3, (byte)6, 'W')
    };

    public boolean place(FeaturePlaceContext<?> context) {
        WorldGenLevel level = context.level();
        int chunkX = Math.floorDiv(context.origin().getX(), 16);
        int chunkZ = Math.floorDiv(context.origin().getZ(), 16);
        if (!AgeLegacySmallLibraryPlacementMath.isLibraryChunk(level.getSeed(), chunkX, chunkZ)) return false;

        int turns = context.random().nextInt(4);
        int baseX = chunkX * 16;
        int baseZ = chunkZ * 16;
        int ground = averageGround(level, baseX, baseZ, turns);
        if (ground < level.getMinBuildHeight()) return false;
        int baseY = ground + 1;

        // Legacy foundation: replace air/liquid downward below the 11x11 footprint.
        for (int lx = 0; lx < SIZE; ++lx) {
            for (int lz = 0; lz < SIZE; ++lz) {
                BlockPos surface = map(baseX, baseY - 1, baseZ, lx, 0, lz, turns);
                for (int y = surface.getY(); y > level.getMinBuildHeight() + 1; --y) {
                    BlockPos at = new BlockPos(surface.getX(), y, surface.getZ());
                    BlockState existing = level.getBlockState(at);
                    if (!existing.isAir() && existing.getFluidState().isEmpty()) break;
                    level.setBlock(at, Blocks.COBBLESTONE.defaultBlockState(), 2);
                }
            }
        }

        for (Placement placement : BLUEPRINT) {
            BlockPos at = map(baseX, baseY, baseZ, placement.x(), placement.y(), placement.z(), turns);
            level.setBlock(at, stateFor(placement.state(), turns), 2);
        }

        placeTreasureChest(level, context.random(), map(baseX, baseY, baseZ, 4, 1, 2, turns));
        for (int i = 0; i < LECTERNS.length; ++i) {
            int[] local = LECTERNS[i];
            int[] target = LECTERN_TARGETS[i];
            BlockPos at = map(baseX, baseY, baseZ, local[0], local[1], local[2], turns);
            BlockPos lookAt = map(baseX, baseY, baseZ, target[0], local[1], target[1], turns);
            Direction facing = legacyLecternFacing(at, lookAt);
            level.setBlock(at, MystBlocks.LECTERN.get().defaultBlockState()
                    .setValue(BlockBookDisplay.FACING, facing), 2);
            if (level.getBlockEntity(at) instanceof BookDisplayBlockEntity display) {
                ItemStack treasure = MystcraftTreasureGenerator.generateLecternItem(context.random(), true);
                if (!treasure.isEmpty()) display.insertBook(treasure);
            }
        }
        return true;
    }


    /**
     * Exact 0.13.7.06 StructureGenerationUtils lectern-facing calculation.
     * The legacy helper first calculated an integer angle from lectern -> target, then used
     * EnumFacing.fromAngle(360 - angle + 90). Direction#fromYRot is the modern equivalent.
     */
    private static Direction legacyLecternFacing(BlockPos pos, BlockPos lookPos) {
        int rotation = legacyRotation(pos.getX(), pos.getZ(), lookPos.getX(), lookPos.getZ());
        return Direction.fromYRot(360.0D - rotation + 90.0D);
    }

    private static int legacyRotation(int x1, int z1, int x2, int z2) {
        int deltaX = x2 - x1;
        int deltaZ = -(z2 - z1);
        if (deltaZ == 0) return deltaX < 0 ? 180 : 0;
        if (deltaX == 0) return deltaZ < 0 ? 270 : 90;
        float ratio = (float) deltaZ / (float) deltaX;
        return (int) (Math.atan(ratio) * 180.0D / Math.PI);
    }

    private static int averageGround(WorldGenLevel level, int baseX, int baseZ, int turns) {
        long total = 0L;
        int count = 0;
        for (int lx = 0; lx < SIZE; ++lx) {
            for (int lz = 0; lz < SIZE; ++lz) {
                BlockPos p = map(baseX, 0, baseZ, lx, 0, lz, turns);
                total += level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, p.getX(), p.getZ());
                ++count;
            }
        }
        return count == 0 ? -1 : (int)(total / count);
    }

    private static void placeTreasureChest(WorldGenLevel level, RandomSource random, BlockPos pos) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 2);
        if (!(level.getBlockEntity(pos) instanceof Container container)) return;
        List<Integer> empty = new ArrayList<>(container.getContainerSize());
        for (int i = 0; i < container.getContainerSize(); ++i) empty.add(i);
        for (ItemStack stack : MystcraftTreasureGenerator.generate(random)) {
            if (empty.isEmpty()) break;
            int pick = random.nextInt(empty.size());
            int slot = empty.remove(pick);
            container.setItem(slot, stack);
        }
        container.setChanged();
    }

    private static BlockPos map(int baseX, int baseY, int baseZ, int x, int y, int z, int turns) {
        int rx, rz;
        switch (turns & 3) {
            case 1 -> { rx = SIZE - 1 - z; rz = x; }
            case 2 -> { rx = SIZE - 1 - x; rz = SIZE - 1 - z; }
            case 3 -> { rx = z; rz = SIZE - 1 - x; }
            default -> { rx = x; rz = z; }
        }
        return new BlockPos(baseX + rx, baseY + y, baseZ + rz);
    }

    private static Direction rotate(Direction direction, int turns) {
        Direction result = direction;
        for (int i = 0; i < (turns & 3); ++i) result = result.getClockWise();
        return result;
    }

    private static BlockState stateFor(char code, int turns) {
        return switch (code) {
            case 'A' -> Blocks.AIR.defaultBlockState();
            case 'B' -> Blocks.BOOKSHELF.defaultBlockState();
            case 'C' -> Blocks.COBBLESTONE.defaultBlockState();
            case 'W' -> Blocks.COBBLESTONE_WALL.defaultBlockState();
            case 'P' -> Blocks.OAK_PLANKS.defaultBlockState();
            case 'L' -> Blocks.COBBLESTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            case 'U' -> Blocks.COBBLESTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
            case 'X' -> Blocks.COBWEB.defaultBlockState();
            case 'N' -> stair(Direction.NORTH, Half.BOTTOM, turns);
            case 'S' -> stair(Direction.SOUTH, Half.BOTTOM, turns);
            case 'E' -> stair(Direction.EAST, Half.BOTTOM, turns);
            case 'Q' -> stair(Direction.WEST, Half.BOTTOM, turns);
            case 'n' -> stair(Direction.NORTH, Half.TOP, turns);
            case 's' -> stair(Direction.SOUTH, Half.TOP, turns);
            case 'e' -> stair(Direction.EAST, Half.TOP, turns);
            case 'q' -> stair(Direction.WEST, Half.TOP, turns);
            default -> Blocks.AIR.defaultBlockState();
        };
    }

    private static BlockState stair(Direction facing, Half half, int turns) {
        return Blocks.STONE_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, rotate(facing, turns))
                .setValue(StairBlock.HALF, half);
    }
}
