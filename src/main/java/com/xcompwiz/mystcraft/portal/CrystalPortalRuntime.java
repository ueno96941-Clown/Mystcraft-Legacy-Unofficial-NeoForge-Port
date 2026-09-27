package com.xcompwiz.mystcraft.portal;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.block.BlockBookReceptacle;
import com.xcompwiz.mystcraft.block.BlockLinkPortal;
import com.xcompwiz.mystcraft.blockentity.BookReceptacleBlockEntity;
import com.xcompwiz.mystcraft.registry.MystBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Crystal Portal runtime.
 *
 * <p>CP263B stopped the CP262 reactivation storm by accepting only complete vertical
 * rectangles.  CP265 restores the original 0.13.7.06 force-field freedom: connected
 * Crystal networks may form horizontal, irregular and multi-face portal surfaces.
 * The Legacy pulse/tension algorithm is simulated entirely in memory first, then the
 * final stable cells are committed atomically, preserving the CP263B freeze guard.</p>
 */
public final class CrystalPortalRuntime {
    private static final int MAX_WORK = 16384;

    private CrystalPortalRuntime() {}

    public static boolean activate(ServerLevel level, BlockPos controllerPos, BookReceptacleBlockEntity controller) {
        BlockState controllerState = level.getBlockState(controllerPos);
        if (!controllerState.is(MystBlocks.BOOK_RECEPTACLE.get())) return false;

        List<BlockPos> bases = adjacentCrystalBases(level, controllerPos, controllerState);
        if (bases.isEmpty()) {
            Mystcraft.LOGGER.warn("Crystal Portal: receptacle {} has no adjacent Crystal", controllerPos);
            controller.setPortalGeometry(List.of(), List.of());
            return false;
        }

        controller.beginPortalMutation();
        try {
            for (BlockPos base : bases) {
                Set<BlockPos> component = collectCrystalComponent(level, base);
                PortalShape shape = discoverLegacyShape(level, component);
                if (shape == null) continue;

                List<BlockPos> portals = fillShape(level, shape);
                if (portals.isEmpty()) continue;

                controller.setPortalGeometry(shape.frame(), portals);
                refreshRenderStates(level, portals);
                Mystcraft.LOGGER.debug(
                        "Crystal Portal activated at {} (legacySurface crystals={}, portalCells={})",
                        controllerPos, shape.frame().size(), portals.size());
                return true;
            }

            controller.setPortalGeometry(List.of(), List.of());
            Mystcraft.LOGGER.warn("Crystal Portal: no stable enclosed Crystal surface adjacent to {}", controllerPos);
            return false;
        } finally {
            controller.endPortalMutation();
        }
    }

    public static boolean isStillValid(ServerLevel level, BookReceptacleBlockEntity controller) {
        if (!controller.hasBook() || controller.getPortalBlocks().isEmpty() || controller.getFrameBlocks().isEmpty()) return false;
        if (!level.getBlockState(controller.getBlockPos()).is(MystBlocks.BOOK_RECEPTACLE.get())) return false;

        for (BlockPos frame : controller.getFrameBlocks()) {
            if (!level.getBlockState(frame).is(MystBlocks.CRYSTAL.get())) return false;
        }
        for (BlockPos portal : controller.getPortalBlocks()) {
            if (!level.getBlockState(portal).is(MystBlocks.LINK_PORTAL.get())) return false;
        }
        return true;
    }

    /** Re-evaluate receptacles reachable from the changed Crystal/Portal component. */
    public static void refreshCrystalNetwork(ServerLevel level, BlockPos start) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> controllers = new HashSet<>();
        queue.add(start);

