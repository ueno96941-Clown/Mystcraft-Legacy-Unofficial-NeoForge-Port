package com.xcompwiz.mystcraft.world.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Disk snapshot for Legacy population writes which are waiting for a target chunk.
 *
 * <p>The live queue remains concurrent because FEATURES generation can run on chunk
 * workers. SavedData is touched only by level lifecycle/save events on the server
 * thread: we snapshot the live queue before a normal save and restore it when the
 * dimension is loaded again.</p>
 */
final class AgeLegacyDeferredBlockWritesData extends SavedData {
    private static final String FILE_ID = "mystcraft_deferred_population_writes";
    private static final String TAG_WRITES = "Writes";

    private List<AgeLegacyDeferredBlockWrites.PersistedWrite> writes = List.of();

    static SavedData.Factory<AgeLegacyDeferredBlockWritesData> factory() {
        return new SavedData.Factory<>(AgeLegacyDeferredBlockWritesData::new,
                AgeLegacyDeferredBlockWritesData::load);
    }

    static AgeLegacyDeferredBlockWritesData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), FILE_ID);
    }

    static void capture(ServerLevel level) {
        AgeLegacyDeferredBlockWritesData data = get(level);
        data.writes = AgeLegacyDeferredBlockWrites.snapshot(level.dimension());
        data.setDirty();
    }

    static void captureAndFlush(ServerLevel level) {
        capture(level);
        // NeoForge posts LevelEvent.Save after ServerLevel#saveLevelData has already
        // flushed DimensionDataStorage. Flush once more here so the snapshot created
        // by the event is durable in the same save cycle rather than the next one.
        level.getDataStorage().save();
    }

    static void restore(ServerLevel level) {
        AgeLegacyDeferredBlockWritesData data = get(level);
        AgeLegacyDeferredBlockWrites.restore(level.dimension(), data.writes);
    }

    private static AgeLegacyDeferredBlockWritesData load(CompoundTag tag, HolderLookup.Provider registries) {
        AgeLegacyDeferredBlockWritesData data = new AgeLegacyDeferredBlockWritesData();
        ListTag list = tag.getList(TAG_WRITES, Tag.TAG_COMPOUND);
        ArrayList<AgeLegacyDeferredBlockWrites.PersistedWrite> restored = new ArrayList<>(list.size());
        HolderLookup.RegistryLookup<net.minecraft.world.level.block.Block> blocks =
                registries.lookupOrThrow(Registries.BLOCK);
        for (int i = 0; i < list.size(); ++i) {
            CompoundTag entry = list.getCompound(i);
            BlockPos pos = BlockPos.of(entry.getLong("Pos"));
            BlockState state = NbtUtils.readBlockState(blocks, entry.getCompound("State"));
            if (state.isAir() && !"minecraft:air".equals(entry.getCompound("State").getString("Name"))) {
                // Missing registry entries must not turn a deferred authored block into air.
                // Drop that write rather than destroying terrain on load.
                continue;
            }
            restored.add(new AgeLegacyDeferredBlockWrites.PersistedWrite(pos, state, entry.getInt("Flags")));
        }
        data.writes = List.copyOf(restored);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (AgeLegacyDeferredBlockWrites.PersistedWrite write : writes) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", write.pos().asLong());
            entry.put("State", NbtUtils.writeBlockState(write.state()));
            entry.putInt("Flags", write.flags());
            list.add(entry);
        }
        tag.put(TAG_WRITES, list);
        return tag;
    }
}
