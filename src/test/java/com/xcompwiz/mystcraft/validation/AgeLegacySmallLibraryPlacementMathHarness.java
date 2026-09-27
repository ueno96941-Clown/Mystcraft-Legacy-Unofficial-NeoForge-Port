package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.world.worldgen.AgeLegacySmallLibraryPlacementMath;

/** Executable regression for legacy Small Library candidate spacing including negative regions. */
public final class AgeLegacySmallLibraryPlacementMathHarness {
    public static void main(String[] args) {
        verifySeed(0L);
        verifySeed(1L);
        verifySeed(-9876543212345L);
        verifySeed(0x123456789ABCDEFL);
        System.out.println("AgeLegacySmallLibraryPlacementMathHarness: PASS");
    }

    private static void verifySeed(long seed) {
        for (int regionX = -5; regionX <= 5; ++regionX) {
            for (int regionZ = -5; regionZ <= 5; ++regionZ) {
                int hits = 0;
                for (int x = regionX * 32; x < regionX * 32 + 32; ++x) {
                    for (int z = regionZ * 32; z < regionZ * 32 + 32; ++z) {
                        if (AgeLegacySmallLibraryPlacementMath.isLibraryChunk(seed, x, z)) ++hits;
                    }
                }
                if (hits != 1) throw new AssertionError("expected one candidate per 32x32 region, seed=" + seed + " region=" + regionX + "," + regionZ + " hits=" + hits);
            }
        }
    }
}
