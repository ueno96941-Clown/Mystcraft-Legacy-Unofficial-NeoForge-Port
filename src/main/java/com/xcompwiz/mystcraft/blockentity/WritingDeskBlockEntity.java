package com.xcompwiz.mystcraft.blockentity;

import com.xcompwiz.mystcraft.Mystcraft;
import com.xcompwiz.mystcraft.advancement.MystCriteriaTriggers;
import com.xcompwiz.mystcraft.block.BlockWritingDesk;
import com.xcompwiz.mystcraft.inventory.WritingDeskMenu;
import com.xcompwiz.mystcraft.item.ItemAgebook;
import com.xcompwiz.mystcraft.item.ItemPageContainer;
import com.xcompwiz.mystcraft.item.ItemLinking;
import com.xcompwiz.mystcraft.page.Page;
import com.xcompwiz.mystcraft.linking.LinkController;
import com.xcompwiz.mystcraft.linking.LinkOptions;
import com.xcompwiz.mystcraft.world.agedata.AgeManager;
import com.xcompwiz.mystcraft.world.agedata.AgeRegistryData;
import com.xcompwiz.mystcraft.registry.MystBlockEntities;
import com.xcompwiz.mystcraft.registry.MystItems;
import com.xcompwiz.mystcraft.registry.MystFluids;
import com.xcompwiz.mystcraft.registry.MystBlocks;
import com.xcompwiz.mystcraft.registry.MystVillagers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.UUID;

/**
 * Persistent state for the legacy Writing Desk.
 *
 * <p>0.13.7.06 had four work slots (target, paper, fluid container, output),
 * twenty-five surface-tab slots and a one-bucket inkwell. The dedicated legacy
 * menu is still being reconstructed, but the complete inventory shape is kept
 * here now so GUI work can bind to the correct server-side state.</p>
 */
