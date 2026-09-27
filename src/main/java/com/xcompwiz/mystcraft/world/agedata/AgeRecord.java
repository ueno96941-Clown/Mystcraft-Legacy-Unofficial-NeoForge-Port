package com.xcompwiz.mystcraft.world.agedata;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import com.xcompwiz.mystcraft.world.dimension.AgeDimensionKeys;
import com.xcompwiz.mystcraft.instability.InstabilityPolicy;
import com.xcompwiz.mystcraft.symbol.SymbolRemappings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistent compatibility record for one Mystcraft Age reservation. */
public final class AgeRecord {
    public static final String VERSION = "4.3";
    public static final String PORT_VERSION = "1.21.1-port-3";

    private final int ageUid;
    private String dimensionKey;
    private String ageName;
    private long seed;
    private UUID uuid;
    private short baseInstability;
    private boolean storedInstabilityEnabled = true;
    private boolean visited;
    private boolean dead;
    private boolean starFissureGenerated;
    private boolean starFissureProcessed;
    private long worldTime;
    private long dayTime;
    private int clearWeatherTime, thunderTime, rainTime;
    private boolean thundering, raining;
    private BlockPos spawn;
    private final List<ItemStack> pages = new ArrayList<>();
    private final List<String> symbols = new ArrayList<>();
    private final Set<String> authors = new LinkedHashSet<>();
    /** CP292: persisted legacy Instability deck order. Active provider levels are reconstructed. */
    private final Map<String, List<String>> instabilityDeckOrders = new LinkedHashMap<>();

    public AgeRecord(int ageUid, String dimensionKey, String ageName, long seed, UUID uuid) {
        this.ageUid = ageUid;
        this.dimensionKey = dimensionKey;
        this.ageName = ageName;
        this.seed = seed;
        this.uuid = uuid;
    }

