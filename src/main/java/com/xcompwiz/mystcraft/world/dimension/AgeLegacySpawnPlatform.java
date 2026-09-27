package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.world.agedata.AgeRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Recreates the 0.13.7.06 Mystcraft Age spawn platform.
 *
 * <p>The original MystWorldGenerator populated the Age spawn chunk with a 5x5 cobblestone pad
 * one block below world spawn and cleared four blocks of headroom over the entire pad.  This is
 * separate from the optional Link Panel "Generate Platform" property, which only creates a single
 * stone block when linking over a drop.</p>
 */
public final class AgeLegacySpawnPlatform {
    private static final int RADIUS = 2;
    private static final int CLEAR_HEIGHT = 4;

    private AgeLegacySpawnPlatform() {}

    /**
     * Ensures the legacy platform exists for a not-yet-visited Mystcraft Age.
     * Re-running before a successful first arrival is safe and idempotent.
     */
    public static boolean ensureForFirstVisit(ServerLevel level, AgeRecord age) {
        if (age.visited()) return false;

        BlockPos spawn = level.getSharedSpawnPos();
        int floorY = spawn.getY() - 1;
        if (floorY < level.getMinBuildHeight() || floorY >= level.getMaxBuildHeight()) {
            Mystcraft.LOGGER.warn(
                    "Skipped legacy spawn platform for Age {}: spawn {} outside build bounds {}..{}",
                    age.ageUid(), spawn, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
            return false;
        }

        // Force the spawn chunk live before block writes.  The 5x5 pad may straddle a chunk edge;
        // ServerLevel#setBlock loads/touches any adjacent positions as required.
        level.getChunkAt(spawn);

        int updateFlags = Block.UPDATE_CLIENTS;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                BlockPos floor = new BlockPos(spawn.getX() + dx, floorY, spawn.getZ() + dz);
                level.setBlock(floor, Blocks.COBBLESTONE.defaultBlockState(), updateFlags);

                for (int dy = 1; dy <= CLEAR_HEIGHT; dy++) {
                    int y = floorY + dy;
                    if (y >= level.getMaxBuildHeight()) break;
                    level.setBlock(new BlockPos(floor.getX(), y, floor.getZ()), Blocks.AIR.defaultBlockState(), updateFlags);
                }
            }
        }

        Mystcraft.LOGGER.debug(
                "Generated legacy 5x5 cobblestone spawn platform for Age {} at {}",
                age.ageUid(), spawn);
        return true;
    }
}
