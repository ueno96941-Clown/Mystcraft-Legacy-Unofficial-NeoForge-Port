package com.xcompwiz.mystcraft.symbol;

import com.xcompwiz.mystcraft.page.Page;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Legacy 0.13.7.06 symbol-id migration table.
 *
 * <p>The original mod migrated old Page ids lazily when pages/books/folders were
 * loaded. 1.21 ResourceLocation rules cannot represent several of those historic
 * mixed-case ids directly, so this port keeps the persisted legacy String as the
 * migration identity and only canonicalizes for map lookup.</p>
 *
 * <p>This class intentionally performs data migration only. Runtime Instability and Decay
 * activation are controlled separately by the current Instability runtime policy.</p>
 */
public final class SymbolRemappings {
    private static final Map<String, List<String>> MAPPINGS = new HashMap<>();
    private static boolean initialized;

    private SymbolRemappings() {}

    private static synchronized void initialize() {
        if (initialized) return;

        fogColorRemappings();
        cloudColorRemappings();
        skyColorRemappings();
        horizonColorRemappings();

        // Historic material ids. The final 1.12 table first routed these through
        // transient registry names; the 1.21 port can safely land on the equivalent
        // current material Symbol ids directly.
        add("ModMat_tile.stone", "ModMat_stone_0");
        add("ModMat_tile.lava", "ModMat_lava_0");
        add("ModMat_tile.water", "ModMat_water_0");
        add("ModMat_minecraft:stone_0", "ModMat_stone_0");
        add("ModMat_minecraft:flowing_lava_0", "ModMat_lava_0");
        add("ModMat_minecraft:flowing_water_0", "ModMat_water_0");
        add("modmat_flowing_water_0", "ModMat_water_0");
        add("ModMat_tile.hellrock", "ModMat_netherrack_0");
        add("ModMat_tile.whiteStone", "ModMat_end_stone_0");
        add("ModMat_tile.oreDiamond", "ModMat_diamond_ore_0");
        add("ModMat_tile.ice", "ModMat_ice_0");
        add("ModMat_tile.myst.crystal", "ModMat_blockcrystal_0");
        add("ModMat_tile.myst.lightgem", "ModMat_glowstone_0");
        add("ModMat_tile.myst.netherquartz", "ModMat_quartz_ore_0");

        add("ModMaroon", "ModColorMaroon");
        add("ModRed", "ModColorRed");
        add("ModOlive", "ModColorOlive");
        add("ModYellow", "ModColorYellow");
        add("ModDark Green", "ModColorDarkGreen");
        add("ModGreen", "ModColorGreen");
        add("ModTeal", "ModColorTeal");
        add("ModCyan", "ModColorCyan");
        add("ModNavy", "ModColorNavy");
        add("ModBlue", "ModColorBlue");
        add("ModPurple", "ModColorPurple");
        add("ModMagenta", "ModColorMagenta");
        add("ModBlack", "ModColorBlack");
        add("ModGrey", "ModColorGrey");
        add("ModSilver", "ModColorSilver");
        add("ModWhite", "ModColorWhite");

        add("LavaLakes", "ModMat_tile.lava", "LakesDeep");
        add("Lakes", "ModMat_tile.water", "LakesSurface");
        add("CryFormCry", "ModMat_tile.myst.crystal", "CryForm");
        add("CryFormGlow", "ModMat_tile.myst.lightgem", "CryForm");
        add("CryFormQuartz", "ModMat_tile.myst.netherquartz", "CryForm");

        add("Standard Terrain", "TerrainNormal");
        add("Star Fissure", "StarFissure");
        add("Rain", "WeatherRain");
        add("Snow", "WeatherSnow");
        add("Huge Trees", "HugeTrees");
        add("NormalStars", "StarsNormal");
        add("Single Biome", "BioConSingle");
        add("Checkerboard Biomes", "BioConTiled");
        add("BiomeControllerNative", "BioConNative");
        add("Lava Lakes", "LavaLakes");
        add("WeatherSun", "WeatherOff");
        add("Standard Lighting", "LightingNormal");
        add("Storm", "WeatherStorm");
        add("Fog", "ColorFog");

        add("ModFluid_tile.lava", "ModMat_tile.lava");
        add("ModFluid_tile.water", "ModMat_tile.water");
        add("ModFluidtile.water", "ModMat_tile.water");
        add("ModFluidtile.lava", "ModMat_tile.lava");
        add("ModLavaSea", "ModMat_tile.lava");
        add("ModNetherTerrain", "ModMat_tile.hellrock");
        add("ModMattile.hellrock", "ModMat_tile.hellrock");
        add("ModMattile.whiteStone", "ModMat_tile.whiteStone");
        add("ModMattile.oreDiamond", "ModMat_tile.oreDiamond");

        add("TendrilsIce", "ModMat_tile.ice", "Tendrils");
        add("WoodCaves", "Tendrils");
        add("SkyDropDark", "StarsDark");

        add("FTime", "ModHalf", "SunNormal", "ModHalf", "MoonNormal");
        add("STime", "ModDouble", "SunNormal", "ModDouble", "MoonNormal");
        add("NTime", "ModFull", "SunNormal", "ModFull", "MoonNormal");
        add("Dusk", "ModZero", "ModSetting", "SunNormal", "ModZero", "MoonNormal");
        add("Night", "SunDark", "ModZero", "MoonNormal");
        add("Day", "MoonDark", "ModZero", "ModNoon", "SunNormal");

        add("Heavy Resources", "DenseOres");
        add("SunsetNormal", "SunsetRed");
        add("CloudNormal", "CloudWhite");
        add("Normal Sunset Colors", "SunsetRed");
        add("NativeBiomeController", "BioConLarge");
        add("Flat Sea", "TerrainFlat");
        add("Sky Islands", "Skylands");
        add("Tree Age", "Huge Trees", "TerrainFlat", "Swampland", "BioConSingle");
        add("DefaultBiome", "BioConSingle");
        add("DefaultLighting", "Standard Lighting");
        add("DefaultSunrise", "Normal Sunset Colors");
        add("DefaultTerrain", "Standard Terrain");
        add("Flat", "TerrainFlat");
        add("Void", "TerrainVoid");

        initialized = true;
    }

