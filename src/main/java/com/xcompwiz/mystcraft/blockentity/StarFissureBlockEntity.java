package com.xcompwiz.mystcraft.blockentity;

import com.xcompwiz.mystcraft.registry.MystBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Render-only block entity for the legacy Star Fissure.
 *
 * <p>Mystcraft 0.13.7.06 rendered the fissure with the vanilla End sky/portal
 * textures using the same layered projection family as the End Portal.  By
 * subclassing the modern End Portal BE we can reuse that renderer without
 * restoring obsolete fixed-function OpenGL code.</p>
 */
public final class StarFissureBlockEntity extends TheEndPortalBlockEntity {
    public StarFissureBlockEntity(BlockPos pos, BlockState state) {
        super(MystBlockEntities.STAR_FISSURE.get(), pos, state);
    }

    /** The old renderer explicitly drew the top and bottom faces only. */
    @Override
    public boolean shouldRenderFace(Direction direction) {
        return direction == Direction.UP || direction == Direction.DOWN;
    }
}
