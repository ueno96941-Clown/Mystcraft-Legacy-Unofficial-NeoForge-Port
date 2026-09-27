package com.xcompwiz.mystcraft.block;

import com.xcompwiz.mystcraft.api.event.StarFissureLinkEvent;
import com.xcompwiz.mystcraft.api.impl.LinkInfoAdapter;
import com.xcompwiz.mystcraft.api.linking.ILinkInfo;
import com.xcompwiz.mystcraft.linking.LinkController;
import com.xcompwiz.mystcraft.linking.LinkProperties;
import com.mojang.serialization.MapCodec;
import com.xcompwiz.mystcraft.registry.MystSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;


/** Legacy Star Fissure surface: non-colliding horizontal portal back to the home world. */
public final class BlockStarFissure extends BaseEntityBlock {
    public static final MapCodec<BlockStarFissure> CODEC = simpleCodec(BlockStarFissure::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1.6, 16);

    public BlockStarFissure(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<BlockStarFissure> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new com.xcompwiz.mystcraft.blockentity.StarFissureBlockEntity(pos, state);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel source)) return;
        if (entity.isOnPortalCooldown()) return;

        // 0.13.7.06 BlockStarFissure builds a fresh natural/external link to
        // homeDimension, lets StarFissureLinkEvent mutate it, then delegates to
        // the normal LinkController.  Keep the numeric id 0 for API fidelity.
        ILinkInfo info = LinkInfoAdapter.detached(new CompoundTag());
        info.setDimensionUID(0);
        info.setFlag(LinkProperties.NATURAL, true);
        info.setFlag(LinkProperties.EXTERNAL, true);
        info.setProperty(LinkProperties.SOUND, "mystcraft:linking.link-fissure");
        info.setSpawnYaw(entity.getYRot());
        NeoForge.EVENT_BUS.post(new StarFissureLinkEvent(source, entity, info));
        LinkController.travelEntity(source, entity, info, MystSounds.LINK_FISSURE.get());
    }
}