    public static boolean hasRemapping(String legacyId) {
        initialize();
        if (legacyId == null || legacyId.isBlank()) return false;
        String qualified = LegacySymbolId.qualify(legacyId);
        int split = qualified.indexOf(':');
        String namespace = split < 0 ? "mystcraft" : qualified.substring(0, split);
        return namespace.toLowerCase(Locale.ROOT).startsWith("modmat_")
                || namespace.equalsIgnoreCase("minecraft")
                || MAPPINGS.containsKey(key(qualified));
    }

    /** Returns a one-step legacy remapping, preserving target mixed-case ids. */
    public static List<String> remap(String legacyId) {
        initialize();
        if (legacyId == null || legacyId.isBlank()) return List.of();

        String qualified = LegacySymbolId.qualify(legacyId);
        int split = qualified.indexOf(':');
        String namespace = split < 0 ? "mystcraft" : qualified.substring(0, split);
        String path = split < 0 ? qualified : qualified.substring(split + 1);

        // 0.13.7.06 compatibility rules from SymbolRemappings.remap(ResourceLocation).
        if (namespace.toLowerCase(Locale.ROOT).startsWith("modmat_")) {
            String realNamespace = namespace.substring("modmat_".length());
            qualified = realNamespace + ":modmat_" + path;
        } else if (namespace.equalsIgnoreCase("minecraft")) {
            qualified = "mystcraft:" + path;
        }

        List<String> mapped = MAPPINGS.get(key(qualified));
        if (mapped == null) return List.of(LegacySymbolId.qualify(qualified));
        return mapped;
    }

    /**
     * Expands chained historic mappings to the current representation. A guard
     * prevents malformed/cyclic external ids from looping forever.
     */
    public static List<String> remapFully(String legacyId) {
        List<String> pending = new ArrayList<>();
        pending.add(LegacySymbolId.qualify(legacyId));
        List<String> out = new ArrayList<>();
        int guard = 0;
        while (!pending.isEmpty() && guard++ < 256) {
            String current = pending.removeFirst();
            if (!hasRemapping(current)) {
                out.add(current);
                continue;
            }
            List<String> step = remap(current);
            if (step.size() == 1 && sameId(step.getFirst(), current)) {
                out.add(step.getFirst());
                continue;
            }
            pending.addAll(0, step);
        }
        if (!pending.isEmpty()) out.addAll(pending);
        return List.copyOf(out);
    }

    /** Mirrors old SymbolRemappings.remap(ItemStack), including multi-page splits. */
    public static List<ItemStack> remapPage(ItemStack page) {
        if (page == null || page.isEmpty() || Page.isBlank(page) || Page.isLinkPanel(page)) {
            return page == null || page.isEmpty() ? List.of() : List.of(page.copy());
        }
        String symbol = Page.getSymbolId(page);
        if (symbol == null) return List.of(page.copy());
        // Legacy 0.13.7.06 applied exactly one remapping step per ItemStack
        // load/update. Chained historic ids therefore migrate lazily across
        // subsequent loads/ticks instead of being collapsed in one call.
        List<String> mapped = remap(symbol);
        if (mapped.isEmpty()) return List.of();

        List<ItemStack> result = new ArrayList<>(mapped.size());
        ItemStack first = page.copyWithCount(1);
        Page.setSymbolId(first, mapped.getFirst());
        result.add(first);
        for (int i = 1; i < mapped.size(); i++) {
            result.add(Page.createSymbolPage(mapped.get(i)));
        }
        return result;
    }

