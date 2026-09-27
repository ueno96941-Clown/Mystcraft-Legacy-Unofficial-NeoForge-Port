package com.xcompwiz.mystcraft.linking;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.world.dimension.AgeRuntimeWorldManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Post-teleport transaction repair queue for Crystal Portal transfers.
 *
 * <p>CP268 proved that cross-dimension vehicle/passenger transfers can expose the
 * destination-side vehicle and rider on different ticks.  Treating the transfer as
 * complete after only a handful of ticks therefore caused intermittent forced
 * dismounts during repeated Portal travel.  CP271 keeps the whole riding group in a
 * pending transaction until every relationship is rebuilt, while also retaining a
 * second-stage hard recovery path if vanilla delays entity registration unusually
 * long.</p>
 */
public final class PortalTransferRuntime {
    private static final Map<MinecraftServer, List<PendingState>> PENDING = new IdentityHashMap<>();
    /**
     * CP320: one-shot virtual riding graph for an immediate next-portal transfer.
     *
     * <p>Do not call startRiding merely to reconstruct the server graph before an
     * immediate re-transfer. startRiding broadcasts a passengers packet, and under
     * abusive short portal chains the client can receive that packet before the
     * destination minecart spawn packet.  Instead, preserve the parent relation as
     * metadata and let LinkController consume it directly for the next transaction.</p>
     */
    private static final Map<MinecraftServer, Map<UUID, PreparedGroup>> PREPARED = new IdentityHashMap<>();

    /** Five seconds at 20 TPS: long enough to cover delayed ServerPlayer/non-player registration. */
    private static final int DEFAULT_TTL = 100;
    /** Keep restoring momentum through several ordinary entity ticks. */
    private static final int MOMENTUM_APPLICATIONS = 6;
    /** Allow destination spawn packets to reach the client before sending passengers. */
    private static final int RIDING_SYNC_DELAY = 10;
    /** After one second, escalate from normal re-seat to positional hard recovery. */
    private static final int HARD_RECOVERY_AFTER = 20;

    private PortalTransferRuntime() {}

    public static boolean isPending(MinecraftServer server, UUID entityId) {
        if (server == null || entityId == null) return false;
        List<PendingState> states = PENDING.get(server);
        if (states == null) return false;
        for (PendingState state : states) {
            if (entityId.equals(state.entityId) || entityId.equals(state.rootId)) return true;
        }
        return false;
    }

    public static void stage(
            MinecraftServer server,
            ResourceKey<Level> destination,
            UUID rootId,
            UUID entityId,
            UUID parentId,
            Vec3 velocity,
            float fallDistance,
            boolean maintainMomentum,
            Vec3 fallbackPosition,
            float yaw,
            float pitch) {
        if (server == null || destination == null || entityId == null || rootId == null) return;
        PENDING.computeIfAbsent(server, ignored -> new ArrayList<>()).add(new PendingState(
                destination,
                rootId,
                entityId,
                parentId,
                velocity == null ? Vec3.ZERO : velocity,
                fallDistance,
                maintainMomentum ? MOMENTUM_APPLICATIONS : 0,
                DEFAULT_TTL,
                fallbackPosition == null ? Vec3.ZERO : fallbackPosition,
                yaw,
                pitch));
    }

