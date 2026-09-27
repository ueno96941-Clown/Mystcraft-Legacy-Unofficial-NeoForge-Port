package com.xcompwiz.mystcraft.world.dimension;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Replaces 1.21.1's Xoroshiro/forked stronghold ring positions with the 1.12 Java-Random stream.
 *
 * <p>Minecraft 1.12 MapGenStronghold used one java.util.Random continuously for ring angles,
 * radial jitter and BiomeProvider#findBiomePosition reservoir choices. Modern 1.21.1 uses
 * RandomSource.create() (Xoroshiro) and forks the biome-search RNG, so equal distance/spread/count
 * constants still produce different strongholds. LegacyRandomSource is the modern implementation
 * of Java's 48-bit LCG, allowing the old sequential stream to be restored.</p>
 */
final class AgeLegacyStrongholdRingInstaller {
    private static final int BIOME_SEARCH_RADIUS = 112;

    private AgeLegacyStrongholdRingInstaller() {}

    static void replaceRings(ChunkGeneratorStructureState state, BiomeSource biomeSource, long ageSeed) {
        for (Holder<StructureSet> holder : state.possibleStructureSets()) {
            if (!(holder.value().placement() instanceof ConcentricRingsStructurePlacement placement)) {
                continue;
            }
            List<ChunkPos> positions = generateLegacyPositions(
                    state, biomeSource, placement, ageSeed);
            state.ringPositions.put(
                    placement, CompletableFuture.completedFuture(List.copyOf(positions)));
        }
    }

    private static List<ChunkPos> generateLegacyPositions(
            ChunkGeneratorStructureState state,
            BiomeSource biomeSource,
            ConcentricRingsStructurePlacement placement,
            long ageSeed) {

        int count = placement.count();
        if (count == 0) return List.of();

        int distance = placement.distance();
        int spread = placement.spread();
        HolderSet<Biome> preferred = placement.preferredBiomes();
        RandomSource random = new LegacyRandomSource(ageSeed);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        int ring = 0;
        int ringIndex = 0;
        ArrayList<ChunkPos> out = new ArrayList<>(count);

        for (int index = 0; index < count; ++index) {
            double radius = 4.0D * distance
                    + (double) distance * ring * 6.0D
                    + (random.nextDouble() - 0.5D) * (double) distance * 2.5D;
            int chunkX = (int) Math.round(Math.cos(angle) * radius);
            int chunkZ = (int) Math.round(Math.sin(angle) * radius);

            ChunkPos relocated = relocateToPreferredBiome(
                    state, biomeSource, preferred, random, chunkX, chunkZ);
            out.add(relocated != null ? relocated : new ChunkPos(chunkX, chunkZ));

            angle += (Math.PI * 2.0D) / (double) spread;
            ++ringIndex;
            if (ringIndex == spread) {
                ++ring;
                ringIndex = 0;
                spread += 2 * spread / (ring + 1);
                spread = Math.min(spread, count - index);
                angle += random.nextDouble() * Math.PI * 2.0D;
            }
        }
        return out;
    }

    private static ChunkPos relocateToPreferredBiome(
            ChunkGeneratorStructureState state,
            BiomeSource biomeSource,
            HolderSet<Biome> preferred,
            RandomSource random,
            int chunkX,
            int chunkZ) {

        int centerX = SectionPos.sectionToBlockCoord(chunkX, 8);
        int centerZ = SectionPos.sectionToBlockCoord(chunkZ, 8);

        // 1.12 BiomeProviderSingle had a specialized two-nextInt implementation instead of
        // the ordinary reservoir scan. Age single-biome sources expose one possible biome,
        // so preserve that RNG consumption where the single biome is stronghold-compatible.
        if (biomeSource.possibleBiomes().size() == 1) {
            Holder<Biome> only = biomeSource.possibleBiomes().iterator().next();
            if (!preferred.contains(only)) return null;
            int blockX = centerX - BIOME_SEARCH_RADIUS
                    + random.nextInt(BIOME_SEARCH_RADIUS * 2 + 1);
            int blockZ = centerZ - BIOME_SEARCH_RADIUS
                    + random.nextInt(BIOME_SEARCH_RADIUS * 2 + 1);
            return new ChunkPos(
                    SectionPos.blockToSectionCoord(blockX),
                    SectionPos.blockToSectionCoord(blockZ));
        }

        // BiomeSource#findBiomeHorizontal uses the same 4-block raster/reservoir shape as the
        // old BiomeProvider#findBiomePosition. Passing the same LegacyRandomSource object (not a
        // fork) keeps every successful candidate's nextInt() in the main stronghold stream.
        Pair<BlockPos, Holder<Biome>> pair = biomeSource.findBiomeHorizontal(
                centerX,
                0,
                centerZ,
                BIOME_SEARCH_RADIUS,
                preferred::contains,
                random,
                state.randomState().sampler());
        if (pair == null) return null;
        BlockPos pos = pair.getFirst();
        return new ChunkPos(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()));
    }
}
