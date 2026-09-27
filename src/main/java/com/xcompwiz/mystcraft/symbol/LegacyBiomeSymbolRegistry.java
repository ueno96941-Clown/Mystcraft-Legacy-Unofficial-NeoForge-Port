package com.xcompwiz.mystcraft.symbol;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import com.xcompwiz.mystcraft.world.worldgen.LegacyModernBiomeIdResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.Comparator;
import java.lang.ref.WeakReference;

/**
 * Conservative 1.12 numeric-biome-ID compatibility map.
 *
 * <p>Old Page IDs stay exactly `mystcraft:Biome&lt;numeric id&gt;`. Modern mappings are
 * intentionally best-effort for vanilla IDs whose names changed or whose variants were
 * folded into newer biome keys. At server/client runtime, all additional registered 1.21
 * biomes are exposed through deterministic high-range synthetic numeric IDs while the
 * original 1.12 numeric identities remain untouched.</p>
 */
public final class LegacyBiomeSymbolRegistry {
    private static final Map<String, LegacyBiomeSymbolDefinition> BY_LEGACY_ID = new LinkedHashMap<>();
    private static final Set<String> RUNTIME_IDS = new HashSet<>();
    private static boolean bootstrapped;
    private static String runtimeFingerprint = "";
    private static WeakReference<Registry<Biome>> lastRuntimeRegistry = new WeakReference<>(null);
    private static int lastRuntimeRegistrySize = -1;

    /** Synthetic numeric range reserved for 1.21+ biomes which had no 1.12 numeric ID. */
    private static final int RUNTIME_ID_BASE = 1_000_000_000;
    private static final int RUNTIME_ID_SPAN = 1_000_000_000;

    private static final Map<Integer, String> LEGACY_BIOME_NAMES = Map.ofEntries(
            Map.entry(0, "Ocean"),
            Map.entry(1, "Plains"),
            Map.entry(2, "Desert"),
            Map.entry(3, "Extreme Hills"),
            Map.entry(4, "Forest"),
            Map.entry(5, "Taiga"),
            Map.entry(6, "Swampland"),
            Map.entry(7, "River"),
            Map.entry(8, "Hell"),
            Map.entry(9, "Sky"),
            Map.entry(10, "FrozenOcean"),
            Map.entry(11, "FrozenRiver"),
            Map.entry(12, "Ice Plains"),
            Map.entry(13, "Ice Mountains"),
            Map.entry(14, "MushroomIsland"),
            Map.entry(15, "MushroomIslandShore"),
            Map.entry(16, "Beach"),
            Map.entry(17, "DesertHills"),
            Map.entry(18, "ForestHills"),
            Map.entry(19, "TaigaHills"),
            Map.entry(20, "Smaller Extreme Hills"),
            Map.entry(21, "Jungle"),
            Map.entry(22, "JungleHills"),
            Map.entry(23, "JungleEdge"),
            Map.entry(24, "Deep Ocean"),
            Map.entry(25, "Stone Beach"),
            Map.entry(26, "Cold Beach"),
            Map.entry(27, "Birch Forest"),
            Map.entry(28, "Birch Forest Hills"),
            Map.entry(29, "Roofed Forest"),
            Map.entry(30, "Cold Taiga"),
            Map.entry(31, "Cold Taiga Hills"),
            Map.entry(32, "Redwood Taiga"),
            Map.entry(33, "Redwood Taiga Hills"),
            Map.entry(34, "Extreme Hills With Trees"),
            Map.entry(35, "Savanna"),
            Map.entry(36, "Savanna Plateau"),
            Map.entry(37, "Mesa"),
            Map.entry(38, "Mesa Plateau F"),
            Map.entry(39, "Mesa Plateau"),
            Map.entry(127, "The Void"),
            Map.entry(129, "Sunflower Plains"),
            Map.entry(130, "Desert M"),
            Map.entry(131, "Extreme Hills M"),
            Map.entry(132, "Flower Forest"),
            Map.entry(133, "Taiga M"),
            Map.entry(134, "Swampland M"),
            Map.entry(140, "Ice Plains Spikes"),
            Map.entry(149, "Jungle M"),
            Map.entry(151, "JungleEdge M"),
            Map.entry(155, "Birch Forest M"),
            Map.entry(156, "Birch Forest Hills M"),
            Map.entry(157, "Roofed Forest M"),
            Map.entry(158, "Cold Taiga M"),
            Map.entry(160, "Redwood Taiga M"),
            Map.entry(161, "Redwood Taiga Hills M"),
            Map.entry(162, "Extreme Hills+ M"),
            Map.entry(163, "Savanna M"),
            Map.entry(164, "Savanna Plateau M"),
            Map.entry(165, "Mesa (Bryce)"),
            Map.entry(166, "Mesa Plateau F M"),
            Map.entry(167, "Mesa Plateau M")
    );

    private LegacyBiomeSymbolRegistry() {}