    /**
     * CP318 fast-chain bridge.
     *
     * <p>A minecart can reach another Crystal Portal before the normal delayed
     * passenger re-seat window has elapsed.  The old sweep simply skipped every
     * pending root, which made a perfectly detected high-speed crossing pass through
     * the next portal.  When a new crossing is actually imminent, resolve the whole
     * staged group in the current destination level and finish the riding graph now.
     * This keeps the conservative delayed sync during ordinary travel, but lets
     * deliberate portal railguns / short portal chains continue without splitting
     * the rider from the vehicle.</p>
     *
     * @return true when no pending transaction exists or the complete pending group
     *         is ready for another Crystal Portal transaction; false when the caller
     *         must hold the root in front of the portal and retry next tick.
     */
    public static boolean prepareForImmediateRetransfer(MinecraftServer server, Entity root) {
        if (server == null || root == null || root.isRemoved()) return false;
        List<PendingState> states = PENDING.get(server);
        if (states == null || states.isEmpty()) return true;

        UUID rootId = root.getUUID();
        List<PendingState> group = new ArrayList<>();
        for (PendingState state : states) {
            if (rootId.equals(state.rootId)) group.add(state);
        }
        if (group.isEmpty()) return true;
        if (!(root.level() instanceof ServerLevel currentLevel)) return false;

        // A live root passed by the sweep is authoritative for the current level.
        // Retire repair states which still point at an older dimension, but keep the
        // current-dimension states alive until LinkController actually commits the
        // next portal transfer.
        boolean removedStale = states.removeIf(state -> rootId.equals(state.rootId)
                && !currentLevel.dimension().equals(state.destination));
        if (removedStale) {
            group.removeIf(state -> !currentLevel.dimension().equals(state.destination));
            Mystcraft.LOGGER.debug(
                    "Retired stale Crystal Portal pending states for live root={} in {}",
                    rootId, currentLevel.dimension().location());
        }
        if (group.isEmpty()) {
            if (states.isEmpty()) PENDING.remove(server);
            return true;
        }

        Map<UUID, Entity> resolved = new HashMap<>();
        collectLiveTree(root, resolved);
        resolved.put(rootId, root);
        for (PendingState state : group) {
            Entity entity = resolved.get(state.entityId);
            if (entity == null || entity.isRemoved()) {
                entity = resolveDestinationEntity(server, currentLevel, state.entityId);
            }
            if (entity == null || entity.isRemoved()) return false;
            entity.setDeltaMovement(state.velocity);
            entity.fallDistance = state.fallDistance;
            resolved.put(state.entityId, entity);
        }

        // CP320: build a root-first virtual passenger tree WITHOUT startRiding().
        // This avoids emitting a transient ClientboundSetPassengersPacket between two
        // near-immediate dimension transfers.  LinkController consumes the parent IDs
        // directly, so the transfer still contains the complete vehicle/rider group.
        List<PreparedMember> prepared = new ArrayList<>();
        java.util.Set<UUID> added = new java.util.HashSet<>();
        collectPreparedLiveTree(root, null, prepared, added);
        int guard = group.size() + 2;
        while (prepared.size() < resolved.size() && guard-- > 0) {
            boolean progress = false;
            for (PendingState state : group) {
                if (added.contains(state.entityId)) continue;
                UUID parentId = state.parentId;
                if (parentId != null && !added.contains(parentId)) continue;
                Entity entity = resolved.get(state.entityId);
                if (entity == null || entity.isRemoved()) return false;
                prepared.add(new PreparedMember(entity, parentId));
                added.add(state.entityId);
                progress = true;
            }
            if (!progress) break;
        }
        if (added.size() != resolved.size()) return false;

        PREPARED.computeIfAbsent(server, ignored -> new HashMap<>())
                .put(rootId, new PreparedGroup(rootId, currentLevel.dimension(), prepared));
        Mystcraft.LOGGER.debug(
                "Prepared virtual Crystal Portal riding group for immediate re-transfer: root={}, members={}, dimension={}",
                rootId, prepared.size(), currentLevel.dimension().location());
        return true;
    }

    /** Returns the one-shot virtual riding group, if CP320 prepared one for this root. */
    public static List<PreparedMember> getPreparedGroup(MinecraftServer server, UUID rootId) {
        if (server == null || rootId == null) return List.of();
        Map<UUID, PreparedGroup> groups = PREPARED.get(server);
        if (groups == null) return List.of();
        PreparedGroup group = groups.get(rootId);
        if (group == null) return List.of();
        return List.copyOf(group.members);
    }