    /**
     * Mirrors 0.13.7.06 {@code SymbolRemappings.remap(List)} ordering exactly.
     * The first element produced by a mapping is skipped for this pass while
     * any additional elements are visited by later loop iterations. This odd
     * ordering is intentional legacy migration behaviour.
     */
    public static List<ItemStack> remapPages(List<ItemStack> pages) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack page : pages) {
            if (page != null && !page.isEmpty()) result.add(page.copy());
        }
        for (int i = 0; i < result.size();) {
            ItemStack element = result.remove(i);
            List<ItemStack> mapping = remapPage(element);
            result.addAll(i, mapping);
            ++i;
        }
        return result;
    }

    /** String-id counterpart used by loaded AgeData symbol lists. */
    public static List<String> remapIds(List<String> symbols) {
        List<String> result = new ArrayList<>();
        for (String symbol : symbols) {
            if (symbol != null && !symbol.isBlank()) result.add(LegacySymbolId.qualify(symbol));
        }
        for (int i = 0; i < result.size();) {
            String element = result.remove(i);
            List<String> mapping = remap(element);
            result.addAll(i, mapping);
            ++i;
        }
        return result;
    }

    private static void fogColorRemappings() {
        add("FogChromatic", chromatic("ColorFog"));
        add("FogRed", "ModRed", "ColorFog");
        add("FogGreen", "ModGreen", "ColorFog");
        add("FogBlue", "ModBlue", "ColorFog");
        add("FogBlack", "ModBlack", "ColorFog");
        add("FogWhite", "ModWhite", "ColorFog");
        add("FogNormal", "ModWhite", "ColorFog");
    }

    private static void cloudColorRemappings() {
        add("CloudChromatic", chromatic("ColorCloud"));
        add("CloudRed", "ModRed", "ColorCloud");
        add("CloudGreen", "ModGreen", "ColorCloud");
        add("CloudBlue", "ModBlue", "ColorCloud");
        add("CloudBlack", "ModBlack", "ColorCloud");
        add("CloudWhite", "ModWhite", "ColorCloud");
    }

    private static void skyColorRemappings() {
        add("ModGradient_HERE", "ModGradient", "ColorSky");
        add("SkyChromatic", chromatic("ColorSky"));
        add("SkyRed", "ModRed", "ColorSky");
        add("SkyGreen", "ModGreen", "ColorSky");
        add("SkyBlue", "ModBlue", "ColorSky");
        add("SkyBlack", "ModBlack", "ColorSky");
        add("SkyWhite", "ModWhite", "ColorSky");
        add("SkyNormal", "ModBlue", "ColorSky");
    }

    private static void horizonColorRemappings() {
        add("SunsetChromatic", chromatic("ColorHorizon"));
        add("SunsetRed", "ModRed", "ColorHorizon");
        add("SunsetGreen", "ModGreen", "ColorHorizon");
        add("SunsetBlue", "ModBlue", "ColorHorizon");
        add("SunsetBlack", "ModBlack", "ColorHorizon");
        add("SunsetWhite", "ModWhite", "ColorHorizon");
    }

    private static String[] chromatic(String terminal) {
        return new String[]{
                "ModBlack", "ModRed", "ModRed", "ModGradient",
                "ModBlack", "ModGreen", "ModGreen", "ModGradient",
                "ModBlack", "ModBlue", "ModBlue", "ModGradient", terminal
        };
    }

    private static void add(String oldId, String... newIds) {
        List<String> mapped = new ArrayList<>(newIds.length);
        for (String id : newIds) mapped.add(localId(id));
        MAPPINGS.put(key(localId(oldId)), List.copyOf(mapped));
    }

    /** Original addSymbolRemappingInternal always used the mystcraft namespace,
     * even for historic path text that itself contained a colon. */
    private static String localId(String id) {
        if (id == null || id.isBlank()) return "";
        return id.startsWith("mystcraft:") ? id : "mystcraft:" + id;
    }

    private static String key(String id) {
        var canonical = LegacySymbolId.canonicalKey(id);
        return canonical == null ? LegacySymbolId.qualify(id).toLowerCase(Locale.ROOT) : canonical.toString();
    }

    private static boolean sameId(String a, String b) {
        return key(a).equals(key(b));
    }
}