    public static synchronized void bootstrapDefaults() {
        if (bootstrapped) return;
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome0", 0, "minecraft:ocean", true, -1.0D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome1", 1, "minecraft:plains", true, 0.125D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome2", 2, "minecraft:desert", true, 0.125D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome3", 3, "minecraft:mountains", false, 1.0D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome4", 4, "minecraft:forest", true, 0.1D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome5", 5, "minecraft:taiga", true, 0.2D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome6", 6, "minecraft:swamp", true, -0.2D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome7", 7, "minecraft:river", true, -0.5D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome8", 8, "minecraft:nether_wastes", true, 0.1D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome9", 9, "minecraft:the_end", true, 0.1D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome10", 10, "minecraft:frozen_ocean", true, -1.0D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome11", 11, "minecraft:frozen_river", true, -0.5D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome12", 12, "minecraft:snowy_plains", false, 0.125D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome13", 13, "minecraft:snowy_mountains", false, 0.45D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome14", 14, "minecraft:mushroom_fields", true, 0.2D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome15", 15, "minecraft:mushroom_field_shore", false, 0.0D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome16", 16, "minecraft:beach", true, 0.0D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome17", 17, "minecraft:desert_hills", false, 0.45D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome18", 18, "minecraft:wooded_hills", false, 0.45D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome19", 19, "minecraft:taiga_hills", false, 0.45D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome20", 20, "minecraft:mountain_edge", false, 0.8D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome21", 21, "minecraft:jungle", true, 0.1D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome22", 22, "minecraft:jungle_hills", false, 0.45D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome23", 23, "minecraft:sparse_jungle", false, 0.1D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome24", 24, "minecraft:deep_ocean", true, -1.8D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome25", 25, "minecraft:stony_shore", true, 0.1D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome26", 26, "minecraft:snowy_beach", true, 0.0D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome27", 27, "minecraft:birch_forest", true, 0.1D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome28", 28, "minecraft:birch_forest_hills", false, 0.45D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome29", 29, "minecraft:dark_forest", true, 0.1D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome30", 30, "minecraft:snowy_taiga", true, 0.2D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome31", 31, "minecraft:snowy_taiga_hills", false, 0.45D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome32", 32, "minecraft:old_growth_pine_taiga", true, 0.2D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome33", 33, "minecraft:old_growth_spruce_taiga", false, 0.45D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome34", 34, "minecraft:wooded_mountains", false, 1.0D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome35", 35, "minecraft:savanna", true, 0.125D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome36", 36, "minecraft:savanna_plateau", true, 1.5D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome37", 37, "minecraft:badlands", true, 0.1D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome38", 38, "minecraft:wooded_badlands", true, 1.5D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome39", 39, "minecraft:badlands", true, 1.5D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome127", 127, "minecraft:the_void", true, 0.1D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome129", 129, "minecraft:sunflower_plains", true, 0.125D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome130", 130, "minecraft:desert_lakes", false, 0.225D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome131", 131, "minecraft:gravelly_mountains", false, 1.0D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome132", 132, "minecraft:flower_forest", true, 0.1D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome133", 133, "minecraft:taiga_mountains", false, 0.3D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome134", 134, "minecraft:swamp_hills", false, -0.1D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome140", 140, "minecraft:ice_spikes", true, 0.425D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome149", 149, "minecraft:modified_jungle", false, 0.2D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome151", 151, "minecraft:modified_jungle_edge", false, 0.2D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome155", 155, "minecraft:tall_birch_forest", true, 0.2D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome156", 156, "minecraft:tall_birch_hills", false, 0.55D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome157", 157, "minecraft:dark_forest_hills", false, 0.2D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome158", 158, "minecraft:snowy_taiga_mountains", false, 0.3D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome160", 160, "minecraft:giant_spruce_taiga", true, 0.2D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome161", 161, "minecraft:giant_spruce_taiga_hills", false, 0.45D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome162", 162, "minecraft:modified_gravelly_mountains", false, 1.0D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome163", 163, "minecraft:shattered_savanna", true, 0.3625D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome164", 164, "minecraft:shattered_savanna_plateau", false, 1.05D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome165", 165, "minecraft:eroded_badlands", false, 0.1D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome166", 166, "minecraft:modified_wooded_badlands_plateau", false, 1.5D));
        register(new LegacyBiomeSymbolDefinition("mystcraft:Biome167", 167, "minecraft:modified_badlands_plateau", false, 1.5D));
        bootstrapped = true;
    }