public final class WritingDeskBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int CAPACITY = 1000;
    public static final int WRITE_COST = 50;
    public static final int MAX_TITLE_LENGTH = 21;

    public static final int TARGET_SLOT = 0;
    public static final int PAPER_SLOT = 1;
    public static final int CONTAINER_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int WORK_SLOT_COUNT = 4;
    public static final int TAB_SLOT_COUNT = 25;
    public static final int FIRST_TAB_SLOT = WORK_SLOT_COUNT;
    public static final int SLOT_COUNT = WORK_SLOT_COUNT + TAB_SLOT_COUNT;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int inkAmount;
    /** Structure-only one-shot bridge replacing unreliable template entity placement. */
    private boolean archivistSpawnPending;
    /** Persistent guard retained for migration from CP241/CP242 worlds. */
    private boolean archivistHouseInitialized;
    /** UUID of the shopkeeper once CP243 has positively bound one to this house. */
    private UUID archivistVillagerUuid;
    /**
     * Always wait for the completed chunk before probing the furniture layout.  CP241/CP242 could
     * trust a template BlockEntity marker too early and then permanently suppress later repair.
     */
    private int archivistHouseProbeTicks = 20;
    private boolean archivistHouseProbeDone;
    /**
     * CP261: transient structure/POI timing can still make the first delayed probe miss. Retry a
     * handful of times instead of permanently giving up after one failure. Ordinary player desks
     * only pay for these few cheap geometry probes and are then left alone.
     */
    private int archivistRepairRetryTicks = 40;
    private int archivistRepairRetriesRemaining = 5;
    /** One delayed post-bind verification so vanilla AI has time to prove the POI claim is stable. */
    private int archivistBindingVerifyTicks = 100;
    private boolean archivistBindingVerified;

    public WritingDeskBlockEntity(BlockPos pos, BlockState state) {
        super(MystBlockEntities.WRITING_DESK.get(), pos, state);
    }


    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mystcraft.writing_desk");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new WritingDeskMenu(containerId, playerInventory, this, this.worldPosition);
    }

    /** Legacy TileEntityDesk processed its fluid-container slot every server tick. */
    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, WritingDeskBlockEntity desk) {
        if (level.isClientSide) return;

        // CP243: never let the old "initialized" flag suppress the one geometry probe on load.
        // This is what allows already-generated CP241/CP242 houses to self-heal.
        if (!desk.archivistHouseProbeDone) {
            if (desk.archivistHouseProbeTicks <= 0) {
                desk.detectAndRepairArchivistHouse(level, pos, state);
                desk.archivistHouseProbeDone = true;
            } else {
                --desk.archivistHouseProbeTicks;
            }
        } else if (!desk.archivistHouseInitialized && desk.archivistRepairRetriesRemaining > 0) {
            if (desk.archivistRepairRetryTicks <= 0) {
                --desk.archivistRepairRetriesRemaining;
                desk.archivistRepairRetryTicks = 40;
                desk.detectAndRepairArchivistHouse(level, pos, state);
            } else {
                --desk.archivistRepairRetryTicks;
            }
        } else if (!desk.archivistBindingVerified && desk.archivistHouseInitialized) {
            if (desk.archivistBindingVerifyTicks <= 0) {
                desk.verifyArchivistBinding((ServerLevel) level, pos);
                desk.archivistBindingVerified = true;
            } else {
                --desk.archivistBindingVerifyTicks;
            }
        }

        desk.processFluidContainer();
    }

    /**
     * Detect the original Archivist-house furniture pattern and repair it after the whole Jigsaw
     * piece is live.  Do not trust FACING alone: old CP239/CP241 structures can contain a valid
     * main/foot desk pair whose persisted facing was produced by a different rotation path.
     * The actual foot block is therefore the authoritative local-forward direction.
     */
    private boolean detectAndRepairArchivistHouse(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        if (!state.hasProperty(BlockWritingDesk.FACING)
                || state.getValue(BlockWritingDesk.IS_TOP)
                || state.getValue(BlockWritingDesk.IS_FOOT)) return false;

        Direction forward = findDeskFootDirection(level, pos);
        if (forward == null) {
            // Keep a conservative fallback for structures generated before CP240.
            forward = state.getValue(BlockWritingDesk.FACING);
        }

        Direction matchedSide = null;
        BlockPos lecternA = null;
        BlockPos lecternB = null;
        for (Direction side : new Direction[]{forward.getClockWise(), forward.getCounterClockWise()}) {
            BlockPos a = pos.relative(side, 3).above();
            BlockPos b = a.relative(forward);
            if (level.getBlockState(a).is(MystBlocks.LECTERN.get())
                    && level.getBlockState(b).is(MystBlocks.LECTERN.get())) {
                matchedSide = side;
                lecternA = a;
                lecternB = b;
                break;
            }
        }
        if (matchedSide == null) {
            return false;
        }

        // Original 0.13.7.06 Archivist House uses local lectern positions x=1 and
        // look targets x=2. StructureGenerationUtils then stores WEST for that west-wall pair;
        // the legacy TESR applies its own orientation correction. CP242/CP244 accidentally used
        // the opposite (EAST), which turns the displayed Symbol Pages toward the wall.
        Direction lecternFacing = matchedSide;
        boolean filledA = repairArchivistLectern(serverLevel, lecternA, lecternFacing);
        boolean filledB = repairArchivistLectern(serverLevel, lecternB, lecternFacing);

        // The old 1.12 village helper accepted WEST for this doorway, but that value is not
        // equivalent to a modern DoorBlock FACING.  In CP241-CP246 houses it leaves the door
        // plane perpendicular to the wall: open=false looks physically open, so villagers
        // "open" it into the blocking position and can camp at the entrance indefinitely.
        // Derive the doorway from the surviving furniture geometry so already-generated test
        // villages self-heal as well as newly generated CP247 structures.
        BlockPos doorLower = pos.relative(matchedSide.getOpposite(), 2).relative(forward.getOpposite());
        boolean doorReady = repairArchivistDoor(serverLevel, doorLower, forward.getOpposite());

        boolean shopkeeperReady = ensureArchivistShopkeeper(serverLevel, pos, forward);
        archivistSpawnPending = false;
        archivistHouseInitialized = shopkeeperReady;
        setChanged();

        Mystcraft.LOGGER.debug(
                "Archivist house repair at {}: forward={}, lecterns=({}, {}), door={}, shopkeeper={}",
                pos, forward, filledA, filledB, doorReady, shopkeeperReady);
        return true;
    }

    private Direction findDeskFootDirection(net.minecraft.world.level.Level level, BlockPos mainPos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState candidate = level.getBlockState(mainPos.relative(direction));
            if (candidate.is(MystBlocks.WRITING_DESK.get())
                    && candidate.hasProperty(BlockWritingDesk.IS_TOP)
                    && candidate.hasProperty(BlockWritingDesk.IS_FOOT)
                    && !candidate.getValue(BlockWritingDesk.IS_TOP)
                    && candidate.getValue(BlockWritingDesk.IS_FOOT)) {
                return direction;
            }
        }
        return null;
    }


    private boolean repairArchivistDoor(ServerLevel level, BlockPos lowerPos, Direction outwardFacing) {
        BlockPos upperPos = lowerPos.above();
        BlockState lower = level.getBlockState(lowerPos);
        BlockState upper = level.getBlockState(upperPos);
        if (!lower.is(Blocks.OAK_DOOR) || !upper.is(Blocks.OAK_DOOR)) return false;
        if (!lower.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                || !upper.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return false;

        boolean changed = false;
        if (lower.getValue(BlockStateProperties.HORIZONTAL_FACING) != outwardFacing) {
            level.setBlock(lowerPos, lower.setValue(BlockStateProperties.HORIZONTAL_FACING, outwardFacing), Block.UPDATE_ALL);
            changed = true;
        }
        // Re-read after the lower-half update so any neighbour synchronization performed by the
        // vanilla door block cannot leave us with a stale upper state.
        upper = level.getBlockState(upperPos);
        if (upper.is(Blocks.OAK_DOOR)
                && upper.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                && upper.getValue(BlockStateProperties.HORIZONTAL_FACING) != outwardFacing) {
            level.setBlock(upperPos, upper.setValue(BlockStateProperties.HORIZONTAL_FACING, outwardFacing), Block.UPDATE_ALL);
            changed = true;
        }
        if (changed) {
            Mystcraft.LOGGER.debug("Repaired Archivist house door at {} to facing {}", lowerPos, outwardFacing);
        }
        return true;
    }

    private boolean repairArchivistLectern(ServerLevel level, BlockPos lecternPos, Direction facing) {
        BlockState lecternState = level.getBlockState(lecternPos);
        if (!lecternState.is(MystBlocks.LECTERN.get())) return false;
        if (lecternState.hasProperty(com.xcompwiz.mystcraft.block.BlockBookDisplay.FACING)
                && lecternState.getValue(com.xcompwiz.mystcraft.block.BlockBookDisplay.FACING) != facing) {
            level.setBlock(lecternPos, lecternState.setValue(
                    com.xcompwiz.mystcraft.block.BlockBookDisplay.FACING, facing), Block.UPDATE_ALL);
        }
        if (level.getBlockEntity(lecternPos) instanceof BookDisplayBlockEntity display) {
            if (!display.hasBook()) {
                display.populateLegacyTreasure(level.getRandom(), false);
            }
            return display.hasBook();
        }
        return false;
    }

    /**
     * Ensure the house owns one stable Archivist.  CP241/CP242 could spawn a villager and mark the
     * house initialized without giving the villager a JOB_SITE memory; a level-1 villager can then
     * immediately reset to the unemployed profession.  That produces exactly the observed
     * "plain villager camping by the shop door" migration state.
     */
    private boolean ensureArchivistShopkeeper(ServerLevel level, BlockPos deskPos, Direction forward) {
        BlockPos spawnPos = deskPos.relative(forward, 3).above();
        AABB search = new AABB(spawnPos).inflate(6.0D, 3.0D, 6.0D);

        // First repair the exact villager CP243 previously bound, if it is currently loaded.
        // CP243 only wrote JOB_SITE memory; it never acquired the POI ticket, so vanilla was free
        // to discard the fake assignment and let the villager walk to a real workstation.
        if (archivistVillagerUuid != null) {
            net.minecraft.world.entity.Entity bound = level.getEntity(archivistVillagerUuid);
            if (bound instanceof Villager villager) {
                return bindArchivist(villager, level, deskPos);
            }
            // A stored UUID is authoritative while the villager's chunk is unloaded.
            return true;
        }

        java.util.List<Villager> nearby = level.getEntitiesOfClass(Villager.class, search, v -> !v.isBaby());
        for (Villager villager : nearby) {
            if (villager.getVillagerData().getProfession() == MystVillagers.ARCHIVIST.get()) {
                if (!bindArchivist(villager, level, deskPos)) continue;
                archivistVillagerUuid = villager.getUUID();
                return true;
            }
        }

        // Migrate CP241-CP243 worlds without duplicating villagers. Prefer the nearest adult whose
        // profession is NONE, or the old bound smith/armorer/toolsmith UUID above when available.
        if (archivistHouseInitialized) {
            Villager nearestUnemployed = null;
            double bestDistance = Double.MAX_VALUE;
            for (Villager villager : nearby) {
                if (!isUnemployed(villager)) continue;
                double distance = villager.distanceToSqr(
                        spawnPos.getX() + 0.5D, spawnPos.getY() + 0.5D, spawnPos.getZ() + 0.5D);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    nearestUnemployed = villager;
                }
            }
            if (nearestUnemployed != null && bindArchivist(nearestUnemployed, level, deskPos)) {
                archivistVillagerUuid = nearestUnemployed.getUUID();
                Mystcraft.LOGGER.debug("Promoted legacy shop villager {} to Archivist for desk {}",
                        nearestUnemployed.getUUID(), deskPos);
                return true;
            }
        }

        if (!level.isLoaded(spawnPos)) return false;
        Villager villager = EntityType.VILLAGER.spawn(level, spawnPos, MobSpawnType.STRUCTURE);
        if (villager == null) {
            Mystcraft.LOGGER.warn("Could not spawn Archivist for desk {} at {}", deskPos, spawnPos);
            return false;
        }
        if (!bindArchivist(villager, level, deskPos)) {
            Mystcraft.LOGGER.warn("Spawned villager but could not claim Archivist POI for desk {}; discarding {}",
                    deskPos, villager.getUUID());
            villager.discard();
            return false;
        }
        archivistVillagerUuid = villager.getUUID();
        Mystcraft.LOGGER.debug("Spawned Archivist {} for desk {} at {}",
                villager.getUUID(), deskPos, spawnPos);
        return true;
    }

    /**
     * Bind the legacy fixed Archivist profession to a real modern POI reservation.  Merely writing
     * JOB_SITE memory is not enough: vanilla validates that memory against PoiManager ownership and
     * will otherwise clear it, after which the villager can immediately claim a smithing workstation.
     */
    private boolean bindArchivist(Villager villager, ServerLevel level, BlockPos deskPos) {
        var poiManager = level.getPoiManager();
        var archivistPoi = MystVillagers.ARCHIVIST_POI;

        // Structure placement should have registered the Writing Desk POI automatically, but older
        // generated chunks are repaired explicitly so migration does not depend on that callback.
        if (!poiManager.exists(deskPos, holder -> holder.value() == archivistPoi.get())) {
            poiManager.add(deskPos, archivistPoi);
        }

        var oldJob = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        boolean alreadyBound = oldJob.isPresent()
                && oldJob.get().dimension().equals(level.dimension())
                && oldJob.get().pos().equals(deskPos)
                && poiManager.getFreeTickets(deskPos) == 0;

        if (!alreadyBound) {
            // CP243 victims may currently own a smith/grindstone/blast-furnace POI. Release that
            // ticket before moving them back to the Archivist's Writing Desk.
            oldJob.ifPresent(globalPos -> {
                if (globalPos.dimension().equals(level.dimension()) && !globalPos.pos().equals(deskPos)) {
                    poiManager.release(globalPos.pos());
                }
            });
            villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
            villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);

            var claimed = poiManager.take(
                    holder -> holder.value() == archivistPoi.get(),
                    (holder, pos) -> pos.equals(deskPos),
                    deskPos, 1);
            if (claimed.isEmpty()) {
                Mystcraft.LOGGER.warn(
                        "Failed to reserve Archivist POI at {} for villager {} (freeTickets={})",
                        deskPos, villager.getUUID(), poiManager.getFreeTickets(deskPos));
                return false;
            }
        }

        villager.setVillagerData(villager.getVillagerData().setProfession(MystVillagers.ARCHIVIST.get()));
        // Profession-specific work activities are installed when the Brain is refreshed. Do this
        // before writing the claimed JOB_SITE back so the final memory survives the refresh.
        villager.refreshBrain(level);
        villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
        villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), deskPos));
        // 1.12 Archivists had a fixed profession rather than the modern novice job-site reset
        // mechanic. A minimal non-zero XP locks that legacy semantics without changing villager level.
        if (villager.getVillagerXp() == 0) villager.setVillagerXp(1);
        villager.setPersistenceRequired();

        Mystcraft.LOGGER.debug(
                "Bound Archivist {} to POI {} (freeTickets={}, xp={})",
                villager.getUUID(), deskPos, poiManager.getFreeTickets(deskPos), villager.getVillagerXp());
        return true;
    }

    private void verifyArchivistBinding(ServerLevel level, BlockPos deskPos) {
        if (archivistVillagerUuid == null) return;
        net.minecraft.world.entity.Entity entity = level.getEntity(archivistVillagerUuid);
        if (!(entity instanceof Villager villager)) return;
        var job = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        boolean correctProfession = villager.getVillagerData().getProfession() == MystVillagers.ARCHIVIST.get();
        boolean correctJob = job.isPresent() && job.get().dimension().equals(level.dimension())
                && job.get().pos().equals(deskPos);
        Mystcraft.LOGGER.debug(
                "Delayed Archivist verification {}: professionOk={}, jobSiteOk={}, profession={}",
                villager.getUUID(), correctProfession, correctJob,
                BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession()));
        if (!correctProfession || !correctJob) {
            bindArchivist(villager, level, deskPos);
        }
    }

    private static boolean isUnemployed(Villager villager) {
        ResourceLocation id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(
                villager.getVillagerData().getProfession());
        return ResourceLocation.withDefaultNamespace("none").equals(id);
    }

    private void processFluidContainer() {
        ItemStack input = items.get(CONTAINER_SLOT);
        if (input.isEmpty()) return;

        // Operate on one copied container first. Capability mutations then become a
        // transaction: only consume the real input if its resulting container fits
        // the output slot. This is the 1.21 analogue of FluidUtils/FluidUtil in 1.12.
        ItemStack working = input.copyWithCount(1);
        IFluidHandlerItem handler = working.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) return;

        int room = CAPACITY - inkAmount;
        if (room > 0) {
            FluidStack request = new FluidStack(MystFluids.BLACK_INK.get(), room);
            FluidStack drained = handler.drain(request, IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty() && drained.is(MystFluids.BLACK_INK.get()) && drained.getAmount() > 0) {
                ItemStack resultContainer = handler.getContainer().copy();
                if (canMergeOutput(resultContainer)) {
                    consumeOneContainerInput();
                    mergeOutput(resultContainer);
                    setInkAmount(inkAmount + drained.getAmount());
                    return;
                }
            }
        }

        if (inkAmount <= 0) return;
        // Recreate the copy because the failed/blocked drain attempt above may have
        // mutated its handler even though the transaction was not committed.
        working = input.copyWithCount(1);
        handler = working.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) return;
        int filled = handler.fill(new FluidStack(MystFluids.BLACK_INK.get(), inkAmount), IFluidHandler.FluidAction.EXECUTE);
        if (filled <= 0) return;
        ItemStack resultContainer = handler.getContainer().copy();
        if (!canMergeOutput(resultContainer)) return;
        consumeOneContainerInput();
        mergeOutput(resultContainer);
        setInkAmount(inkAmount - filled);
    }

    private void consumeOneContainerInput() {
        ItemStack input = items.get(CONTAINER_SLOT);
        input.shrink(1);
        if (input.isEmpty()) items.set(CONTAINER_SLOT, ItemStack.EMPTY);
    }

    private boolean canMergeOutput(ItemStack stack) {
        if (stack.isEmpty()) return true;
        ItemStack existing = items.get(OUTPUT_SLOT);
        if (existing.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(existing, stack)
                && existing.getCount() + stack.getCount() <= existing.getMaxStackSize();
    }

    private void mergeOutput(ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack existing = items.get(OUTPUT_SLOT);
        if (existing.isEmpty()) items.set(OUTPUT_SLOT, stack.copy());
        else existing.grow(stack.getCount());
        setChanged();
    }

    public int inkAmount() {
        return inkAmount;
    }

    /** Capability-safe setter used by the external black-ink tank view. */
    public void setInkAmount(int amount) {
        int clamped = Mth.clamp(amount, 0, CAPACITY);
        if (clamped == inkAmount) return;
        inkAmount = clamped;
        setChanged();
    }

    public int fillInk(int amount, boolean execute) {
        if (amount <= 0) return 0;
        int accepted = Math.min(amount, CAPACITY - inkAmount);
        if (execute && accepted > 0) setInkAmount(inkAmount + accepted);
        return accepted;
    }

    public int drainInk(int amount, boolean execute) {
        if (amount <= 0) return 0;
        int drained = Math.min(amount, inkAmount);
        if (execute && drained > 0) setInkAmount(inkAmount - drained);
        return drained;
    }

    public boolean canAcceptVial() {
        return inkAmount <= CAPACITY - 1000;
    }

    public boolean addVial() {
        if (!canAcceptVial()) return false;
        inkAmount = CAPACITY;
        setChanged();
        return true;
    }

    public boolean hasWritingInk() {
        return inkAmount >= WRITE_COST;
    }

    public boolean consumeWritingInk() {
        if (!hasWritingInk()) return false;
        inkAmount -= WRITE_COST;
        setChanged();
        return true;
    }

    public ItemStack getTarget() {
        return items.get(TARGET_SLOT);
    }

    public ItemStack getTab(int index) {
        if (index < 0 || index >= TAB_SLOT_COUNT) return ItemStack.EMPTY;
        return items.get(FIRST_TAB_SLOT + index);
    }

    /** Legacy IItemRenameable bridge for Agebook, Linkbook, Folder and Portfolio. */
    public boolean isTargetRenameable() {
        ItemStack target = getTarget();
        return !target.isEmpty() && (target.getItem() instanceof ItemLinking
                || target.getItem() instanceof ItemPageContainer);
    }

    public String getTargetTitle() {
        ItemStack target = getTarget();
        if (target.isEmpty()) return "";
        if (target.getItem() instanceof ItemLinking) {
            String title = LinkOptions.getDisplayName(target);
            return title == null ? "" : title;
        }
        if (target.getItem() instanceof ItemPageContainer container) {
            return container.getLegacyName(target);
        }
        return "";
    }

    public void setTargetTitle(Player player, String title) {
        ItemStack target = getTarget();
        if (target.isEmpty() || !isTargetRenameable()) return;
        String normalized = title == null ? "" : title;
        if (normalized.length() > MAX_TITLE_LENGTH) normalized = normalized.substring(0, MAX_TITLE_LENGTH);

        if (target.getItem() instanceof ItemLinking) {
            LinkOptions.setDisplayName(target, normalized);
            // Legacy Agebook rename also renamed the persisted Age once established.
            if (target.getItem() instanceof ItemAgebook && player instanceof ServerPlayer serverPlayer) {
                var server = serverPlayer.getServer();
                if (server != null) {
                    String finalName = normalized;
                    AgeManager.resolveReservation(server, target).ifPresent(age -> {
                        age.setAgeName(finalName);
                        AgeRegistryData.flush(server);
                    });
                }
            }
        } else if (target.getItem() instanceof ItemPageContainer container) {
            container.setLegacyName(target, normalized);
        }
        setChanged();
        syncClient();
    }

    public boolean canLinkTarget(Player player) {
        ItemStack target = getTarget();
        if (!(target.getItem() instanceof ItemLinking)) return false;
        if (target.getItem() instanceof ItemAgebook && ItemAgebook.isNewAgebook(target, player.level().registryAccess())) return true;
        String dimension = LinkOptions.getDimensionKey(target);
        return dimension != null && !dimension.isBlank();
    }

    /** Writing Desk linking never consumes/drops the book because it is not held. */
    public boolean activateLink(ServerPlayer player) {
        ItemStack target = getTarget();
        if (!(target.getItem() instanceof ItemLinking)) return false;
        if (target.getItem() instanceof ItemAgebook && ItemAgebook.isNewAgebook(target, player.level().registryAccess())) {
            try {
                AgeManager.establish(player, target);
            } catch (IllegalStateException ex) {
                player.displayClientMessage(Component.literal(ex.getMessage()), true);
                return false;
            }
        }
        boolean linked = LinkController.travelPlayerFromPortal(player, target);
        if (linked) setChanged();
        return linked;
    }

    /**
     * Legacy writing rule: if the target is empty, one paper is promoted to a
     * blank Page first. Writing then costs exactly 50 mB only when a symbol is
     * actually committed.
     */
    public boolean writeSymbol(Player player, String legacySymbolId) {
        if (level == null || level.isClientSide || legacySymbolId == null || legacySymbolId.isBlank()) return false;
        if (!hasWritingInk()) return false;

        if (items.get(TARGET_SLOT).isEmpty() && !items.get(PAPER_SLOT).isEmpty()) {
            ItemStack paper = items.get(PAPER_SLOT);
            if (paper.is(Items.PAPER)) {
                items.set(TARGET_SLOT, Page.createPage());
                paper.shrink(1);
                if (paper.isEmpty()) items.set(PAPER_SLOT, ItemStack.EMPTY);
            }
        }

        ItemStack target = items.get(TARGET_SLOT);
        if (target.isEmpty()) return false;
        boolean written = false;

        if (target.is(MystItems.PAGE.get()) && Page.isBlank(target)) {
            Page.setSymbolId(target, legacySymbolId);
            written = true;
        } else if (target.getItem() instanceof ItemAgebook agebook) {
            written = agebook.writeSymbolLegacy(target, level.registryAccess(), player, legacySymbolId);
        } else if (target.getItem() instanceof ItemPageContainer container) {
            // Legacy Folder was writable: it fills an existing blank page. Portfolio
            // was not writable, so only the Folder identity is accepted here.
            if (target.is(MystItems.FOLDER.get())) {
                var pages = new java.util.ArrayList<>(container.getPages(target));
                for (ItemStack page : pages) {
                    if (Page.isBlank(page)) {
                        Page.setSymbolId(page, legacySymbolId);
                        container.setPages(target, pages);
                        written = true;
                        break;
                    }
                }
                // Legacy IItemPageAcceptor fallback: with paper available, a Folder
                // that has no blank page receives a newly written Symbol Page.
                if (!written && pages.size() < container.pageCapacity()) {
                    ItemStack paper = items.get(PAPER_SLOT);
                    if (!paper.isEmpty() && paper.is(Items.PAPER)) {
                        ItemStack page = Page.createSymbolPage(legacySymbolId);
                        pages.add(page);
                        container.setPages(target, pages);
                        paper.shrink(1);
                        if (paper.isEmpty()) items.set(PAPER_SLOT, ItemStack.EMPTY);
                        written = true;
                    }
                }
            }
        }

        if (written) {
            inkAmount -= WRITE_COST;
            setChanged();
            syncClient();
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                MystCriteriaTriggers.WRITING_DESK_WRITE.get().trigger(serverPlayer);
            }
        }
        return written;
    }

    public static boolean isValidTarget(ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() != 1) return false;
        return stack.is(MystItems.PAGE.get())
                || stack.is(MystItems.AGEBOOK.get())
                || stack.is(MystItems.LINKBOOK.get())
                || stack.is(MystItems.FOLDER.get())
                || stack.is(MystItems.PORTFOLIO.get());
    }

    public static boolean isValidTab(ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() != 1) return false;
        return stack.is(MystItems.PAGE.get())
                || stack.is(MystItems.AGEBOOK.get())
                || stack.is(MystItems.FOLDER.get())
                || stack.is(MystItems.PORTFOLIO.get());
    }

    private void syncClient() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        CompoundTag tag = packet.getTag();
        if (tag != null) loadAdditional(tag, registries);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
            if (slot == TARGET_SLOT) syncClient();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(items, slot);
        if (!result.isEmpty() && slot == TARGET_SLOT) syncClient();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty()) {
            boolean singleOnly = slot == TARGET_SLOT || (slot >= FIRST_TAB_SLOT && slot < SLOT_COUNT);
            int max = singleOnly ? 1 : Math.min(stack.getMaxStackSize(), getMaxStackSize());
            if (stack.getCount() > max) stack = stack.copyWithCount(max);
        }
        items.set(slot, stack);
        setChanged();
        if (slot == TARGET_SLOT) syncClient();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == TARGET_SLOT) return isValidTarget(stack);
        if (slot == PAPER_SLOT) return stack.is(Items.PAPER);
        if (slot == CONTAINER_SLOT) {
            if (stack.isEmpty()) return false;
            ItemStack single = stack.copyWithCount(1);
            IFluidHandlerItem handler = single.getCapability(Capabilities.FluidHandler.ITEM);
            if (handler == null) return false;
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                FluidStack fluid = handler.getFluidInTank(tank);
                if (!fluid.isEmpty() && fluid.is(MystFluids.BLACK_INK.get())) return true;
            }
            return handler.fill(new FluidStack(MystFluids.BLACK_INK.get(), CAPACITY), IFluidHandler.FluidAction.SIMULATE) > 0;
        }
        if (slot == OUTPUT_SLOT) return false;
        if (slot >= FIRST_TAB_SLOT && slot < SLOT_COUNT) return isValidTab(stack);
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inkAmount = Mth.clamp(tag.getInt("InkAmount"), 0, CAPACITY);
        archivistSpawnPending = tag.getBoolean("ArchivistSpawnPending");
        archivistHouseInitialized = tag.getBoolean("ArchivistHouseInitialized");
        archivistVillagerUuid = tag.hasUUID("ArchivistVillager") ? tag.getUUID("ArchivistVillager") : null;
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("InkAmount", inkAmount);
        if (archivistSpawnPending) tag.putBoolean("ArchivistSpawnPending", true);
        if (archivistHouseInitialized) tag.putBoolean("ArchivistHouseInitialized", true);
        if (archivistVillagerUuid != null) tag.putUUID("ArchivistVillager", archivistVillagerUuid);
        ContainerHelper.saveAllItems(tag, items, registries);
    }
}
