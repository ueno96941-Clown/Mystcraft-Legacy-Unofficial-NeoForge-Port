package com.xcompwiz.mystcraft.portal;

import com.xcompwiz.mystcraft.block.BlockLinkPortal;
import com.xcompwiz.mystcraft.linking.PortalTransferRuntime;
import com.xcompwiz.mystcraft.registry.MystBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * CP318 swept Crystal Portal crossing bridge.
 *
 * <p>Every server tick we inspect the full swept AABB of each root entity/group,
 * independently of Block#entityInside.  Candidate Crystal Portal cells are tested
 * with an exact segment/plane intersection and the earliest crossing wins.  This
 * closes the tunnelling hole where a powered minecart, falling entity or other
 * high-speed traveller can move from one side of a non-colliding portal to the
 * other without ever occupying the portal block at the end of a tick.</p>
 *
 * <p>If the same root is still in the delayed passenger-repair stage from the
 * previous portal, CP318 first tries to finish that server-side riding graph on
 * demand.  If it cannot yet be resolved, the root is held immediately in front of
 * the crossed portal instead of being allowed to pass through.  Deliberately weird
 * short portal chains therefore remain usable without sacrificing rider safety.</p>
 */
public final class CrystalPortalProjectileSweep {
    /** Maximum number of block cells scanned by the exact swept-AABB broad phase. */
    private static final int MAX_SWEEP_BLOCKS = 16384;
    /** Fallback density for absurdly large one-tick motion where the swept box is huge. */
    private static final double FALLBACK_SAMPLES_PER_BLOCK = 12.0D;
    private static final int MAX_FALLBACK_SAMPLES = 8192;
    private static final int MAX_BLOCKS_PER_FALLBACK_SAMPLE = 128;

    private CrystalPortalProjectileSweep() {}

    public static void tick(MinecraftServer server) {
        if (server == null) return;
        Set<UUID> handled = new HashSet<>();

        List<ServerLevel> levels = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) levels.add(level);

        for (ServerLevel level : levels) {
            List<Entity> entities = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) entities.add(entity);

            for (Entity root : entities) {
                if (root == null || root.isRemoved() || root.isPassenger()) continue;
                UUID id = root.getUUID();
                if (!handled.add(id)) continue;

                Vec3 start = new Vec3(root.xo, root.yo, root.zo);
                Vec3 end = root.position();
                Vec3 delta = end.subtract(start);
                double distance = delta.length();
                double velocityDistance = root.getDeltaMovement().length();
                // A dimension change / command teleport can leave xo/yo/zo in a
                // completely different coordinate space for one observation.  Do not
                // interpret that discontinuity as a physical portal sweep.  Genuine
                // launchers remain eligible because their displacement is backed by a
                // correspondingly large velocity.
                double discontinuityLimit = Math.max(64.0D, velocityDistance * 8.0D + 8.0D);
                if (Double.isFinite(distance) && distance > discontinuityLimit) continue;

                if (!Double.isFinite(distance) || distance < 0.015625D) continue;

                PortalHit hit = findFirstCrossing(level, root, start, end);
                if (hit == null) continue;

                // CP318: the previous transfer may still be intentionally waiting to
                // re-seat passengers for client synchronization.  A new portal crossing
                // is stronger evidence that the group must be made coherent now.  Do so
                // on demand; if any member is not yet resolvable, fail closed at the
                // portal plane instead of letting the root tunnel through.
                if (PortalTransferRuntime.isPending(server, id)
                        && !PortalTransferRuntime.prepareForImmediateRetransfer(server, root)) {
                    BlockLinkPortal.holdBeforePortal(hit.state(), hit.pos(), root, start, end);
                    continue;
                }

                // Use the exact hit cell discovered from the complete movement segment.
                // tryPortalCollision re-validates the controller/book before linking.
                BlockLinkPortal.tryPortalCollision(level, hit.pos(), root);
            }
        }
    }

    private static PortalHit findFirstCrossing(ServerLevel level, Entity root, Vec3 start, Vec3 end) {
        AABB endBox = collectGroupBox(root);
        Vec3 back = start.subtract(end);
        AABB startBox = endBox.move(back);
        // Expand slightly because a minecart/rider can visually cross a portal while
        // its origin tracks the rail block immediately below/alongside the plane.
        AABB swept = startBox.minmax(endBox).inflate(0.35D);

        int minX = (int) Math.floor(swept.minX);
        int minY = (int) Math.floor(swept.minY);
        int minZ = (int) Math.floor(swept.minZ);
        int maxX = (int) Math.floor(swept.maxX);
        int maxY = (int) Math.floor(swept.maxY);
        int maxZ = (int) Math.floor(swept.maxZ);
        long volume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);

        PortalHit best = null;
        if (volume > 0L && volume <= MAX_SWEEP_BLOCKS) {
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        var state = level.getBlockState(pos);
                        if (!state.is(MystBlocks.LINK_PORTAL.get())) continue;
                        double t = BlockLinkPortal.portalCrossingParameter(state, pos, root, start, end);
                        if (!Double.isFinite(t)) continue;
                        if (best == null || t < best.t()) best = new PortalHit(pos.immutable(), state, t);
                    }
                }
            }
            return best;
        }

        // Extremely large one-tick motion (commands, launchers, pathological mod
        // interactions): bound CPU cost while still sweeping densely along the path.
        Vec3 delta = end.subtract(start);
        double distance = delta.length();
        int samples = Math.min(MAX_FALLBACK_SAMPLES,
                Math.max(1, (int) Math.ceil(distance * FALLBACK_SAMPLES_PER_BLOCK)));
        Set<BlockPos> visited = new HashSet<>();
        for (int i = 0; i <= samples; i++) {
            double sampleT = (double) i / (double) samples;
            Vec3 point = start.add(delta.scale(sampleT));
            Vec3 offset = point.subtract(end);
            AABB sampleBox = endBox.move(offset).inflate(0.35D);
            int sx0 = (int) Math.floor(sampleBox.minX);
            int sy0 = (int) Math.floor(sampleBox.minY);
            int sz0 = (int) Math.floor(sampleBox.minZ);
            int sx1 = (int) Math.floor(sampleBox.maxX);
            int sy1 = (int) Math.floor(sampleBox.maxY);
            int sz1 = (int) Math.floor(sampleBox.maxZ);
            long sampleVolume = (long) (sx1 - sx0 + 1) * (sy1 - sy0 + 1) * (sz1 - sz0 + 1);
            if (sampleVolume <= 0L || sampleVolume > MAX_BLOCKS_PER_FALLBACK_SAMPLE) continue;
            for (int x = sx0; x <= sx1; x++) {
                for (int y = sy0; y <= sy1; y++) {
                    for (int z = sz0; z <= sz1; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (!visited.add(pos)) continue;
                        var state = level.getBlockState(pos);
                        if (!state.is(MystBlocks.LINK_PORTAL.get())) continue;
                        double t = BlockLinkPortal.portalCrossingParameter(state, pos, root, start, end);
                        if (!Double.isFinite(t)) continue;
                        if (best == null || t < best.t()) best = new PortalHit(pos.immutable(), state, t);
                    }
                }
            }
        }
        return best;
    }

    private record PortalHit(BlockPos pos, net.minecraft.world.level.block.state.BlockState state, double t) {}

    private static AABB collectGroupBox(Entity root) {
        AABB box = root.getBoundingBox();
        for (Entity passenger : root.getIndirectPassengers()) {
            if (passenger != null && !passenger.isRemoved()) box = box.minmax(passenger.getBoundingBox());
        }
        return box;
    }
}
