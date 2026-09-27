package com.xcompwiz.mystcraft.world.worldgen;

/**
 * Fixed legacy biome-controller Symbols from Mystcraft 0.13.7.06.
 *
 * <p>The zoom scale is the value passed to the shared legacy Large controller.
 * Native/Grid/Tiled/Single used dedicated implementations.</p>
 */
public enum AgeBiomeControllerMode {
    NATIVE(0, -1),
    SINGLE(1, -1),
    GRID(2, -1),
    TILED(2, -1),
    HUGE(3, 4),
    LARGE(3, 3),
    MEDIUM(3, 2),
    SMALL(3, 1),
    TINY(3, 0),
    UNKNOWN(0, -1);

    private final int minimumBiomeCount;
    private final int zoomScale;

    AgeBiomeControllerMode(int minimumBiomeCount, int zoomScale) {
        this.minimumBiomeCount = minimumBiomeCount;
        this.zoomScale = zoomScale;
    }

    public int minimumBiomeCount() {
        return minimumBiomeCount;
    }

    public int zoomScale() {
        return zoomScale;
    }

    public boolean usesNativeProvider() {
        return this == NATIVE;
    }

    public boolean consumesAllQueuedBiomes() {
        return switch (this) {
            case GRID, TILED, HUGE, LARGE, MEDIUM, SMALL, TINY -> true;
            default -> false;
        };
    }
}
