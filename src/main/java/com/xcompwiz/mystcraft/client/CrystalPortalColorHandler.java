package com.xcompwiz.mystcraft.client;

import com.xcompwiz.mystcraft.blockentity.BookReceptacleBlockEntity;
import com.xcompwiz.mystcraft.registry.MystBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Client tint bridge for the legacy coloured Crystal Portal surface. */
public final class CrystalPortalColorHandler {
    private static final int MAX_SCAN = 1024;

    private CrystalPortalColorHandler() {}

    public static int color(BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex) {
        if (level == null || pos == null || !state.is(MystBlocks.LINK_PORTAL.get())) return 0xFFFFFF;

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(pos.immutable());

        while (!queue.isEmpty() && visited.size() < MAX_SCAN) {
            BlockPos cursor = queue.removeFirst();
            if (!visited.add(cursor)) continue;
            BlockState cursorState = level.getBlockState(cursor);
            if (!isNetwork(cursorState)) continue;

            for (Direction direction : Direction.values()) {
                BlockPos next = cursor.relative(direction);
                if (level.getBlockEntity(next) instanceof BookReceptacleBlockEntity receptacle
                        && receptacle.hasBook() && receptacle.controls(pos)) {
                    return receptacle.getPortalColor();
                }
                if (!visited.contains(next) && isNetwork(level.getBlockState(next))) queue.addLast(next.immutable());
            }
        }
        return 0xFFFFFF;
    }

    private static boolean isNetwork(BlockState state) {
        return state.is(MystBlocks.CRYSTAL.get()) || state.is(MystBlocks.LINK_PORTAL.get());
    }
}
