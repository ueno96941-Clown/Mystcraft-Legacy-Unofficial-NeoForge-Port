package com.xcompwiz.mystcraft.world.worldgen;

/**
 * Terrain-generator family selected from the Grammar-completed legacy Symbol sequence.
 *
 * <p>Checkpoint 13D-3 covers every fixed terrain-generator Symbol restored so far.
 * Material modifiers and the legacy visual side effects of Nether/End remain separate
 * concerns for later checkpoints.</p>
 */
public enum AgeTerrainMode {
    NORMAL,
    AMPLIFIED,
    NETHER,
    END,
    FLAT,
    VOID,
    UNKNOWN
}
