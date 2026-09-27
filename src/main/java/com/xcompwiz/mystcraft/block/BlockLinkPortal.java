package com.xcompwiz.mystcraft.block;

import com.mojang.serialization.MapCodec;
import com.xcompwiz.mystcraft.blockentity.BookReceptacleBlockEntity;
import com.xcompwiz.mystcraft.api.item.IItemPortalActivator;
import net.minecraft.world.item.ItemStack;
import com.xcompwiz.mystcraft.portal.CrystalPortalRuntime;
import com.xcompwiz.mystcraft.registry.MystBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public final class BlockLinkPortal extends Block {
    public static final MapCodec<BlockLinkPortal> CODEC = simpleCodec(BlockLinkPortal::new);
    /** Normal axis of the single rendered portal plane. */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    /** Legacy HAS_ROTATION=false equivalent: render as the full portal cube. */
    public static final BooleanProperty FULL = BooleanProperty.create("full");

    public BlockLinkPortal(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(FULL, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, FULL);
    }

    @Override
    protected MapCodec<BlockLinkPortal> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        float minX = isPortalNetworkBlock(level.getBlockState(pos.west())) ? 0.0F : 0.25F;
        float maxX = isPortalNetworkBlock(level.getBlockState(pos.east())) ? 1.0F : 0.75F;
        float minY = isPortalNetworkBlock(level.getBlockState(pos.below())) ? 0.0F : 0.25F;
        float maxY = isPortalNetworkBlock(level.getBlockState(pos.above())) ? 1.0F : 0.75F;
        float minZ = isPortalNetworkBlock(level.getBlockState(pos.north())) ? 0.0F : 0.25F;
        float maxZ = isPortalNetworkBlock(level.getBlockState(pos.south())) ? 1.0F : 0.75F;
        return box(minX * 16.0, minY * 16.0, minZ * 16.0, maxX * 16.0, maxY * 16.0, maxZ * 16.0);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level instanceof ServerLevel serverLevel && entity != null) {
            tryPortalCollision(serverLevel, pos, entity);
        }
    }

    /**
     * Shared Crystal Portal entry point used both by ordinary entityInside callbacks
     * and CP276's swept projectile detector.
     */
    public static void tryPortalCollision(ServerLevel serverLevel, BlockPos pos, Entity entity) {
        if (serverLevel == null || pos == null || entity == null || entity.isRemoved()) return;

        // CP316: a rider may overlap the portal surface immediately after mounting a
        // minecart/boat which is parked directly underneath/beside the frame.  Contact
        // alone must not start a dimension transfer: the root vehicle itself has to
        // cross the portal plane.  This keeps a stationary vehicle mountable while
        // still promoting a genuine crossing to the complete passenger tree.
        Entity traveller = entity.getRootVehicle();
        if (traveller == null) traveller = entity;
        BlockState portalState = serverLevel.getBlockState(pos);
        if (!portalState.is(MystBlocks.LINK_PORTAL.get())) return;
        boolean crossed = crossedPortalPlane(portalState, pos, traveller);
        if (!crossed) return;

        // CP275: Crystal Portals deliberately do not obey vanilla's generic portal
        // cooldown here.  Cross-dimension entities receive that cooldown during the
        // transfer, and in a chained Mystcraft portal setup it can outlive the short
        // physical crossing by many seconds, making the next portal look randomly
        // inert.  Crystal Portal loop prevention is instead owned by
        // PortalTransferRuntime's UUID transaction gate in LinkController.
        BookReceptacleBlockEntity controller = CrystalPortalRuntime.findController(serverLevel, pos);
        if (controller == null || !controller.hasBook()) {
            serverLevel.removeBlock(pos, false);
            return;
        }
        ItemStack activatorStack = controller.getBook();
        if (!(activatorStack.getItem() instanceof IItemPortalActivator activator)) {
            controller.deactivatePortal(serverLevel);
            return;
        }
        activator.onPortalCollision(activatorStack, serverLevel, traveller, pos);
    }

    /**
     * True only when the root traveller moved through this portal cell's actual plane
     * during the current entity tick.  Passenger AABB overlap by itself is not enough.
     *
     * <p>Ordinary planar cells use their rendered AXIS.  FULL junction/corner cells
     * do not have one unambiguous plane, so the dominant movement axis is used as a
     * deterministic fallback.  The tangential bounds are deliberately expanded by
     * the entity half extents: minecarts run on the rail block below a vertical portal
     * while their rider/body intersects the portal cell above it.</p>
     */
    public static boolean crossedPortalPlane(BlockState state, BlockPos pos, Entity traveller) {
        if (state == null || pos == null || traveller == null) return false;
        Vec3 start = new Vec3(traveller.xo, traveller.yo, traveller.zo);
        return crossedPortalPlane(state, pos, traveller, start, traveller.position());
    }

    public static boolean crossedPortalPlane(BlockState state, BlockPos pos, Entity traveller, Vec3 start, Vec3 end) {
        return Double.isFinite(portalCrossingParameter(state, pos, traveller, start, end));
    }

    /**
     * CP318 exact segment/portal-plane intersection parameter.
     * Returns NaN when the movement segment does not cross the usable portal cell.
     */
    public static double portalCrossingParameter(BlockState state, BlockPos pos, Entity traveller, Vec3 start, Vec3 end) {
        if (state == null || pos == null || traveller == null || start == null || end == null) return Double.NaN;
        Vec3 delta = end.subtract(start);
        if (!Double.isFinite(delta.x) || !Double.isFinite(delta.y) || !Double.isFinite(delta.z)
                || delta.lengthSqr() < 1.0E-8D) return Double.NaN;

        Direction.Axis axis = state.getValue(AXIS);
        if (state.getValue(FULL)) {
            double ax = Math.abs(delta.x);
            double ay = Math.abs(delta.y);
            double az = Math.abs(delta.z);
            axis = ax >= ay && ax >= az ? Direction.Axis.X : (ay >= az ? Direction.Axis.Y : Direction.Axis.Z);
        }

        double startAxis = axisValue(start, axis);
        double endAxis = axisValue(end, axis);
        double plane = axis == Direction.Axis.X ? pos.getX() + 0.5D
                : axis == Direction.Axis.Y ? pos.getY() + 0.5D
                : pos.getZ() + 0.5D;
        double normalDelta = endAxis - startAxis;
        if (Math.abs(normalDelta) < 1.0E-8D) return Double.NaN;

        double t = (plane - startAxis) / normalDelta;
        if (t < 0.0D || t > 1.0D) return Double.NaN;
        Vec3 hit = start.add(delta.scale(t));

        // Slightly generous trigger margins are intentional.  Rail slopes move a
        // minecart's origin vertically while its body still intersects a vertical
        // Crystal Portal, and unusual fast entities should be caught rather than
        // tunnelling through a visually crossed surface.
        double halfWidth = Math.max(0.10D, traveller.getBbWidth() * 0.5D + 0.20D);
        double halfHeight = Math.max(0.10D, traveller.getBbHeight() * 0.5D + 0.20D);
        boolean tangentialHit = switch (axis) {
            case X -> within(hit.y, pos.getY(), pos.getY() + 1.0D, halfHeight)
                    && within(hit.z, pos.getZ(), pos.getZ() + 1.0D, halfWidth);
            case Y -> within(hit.x, pos.getX(), pos.getX() + 1.0D, halfWidth)
                    && within(hit.z, pos.getZ(), pos.getZ() + 1.0D, halfWidth);
            case Z -> within(hit.x, pos.getX(), pos.getX() + 1.0D, halfWidth)
                    && within(hit.y, pos.getY(), pos.getY() + 1.0D, halfHeight);
        };
        return tangentialHit ? t : Double.NaN;
    }

    /**
     * CP318 fail-closed barrier for a detected crossing whose previous riding
     * transaction is not yet resolvable.  Put the root a few centimetres before the
     * plane and cancel only the velocity component normal to that plane.  Powered
     * rails / gravity can try again on the next tick; the entity is never silently
     * allowed to tunnel through the portal.
     */
    public static void holdBeforePortal(BlockState state, BlockPos pos, Entity traveller, Vec3 start, Vec3 end) {
        double t = portalCrossingParameter(state, pos, traveller, start, end);
        if (!Double.isFinite(t)) return;
        Vec3 delta = end.subtract(start);
        Direction.Axis axis = state.getValue(AXIS);
        if (state.getValue(FULL)) {
            double ax = Math.abs(delta.x);
            double ay = Math.abs(delta.y);
            double az = Math.abs(delta.z);
            axis = ax >= ay && ax >= az ? Direction.Axis.X : (ay >= az ? Direction.Axis.Y : Direction.Axis.Z);
        }
        double normalDistance = Math.abs(axis == Direction.Axis.X ? delta.x : axis == Direction.Axis.Y ? delta.y : delta.z);
        double backT = normalDistance < 1.0E-8D ? t : Math.min(t, 0.075D / normalDistance);
        Vec3 safe = start.add(delta.scale(Math.max(0.0D, t - backT)));
        traveller.setPos(safe.x, safe.y, safe.z);
        Vec3 velocity = traveller.getDeltaMovement();

        // CP319: do not zero the approach component.  A fail-closed retry exists to
        // prevent tunnelling, not to turn the portal into a permanent brake.
        // Keep a small approach velocity so powered rails / gravity can present the
        // same root to the sweep again on the next tick while the previous riding
        // graph finishes registering.  Cap only the normal component to avoid a
        // high-speed entity hammering the barrier several blocks per tick.
        double retrySpeed = 0.08D;
        traveller.setDeltaMovement(switch (axis) {
            case X -> new Vec3(Math.copySign(Math.min(Math.abs(velocity.x), retrySpeed), velocity.x), velocity.y, velocity.z);
            case Y -> new Vec3(velocity.x, Math.copySign(Math.min(Math.abs(velocity.y), retrySpeed), velocity.y), velocity.z);
            case Z -> new Vec3(velocity.x, velocity.y, Math.copySign(Math.min(Math.abs(velocity.z), retrySpeed), velocity.z));
        });
        traveller.fallDistance = 0.0F;
    }

    private static double axisValue(Vec3 value, Direction.Axis axis) {
        return axis == Direction.Axis.X ? value.x : axis == Direction.Axis.Y ? value.y : value.z;
    }

    private static boolean within(double value, double min, double max, double margin) {
        return value >= min - margin && value <= max + margin;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level instanceof ServerLevel serverLevel) {
            BlockState renderState = deriveRenderState(level, pos, state);
            if (renderState != state) level.setBlock(pos, renderState, 2);
            CrystalPortalRuntime.validatePortalAt(serverLevel, pos);
        }
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }

    /** Reimplements legacy getActualState(HAS_ROTATION/RENDER_ROTATION). */
    public static BlockState deriveRenderState(BlockGetter level, BlockPos pos, BlockState state) {
        if (!state.is(MystBlocks.LINK_PORTAL.get())) return state;
        List<Direction.Axis> valid = new ArrayList<>(3);

        // Plane normal X: ring in the YZ plane.
        if (allPortalNetwork(level, pos, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH)) valid.add(Direction.Axis.X);
        // Plane normal Y: ring in the XZ plane.
        if (allPortalNetwork(level, pos, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) valid.add(Direction.Axis.Y);
        // Plane normal Z: ring in the XY plane.
        if (allPortalNetwork(level, pos, Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST)) valid.add(Direction.Axis.Z);

        if (valid.size() == 1) {
            return state.setValue(FULL, false).setValue(AXIS, valid.getFirst());
        }
        return state.setValue(FULL, true);
    }

    private static boolean allPortalNetwork(BlockGetter level, BlockPos pos, Direction... directions) {
        for (Direction direction : directions) {
            if (!isPortalNetworkBlock(level.getBlockState(pos.relative(direction)))) return false;
        }
        return true;
    }

    private static boolean isPortalNetworkBlock(BlockState state) {
        return state.is(MystBlocks.CRYSTAL.get()) || state.is(MystBlocks.LINK_PORTAL.get());
    }
}