    /**
     * Adds every biome visible in the current dynamic biome registry while preserving the
     * original 1.12 numeric IDs for legacy pages. New 1.21/modded biomes receive a stable
     * synthetic numeric ID derived from their ResourceLocation, so they retain the original
     * {@code mystcraft:Biome<number>} shape without colliding with vanilla 1.12 IDs.
     */
    public static synchronized boolean bootstrapRuntime(RegistryAccess access) {
        bootstrapDefaults();
        Registry<Biome> registry = access.registryOrThrow(Registries.BIOME);
        Registry<Biome> previous = lastRuntimeRegistry.get();
        if (previous == registry && lastRuntimeRegistrySize == registry.size()) return false;
        ArrayList<ResourceLocation> keys = new ArrayList<>(registry.keySet());
        keys.sort(Comparator.comparing(ResourceLocation::toString));
        String fingerprint = String.join("\n", keys.stream().map(ResourceLocation::toString).toList());
        if (fingerprint.equals(runtimeFingerprint)) {
            lastRuntimeRegistry = new WeakReference<>(registry);
            lastRuntimeRegistrySize = registry.size();
            return false;
        }

        // Runtime/datapack registries can change between integrated-server sessions.
        // Keep the immutable 1.12 compatibility table, but rebuild synthetic entries.
        for (String id : RUNTIME_IDS) BY_LEGACY_ID.remove(id);
        RUNTIME_IDS.clear();

        HashSet<String> representedModernIds = new HashSet<>();
        for (LegacyBiomeSymbolDefinition definition : BY_LEGACY_ID.values()) {
            representedModernIds.add(LegacyModernBiomeIdResolver.resolveModernId(definition.modernBiomeId()));
        }
        HashSet<Integer> usedNumericIds = new HashSet<>();
        for (LegacyBiomeSymbolDefinition definition : BY_LEGACY_ID.values()) usedNumericIds.add(definition.legacyNumericId());

        for (ResourceLocation key : keys) {
            String modernId = key.toString();
            if (representedModernIds.contains(modernId)) continue;
            int numericId = stableRuntimeNumericId(modernId, usedNumericIds);
            LegacyBiomeSymbolDefinition definition = new LegacyBiomeSymbolDefinition(
                    "mystcraft:Biome" + numericId, numericId, modernId, true, 0.125D);
            register(definition);
            RUNTIME_IDS.add(definition.legacyId());
            representedModernIds.add(modernId);
            usedNumericIds.add(numericId);
        }
        runtimeFingerprint = fingerprint;
        lastRuntimeRegistry = new WeakReference<>(registry);
        lastRuntimeRegistrySize = registry.size();
        return true;
    }

    private static int stableRuntimeNumericId(String modernId, Set<Integer> used) {
        for (int salt = 0; salt < 10_000; salt++) {
            String input = salt == 0 ? modernId : modernId + "#" + salt;
            int candidate = RUNTIME_ID_BASE + Math.floorMod(input.hashCode(), RUNTIME_ID_SPAN);
            if (!used.contains(candidate)) return candidate;
        }
        throw new IllegalStateException("Could not allocate stable runtime biome symbol id for " + modernId);
    }

    public static synchronized int runtimeSize() {
        return RUNTIME_IDS.size();
    }

    private static void register(LegacyBiomeSymbolDefinition definition) {
        LegacyBiomeSymbolDefinition prior = BY_LEGACY_ID.putIfAbsent(definition.legacyId(), definition);
        if (prior != null && !prior.equals(definition)) {
            throw new IllegalStateException("Duplicate legacy biome symbol: " + definition.legacyId());
        }
    }

    public static Optional<LegacyBiomeSymbolDefinition> resolveLegacy(String legacyId) {
        bootstrapDefaults();
        return Optional.ofNullable(BY_LEGACY_ID.get(LegacySymbolId.qualify(legacyId)));
    }

    public static boolean containsLegacy(String legacyId) {
        return resolveLegacy(legacyId).isPresent();
    }

    /** Human-readable 1.12 biome name, absent for synthetic 1.21+/modded entries. */
    public static Optional<String> legacyDisplayName(LegacyBiomeSymbolDefinition definition) {
        if (definition == null) return Optional.empty();
        return Optional.ofNullable(LEGACY_BIOME_NAMES.get(definition.legacyNumericId()));
    }

    public static Collection<LegacyBiomeSymbolDefinition> values() {
        bootstrapDefaults();
        return Collections.unmodifiableList(new ArrayList<>(BY_LEGACY_ID.values()));
    }


    /** Exact 1.12 biome-name word used by SymbolBiome for legacy numeric IDs. */
    public static String legacyVisualWord(LegacyBiomeSymbolDefinition definition) {
        String legacyName = LEGACY_BIOME_NAMES.get(definition.legacyNumericId());
        if (legacyName != null) return legacyName + definition.legacyNumericId();
        ResourceLocation modern = ResourceLocation.tryParse(definition.modernBiomeId());
        String fallback = modern == null
                ? definition.modernBiomeId().replace(':', '_')
                : modern.getNamespace() + "_" + modern.getPath();
        return fallback + definition.legacyNumericId();
    }

    public static int size() {
        bootstrapDefaults();
        return BY_LEGACY_ID.size();
    }
}
