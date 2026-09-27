package com.xcompwiz.mystcraft.instability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Persisted block-instability profiler. Ore abundance is retained for compatibility diagnostics
 * but never determines fixed Dense Ores tiers; non-ore watched blocks remain live score input.
 */
public final class LegacyChunkProfilerData extends SavedData {
    private static final String FILE_ID = "mystcraft_instability_profile_cp291_v4";

    private int minY = Integer.MIN_VALUE;
    private int height;
    private int count;
    private long[] solidByY = new long[0];
    private final Map<String, long[]> blocksByY = new LinkedHashMap<>();
    private final Set<Long> profiledChunks = new LinkedHashSet<>();
    private transient Integer cachedNonOreInstability;

    public static LegacyChunkProfilerData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(LegacyChunkProfilerData::new, LegacyChunkProfilerData::load), FILE_ID);
    }

    public int count() { return count; }
    public boolean ready() { return count >= LegacyInstabilityData.PROFILE_CHUNKS; }

    public boolean profile(ServerLevel level, LevelChunk chunk, Map<net.minecraft.world.level.block.Block, LegacyInstabilityBlockManager.Factor> watched) {
        if (ready()) return false;
        long posKey = chunk.getPos().toLong();
        if (!profiledChunks.add(posKey)) return false;
        ensureShape(level.getMinBuildHeight(), level.getHeight());

        Map<String, LegacyInstabilityBlockManager.Factor> factors = logicalFactors(watched);
        for (String key : factors.keySet()) blocksByY.computeIfAbsent(key, k -> new long[height]);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        for (int yi = 0; yi < height; ++yi) {
            int y = minY + yi;
            long solid = 0L;
            for (int z = 0; z < 16; ++z) {
                for (int x = 0; x < 16; ++x) {
                    pos.set(chunkX + x, y, chunkZ + z);
                    BlockState state = chunk.getBlockState(pos);
                    LegacyInstabilityBlockManager.Factor factor = watched.get(state.getBlock());
                    if (factor != null) {
                        blocksByY.get(factor.logicalKey())[yi]++;
                        solid += 1L; // legacy profiler treated watched blocks as partially accessible
                    } else if (state.isAir()) {
                        // 0
                    } else {
                        try {
                            solid += state.getCollisionShape(level, pos).isEmpty() ? 1L : 2L;
                        } catch (Throwable ignored) {
                            solid += 2L;
                        }
                    }
                }
            }
            solidByY[yi] += solid;
        }
        ++count;
        cachedNonOreInstability = null;
        setDirty();
        return true;
    }

    public Map<String, Float> calculateSplitInstability(Map<net.minecraft.world.level.block.Block, LegacyInstabilityBlockManager.Factor> watched) {
        return LegacyChunkProfilerMath.calculateSplit(count, solidByY, blocksByY, logicalFactors(watched));
    }

    public int calculateNonOreInstability(Map<net.minecraft.world.level.block.Block, LegacyInstabilityBlockManager.Factor> watched) {
        // Once the 400-chunk sample is complete it is immutable for this dimension height.
        // Live Instability runtime asks for this value every tick, so avoid rebuilding the
        // split maps/arrays after readiness. Invalidate whenever profiling or shape changes.
        if (ready() && cachedNonOreInstability != null) return cachedNonOreInstability;
        Map<String, LegacyInstabilityBlockManager.Factor> factors = logicalFactors(watched);
        int value = LegacyChunkProfilerMath.sumNonOre(calculateSplitInstability(watched), factors);
        if (ready()) cachedNonOreInstability = value;
        return value;
    }

    public Map<String, Long> logicalBlockCounts() {
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        for (Map.Entry<String, long[]> entry : blocksByY.entrySet()) {
            long total = 0L;
            for (long v : entry.getValue()) total += v;
            if (total > 0L) out.put(entry.getKey(), total);
        }
        return Map.copyOf(out);
    }

    public Map<String, Long> oreBlockCounts(Map<net.minecraft.world.level.block.Block, LegacyInstabilityBlockManager.Factor> watched) {
        Map<String, LegacyInstabilityBlockManager.Factor> factors = logicalFactors(watched);
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : logicalBlockCounts().entrySet()) {
            LegacyInstabilityBlockManager.Factor f = factors.get(entry.getKey());
            if (f != null && f.oreResource()) out.put(entry.getKey(), entry.getValue());
        }
        return Map.copyOf(out);
    }

    private void ensureShape(int newMinY, int newHeight) {
        if (height == 0) {
            minY = newMinY;
            height = newHeight;
            solidByY = new long[height];
            return;
        }
        if (minY == newMinY && height == newHeight) return;
        // A dimension-height change invalidates only diagnostic data, never Age identity.
        minY = newMinY;
        height = newHeight;
        count = 0;
        solidByY = new long[height];
        blocksByY.clear();
        profiledChunks.clear();
        cachedNonOreInstability = null;
    }

    private static Map<String, LegacyInstabilityBlockManager.Factor> logicalFactors(
            Map<net.minecraft.world.level.block.Block, LegacyInstabilityBlockManager.Factor> watched) {
        LinkedHashMap<String, LegacyInstabilityBlockManager.Factor> out = new LinkedHashMap<>();
        for (LegacyInstabilityBlockManager.Factor f : watched.values()) out.putIfAbsent(f.logicalKey(), f);
        return out;
    }

    public static LegacyChunkProfilerData load(CompoundTag tag, HolderLookup.Provider registries) {
        LegacyChunkProfilerData data = new LegacyChunkProfilerData();
        data.minY = tag.getInt("MinY");
        data.height = tag.getInt("Height");
        data.count = tag.getInt("Count");
        data.solidByY = tag.getLongArray("SolidByY");
        boolean invalidShape = data.height <= 0 || data.height > 4096 || data.solidByY.length != data.height;
        boolean invalidCount = data.count < 0 || data.count > LegacyInstabilityData.PROFILE_CHUNKS;
        if (invalidShape || invalidCount) {
            // Profiler data is reconstructible, unlike Age identity. Corrupt or
            // impossible samples are discarded so the dimension can safely re-profile.
            data.minY = Integer.MIN_VALUE;
            data.height = 0;
            data.count = 0;
            data.solidByY = new long[0];
            return data;
        }
        for (long key : tag.getLongArray("ProfiledChunks")) data.profiledChunks.add(key);
        if (data.profiledChunks.size() < data.count) {
            // A count larger than the recorded unique chunks cannot be trusted. Re-profile
            // rather than feeding a denominator inconsistent with the persisted sample.
            data.minY = Integer.MIN_VALUE;
            data.height = 0;
            data.count = 0;
            data.solidByY = new long[0];
            data.profiledChunks.clear();
            return data;
        }
        ListTag list = tag.getList("Blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); ++i) {
            CompoundTag row = list.getCompound(i);
            String key = row.getString("Key");
            long[] values = row.getLongArray("ByY");
            if (!key.isBlank() && values.length == data.height) data.blocksByY.put(key, values);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("MinY", minY == Integer.MIN_VALUE ? 0 : minY);
        tag.putInt("Height", height);
        tag.putInt("Count", count);
        tag.putLongArray("SolidByY", solidByY);
        long[] seen = new long[profiledChunks.size()];
        int si = 0; for (long key : profiledChunks) seen[si++] = key;
        tag.putLongArray("ProfiledChunks", seen);
        ListTag list = new ListTag();
        for (Map.Entry<String, long[]> entry : blocksByY.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putString("Key", entry.getKey());
            row.putLongArray("ByY", entry.getValue());
            list.add(row);
        }
        tag.put("Blocks", list);
        return tag;
    }
}
