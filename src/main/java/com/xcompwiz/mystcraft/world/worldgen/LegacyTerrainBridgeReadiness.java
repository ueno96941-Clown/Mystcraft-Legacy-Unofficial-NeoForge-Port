package com.xcompwiz.mystcraft.world.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Static hand-off contract for the legacy-compatible terrain kernels.
 *
 * <p>This class deliberately does not replace the live 1.21.1 ChunkGenerator. It proves which
 * pieces are already available without a runtime test, and records the exact runtime-gated work
 * that remains before the legacy density kernels can safely own live chunk terrain.</p>
 */
public record LegacyTerrainBridgeReadiness(
        AgeTerrainMode terrainMode,
        boolean legacyDensityKernelAvailable,
        boolean legacyBiomeIdentityRetained,
        boolean liveBridgeEnabled,
        List<String> runtimeGates) {

    public LegacyTerrainBridgeReadiness {
        Objects.requireNonNull(terrainMode, "terrainMode");
        runtimeGates = List.copyOf(Objects.requireNonNull(runtimeGates, "runtimeGates"));
    }

    public static LegacyTerrainBridgeReadiness evaluate(AgeTerrainMode mode, AgeBiomeSourceSelection biomes) {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(biomes, "biomes");

        boolean kernel = switch (mode) {
            case NORMAL, AMPLIFIED, NETHER, END -> true;
            default -> false;
        };
        boolean identity = !biomes.legacyBiomeNumericIds().isEmpty()
                && biomes.legacyBiomeNumericIds().size() == biomes.modernBiomeIds().size();

        ArrayList<String> gates = new ArrayList<>();
        if (kernel) {
            gates.add("Implement a 1.21.1 ChunkGenerator adapter that consumes LegacyTerrainDensityKernel without changing FEATURES/structure ordering.");
            if (mode == AgeTerrainMode.NORMAL || mode == AgeTerrainMode.AMPLIFIED) {
                gates.add("Provide the legacy 10x10 biome-id window expected by the 5x5 parabolic biome blend for every density cell.");
                if (biomes.mode() == AgeBiomeControllerMode.NATIVE) {
                    gates.add("Native Biome Controller still uses modern MultiNoise; a legacy-compatible native biome sampler is required before exact Normal/Amplified terrain can be enabled.");
                } else if (!identity) {
                    gates.add("Preserve positional legacy biome identity through the live BiomeSource before enabling the density bridge.");
                }
            }
            gates.add("Runtime-test chunk seams, spawn chunk generation, save/reload and dedicated-server generation before enabling by default.");
        }

        return new LegacyTerrainBridgeReadiness(mode, kernel, identity, false, gates);
    }

    /** True only for terrain families whose exact old density kernel has already been ported. */
    public boolean kernelPrepared() {
        return legacyDensityKernelAvailable;
    }

    /**
     * The live bridge intentionally remains off until the runtime gates are exercised.
     * This prevents a source-only checkpoint from silently replacing a known-bootable generator.
     */
    public boolean requiresRuntimeGate() {
        return legacyDensityKernelAvailable && !liveBridgeEnabled;
    }
}
