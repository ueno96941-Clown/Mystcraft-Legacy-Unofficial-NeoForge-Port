package com.xcompwiz.mystcraft.world.dimension;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.StructureCheck;

import java.util.Objects;

/**
 * Keeps StructureStart generation/reference/locator state enabled while suppressing vanilla's
 * automatic structure-piece placement during biome decoration for runtime Mystcraft Ages.
 *
 * <p>ChunkStatusTasks creates StructureStarts from the server's global WorldOptions before it
 * reaches FEATURES. ChunkGenerator.applyBiomeDecoration, however, gates piece placement through
 * the WorldGenRegion-scoped StructureManager.shouldGenerateStructures(). The top-level Age
 * manager continues to report structures enabled, but its {@link StructureManager#forWorldGenRegion}
 * view uses generateStructures=false. This isolates suppression to worldgen-region decoration
 * without making locators or other ServerLevel callers believe the Age has no structures.</p>
 */
public final class AgeLegacyStructurePlacementRuntime {
    private AgeLegacyStructurePlacementRuntime() {}

    public static void install(ServerLevel level, long ageSeed) {
        Objects.requireNonNull(level, "level");
        if (level.getSeed() != ageSeed) {
            throw new IllegalStateException("Mystcraft structure-placement suppression seed mismatch");
        }

        level.structureManager = new PopulationAwareStructureManager(
                level,
                level.structureCheck,
                ageSeed);
    }

    private static final class PopulationAwareStructureManager extends StructureManager {
        private final ServerLevel sourceLevel;
        private final StructureCheck structureCheck;
        private final WorldOptions suppressedWorldgenOptions;

        PopulationAwareStructureManager(ServerLevel level, StructureCheck structureCheck, long ageSeed) {
            super(level, new WorldOptions(ageSeed, true, false), structureCheck);
            this.sourceLevel = level;
            this.structureCheck = structureCheck;
            this.suppressedWorldgenOptions = new WorldOptions(ageSeed, false, false);
        }

        @Override
        public StructureManager forWorldGenRegion(WorldGenRegion region) {
            if (region.getLevel() != sourceLevel) {
                throw new IllegalStateException("Using invalid structure manager for Mystcraft Age worldgen region");
            }
            return new StructureManager(region, suppressedWorldgenOptions, structureCheck);
        }
    }
}
