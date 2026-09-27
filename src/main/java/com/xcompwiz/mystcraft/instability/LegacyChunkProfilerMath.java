package com.xcompwiz.mystcraft.instability;

import java.util.LinkedHashMap;
import java.util.Map;

/** Pure-Java form of the legacy ChunkProfiler accessibility calculation. */
public final class LegacyChunkProfilerMath {
    private LegacyChunkProfilerMath() {}

    public static Map<String, Float> calculateSplit(
            int chunkCount,
            long[] solidByY,
            Map<String, long[]> blocksByY,
            Map<String, LegacyInstabilityBlockManager.Factor> factors) {
        LinkedHashMap<String, Float> result = new LinkedHashMap<>();
        if (chunkCount <= 0 || solidByY.length == 0) return result;

        int layers = solidByY.length;
        float[] average = new float[layers];
        float[] rounded = new float[layers];
        float minimum = 1F;
        for (int y = 0; y < layers; ++y) {
            average[y] = solidByY[y] / (float) (chunkCount * 2L * 256L);
            minimum = Math.min(minimum, average[y]);
        }
        float groundSum = 0F;
        int groundCount = 0;
        for (int y = 0; y < layers; ++y) {
            float filtered = Math.max(0F, average[y] - minimum);
            rounded[y] = Math.round(100F * filtered) / 100F;
            if (rounded[y] > 0F) { groundSum += rounded[y]; ++groundCount; }
        }
        float ground = groundCount == 0 ? 0F : groundSum / groundCount;

        for (Map.Entry<String, long[]> entry : blocksByY.entrySet()) {
            LegacyInstabilityBlockManager.Factor factor = factors.get(entry.getKey());
            if (factor == null) continue;
            long[] counts = entry.getValue();
            float value = 0F;
            for (int y = 0; y < Math.min(layers, counts.length); ++y) {
                float accessibility = rounded[y] > ground ? 1F - rounded[y] : 1F;
                float abundance = counts[y] / (float) chunkCount;
                value += abundance * accessibility * factor.accessibilityFactor()
                        + abundance * factor.flatFactor();
            }
            result.put(entry.getKey(), value);
        }
        return result;
    }

    public static int sumNonOre(Map<String, Float> split, Map<String, LegacyInstabilityBlockManager.Factor> factors) {
        float total = 0F;
        for (Map.Entry<String, Float> entry : split.entrySet()) {
            LegacyInstabilityBlockManager.Factor factor = factors.get(entry.getKey());
            if (factor != null && !factor.oreResource() && entry.getValue() > 0F) total += entry.getValue();
        }
        return Math.max(0, Math.round(total));
    }
}