        while (!queue.isEmpty() && visited.size() < MAX_WORK) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) continue;
            if (!isPortalNetworkBlock(level.getBlockState(pos))) continue;
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                BlockEntity be = level.getBlockEntity(next);
                if (be instanceof BookReceptacleBlockEntity) controllers.add(next.immutable());
                if (!visited.contains(next) && isPortalNetworkBlock(level.getBlockState(next))) queue.addLast(next);
            }
        }

        for (BlockPos pos : controllers) {
            if (level.getBlockEntity(pos) instanceof BookReceptacleBlockEntity receptacle) {
                receptacle.refreshPortal(level);
            }
        }
    }

    public static BookReceptacleBlockEntity findController(ServerLevel level, BlockPos portalPos) {
        if (!level.getBlockState(portalPos).is(MystBlocks.LINK_PORTAL.get())) return null;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(portalPos);
        while (!queue.isEmpty() && visited.size() < MAX_WORK) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) continue;
            if (!isPortalNetworkBlock(level.getBlockState(pos))) continue;
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                BlockEntity be = level.getBlockEntity(next);
                if (be instanceof BookReceptacleBlockEntity receptacle && receptacle.hasBook() && receptacle.controls(portalPos)) {
                    return receptacle;
                }
                if (!visited.contains(next) && isPortalNetworkBlock(level.getBlockState(next))) queue.addLast(next);
            }
        }
        return null;
    }

    public static void validatePortalAt(ServerLevel level, BlockPos portalPos) {
        if (!level.getBlockState(portalPos).is(MystBlocks.LINK_PORTAL.get())) return;
        BookReceptacleBlockEntity controller = findController(level, portalPos);
        if (controller == null) {
            level.removeBlock(portalPos, false);
            return;
        }
        if (!controller.isPortalMutation() && !isStillValid(level, controller)) controller.refreshPortal(level);
    }

    /** Retained for compatibility/tests; complete rectangular portals satisfy this naturally. */
    public static boolean checkPortalTension(ServerLevel level, BlockPos pos) {
        int score = 0;
        if (isPortalNetworkBlock(level.getBlockState(pos.east())) && isPortalNetworkBlock(level.getBlockState(pos.west()))) score++;
        if (isPortalNetworkBlock(level.getBlockState(pos.above())) && isPortalNetworkBlock(level.getBlockState(pos.below()))) score++;
        if (isPortalNetworkBlock(level.getBlockState(pos.south())) && isPortalNetworkBlock(level.getBlockState(pos.north()))) score++;
        return score > 1;
    }

    private static List<BlockPos> adjacentCrystalBases(ServerLevel level, BlockPos controllerPos, BlockState controllerState) {
        List<BlockPos> bases = new ArrayList<>();
        Direction facing = controllerState.getValue(BlockBookReceptacle.ROTATION);
        BlockPos preferred = controllerPos.relative(facing.getOpposite());
        if (level.getBlockState(preferred).is(MystBlocks.CRYSTAL.get())) bases.add(preferred.immutable());
        for (Direction direction : Direction.values()) {
            BlockPos candidate = controllerPos.relative(direction);
            if (level.getBlockState(candidate).is(MystBlocks.CRYSTAL.get()) && !bases.contains(candidate)) bases.add(candidate.immutable());
        }
        return bases;
    }

    private static Set<BlockPos> collectCrystalComponent(ServerLevel level, BlockPos base) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        if (level.getBlockState(base).is(MystBlocks.CRYSTAL.get())) queue.add(base);
        while (!queue.isEmpty() && visited.size() < MAX_WORK) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) continue;
            if (!level.getBlockState(pos).is(MystBlocks.CRYSTAL.get())) continue;
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!visited.contains(next) && level.getBlockState(next).is(MystBlocks.CRYSTAL.get())) queue.addLast(next);
            }
        }
        return visited;
    }

    /**
     * Replays the useful result of Legacy PortalUtils#onpulse without mutating the world
     * during discovery.  The old code grew portal cells from any air position touching at
     * least two Crystal/Portal neighbours, then removed cells which lacked tension on two
     * opposing axes.  Doing that incrementally in 1.21 caused neighbour-update re-entry and
     * the CP262 one-grey-cell retry storm, so CP265 performs the same growth in memory and
     * commits only the final stable surface.
     */
    private static PortalShape discoverLegacyShape(ServerLevel level, Set<BlockPos> component) {
        if (component.size() < 3) return null;

        int minX = component.stream().mapToInt(BlockPos::getX).min().orElse(0);
        int maxX = component.stream().mapToInt(BlockPos::getX).max().orElse(0);
        int minY = component.stream().mapToInt(BlockPos::getY).min().orElse(0);
        int maxY = component.stream().mapToInt(BlockPos::getY).max().orElse(0);
        int minZ = component.stream().mapToInt(BlockPos::getZ).min().orElse(0);
        int maxZ = component.stream().mapToInt(BlockPos::getZ).max().orElse(0);

        Set<BlockPos> active = new HashSet<>();
        for (BlockPos pos : component) active.add(pos.immutable());
        Set<BlockPos> portals = new HashSet<>();
        ArrayDeque<BlockPos> candidates = new ArrayDeque<>();
        Set<BlockPos> queued = new HashSet<>();

        for (BlockPos crystal : component) addLegacySurrounding(candidates, queued, crystal, minX, maxX, minY, maxY, minZ, maxZ);

        int examined = 0;
        while (!candidates.isEmpty() && examined++ < MAX_WORK) {
            BlockPos pos = candidates.removeFirst();
            queued.remove(pos);
            if (active.contains(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!(state.isAir() || state.is(MystBlocks.LINK_PORTAL.get()))) continue;
            if (validNeighbourCount(active, pos) <= 1) continue;

            BlockPos stable = pos.immutable();
            active.add(stable);
            portals.add(stable);
            addLegacySurrounding(candidates, queued, stable, minX, maxX, minY, maxY, minZ, maxZ);
        }

        if (examined >= MAX_WORK) {
            Mystcraft.LOGGER.warn("Crystal Portal discovery hit safety cap (crystals={}, candidatesRemaining={})", component.size(), candidates.size());
            return null;
        }

        // Legacy validates created cells after growth.  Prune repeatedly to a fixed point so
        // no committed portal cell depends on another cell which itself cannot remain stable.
        boolean changed;
        do {
            changed = false;
            List<BlockPos> remove = new ArrayList<>();
            for (BlockPos pos : portals) {
                if (!hasPortalTension(active, pos)) remove.add(pos);
            }
            if (!remove.isEmpty()) {
                changed = true;
                portals.removeAll(remove);
                active.removeAll(remove);
            }
        } while (changed && !portals.isEmpty());

        if (portals.isEmpty()) return null;
        List<BlockPos> frame = component.stream().map(BlockPos::immutable).toList();
        List<BlockPos> interior = portals.stream().map(BlockPos::immutable).toList();
        return new PortalShape(frame, interior);
    }

    private static int validNeighbourCount(Set<BlockPos> active, BlockPos pos) {
        int score = 0;
        for (Direction direction : Direction.values()) if (active.contains(pos.relative(direction))) score++;
        return score;
    }

    private static boolean hasPortalTension(Set<BlockPos> active, BlockPos pos) {
        int score = 0;
        if (active.contains(pos.east()) && active.contains(pos.west())) score++;
        if (active.contains(pos.above()) && active.contains(pos.below())) score++;
        if (active.contains(pos.south()) && active.contains(pos.north())) score++;
        return score > 1;
    }

    /** Legacy PortalUtils#addSurrounding: six orthogonal + twelve edge-diagonal positions. */
    private static void addLegacySurrounding(ArrayDeque<BlockPos> queue, Set<BlockPos> queued, BlockPos pos,
                                             int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        int[][] offsets = {
                {1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1},
                {1,1,0},{-1,1,0},{1,-1,0},{-1,-1,0},
                {0,1,1},{0,1,-1},{0,-1,1},{0,-1,-1},
                {1,0,1},{-1,0,1},{1,0,-1},{-1,0,-1}
        };
        for (int[] o : offsets) {
            BlockPos next = pos.offset(o[0], o[1], o[2]);
            // A stable Legacy force-field surface never needs to escape the Crystal network's
            // own bounding box.  This bound is the hard guard against open-frame runaway.
            if (next.getX() < minX || next.getX() > maxX
                    || next.getY() < minY || next.getY() > maxY
                    || next.getZ() < minZ || next.getZ() > maxZ) continue;
            BlockPos immutable = next.immutable();
            if (queued.add(immutable)) queue.addLast(immutable);
        }
    }

    private static List<BlockPos> fillShape(ServerLevel level, PortalShape shape) {
        List<BlockPos> placed = new ArrayList<>(shape.interior().size());
        for (BlockPos pos : shape.interior()) {
            BlockState state = level.getBlockState(pos);
            if (!(state.isAir() || state.is(MystBlocks.LINK_PORTAL.get()))) return List.of();
        }
        for (BlockPos pos : shape.interior()) {
            // Start full; once every cell exists deriveRenderState can safely choose a single
            // plane for ordinary walls while corners/multi-face junctions remain full cubes.
            level.setBlock(pos, MystBlocks.LINK_PORTAL.get().defaultBlockState()
                    .setValue(BlockLinkPortal.FULL, true), 2);
            placed.add(pos.immutable());
        }
        return List.copyOf(placed);
    }

    private static void refreshRenderStates(ServerLevel level, List<BlockPos> portals) {
        for (BlockPos pos : portals) {
            BlockState oldState = level.getBlockState(pos);
            if (!oldState.is(MystBlocks.LINK_PORTAL.get())) continue;
            BlockState newState = BlockLinkPortal.deriveRenderState(level, pos, oldState);
            if (!newState.equals(oldState)) level.setBlock(pos, newState, 2);
        }
    }

    private static boolean isPortalNetworkBlock(BlockState state) {
        return state.is(MystBlocks.CRYSTAL.get()) || state.is(MystBlocks.LINK_PORTAL.get());
    }

    private record PortalShape(List<BlockPos> frame, List<BlockPos> interior) {}
}
