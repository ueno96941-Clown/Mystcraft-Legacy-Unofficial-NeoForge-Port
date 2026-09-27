package com.xcompwiz.mystcraft.world.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Replays the 1.12 biome modifier queue across all fixed BiomeController Symbols.
 *
 * <p>Legacy `ModifierUtils.pushBiome` appended to the list and `popBiome` removed from
 * the end, so the queue is LIFO. Single consumes one; Grid/Tiled and the zoom-family
 * controllers consume all queued biomes; Native consumes none. Legacy registerInterface then lets the last controller replace earlier ones.</p>
 */
public final class AgeBiomeControllerResolver {
    private static final Pattern LEGACY_BIOME_SYMBOL =
            Pattern.compile("^mystcraft:Biome\\d+$");

    private AgeBiomeControllerResolver() {}

    public static AgeBiomeControllerPlan resolve(List<String> effectiveSymbols) {
        List<String> queue = new ArrayList<>();
        AgeBiomeControllerPlan selected = new AgeBiomeControllerPlan(
                AgeBiomeControllerMode.UNKNOWN, List.of(), 0, 0, -1);

        for (String symbol : effectiveSymbols) {
            if ("mystcraft:ModClear".equals(symbol)) {
                queue.clear();
                continue;
            }

            if (isLegacyBiomeSymbol(symbol)) {
                queue.add(symbol);
                continue;
            }

            AgeBiomeControllerMode mode = controllerMode(symbol);
            if (mode == AgeBiomeControllerMode.UNKNOWN) continue;

            List<String> consumed = new ArrayList<>();
            if (mode == AgeBiomeControllerMode.SINGLE) {
                String biome = popBiome(queue);
                if (biome != null) consumed.add(biome);
            } else if (mode.consumesAllQueuedBiomes()) {
                String biome;
                while ((biome = popBiome(queue)) != null) consumed.add(biome);
            }

            int missing = Math.max(0, mode.minimumBiomeCount() - consumed.size());
            selected = new AgeBiomeControllerPlan(mode, consumed, mode.minimumBiomeCount(), missing, mode.zoomScale());
            // Do not return: legacy registerInterface(IBiomeController) let later controllers
            // replace this one, while this controller's modifier consumption remained real.
        }

        return selected;
    }

    static boolean isLegacyBiomeSymbol(String symbol) {
        return symbol != null && LEGACY_BIOME_SYMBOL.matcher(symbol).matches();
    }

    static AgeBiomeControllerMode controllerMode(String symbol) {
        if ("mystcraft:BioConNative".equals(symbol)) return AgeBiomeControllerMode.NATIVE;
        if ("mystcraft:BioConSingle".equals(symbol)) return AgeBiomeControllerMode.SINGLE;
        if ("mystcraft:BioConGrid".equals(symbol)) return AgeBiomeControllerMode.GRID;
        if ("mystcraft:BioConTiled".equals(symbol)) return AgeBiomeControllerMode.TILED;
        if ("mystcraft:BioConHuge".equals(symbol)) return AgeBiomeControllerMode.HUGE;
        if ("mystcraft:BioConLarge".equals(symbol)) return AgeBiomeControllerMode.LARGE;
        if ("mystcraft:BioConMedium".equals(symbol)) return AgeBiomeControllerMode.MEDIUM;
        if ("mystcraft:BioConSmall".equals(symbol)) return AgeBiomeControllerMode.SMALL;
        if ("mystcraft:BioConTiny".equals(symbol)) return AgeBiomeControllerMode.TINY;
        return AgeBiomeControllerMode.UNKNOWN;
    }

    private static String popBiome(List<String> queue) {
        return queue.isEmpty() ? null : queue.remove(queue.size() - 1);
    }
}
