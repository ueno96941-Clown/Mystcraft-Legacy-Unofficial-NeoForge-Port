package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.AgeLegacyMineshaftPlacementMath;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

/**
 * Runtime-only placement adapter for Minecraft 1.12 MapGenMineshaft.
 *
 * <p>Using a RandomSpreadStructurePlacement subclass keeps vanilla locate code on its
 * RandomSpread branch. spacing=1 makes every queried chunk its own potential chunk, while
 * {@link #isPlacementChunk} restores the actual 1.12 0.004 + distance gate.</p>
 */
final class AgeLegacyMineshaftPlacement extends RandomSpreadStructurePlacement {
    AgeLegacyMineshaftPlacement() {
        super(1, 0, RandomSpreadType.LINEAR, 0);
    }

    @Override
    protected boolean isPlacementChunk(
            net.minecraft.world.level.chunk.ChunkGeneratorStructureState state,
            int chunkX,
            int chunkZ) {
        return AgeLegacyMineshaftPlacementMath.isPlacementChunk(
                state.getLevelSeed(), chunkX, chunkZ);
    }
}
