package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.agedata.AgeRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.Objects;
import java.util.Random;

/** Resolves the persistent initial spawn before FEATURES/Legacy population are allowed to run. */
public final class AgeSpawnResolver {
    private static final int SEARCH_ATTEMPTS = 1000;

    private AgeSpawnResolver() {}

    /**
     * Selects X/Z from terrain that has reached CARVERS but not FEATURES, then reproduces the
     * Legacy fallback of starting at sea level and walking upward until air. Keeping the search
     * below FEATURES is critical: SymbolStarFissure needs the final spawn to be known before the
     * spawn chunk's ordered IPopulate stream executes.
     *
     * <p>Legacy first called BiomeProviderMyst#findBiomePosition, whose world-dependent reservoir
     * sampling advanced the same Random before these coordinate draws. The modern port still does
     * not replay that biome-provider RNG consumption; the remaining coordinate sequence is kept
     * deterministic from the Age seed.</p>
     */
    public static BlockPos resolveInitialSpawn(ServerLevel level, AgeRecord age) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(age, "age");
        if (age.spawn() != null) return age.spawn();

        Random random = new Random(age.seed());
        int x = random.nextInt(64) - random.nextInt(64);
        int z = random.nextInt(64) - random.nextInt(64);

        for (int attempt = 0; attempt < SEARCH_ATTEMPTS; ++attempt) {
            if (canCoordinateBeSpawn(level, x, z)) break;
            x = random.nextInt(64) - random.nextInt(64);
            z = random.nextInt(64) - random.nextInt(64);
        }

        ChunkAccess chunk = carversChunk(level, x, z);
        int y = Math.max(level.getSeaLevel(), level.getMinBuildHeight());
        int top = level.getMaxBuildHeight();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, y, z);
        while (y < top && !chunk.getBlockState(cursor.set(x, y, z)).isAir()) ++y;
        if (y >= top) y = top - 1;
        return new BlockPos(x, y, z);
    }

    private static boolean canCoordinateBeSpawn(ServerLevel level, int x, int z) {
        ChunkAccess chunk = carversChunk(level, x, z);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = level.getMaxBuildHeight() - 1; y >= level.getMinBuildHeight(); --y) {
            BlockState state = chunk.getBlockState(cursor.set(x, y, z));
            if (state.isAir()) continue;
            if (state.is(Blocks.BEDROCK)) return false;
            return state.blocksMotion();
        }
        return false;
    }

    private static ChunkAccess carversChunk(ServerLevel level, int x, int z) {
        return level.getChunk(Math.floorDiv(x, 16), Math.floorDiv(z, 16), ChunkStatus.CARVERS);
    }
}