    /**
     * Commits consumption of a CP320 virtual group only when LinkController is about
     * to perform the next transfer.  Until this point the original PENDING repair
     * states remain intact, so a canceled/invalid link does not lose recovery data.
     */
    public static void commitPreparedGroup(MinecraftServer server, UUID rootId) {
        if (server == null || rootId == null) return;
        Map<UUID, PreparedGroup> groups = PREPARED.get(server);
        if (groups == null || groups.remove(rootId) == null) return;
        if (groups.isEmpty()) PREPARED.remove(server);
        List<PendingState> states = PENDING.get(server);
        if (states != null) {
            states.removeIf(state -> rootId.equals(state.rootId));
            if (states.isEmpty()) PENDING.remove(server);
        }
    }

    public static record PreparedMember(Entity entity, UUID parentId) {}
    private static record PreparedGroup(UUID rootId, ResourceKey<Level> dimension, List<PreparedMember> members) {}


    private static void collectLiveTree(Entity root, Map<UUID, Entity> out) {
        if (root == null || root.isRemoved() || out == null) return;
        if (out.putIfAbsent(root.getUUID(), root) != null) return;
        for (Entity passenger : root.getPassengers()) {
            collectLiveTree(passenger, out);
        }
    }

    private static void collectPreparedLiveTree(
            Entity entity,
            UUID parentId,
            List<PreparedMember> out,
            java.util.Set<UUID> added) {
        if (entity == null || entity.isRemoved() || out == null || added == null) return;
        UUID id = entity.getUUID();
        if (!added.add(id)) return;
        out.add(new PreparedMember(entity, parentId));
        for (Entity passenger : entity.getPassengers()) {
            collectPreparedLiveTree(passenger, id, out, added);
        }
    }

    public static void tick(MinecraftServer server) {
        // A CP320 prepared group is intended to be consumed synchronously by the
        // portal collision call stack.  If validation/cancellation prevented that
        // transfer, discard only the ephemeral view here; the original PENDING
        // repair states were deliberately left intact.
        PREPARED.remove(server);
        List<PendingState> states = PENDING.get(server);
        if (states == null || states.isEmpty()) return;

        Iterator<PendingState> iterator = states.iterator();
        while (iterator.hasNext()) {
            PendingState state = iterator.next();
            state.age++;
            state.ttl--;

            ServerLevel level = server.getLevel(state.destination);
            if (level == null) {
                if (state.ttl < 0) {
                    Mystcraft.LOGGER.error("Crystal Portal transfer expired before destination level became available: {} in {}",
                            state.entityId, state.destination.location());
                    iterator.remove();
                }
                continue;
            }

            // Layer 1 safety: keep the destination Age pinned for the entire transaction,
            // not merely for the initial teleport call.  CP249's normal unload policy
            // resumes immediately after the last pending state disappears.
            AgeRuntimeWorldManager.holdForPortalTransfer(server, state.destination, 40L);

            Entity entity = resolveDestinationEntity(server, level, state.entityId);
            Entity parent = state.parentId == null ? null : resolveDestinationEntity(server, level, state.parentId);

            if (entity != null && !entity.isRemoved()) {
                if (state.momentumApplications > 0) {
                    entity.setDeltaMovement(state.velocity);
                    entity.fallDistance = state.fallDistance;
                    state.momentumApplications--;
                }

                if (state.parentId == null) {
                    state.ridingReady = true;
                } else if (parent != null && !parent.isRemoved()) {
                    // CP273: do not emit a passengers packet on the very first
                    // destination tick.  Cross-dimension player/vehicle spawn
                    // packets can arrive in different client ticks; immediate
                    // startRiding caused the benign-but-real client warning
                    // "Received passengers for unknown entity".  CP274 extends this to ten
                    // server ticks after the long portal-chain stress test still
                    // produced occasional "Received passengers for unknown entity"
                    // warnings.  The transaction/age hold remains active while we
                    // wait, so this only delays the passengers packet; it does not
                    // permit the pair to become independent portal travellers.
                    if (state.age >= RIDING_SYNC_DELAY && entity.getVehicle() != parent) {
                        entity.startRiding(parent, true);
                    }
                    state.ridingReady = entity.getVehicle() == parent;

                    // Layer 2 safety: if ordinary re-seat has not succeeded after one
                    // second, put the passenger directly beside/on the resolved vehicle
                    // and force one more mount attempt.  This avoids the old failure mode
                    // where player and minecart became two independent portal travellers.
                    if (!state.ridingReady && state.age >= HARD_RECOVERY_AFTER) {
                        hardRecoverRider(level, entity, parent, state);
                        state.ridingReady = entity.getVehicle() == parent;
                    }
                }
            }

            if (state.ridingReady && state.momentumApplications <= 0) {
                iterator.remove();
                continue;
            }

            if (state.ttl < 0) {
                // Final fail-safe before giving up: one last positional recovery if both
                // instances exist.  Never silently declare a broken riding relation done.
                if (entity != null && !entity.isRemoved() && parent != null && !parent.isRemoved()) {
                    hardRecoverRider(level, entity, parent, state);
                    if (entity.getVehicle() == parent) {
                        iterator.remove();
                        continue;
                    }
                }
                Mystcraft.LOGGER.error(
                        "Crystal Portal transfer recovery expired: root={}, entity={}, parent={}, destination={}, age={} ticks",
                        state.rootId, state.entityId, state.parentId, state.destination.location(), state.age);
                iterator.remove();
            }
        }

        if (states.isEmpty()) PENDING.remove(server);
    }

