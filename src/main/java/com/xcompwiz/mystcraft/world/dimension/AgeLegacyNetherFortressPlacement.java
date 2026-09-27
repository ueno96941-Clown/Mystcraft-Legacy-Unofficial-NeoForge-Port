package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.AgeLegacyNetherFortressPlacementMath;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.Optional;

/**
 * Runtime-only RandomSpread adapter whose candidate chunk math matches 1.12 MapGenNetherBridge.
 *
 * <p>The old one-in-three regional gate is part of the exact 1.12 candidate RNG stream,
 * not a second modern frequency-reduction pass.  Both generation and locate therefore use
 * {@link AgeLegacyNetherFortressPlacementMath} directly.</p>
 *
 * <p>This object lives only in an Age-local direct StructureSet and is not serialized as world
 * preset data; returning the vanilla random-spread placement type keeps modern locate logic on
 * the RandomSpread branch while the overridden candidate calculation remains active in memory.</p>
 */
final class AgeLegacyNetherFortressPlacement extends RandomSpreadStructurePlacement {
    private static final int REGION_SIZE = 16;
    private static final int OFFSET_WIDTH = 8;

    AgeLegacyNetherFortressPlacement() {
        super(
                Vec3i.ZERO,
                StructurePlacement.FrequencyReductionMethod.DEFAULT,
                1.0F,
                0,
                Optional.empty(),
                REGION_SIZE,
                REGION_SIZE - OFFSET_WIDTH,
                RandomSpreadType.LINEAR);
    }

    @Override
    public ChunkPos getPotentialStructureChunk(long seed, int chunkX, int chunkZ) {
        var candidate = AgeLegacyNetherFortressPlacementMath.candidate(seed, chunkX, chunkZ);
        return new ChunkPos(candidate.chunkX(), candidate.chunkZ());
    }
    @Override
    protected boolean isPlacementChunk(net.minecraft.world.level.chunk.ChunkGeneratorStructureState structureState, int x, int z) {
        return AgeLegacyNetherFortressPlacementMath.isPlacementChunk(structureState.getLevelSeed(), x, z);
    }

}
