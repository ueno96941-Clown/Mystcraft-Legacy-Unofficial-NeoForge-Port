package com.xcompwiz.mystcraft.entity;

import com.xcompwiz.mystcraft.instability.LegacyInstabilityMeteorModel;
import com.xcompwiz.mystcraft.instability.LegacyMeteorExplosionExecutor;
import com.xcompwiz.mystcraft.api.event.MeteorEvent;
import net.neoforged.neoforge.common.NeoForge;
import com.xcompwiz.mystcraft.registry.MystEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** CP300 live-test meteor entity using the audited 0.13.7.06 impact sequence and visible/audio restoration. */
public final class EntityMeteor extends Entity {
    private float scale = LegacyInstabilityMeteorModel.INSTABILITY_SCALE;
    private int penetration = LegacyInstabilityMeteorModel.INSTABILITY_PENETRATION;
    private int inGroundTime;
    private boolean firstTick = true;
    /** CP326: loaded meteors are retired a few ticks after load instead of being discarded during NBT decode. */
    private boolean retireAfterLoad;
    private int retireAfterLoadTicks;

    public EntityMeteor(EntityType<? extends EntityMeteor> type, Level level) {
        super(type, level);
    }

    public EntityMeteor(Level level, double x, double y, double z, double dx, double dy, double dz) {
        this(MystEntities.METEOR.get(), level);
        setPos(x, y, z);
        setDeltaMovement(dx, dy, dz);
        NeoForge.EVENT_BUS.post(new MeteorEvent.MetorSpawn(this));
    }

    public static EntityMeteor fromPlan(Level level, com.xcompwiz.mystcraft.instability.LegacyInstabilityMeteorPlanService.SpawnPlan spawn) {
        EntityMeteor meteor = new EntityMeteor(level, spawn.x(), spawn.y(), spawn.z(),
                spawn.motionX(), spawn.motionY(), spawn.motionZ());
        meteor.setScale(spawn.scale(), spawn.penetration());
        return meteor;
    }

    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();

        // CP326: 0.13.7.06 meteors do not survive a world reload.  Discarding from
        // readAdditionalSaveData(), however, can leave a just-loaded removed entity in
        // ServerEntity's pairing path and produces "Fetching packet for removed entity".
        // Let the loaded entity become a valid tracked entity first, keep it inert, then
        // retire it after a short grace period.  No impact/carve/explosion is allowed here.
        if (retireAfterLoad) {
            setDeltaMovement(Vec3.ZERO);
            setInvisible(true);
            if (++retireAfterLoadTicks >= 5) {
                discard();
            }
            return;
        }
        Vec3 motion = getDeltaMovement();
        if (firstTick) {
            if (!level().isClientSide()) {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                        SoundSource.WEATHER, 10000.0F, 0.8F + random.nextFloat() * 0.2F);
            }
            alignRotationToMotion(motion, true);
            firstTick = false;
        } else {
            alignRotationToMotion(motion, false);
        }

        Vec3 start = position();
        Vec3 end = start.add(motion);
        HitResult hit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        boolean blockHit = hit.getType() == HitResult.Type.BLOCK;
        inGroundTime = LegacyInstabilityMeteorModel.nextInGroundTime(inGroundTime, blockHit);

        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.5D, getZ(), 2,
                    0.15D, 0.15D, 0.15D, 0.0D);
            if (blockHit) {
                setDeltaMovement(motion.x, LegacyInstabilityMeteorModel.dampedVerticalMotion(motion.y), motion.z);
                carve(server, motion);
                if (LegacyInstabilityMeteorModel.shouldImpactExplode(inGroundTime, penetration)) {
                    impact(server);
                    discard();
                    return;
                }
            }
        }

        move(MoverType.SELF, getDeltaMovement());
        if (getY() < level().getMinBuildHeight() - 64 || getY() > 1024) discard();
    }


    /** Restore the 1.12 meteor's smoothed yaw/pitch alignment to its velocity vector. */
    private void alignRotationToMotion(Vec3 motion, boolean snap) {
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float targetYaw = (float) (Math.atan2(motion.x, motion.z) * (180.0D / Math.PI));
        float targetPitch = (float) (Math.atan2(motion.y, horizontal) * (180.0D / Math.PI));
        if (snap) {
            setYRot(targetYaw);
            setXRot(targetPitch);
            this.yRotO = targetYaw;
            this.xRotO = targetPitch;
        } else {
            setYRot(getYRot() + Mth.wrapDegrees(targetYaw - getYRot()) * 0.2F);
            setXRot(getXRot() + Mth.wrapDegrees(targetPitch - getXRot()) * 0.2F);
        }
    }

    private void carve(ServerLevel level, Vec3 motion) {
        var box = getBoundingBox();
        var bounds = LegacyInstabilityMeteorModel.directCarveBounds(
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                motion.x, motion.y, motion.z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    pos.set(x, y, z);
                    if (!level.isInWorldBounds(pos) || level.getBlockState(pos).isAir()) continue;
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    private void impact(ServerLevel level) {
        for (var burst : LegacyInstabilityMeteorModel.impactExplosions(scale)) {
            double x = getX() + burst.offsetX();
            double y = getY() + burst.offsetY();
            double z = getZ() + burst.offsetZ();
            LegacyMeteorExplosionExecutor.explode(level, this, x, y, z, burst);
        }
        NeoForge.EVENT_BUS.post(new MeteorEvent.MetorImpact(this));
    }

    public void setScale(float scale, int penetration) {
        this.scale = scale;
        this.penetration = penetration;
    }

    public float getScale() { return scale; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Legacy meteors died on reload. Preserve that behavior without removing the
        // entity from inside the NBT-load callback (see CP326 note in tick()).
        retireAfterLoad = true;
        retireAfterLoadTicks = 0;
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Scale", scale);
        tag.putInt("Penetration", penetration);
        tag.putInt("InGround", inGroundTime);
    }
}