    private static Entity resolveDestinationEntity(MinecraftServer server, ServerLevel level, UUID id) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null) {
            return player.serverLevel() == level && !player.isRemoved() ? player : null;
        }
        Entity entity = level.getEntity(id);
        return entity != null && !entity.isRemoved() ? entity : null;
    }

    private static void hardRecoverRider(ServerLevel level, Entity entity, Entity parent, PendingState state) {
        Vec3 anchor = parent.position();
        if (!Double.isFinite(anchor.x) || !Double.isFinite(anchor.y) || !Double.isFinite(anchor.z)) {
            anchor = state.fallbackPosition;
        }
        entity.stopRiding();
        entity.teleportTo(
                level,
                anchor.x,
                anchor.y + Math.max(0.1D, parent.getBbHeight() * 0.5D),
                anchor.z,
                Collections.emptySet(),
                state.yaw,
                state.pitch);
        entity.setDeltaMovement(state.velocity);
        entity.fallDistance = state.fallDistance;
        entity.setPortalCooldown();
        entity.startRiding(parent, true);
        if (entity.getVehicle() == parent && !state.hardRecoveryLogged) {
            Mystcraft.LOGGER.warn(
                    "Crystal Portal hard-recovered riding relation: rider={} vehicle={} after {} ticks",
                    state.entityId, state.parentId, state.age);
            state.hardRecoveryLogged = true;
        }
    }

    public static void clear(MinecraftServer server) {
        if (server != null) {
            PENDING.remove(server);
            PREPARED.remove(server);
        }
    }

    private static final class PendingState {
        private final ResourceKey<Level> destination;
        private final UUID rootId;
        private final UUID entityId;
        private final UUID parentId;
        private final Vec3 velocity;
        private final float fallDistance;
        private final Vec3 fallbackPosition;
        private final float yaw;
        private final float pitch;
        private int momentumApplications;
        private int ttl;
        private int age;
        private boolean ridingReady;
        private boolean hardRecoveryLogged;

        private PendingState(
                ResourceKey<Level> destination,
                UUID rootId,
                UUID entityId,
                UUID parentId,
                Vec3 velocity,
                float fallDistance,
                int momentumApplications,
                int ttl,
                Vec3 fallbackPosition,
                float yaw,
                float pitch) {
            this.destination = destination;
            this.rootId = rootId;
            this.entityId = entityId;
            this.parentId = parentId;
            this.velocity = velocity;
            this.fallDistance = fallDistance;
            this.momentumApplications = momentumApplications;
            this.ttl = ttl;
            this.fallbackPosition = fallbackPosition;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