    public int ageUid() { return ageUid; }
    public String dimensionKey() { return dimensionKey; }
    public String ageName() { return ageName; }
    public long seed() { return seed; }
    public UUID uuid() { return uuid; }
    public short baseInstability() { return baseInstability; }
    /** Runtime gate AND the legacy per-Age compatibility flag must both permit Instability. */
    public boolean instabilityEnabled() { return InstabilityPolicy.runtimeEnabled() && storedInstabilityEnabled; }
    /** Original compatibility flag preserved in NBT without enabling penalties. */
    public boolean storedInstabilityEnabled() { return storedInstabilityEnabled; }
    public boolean visited() { return visited; }
    public boolean dead() { return dead; }
    public boolean starFissureGenerated() { return starFissureGenerated; }
    public boolean starFissureProcessed() { return starFissureProcessed; }
    public long worldTime() { return worldTime; }
    public long dayTime() { return dayTime; }
    public int clearWeatherTime() { return clearWeatherTime; }
    public int thunderTime() { return thunderTime; }
    public int rainTime() { return rainTime; }
    public boolean thundering() { return thundering; }
    public boolean raining() { return raining; }
    public BlockPos spawn() { return spawn; }
    public List<ItemStack> pages() { return List.copyOf(pages); }
    public List<String> symbols() { return List.copyOf(symbols); }
    public Set<String> authors() { return Set.copyOf(authors); }
    public Map<String, List<String>> instabilityDeckOrders() {
        LinkedHashMap<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : instabilityDeckOrders.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    public void setAgeName(String ageName) { this.ageName = ageName == null ? "" : ageName; }
    public void setSeed(long seed) { if (!visited) this.seed = seed; }
    public void setBaseInstability(short value) { this.baseInstability = value; }
    /** Preserve the legacy flag for round-trip compatibility; runtime remains disabled. */
    public void setInstabilityEnabled(boolean value) { this.storedInstabilityEnabled = value; }
    public void setVisited(boolean value) { this.visited = value; }
    public void setDead(boolean value) { this.dead = value; }
    public void setStarFissureGenerated(boolean value) { this.starFissureGenerated = value; }
    public void setStarFissureProcessed(boolean value) { this.starFissureProcessed = value; }
    public void setWorldTime(long value) { this.worldTime = value; }
    public void setDayTime(long value) { this.dayTime = value; }
    public void setWeatherState(int clear, int thunder, int rain, boolean thundering, boolean raining) {
        this.clearWeatherTime=clear; this.thunderTime=thunder; this.rainTime=rain;
        this.thundering=thundering; this.raining=raining;
    }
    public void setSpawn(BlockPos value) { this.spawn = value; }

    public void setPages(List<ItemStack> value) {
        if (visited) throw new IllegalStateException("Cannot change pages after Mystcraft Age activation");
        pages.clear();
        for (ItemStack stack : value) if (!stack.isEmpty()) pages.add(stack.copy());
    }

    public void setSymbols(List<String> value) {
        if (visited) throw new IllegalStateException("Cannot change symbols after Mystcraft Age activation");
        symbols.clear();
        for (String symbol : value) {
            if (symbol != null && !symbol.isBlank()) symbols.add(symbol);
        }
    }

    public void setAuthors(Iterable<String> value) {
        authors.clear();
        for (String author : value) if (author != null && !author.isBlank()) authors.add(author);
    }

    /**
     * Replaces durable Deck order. Unlike authored pages/symbols this may change after first visit
     * when a newer Mystcraft version adds/removes Instability cards, matching 0.13.7.06 migration.
     */
    public void setInstabilityDeckOrders(Map<String, ? extends List<String>> value) {
        instabilityDeckOrders.clear();
        if (value == null) return;
        for (Map.Entry<String, ? extends List<String>> entry : value.entrySet()) {
            String deck = entry.getKey();
            if (deck == null || deck.isBlank()) continue;
            ArrayList<String> cards = new ArrayList<>();
            if (entry.getValue() != null) {
                for (String card : entry.getValue()) {
                    if (card != null && !card.isBlank()) cards.add(card);
                }
            }
            instabilityDeckOrders.put(deck, List.copyOf(cards));
        }
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Version", VERSION);
        tag.putString("PortVersion", PORT_VERSION);
        tag.putInt("AgeUID", ageUid);
        tag.putString("DimensionKey", dimensionKey);
        tag.putString("AgeName", ageName == null ? "" : ageName);
        tag.putLong("Seed", seed);
        tag.putString("UUID", uuid.toString());
        tag.putShort("BaseIns", baseInstability);
        tag.putBoolean("InstabilityEnabled", storedInstabilityEnabled);
        tag.putBoolean("Visited", visited);
        tag.putBoolean("Dead", dead);
        tag.putBoolean("StarFissureGenerated", starFissureGenerated);
        tag.putBoolean("StarFissureProcessed", starFissureProcessed);
        tag.putLong("WorldTime", worldTime);
        tag.putLong("DayTime", dayTime);
        tag.putInt("ClearWeatherTime", clearWeatherTime);
        tag.putInt("ThunderTime", thunderTime);
        tag.putInt("RainTime", rainTime);
        tag.putBoolean("Thundering", thundering);
        tag.putBoolean("Raining", raining);
        if (spawn != null) {
            tag.putInt("SpawnX", spawn.getX());
            tag.putInt("SpawnY", spawn.getY());
            tag.putInt("SpawnZ", spawn.getZ());
        }

        ListTag pageList = new ListTag();
        for (ItemStack page : pages) {
            Tag encoded = page.saveOptional(registries);
            if (encoded instanceof CompoundTag compound) pageList.add(compound);
        }
        tag.put("Pages", pageList);

        ListTag symbolList = new ListTag();
        for (String symbol : symbols) symbolList.add(StringTag.valueOf(symbol));
        tag.put("Symbols", symbolList);

        ListTag authorList = new ListTag();
        for (String author : authors) authorList.add(StringTag.valueOf(author));
        tag.put("Authors", authorList);

        // 0.13.7.06 persisted only Deck order. Provider levels are intentionally not stored; they
        // are reconstructed from the current score and this order after reload.
        ListTag instabilityDeckList = new ListTag();
        for (Map.Entry<String, List<String>> entry : instabilityDeckOrders.entrySet()) {
            CompoundTag deckTag = new CompoundTag();
            deckTag.putString("Name", entry.getKey());
            ListTag cards = new ListTag();
            for (String card : entry.getValue()) cards.add(StringTag.valueOf(card));
            deckTag.put("Cards", cards);
            instabilityDeckList.add(deckTag);
        }
        tag.put("InstabilityDecks", instabilityDeckList);
        return tag;
    }

    public static AgeRecord load(CompoundTag tag, HolderLookup.Provider registries) {
        int uid = tag.getInt("AgeUID");
        String dimensionKey = tag.getString("DimensionKey");
        String ageName = tag.getString("AgeName");
        long seed = tag.getLong("Seed");
        if (uid < 2) {
            throw new IllegalArgumentException("Invalid Mystcraft AgeUID: " + uid);
        }
        if (!AgeDimensionKeys.matches(uid, dimensionKey)) {
            throw new IllegalArgumentException("Invalid Mystcraft DimensionKey for Age " + uid + ": " + dimensionKey);
        }

        String uuidText = tag.getString("UUID");
        if (uuidText == null || uuidText.isBlank()) {
            throw new IllegalArgumentException("Missing UUID for Mystcraft Age " + uid);
        }
        final UUID uuid;
        try {
            uuid = UUID.fromString(uuidText);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid UUID for Mystcraft Age " + uid + ": " + uuidText, ex);
        }

        AgeRecord record = new AgeRecord(uid, dimensionKey, ageName, seed, uuid);
        record.baseInstability = tag.getShort("BaseIns");
        record.storedInstabilityEnabled = !tag.contains("InstabilityEnabled") || tag.getBoolean("InstabilityEnabled");
        record.visited = tag.getBoolean("Visited");
        record.dead = tag.getBoolean("Dead");
        record.starFissureGenerated = tag.getBoolean("StarFissureGenerated");
        record.starFissureProcessed = tag.contains("StarFissureProcessed")
                ? tag.getBoolean("StarFissureProcessed")
                : record.starFissureGenerated;
        record.worldTime = tag.getLong("WorldTime");
        record.dayTime = tag.contains("DayTime", Tag.TAG_LONG) ? tag.getLong("DayTime") : record.worldTime;
        record.clearWeatherTime = tag.getInt("ClearWeatherTime");
        record.thunderTime = tag.getInt("ThunderTime");
        record.rainTime = tag.getInt("RainTime");
        record.thundering = tag.getBoolean("Thundering");
        record.raining = tag.getBoolean("Raining");
        if (tag.contains("SpawnX", Tag.TAG_INT) && tag.contains("SpawnY", Tag.TAG_INT) && tag.contains("SpawnZ", Tag.TAG_INT)) {
            record.spawn = new BlockPos(tag.getInt("SpawnX"), tag.getInt("SpawnY"), tag.getInt("SpawnZ"));
        }

        ListTag pageList = tag.getList("Pages", Tag.TAG_COMPOUND);
        for (int i = 0; i < pageList.size(); i++) {
            ItemStack page = ItemStack.parseOptional(registries, pageList.getCompound(i));
            if (!page.isEmpty()) record.pages.add(page);
        }

        ListTag symbolList = tag.getList("Symbols", Tag.TAG_STRING);
        for (int i = 0; i < symbolList.size(); i++) {
            String id = symbolList.getString(i);
            if (!id.isBlank()) record.symbols.add(id);
        }

        // 0.13.7.06 AgeData.readFromNBT migrated both persisted page stacks
        // and the parallel symbol-id list once on every load. Keep that exact
        // compatibility boundary; do not fully collapse chained remappings.
        List<ItemStack> remappedPages = SymbolRemappings.remapPages(record.pages);
        record.pages.clear();
        record.pages.addAll(remappedPages);
        List<String> remappedSymbols = SymbolRemappings.remapIds(record.symbols);
        record.symbols.clear();
        record.symbols.addAll(remappedSymbols);

        ListTag authorList = tag.getList("Authors", Tag.TAG_STRING);
        for (int i = 0; i < authorList.size(); i++) {
            String author = authorList.getString(i);
            if (!author.isBlank()) record.authors.add(author);
        }

        ListTag instabilityDeckList = tag.getList("InstabilityDecks", Tag.TAG_COMPOUND);
        for (int i = 0; i < instabilityDeckList.size(); i++) {
            CompoundTag deckTag = instabilityDeckList.getCompound(i);
            String deck = deckTag.getString("Name");
            if (deck == null || deck.isBlank()) continue;
            ListTag cardList = deckTag.getList("Cards", Tag.TAG_STRING);
            ArrayList<String> cards = new ArrayList<>();
            for (int j = 0; j < cardList.size(); j++) {
                String card = cardList.getString(j);
                if (!card.isBlank()) cards.add(card);
            }
            record.instabilityDeckOrders.put(deck, List.copyOf(cards));
        }
        return record;
    }
}
