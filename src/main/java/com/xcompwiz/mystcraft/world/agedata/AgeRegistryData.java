package com.xcompwiz.mystcraft.world.agedata;

import com.xcompwiz.mystcraft.instability.LegacyInstabilityPhase2Service;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

/** Global server-wide registry of reserved Mystcraft Ages. Stored on the Overworld. */
public final class AgeRegistryData extends SavedData {
    public static final String DATA_NAME = "mystcraft_ages";
    private final Map<Integer, AgeRecord> ages = new LinkedHashMap<>();
    private int nextUid = 2;

    public static AgeRegistryData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(AgeRegistryData::new, AgeRegistryData::load), DATA_NAME);
    }

    /**
     * Flushes dirty Overworld SavedData immediately. AgeRegistryData is stored
     * on the Overworld even when the runtime state being captured belongs to a
     * dynamically installed Age, so saving the Age level alone is insufficient.
     */
    public static void flush(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        if (!server.isSameThread()) {
            throw new IllegalStateException("Mystcraft Age registry flush must run on the server thread");
        }
        server.overworld().getDataStorage().save();
    }

    public static AgeRegistryData load(CompoundTag tag, HolderLookup.Provider registries) {
        AgeRegistryData data = new AgeRegistryData();
        data.nextUid = Math.max(2, tag.getInt("NextAgeUID"));
        ListTag list = tag.getList("Ages", Tag.TAG_COMPOUND);
        boolean migratedInstabilityDecks = false;
        for (int i = 0; i < list.size(); i++) {
            AgeRecord age = AgeRecord.load(list.getCompound(i), registries);
            if (LegacyInstabilityPhase2Service.ensureDeckPersistence(age)) migratedInstabilityDecks = true;
            if (data.ages.containsKey(age.ageUid())) {
                throw new IllegalArgumentException("Duplicate Mystcraft AgeUID in SavedData: " + age.ageUid());
            }
            if (data.byDimensionKey(age.dimensionKey()).isPresent()) {
                throw new IllegalArgumentException("Duplicate Mystcraft DimensionKey in SavedData: " + age.dimensionKey());
            }
            if (data.byUuid(age.uuid()).isPresent()) {
                throw new IllegalArgumentException("Duplicate Mystcraft UUID in SavedData: " + age.uuid());
            }
            data.ages.put(age.ageUid(), age);
            data.nextUid = age.ageUid() == Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(data.nextUid, age.ageUid() + 1);
        }
        // Existing CP291 worlds have no persisted Deck data. Mark the registry dirty once after
        // deterministic migration so the newly reconstructed order is written on the next save.
        if (migratedInstabilityDecks) data.setDirty();
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("NextAgeUID", nextUid);
        ListTag list = new ListTag();
        for (AgeRecord age : ages.values()) list.add(age.save(registries));
        tag.put("Ages", list);
        return tag;
    }

    public Collection<AgeRecord> all() { return java.util.List.copyOf(ages.values()); }
    public Optional<AgeRecord> byUid(int uid) { return Optional.ofNullable(ages.get(uid)); }
    public Optional<AgeRecord> byDimensionKey(String key) {
        if (key == null) return Optional.empty();
        return ages.values().stream().filter(age -> key.equals(age.dimensionKey())).findFirst();
    }
    public Optional<AgeRecord> byUuid(UUID uuid) {
        if (uuid == null) return Optional.empty();
        return ages.values().stream().filter(age -> uuid.equals(age.uuid())).findFirst();
    }

    public int allocateUid() {
        if (nextUid < 2) nextUid = 2;
        while (ages.containsKey(nextUid)) {
            if (nextUid == Integer.MAX_VALUE) {
                throw new IllegalStateException("Mystcraft AgeUID space exhausted");
            }
            nextUid++;
        }
        int uid = nextUid;
        if (nextUid < Integer.MAX_VALUE) nextUid++;
        setDirty();
        return uid;
    }

    public void put(AgeRecord record) {
        if (record == null) throw new NullPointerException("record");
        AgeRecord uidConflict = ages.get(record.ageUid());
        if (uidConflict != null && uidConflict != record) {
            throw new IllegalStateException("Mystcraft AgeUID already reserved: " + record.ageUid());
        }
        for (AgeRecord existing : ages.values()) {
            if (existing == record) continue;
            if (existing.dimensionKey().equals(record.dimensionKey())) {
                throw new IllegalStateException("Mystcraft DimensionKey already reserved: " + record.dimensionKey());
            }
            if (existing.uuid().equals(record.uuid())) {
                throw new IllegalStateException("Mystcraft Age UUID already reserved: " + record.uuid());
            }
        }
        ages.put(record.ageUid(), record);
        nextUid = record.ageUid() == Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(nextUid, record.ageUid() + 1);
        setDirty();
    }

    public void changed() { setDirty(); }
}
