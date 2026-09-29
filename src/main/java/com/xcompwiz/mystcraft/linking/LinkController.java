package com.xcompwiz.mystcraft.linking;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.advancement.MystCriteriaTriggers;
import com.xcompwiz.mystcraft.item.ItemAgebook;
import com.xcompwiz.mystcraft.item.ItemLinkbook;
import com.xcompwiz.mystcraft.api.event.LinkEvent;
import com.xcompwiz.mystcraft.api.impl.LinkInfoAdapter;
import com.xcompwiz.mystcraft.api.linking.ILinkInfo;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import com.xcompwiz.mystcraft.registry.MystSounds;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import com.xcompwiz.mystcraft.entity.EntityLinkbook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import com.xcompwiz.mystcraft.world.agedata.AgeRecord;
import com.xcompwiz.mystcraft.world.agedata.AgeRegistryData;
import com.xcompwiz.mystcraft.world.dimension.AgeDimensionDefinition;
import com.xcompwiz.mystcraft.world.dimension.AgeRuntimeDimensionLoader;
import com.xcompwiz.mystcraft.world.dimension.AgeRuntimeServerLevelInstaller;
import com.xcompwiz.mystcraft.world.dimension.AgeRuntimeWorldManager;
import com.xcompwiz.mystcraft.world.dimension.AgeRuntimeStateSync;
import com.xcompwiz.mystcraft.network.AgeVisualSync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LinkController {
    private LinkController() {}

    public static boolean travelPlayer(ServerPlayer player, InteractionHand hand, ItemStack book) {
        return travelPlayerInternal(player, hand, book, false, null);
    }

    public static boolean travelPlayerFromPortal(ServerPlayer player, ItemStack book) {
        return travelPlayerInternal(player, null, book, true, null);
    }

    /** Crystal Portal path: retains the controller book but uses the legacy portal-link sound. */
    public static boolean travelPlayerFromPortal(ServerPlayer player, ItemStack book, SoundEvent soundOverride) {
        return travelPlayerInternal(player, null, book, true, soundOverride);
    }

    private static boolean travelPlayerInternal(ServerPlayer player, InteractionHand hand, ItemStack book, boolean portalTravel, SoundEvent soundOverride) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        // 0.13.7.06 initialized unresolved books at activation time.  This matters
        // especially for books stored in a Book Stand/Lectern, because they do not
        // receive inventoryTick while sitting in the block entity.
        AgeRecord reservedAge = null;
        String dimensionKey = LinkOptions.getDimensionKey(book);
        if (dimensionKey == null || dimensionKey.isBlank()) {
            if (book.getItem() instanceof ItemAgebook) {
                try {
                    reservedAge = AgeManager.establish(player, book);
                    dimensionKey = reservedAge.dimensionKey();
                } catch (IllegalStateException | IllegalArgumentException failure) {
                    Mystcraft.LOGGER.warn("Could not establish Descriptive Book before linking", failure);
                    player.displayClientMessage(Component.translatable("message.mystcraft.link_unestablished"), true);
                    return false;
                }
            } else if (book.getItem() instanceof ItemLinkbook linkbook) {
                // A normal Linking Book without destination data is repaired exactly
                // like the old ItemLinking.validate()/initialize path: capture the
                // activator's current dimension, position and yaw without discarding
                // existing Link Panel flags/properties.
                linkbook.initializeAt(player.serverLevel(), book, player);
                dimensionKey = LinkOptions.getDimensionKey(book);
            }
        }

        if (dimensionKey == null || dimensionKey.isBlank()) {
            var reservation = AgeManager.resolveReservation(server, book);
            if (reservation.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.mystcraft.link_unestablished"), true);
                return false;
            }
            reservedAge = reservation.get();
            dimensionKey = reservedAge.dimensionKey();
        }

        // Build API/event link information only after first-use initialization so
        // listeners observe the same established destination the link will use.
        ILinkInfo linkInfo = LinkInfoAdapter.forStack(book);
        if (NeoForge.EVENT_BUS.post(new LinkEvent.LinkEventAllow(player.serverLevel(), player, linkInfo.clone())).isCanceled()) {
            return false;
        }

        ResourceLocation id = ResourceLocation.tryParse(dimensionKey);
        if (id == null) return false;

        ResourceKey<Level> targetKey = ResourceKey.create(Registries.DIMENSION, id);
        ServerLevel target = server.getLevel(targetKey);
        if (target == null) {
            AgeRecord age = reservedAge;
            if (age == null) {
                age = AgeManager.resolveReservation(server, book).orElse(null);
            }
            if (age != null) {
                try {
                    Mystcraft.LOGGER.debug(
                            "Age link target install/reinstall: uid={}, dimension={}, visited={}",
                            age.ageUid(), age.dimensionKey(), age.visited());
                    AgeRuntimeDimensionLoader.PrepareResult prepared =
                            AgeRuntimeDimensionLoader.prepare(server, age);
                    AgeDimensionDefinition definition = prepared.definition();

                    if (prepared.status() == AgeRuntimeDimensionLoader.Status.CONFLICT) {
                        player.displayClientMessage(Component.translatable(
                                "message.mystcraft.link_age_prepare_conflict",
                                age.ageName(), definition.levelKey().location().toString()), true);
                        return false;
                    }
                    if (prepared.status() == AgeRuntimeDimensionLoader.Status.DEAD) {
                        player.displayClientMessage(Component.translatable(
                                "message.mystcraft.link_age_install_failed",
                                age.ageName(), definition.levelKey().location().toString(), "DEAD"), true);
                        return false;
                    }

                    AgeRuntimeServerLevelInstaller.InstallResult installed =
                            AgeRuntimeServerLevelInstaller.install(server, age);
                    if (!installed.usable()) {
                        player.displayClientMessage(Component.translatable(
                                "message.mystcraft.link_age_install_failed",
                                age.ageName(), definition.levelKey().location().toString(), installed.status().name()), true);
                        return false;
                    }
                    target = installed.level();
                    Mystcraft.LOGGER.debug(
                            "Age link target ready: uid={}, dimension={}, liveAges={}",
                            age.ageUid(), age.dimensionKey(), AgeRuntimeWorldManager.countLiveAges(server));
                } catch (RuntimeException failure) {
                    // Invalid/corrupt worldgen data must fail this link cleanly, not
                    // escape through the custom-payload task and threaten the
                    // integrated/dedicated server tick loop. Startup restoration
                    // already isolates Ages individually; interactive linking must
                    // provide the same containment boundary.
                    Mystcraft.LOGGER.error(
                            "Failed to prepare/install Mystcraft Age {} ({}) for linking",
                            age.ageUid(), age.dimensionKey(), failure);
                    if (isFeatureOrderCycle(failure)) {
                        player.displayClientMessage(Component.translatable(
                                "message.mystcraft.link_age_description_contradiction"), true);
                    } else {
                        player.displayClientMessage(Component.translatable(
                                "message.mystcraft.link_age_install_failed",
                                age.ageName(), age.dimensionKey(), failure.getClass().getSimpleName()), true);
                    }
                    return false;
                }
            } else {
                player.displayClientMessage(Component.translatable(
                        "message.mystcraft.link_missing_dimension", dimensionKey), true);
                return false;
            }
        }

        final ServerLevel resolvedTarget = target;

        boolean sameDimension = player.level().dimension().equals(targetKey);
        boolean intra = LinkOptions.getFlag(book, LinkProperties.INTRA_LINKING);
        boolean intraOnly = LinkOptions.getFlag(book, LinkProperties.INTRA_LINKING_ONLY);
        if (sameDimension && !intra && !intraOnly) {
            player.displayClientMessage(Component.translatable("message.mystcraft.link_intra_denied"), true);
            return false;
        }
        if (!sameDimension && intraOnly) {
            player.displayClientMessage(Component.translatable("message.mystcraft.link_intra_only"), true);
            return false;
        }

        BlockPos targetPos = LinkOptions.getSpawn(book);
        if (targetPos == null) targetPos = resolvedTarget.getSharedSpawnPos();

        if (LinkOptions.getFlag(book, LinkProperties.RELATIVE)) {
            BlockPos sourceSpawn = player.serverLevel().getSharedSpawnPos();
            BlockPos targetSpawn = resolvedTarget.getSharedSpawnPos();
            BlockPos current = player.blockPosition();
            targetPos = targetSpawn.offset(
                    current.getX() - sourceSpawn.getX(),
                    current.getY() - sourceSpawn.getY(),
                    current.getZ() - sourceSpawn.getZ());
        }

        if (LinkOptions.getFlag(book, LinkProperties.GENERATE_PLATFORM)
                && resolvedTarget.isEmptyBlock(targetPos.below())
                && resolvedTarget.isEmptyBlock(targetPos.below(2))) {
            resolvedTarget.setBlockAndUpdate(targetPos.below(), Blocks.STONE.defaultBlockState());
        }

        LinkEvent.LinkEventAlter alterEvent = new LinkEvent.LinkEventAlter(
                player.serverLevel(), resolvedTarget, player, linkInfo.clone());
        NeoForge.EVENT_BUS.post(alterEvent);
        if (alterEvent.spawn != null) targetPos = alterEvent.spawn;
        float targetYaw = alterEvent.rotationYaw != null ? alterEvent.rotationYaw : LinkOptions.getSpawnYaw(book);

        if (NeoForge.EVENT_BUS.post(new LinkEvent.LinkEventAllow(player.serverLevel(), player, linkInfo.clone())).isCanceled()) {
            return false;
        }
        NeoForge.EVENT_BUS.post(new LinkEvent.LinkEventStart(player.serverLevel(), player, linkInfo.clone()));

        ServerLevel source = player.serverLevel();
        Vec3 sourcePos = player.position();
        Vec3 sourceVelocity = player.getDeltaMovement();
        ItemStack droppedBook = book.copyWithCount(1);
        boolean disarm = LinkOptions.getFlag(book, LinkProperties.DISARM);
        boolean following = LinkOptions.getFlag(book, LinkProperties.FOLLOWING);

        SoundEvent linkSound = soundOverride != null ? soundOverride : selectLinkSound(book);
        playLinkSound(source, sourcePos, linkSound);
        spawnLinkParticles(source, sourcePos);

        boolean teleported = player.teleportTo(
                resolvedTarget,
                targetPos.getX() + 0.5D,
                targetPos.getY(),
                targetPos.getZ() + 0.5D,
                Collections.emptySet(),
                targetYaw,
                player.getXRot());
        if (!teleported) {
            NeoForge.EVENT_BUS.post(new LinkEvent.LinkEventFailed(source, player, linkInfo.clone()));
            return false;
        }
        playLinkSound(resolvedTarget, player.position(), linkSound);
        spawnLinkParticles(resolvedTarget, player.position());

        // A successful arrival is the compatibility boundary for Visited and
        // runtime clock/spawn persistence. Resolve by dimension rather than by
        // book identity so ordinary Linking Books into an existing Age also sync.
        AgeManager.resolveByLevel(server, resolvedTarget.dimension()).ifPresent(age ->
                AgeRuntimeStateSync.capture(server, age, resolvedTarget, true));
        AgeVisualSync.sendCurrentAge(player);

        if (LinkOptions.getFlag(book, LinkProperties.MAINTAIN_MOMENTUM)) {
            player.setDeltaMovement(redirectMomentum(sourceVelocity, targetYaw));
        } else {
            player.setDeltaMovement(0.0D, 0.2D, 0.0D);
            player.fallDistance = 0.0F;
        }

        if (disarm) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty()) continue;
                ItemStack drop = stack.copy();
                player.getInventory().setItem(i, ItemStack.EMPTY);
                source.addFreshEntity(new ItemEntity(source, sourcePos.x, sourcePos.y, sourcePos.z, drop));
            }
        } else if (!portalTravel && !following) {
            player.setItemInHand(hand, ItemStack.EMPTY);
            ItemEntity location = new ItemEntity(source, sourcePos.x, sourcePos.y, sourcePos.z, droppedBook);
            location.setDeltaMovement(sourceVelocity);
            source.addFreshEntity(new EntityLinkbook(source, location, droppedBook));
        }

        NeoForge.EVENT_BUS.post(new LinkEvent.LinkEventEnd(source, resolvedTarget, player, linkInfo.clone()));
        triggerMystAgeTravelAdvancement(server, resolvedTarget, player);
        return true;
    }


    /**
     * Legacy 0.13.7.06 LinkListenerBasic dimension permission rule shared by
     * non-book entity links and Crystal Portal travel. Ordinary links cannot
     * target the dimension they originate in unless Intra Linking (or
     * Intra Linking Only) is present. Intra Linking Only additionally rejects
     * cross-dimension travel.
     */
    private static boolean isDimensionLinkPermitted(ServerLevel source, ServerLevel target, Entity entity, ILinkInfo info) {
        if (source == null || target == null || entity == null || info == null) return false;
        boolean sameDimension = source.dimension().equals(target.dimension());
        boolean intra = info.getFlag(LinkProperties.INTRA_LINKING);
        boolean intraOnly = info.getFlag(LinkProperties.INTRA_LINKING_ONLY);
        if (sameDimension && !intra && !intraOnly) {
            if (entity instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("message.mystcraft.link_intra_denied"), true);
            }
            return false;
        }
        if (!sameDimension && intraOnly) {
            if (entity instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("message.mystcraft.link_intra_only"), true);
            }
            return false;
        }
        return true;
    }

    /**
     * Public/API-compatible entity linking path.  Unlike the item-book path this
     * consumes only an ILinkInfo, matching 0.13.7.06 LinkController.travelEntity.
     * Numeric vanilla dimension ids retain their historical meanings; any other
     * positive id is resolved as a persisted Mystcraft AgeUID.
     */
    public static boolean travelEntity(ServerLevel source, Entity entity, ILinkInfo info) {
        return travelEntity(source, entity, info, null);
    }

    /** Same as {@link #travelEntity(ServerLevel, Entity, ILinkInfo)} with a forced link sound. */
    public static boolean travelEntity(ServerLevel source, Entity entity, ILinkInfo info, SoundEvent soundOverride) {
        if (source == null || entity == null || info == null || source.isClientSide()) return false;
        MinecraftServer server = source.getServer();
        if (server == null) return false;

        ILinkInfo working = info.clone();
        if (NeoForge.EVENT_BUS.post(new LinkEvent.LinkEventAllow(source, entity, working.clone())).isCanceled()) {
            return false;
        }

        Integer dimension = working.getDimensionUID();
        if (dimension == null) return false;
        ServerLevel target = resolveApiTarget(server, dimension);
        if (target == null) return false;

        if (!isDimensionLinkPermitted(source, target, entity, working)) {
            return false;
        }

        BlockPos targetPos = working.getSpawn();
        if (targetPos == null) {
            targetPos = target.getSharedSpawnPos();
            working.setSpawn(targetPos);
        }
        float targetYaw = working.getSpawnYaw();

        LinkEvent.LinkEventAlter alter = new LinkEvent.LinkEventAlter(source, target, entity, working.clone());
        NeoForge.EVENT_BUS.post(alter);
        if (alter.spawn != null) {
            targetPos = alter.spawn;
            working.setSpawn(targetPos);
        }
        if (alter.rotationYaw != null) {
            targetYaw = alter.rotationYaw;
            working.setSpawnYaw(targetYaw);
        }

        if (NeoForge.EVENT_BUS.post(new LinkEvent.LinkEventAllow(source, entity, working.clone())).isCanceled()) {
            return false;
        }
        NeoForge.EVENT_BUS.post(new LinkEvent.LinkEventStart(source, entity, working.clone()));

        if (working.getFlag(LinkProperties.GENERATE_PLATFORM)
                && target.isEmptyBlock(targetPos.below())
                && target.isEmptyBlock(targetPos.below(2))) {
            target.setBlockAndUpdate(targetPos.below(), Blocks.STONE.defaultBlockState());
        }

        Vec3 sourceVelocity = entity.getDeltaMovement();
        Vec3 sourcePos = entity.position();
        SoundEvent sound = soundOverride != null ? soundOverride : resolveLinkSound(working);
        if (sound != null) playLinkSound(source, sourcePos, sound);
        spawnLinkParticles(source, sourcePos);

        boolean moved = entity.teleportTo(
                target,
                targetPos.getX() + 0.5D,
                targetPos.getY(),
                targetPos.getZ() + 0.5D,
                Collections.emptySet(),
                targetYaw,
                entity.getXRot());
        if (!moved) {
            NeoForge.EVENT_BUS.post(new LinkEvent.LinkEventFailed(source, entity, working.clone()));
            return false;
        }

        if (sound != null) playLinkSound(target, entity.position(), sound);
        spawnLinkParticles(target, entity.position());
        if (working.getFlag(LinkProperties.MAINTAIN_MOMENTUM)) {
            entity.setDeltaMovement(redirectMomentum(sourceVelocity, targetYaw));
        } else {
            entity.setDeltaMovement(0.0D, 0.2D, 0.0D);
            entity.fallDistance = 0.0F;
        }
        entity.setPortalCooldown();

        AgeManager.resolveByLevel(server, target.dimension()).ifPresent(age ->
                AgeRuntimeStateSync.capture(server, age, target, true));
        if (entity instanceof ServerPlayer player) {
            AgeVisualSync.sendCurrentAge(player);
        }
        NeoForge.EVENT_BUS.post(new LinkEvent.LinkEventEnd(source, target, entity, working.clone()));
        if (entity instanceof ServerPlayer player) {
            triggerMystAgeTravelAdvancement(server, target, player);
        }
        return true;
    }


    /**
     * Crystal Portal transfer path for vehicles, riders and ordinary entities.
     *
     * <p>The first entity touching the surface promotes the transfer to its root
     * vehicle.  The complete passenger tree is snapshotted, detached, moved as one
     * transaction, reattached at the destination, and only then released.  This
     * prevents the vanilla cross-dimension teleport from leaving a rider behind
     * while the minecart/boat moves first.</p>
     */
    public static boolean travelPortalGroup(ServerLevel source, Entity touched, ILinkInfo info, SoundEvent soundOverride) {
        if (source == null || touched == null || info == null || source.isClientSide()) return false;
        MinecraftServer server = source.getServer();
        if (server == null) return false;

        Entity root = touched.getRootVehicle();
        if (root == null) root = touched;

        // CP275: do not reject Crystal Portal travel because vanilla's generic
        // portal cooldown is still counting down.  Mystcraft portals are commonly
        // chained only a few blocks apart, while the vanilla cooldown is designed
        // for much slower Nether/End-style dimension travel.  The per-UUID pending
        // transaction gate below is the authoritative anti-bounce/anti-double-link
        // guard for Crystal Portals and remains active until momentum and riding
        // reconstruction have settled.
        List<PortalMember> members = new ArrayList<>();
        List<PortalTransferRuntime.PreparedMember> prepared =
                PortalTransferRuntime.getPreparedGroup(server, root.getUUID());
        if (!prepared.isEmpty()) {
            // CP320: a very short portal chain can arrive before the previous
            // destination's client-side riding graph has been synchronized.  Use the
            // virtual parent relation prepared by PortalTransferRuntime instead of
            // calling startRiding just to snapshot the tree (which itself emits the
            // passengers packet we are trying to avoid racing).
            for (PortalTransferRuntime.PreparedMember member : prepared) {
                Entity entity = member.entity();
                if (entity == null || entity.isRemoved()) return false;
                members.add(new PortalMember(
                        entity,
                        entity.getUUID(),
                        member.parentId(),
                        entity.position(),
                        entity.getDeltaMovement(),
                        entity.getYRot(),
                        entity.getXRot(),
                        entity.fallDistance));
            }
        } else {
            snapshotPortalTree(root, null, members);
        }
        if (members.isEmpty()) return false;

        // CP271 transaction gate: ordinary crossings wait for the previous riding
        // repair.  CP320's prepared virtual group is the explicit fast-chain handoff,
        // so its old pending states are intentionally allowed through and are only
        // consumed once the new transfer has passed all link validation below.
        if (prepared.isEmpty()) {
            for (PortalMember member : members) {
                if (PortalTransferRuntime.isPending(server, member.entityId())) {
                    member.entity().setPortalCooldown();
                    return false;
                }
            }
        }

        ILinkInfo working = info.clone();
        if (NeoForge.EVENT_BUS.post(new LinkEvent.LinkEventAllow(source, root, working.clone())).isCanceled()) {
            return false;
        }

        Integer dimension = working.getDimensionUID();
        if (dimension == null) return false;
        ServerLevel target = resolveApiTarget(server, dimension);
        if (target == null) return false;

        if (!isDimensionLinkPermitted(source, target, root, working)) {
            return false;
        }

        // Crystal Portal travel is a short physical transaction.  The destination
        // Age must remain installed until every vehicle/passenger/projectile has a
        // destination-side entity instance and its captured physics has been restored.
        AgeRuntimeWorldManager.holdForPortalTransfer(server, source.dimension(), 240L);
        AgeRuntimeWorldManager.holdForPortalTransfer(server, target.dimension(), 240L);

        BlockPos targetPos = working.getSpawn();
        if (targetPos == null) {
            targetPos = target.getSharedSpawnPos();
            working.setSpawn(targetPos);
        }
        float targetYaw = working.getSpawnYaw();

        LinkEvent.LinkEventAlter alter = new LinkEvent.LinkEventAlter(source, target, root, working.clone());
        NeoForge.EVENT_BUS.post(alter);
        if (alter.spawn != null) {
            targetPos = alter.spawn;
            working.setSpawn(targetPos);
        }
        if (alter.rotationYaw != null) {
            targetYaw = alter.rotationYaw;
            working.setSpawnYaw(targetYaw);
        }
        if (NeoForge.EVENT_BUS.post(new LinkEvent.LinkEventAllow(source, root, working.clone())).isCanceled()) {
            return false;
        }
        NeoForge.EVENT_BUS.post(new LinkEvent.LinkEventStart(source, root, working.clone()));

        // CP320: all permission/target/event validation has succeeded.  It is now
        // safe to retire the previous pending repair states and consume the virtual
        // riding graph as the authoritative source for this next transaction.
        if (!prepared.isEmpty()) {
            PortalTransferRuntime.commitPreparedGroup(server, root.getUUID());
        }

        if (working.getFlag(LinkProperties.GENERATE_PLATFORM)
                && target.isEmptyBlock(targetPos.below())
                && target.isEmptyBlock(targetPos.below(2))) {
            target.setBlockAndUpdate(targetPos.below(), Blocks.STONE.defaultBlockState());
        }

        Vec3 sourcePos = root.position();
        SoundEvent sound = soundOverride != null ? soundOverride : resolveLinkSound(working);
        if (sound != null) playLinkSound(source, sourcePos, sound);
        spawnLinkParticles(source, sourcePos);

        // Stop repeated entityInside callbacks while the complete riding tree is
        // detached.  We intentionally transfer passengers first, matching the
        // 0.13.7.06 LinkController, then transfer the root vehicle last.
        for (PortalMember member : members) member.entity().setPortalCooldown();
        for (int i = members.size() - 1; i >= 0; i--) {
            PortalMember member = members.get(i);
            if (member.parentId() != null && member.entity().isPassenger()) {
                member.entity().stopRiding();
            }
        }

        double tx = targetPos.getX() + 0.5D;
        double ty = targetPos.getY();
        double tz = targetPos.getZ() + 0.5D;
        boolean keepMomentum = working.getFlag(LinkProperties.MAINTAIN_MOMENTUM);
        Map<UUID, Entity> destinationEntities = new HashMap<>();
        boolean movedAll = true;

        // Reverse root-first snapshot order: deepest passengers cross first and the
        // vehicle crosses last.  More importantly, after each teleport we resolve
        // the entity again by UUID from the destination level.  Cross-dimension
        // teleport may replace the Java Entity instance, so mutating the old object
        // is exactly what caused CP267 riders to separate and arrows to lose motion.
        for (int i = members.size() - 1; i >= 0; i--) {
            PortalMember member = members.get(i);
            Entity original = member.entity();
            float yaw = member.parentId() == null ? targetYaw : member.yaw();

            // Defensive UUID guard.  Dynamic Age unload/reload and repeated portal
            // stress tests exposed rare vanilla entity-manager warnings about an
            // already-present UUID.  Never inject a second non-player entity with
            // the same UUID into the destination.  A pre-existing destination copy
            // is necessarily stale for this transfer because the authoritative
            // original is still in the source level at this point.
            if (!(original instanceof ServerPlayer)) {
                Entity duplicate = target.getEntity(member.entityId());
                if (duplicate != null && duplicate != original && !duplicate.isRemoved()) {
                    Mystcraft.LOGGER.warn(
                            "Removed stale destination entity before Crystal Portal transfer: uuid={}, type={}, destination={}",
                            member.entityId(), duplicate.getType(), target.dimension().location());
                    duplicate.discard();
                }
            }

            boolean moved = original.teleportTo(
                    target, tx, ty, tz, Collections.emptySet(), yaw, member.pitch());
            if (!moved) {
                movedAll = false;
                Mystcraft.LOGGER.warn("Crystal Portal group member failed to teleport: {} ({})",
                        original.getType(), original.getUUID());
                break;
            }

            Entity arrived;
            if (original instanceof ServerPlayer) {
                ServerPlayer destinationPlayer = server.getPlayerList().getPlayer(member.entityId());
                arrived = destinationPlayer != null && destinationPlayer.serverLevel() == target
                        ? destinationPlayer
                        : null;
            } else {
                arrived = target.getEntity(member.entityId());
            }
            if (arrived == null && original.level() == target && !original.isRemoved()) {
                arrived = original;
            }
            if (arrived != null) {
                destinationEntities.put(member.entityId(), arrived);
            }
        }

        if (!movedAll) {
            rollbackPortalGroup(server, source, target, members);
            NeoForge.EVENT_BUS.post(new LinkEvent.LinkEventFailed(source, root, working.clone()));
            return false;
        }

        // CP273 deliberately does NOT re-seat passengers in this same tick.
        // PortalTransferRuntime waits a few ticks so the client knows both the
        // destination vehicle and rider before vanilla sends the passengers packet.
        // This prevents "Received passengers for unknown entity" during long PV-style
        // chains without weakening the transaction/timeout safety net.

        // Apply captured physics to the destination-side instances immediately and
        // stage a short post-teleport repair.  The second application occurs after
        // the first ordinary entity tick, covering projectiles and vehicles whose
        // own arrival logic overwrites velocity after dimension transfer.
        for (PortalMember member : members) {
            Vec3 destinationVelocity = keepMomentum
                    ? redirectMomentum(member.velocity(), targetYaw)
                    : new Vec3(0.0D, 0.2D, 0.0D);
            float destinationFall = keepMomentum ? member.fallDistance() : 0.0F;
            Entity arrived = destinationEntities.get(member.entityId());
            if (arrived != null) {
                arrived.setDeltaMovement(destinationVelocity);
                arrived.fallDistance = destinationFall;
                arrived.setPortalCooldown();
            }
            // CP276: a lone dropped ItemEntity does not need riding-graph recovery.
            // Vanilla may merge/discard the destination stack before the 100-tick
            // repair window finishes, leaving an otherwise successful transfer to
            // expire by UUID.  Preserve the repair stage for every other entity type
            // (especially projectiles, vehicles, players and riding groups).
            boolean needsPostTeleportRepair = !(members.size() == 1
                    && arrived instanceof net.minecraft.world.entity.item.ItemEntity);
            if (needsPostTeleportRepair) {
                PortalTransferRuntime.stage(
                        server,
                        target.dimension(),
                        root.getUUID(),
                        member.entityId(),
                        member.parentId(),
                        destinationVelocity,
                        destinationFall,
                        keepMomentum,
                        new Vec3(tx, ty, tz),
                        member.parentId() == null ? targetYaw : member.yaw(),
                        member.pitch());
            }
        }

        Entity destinationRoot = destinationEntities.get(root.getUUID());
        Vec3 destinationFxPos = destinationRoot != null
                ? destinationRoot.position()
                : new Vec3(tx, ty, tz);
        if (sound != null) playLinkSound(target, destinationFxPos, sound);
        spawnLinkParticles(target, destinationFxPos);
        AgeRuntimeWorldManager.holdForPortalTransfer(server, target.dimension(), 240L);

        AgeManager.resolveByLevel(server, target.dimension()).ifPresent(age ->
                AgeRuntimeStateSync.capture(server, age, target, true));
        for (PortalMember member : members) {
            Entity arrived = destinationEntities.get(member.entityId());
            if (arrived instanceof ServerPlayer player) {
                AgeVisualSync.sendCurrentAge(player);
                triggerMystAgeTravelAdvancement(server, target, player);
            }
        }

        Entity eventRoot = destinationRoot != null ? destinationRoot : root;
        NeoForge.EVENT_BUS.post(new LinkEvent.LinkEventEnd(source, target, eventRoot, working.clone()));
        Mystcraft.LOGGER.debug(
                "Crystal Portal transactional transfer: root={}, members={}, {} -> {}, momentum={}, resolvedNow={}/{}",
                root.getType(), members.size(), source.dimension().location(), target.dimension().location(),
                keepMomentum, destinationEntities.size(), members.size());
        return true;
    }

    private static void snapshotPortalTree(Entity entity, Entity parent, List<PortalMember> out) {
        out.add(new PortalMember(
                entity,
                entity.getUUID(),
                parent == null ? null : parent.getUUID(),
                entity.position(),
                entity.getDeltaMovement(),
                entity.getYRot(),
                entity.getXRot(),
                entity.fallDistance));
        for (Entity passenger : entity.getPassengers()) snapshotPortalTree(passenger, entity, out);
    }

    /**
     * Best-effort transaction rollback for the rare case where a member of a
     * vehicle/passenger tree fails after earlier members have already crossed.
     *
     * <p>Cross-dimension teleport may replace the Java entity instance, so every
     * member is resolved by UUID before being returned.  Riding relations are
     * rebuilt only after all recoverable members are back in the source level.</p>
     */
    private static void rollbackPortalGroup(
            MinecraftServer server,
            ServerLevel source,
            ServerLevel target,
            List<PortalMember> members) {
        Map<UUID, Entity> restored = new HashMap<>();
        boolean complete = true;

        for (PortalMember member : members) {
            Entity current;
            if (member.entity() instanceof ServerPlayer) {
                ServerPlayer player = server.getPlayerList().getPlayer(member.entityId());
                current = player;
            } else {
                current = source.getEntity(member.entityId());
                if (current == null || current.isRemoved()) current = target.getEntity(member.entityId());
                if ((current == null || current.isRemoved())
                        && member.entity().getUUID().equals(member.entityId())
                        && !member.entity().isRemoved()) {
                    current = member.entity();
                }
            }

            if (current == null || current.isRemoved()) {
                complete = false;
                Mystcraft.LOGGER.error(
                        "Crystal Portal rollback could not resolve entity: uuid={}, source={}, target={}",
                        member.entityId(), source.dimension().location(), target.dimension().location());
                continue;
            }

            if (current.level() != source) {
                Vec3 pos = member.position();
                boolean returned = current.teleportTo(
                        source, pos.x, pos.y, pos.z, Collections.emptySet(), member.yaw(), member.pitch());
                if (!returned) {
                    complete = false;
                    Mystcraft.LOGGER.error(
                            "Crystal Portal rollback teleport failed: uuid={}, type={}, {} -> {}",
                            member.entityId(), current.getType(), target.dimension().location(), source.dimension().location());
                    continue;
                }
            }

            Entity sourceEntity;
            if (current instanceof ServerPlayer) {
                ServerPlayer player = server.getPlayerList().getPlayer(member.entityId());
                sourceEntity = player != null && player.serverLevel() == source ? player : null;
            } else {
                sourceEntity = source.getEntity(member.entityId());
                if (sourceEntity == null && current.level() == source && !current.isRemoved()) sourceEntity = current;
            }
            if (sourceEntity == null || sourceEntity.isRemoved()) {
                complete = false;
                Mystcraft.LOGGER.error(
                        "Crystal Portal rollback lost returned entity: uuid={}, source={}",
                        member.entityId(), source.dimension().location());
                continue;
            }

            sourceEntity.stopRiding();
            sourceEntity.setDeltaMovement(member.velocity());
            sourceEntity.fallDistance = member.fallDistance();
            sourceEntity.setPortalCooldown();
            restored.put(member.entityId(), sourceEntity);
        }

        for (PortalMember member : members) {
            if (member.parentId() == null) continue;
            Entity child = restored.get(member.entityId());
            Entity parent = restored.get(member.parentId());
            if (child == null || parent == null || !child.startRiding(parent, true)) {
                complete = false;
                Mystcraft.LOGGER.error(
                        "Crystal Portal rollback could not restore riding relation: rider={}, vehicle={}",
                        member.entityId(), member.parentId());
            }
        }

        Mystcraft.LOGGER.warn(
                "Crystal Portal group transfer rolled back after partial failure: members={}, restored={}, complete={}",
                members.size(), restored.size(), complete);
    }

    private record PortalMember(
            Entity entity,
            UUID entityId,
            UUID parentId,
            Vec3 position,
            Vec3 velocity,
            float yaw,
            float pitch,
            float fallDistance) {}

    /** Legacy LinkListenerBasic Quinn/safe advancement check, executed after LinkEventEnd. */
    private static void triggerMystAgeTravelAdvancement(MinecraftServer server, ServerLevel destination, ServerPlayer player) {
        if (AgeManager.resolveByLevel(server, destination.dimension()).isEmpty()) return;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemLinkbook) {
                MystCriteriaTriggers.ENTER_MYST_DIMENSION_SAFE.get().trigger(player);
                return;
            }
        }
        MystCriteriaTriggers.ENTER_MYST_DIMENSION_QUINN.get().trigger(player);
    }

    private static ServerLevel resolveApiTarget(MinecraftServer server, int dimension) {
        if (dimension == 0) return server.overworld();
        if (dimension == -1) return server.getLevel(Level.NETHER);
        if (dimension == 1) return server.getLevel(Level.END);

        AgeRecord age = AgeRegistryData.get(server).byUid(dimension).orElse(null);
        if (age == null) return null;
        try {
            AgeRuntimeDimensionLoader.PrepareResult prepared = AgeRuntimeDimensionLoader.prepare(server, age);
            if (prepared.status() == AgeRuntimeDimensionLoader.Status.CONFLICT
                    || prepared.status() == AgeRuntimeDimensionLoader.Status.DEAD) return null;
            AgeRuntimeServerLevelInstaller.InstallResult installed = AgeRuntimeServerLevelInstaller.install(server, age);
            return installed.usable() ? installed.level() : null;
        } catch (RuntimeException failure) {
            Mystcraft.LOGGER.error("Failed to resolve API link target Age {} ({})", age.ageUid(), age.dimensionKey(), failure);
            return null;
        }
    }

    private static SoundEvent resolveLinkSound(ILinkInfo info) {
        String property = info.getProperty(LinkProperties.SOUND);
        if (property == null || property.isBlank()) return MystSounds.LINK.get();
        ResourceLocation key = ResourceLocation.tryParse(property);
        if (key == null) return MystSounds.LINK.get();
        return net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getOptional(key).orElse(MystSounds.LINK.get());
    }

    private static SoundEvent selectLinkSound(ItemStack book) {
        if (LinkOptions.getFlag(book, LinkProperties.DISARM)) return MystSounds.LINK_DISARM.get();
        if (LinkOptions.getFlag(book, LinkProperties.FOLLOWING)) return MystSounds.LINK_FOLLOWING.get();
        if (LinkOptions.getFlag(book, LinkProperties.INTRA_LINKING)) return MystSounds.LINK_INTRA.get();
        return MystSounds.LINK.get();
    }

    private static void playLinkSound(ServerLevel level, Vec3 pos, SoundEvent event) {
        float pitch = 0.9F + level.getRandom().nextFloat() * 0.2F;
        level.playSound(null, pos.x, pos.y, pos.z, event, SoundSource.PLAYERS, 0.8F, pitch);
    }

    /**
     * 0.13.7.06 MPacketParticles("link") emitted fifty dark particles in
     * roughly a 2-block-high volume at both ends of every successful link.
     * The old particle-sheet sprite no longer exists as an addressable type in
     * 1.21.1, so the geometry/count/lifecycle are retained using modern smoke.
     */
    private static void spawnLinkParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y + 1.0D, pos.z, 50, 1.0D, 1.0D, 1.0D, 0.01D);
    }

    /** Mirrors the legacy LinkListenerBasic horizontal momentum redirect. */
    private static Vec3 redirectMomentum(Vec3 motion, float targetYaw) {
        double rotationYaw = Math.toDegrees(Math.atan2(motion.x, motion.z));
        double cos = Math.cos(Math.toRadians(-rotationYaw));
        double sin = Math.sin(Math.toRadians(-rotationYaw));
        double localX = cos * motion.x - sin * motion.z;
        double localZ = sin * motion.x + cos * motion.z;

        cos = Math.cos(Math.toRadians(targetYaw));
        sin = Math.sin(Math.toRadians(targetYaw));
        double redirectedX = cos * localX - sin * localZ;
        double redirectedZ = sin * localX + cos * localZ;
        return new Vec3(redirectedX, motion.y + 0.2D, redirectedZ);
    }

    private static boolean isFeatureOrderCycle(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message != null && message.contains("Feature order cycle found")) return true;
        }
        return false;
    }

}
