package com.xcompwiz.mystcraft.instability;

import com.xcompwiz.mystcraft.block.BlockDecay;
import com.xcompwiz.mystcraft.registry.MystBlocks;
import com.xcompwiz.mystcraft.world.dimension.ModernAgeHeight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/** Minecraft-facing restoration of the active Red/Blue/Purple/White Decay handlers. */
public final class LegacyInstabilityDecayRuntime {
    public static final int PLACEMENT_CHANCE_DENOMINATOR = 1_000_000;
    private LegacyInstabilityDecayRuntime() {}

    public static BlockDecay.DecayType typeForProvider(String providerId) {
        return switch (providerId) {
            case "decayred" -> BlockDecay.DecayType.RED;
            case "decayblue" -> BlockDecay.DecayType.BLUE;
            case "decaypurple" -> BlockDecay.DecayType.PURPLE;
            case "decaywhite" -> BlockDecay.DecayType.WHITE;
            default -> null;
        };
    }

    public static int minLegacyY(BlockDecay.DecayType type) {
        return type == BlockDecay.DecayType.WHITE ? 20 : 25;
    }

    public static Integer maxLegacyY(BlockDecay.DecayType type) {
        return type == BlockDecay.DecayType.PURPLE ? 54 : null;
    }

    /** Provider level N registers N EffectDecayBasic instances. */
    public static boolean tickPlacement(ServerLevel level, LevelChunk chunk, String providerId,
                                        int providerLevel, int effectIndex, int controllerScore) {
        BlockDecay.DecayType type = typeForProvider(providerId);
        if (type == null || effectIndex < 0 || effectIndex >= providerLevel) return false;
        if (LegacyInstabilityRuntimeRandom.nextInt(level, PLACEMENT_CHANCE_DENOMINATOR) >= controllerScore) return false;

        int lcg = LegacyInstabilityDecayRuntimeState.advance(level, providerId, effectIndex, "place");
        int coords = lcg >> 2;
        int x = chunk.getPos().getMinBlockX() + (coords & 15);
        int z = chunk.getPos().getMinBlockZ() + ((coords >> 8) & 15);
        int legacyRawY = (coords >> 16) & 255;
        int minLegacy = minLegacyY(type);
        Integer fixedMaxLegacy = maxLegacyY(type);
        int maxLegacy = fixedMaxLegacy != null ? fixedMaxLegacy : modernSurfaceToLegacy(level, x, z);
        if (maxLegacy <= minLegacy && fixedMaxLegacy == null) maxLegacy = modernYToLegacy(level, level.getSeaLevel());
        if (maxLegacy < minLegacy) return false;

        int chosenLegacy = maxLegacy == minLegacy
                ? minLegacy
                : Math.floorMod(legacyRawY, maxLegacy - minLegacy) + minLegacy;
        int y = ModernAgeHeight.legacySampleY(level, chosenLegacy);
        BlockPos pos = new BlockPos(x, y, z);
        LegacyInstabilityBlockMutationBatch.queue(
                level, pos, MystBlocks.DECAY.get().defaultBlockState().setValue(BlockDecay.DECAY, type));
        return true;
    }

    /**
     * Original EffectExtraTicks created one instance per provider level, plus one extra instance for White.
     * Each instance sampled 3 blocks in each of the old 16 vertical storage sections per chunk tick.
     */
    public static int tickExtra(ServerLevel level, LevelChunk chunk, String providerId,
                                int providerLevel, int extraEffectIndex) {
        BlockDecay.DecayType type = typeForProvider(providerId);
        if (type == null) return 0;
        int extraInstances = providerLevel + (type == BlockDecay.DecayType.WHITE ? 1 : 0);
        if (extraEffectIndex < 0 || extraEffectIndex >= extraInstances) return 0;

        int pulses = 0;
        SectionTickLookup sectionTicks = SectionTickLookup.capture(level, chunk);
        for (int slot = 0; slot < LegacyInstabilityPotionRuntimeMath.LEGACY_VERTICAL_SLOTS; slot++) {
            var band = LegacyInstabilityPotionRuntimeMath.band(level.getMinBuildHeight(), level.getHeight(), slot);
            int bandHeight = Math.max(1, band.maxExclusiveY() - band.minY());
            for (int attempt = 0; attempt < 3; attempt++) {
                int lcg = LegacyInstabilityDecayRuntimeState.advance(level, providerId, extraEffectIndex, "extra");
                int bits = lcg >> 2;
                int x = chunk.getPos().getMinBlockX() + (bits & 15);
                int z = chunk.getPos().getMinBlockZ() + ((bits >> 8) & 15);
                int localY = (bits >> 16) & 15;
                int y = band.minY() + Math.min(bandHeight - 1,
                        Math.floorDiv(localY * bandHeight, 16));

                // Exact same gate as before, but avoid linearly scanning every modern section for
                // each of the 48 legacy samples. The original EffectExtraTicks already had direct
                // access to the selected ExtendedBlockStorage and paid only O(1) for this check.
                if (!sectionTicks.needsRandomTicks(y)) continue;

                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(pos);
                if (state.is(MystBlocks.DECAY.get()) && state.getValue(BlockDecay.DECAY) == type) {
                    pulseBatched(level, pos, type, level.getRandom());
                    pulses++;
                }
            }
        }
        return pulses;
    }


    private record SectionTickLookup(int firstBaseY, boolean[] randomTicking) {
        private static SectionTickLookup capture(ServerLevel level, LevelChunk chunk) {
            LevelChunkSection[] sections = chunk.getSections();
            boolean[] flags = new boolean[sections.length];
            int firstBaseY = sections.length == 0 ? level.getMinBuildHeight()
                    : level.getSectionYFromSectionIndex(0) << 4;
            for (int i = 0; i < sections.length; i++) {
                LevelChunkSection section = sections[i];
                flags[i] = section != null && section.isRandomlyTickingBlocks();
            }
            return new SectionTickLookup(firstBaseY, flags);
        }

        private boolean needsRandomTicks(int y) {
            int sectionIndex = Math.floorDiv(y - firstBaseY, 16);
            return sectionIndex >= 0 && sectionIndex < randomTicking.length && randomTicking[sectionIndex];
        }
    }

    /** Immediate path used by the actual Decay block random tick. */
    public static void pulse(ServerLevel level, BlockPos origin, BlockDecay.DecayType type, RandomSource random) {
        BlockState originState = level.getBlockState(origin);
        boolean manualPropagation = originState.is(MystBlocks.DECAY.get())
                && originState.hasProperty(BlockDecay.MANUAL)
                && originState.getValue(BlockDecay.MANUAL);
        for (Direction face : Direction.values()) {
            spreadImmediate(level, origin.relative(face), type, random, manualPropagation);
        }
    }

    /** CP324 buffered path used only by restored EffectExtraTicks during the level Post tick. */
    private static void pulseBatched(ServerLevel level, BlockPos origin, BlockDecay.DecayType type, RandomSource random) {
        BlockState originState = LegacyInstabilityBlockMutationBatch.effectiveState(level, origin);
        boolean manualPropagation = originState.is(MystBlocks.DECAY.get())
                && originState.hasProperty(BlockDecay.MANUAL)
                && originState.getValue(BlockDecay.MANUAL);
        for (Direction face : Direction.values()) {
            spreadBatched(level, origin.relative(face), type, random, manualPropagation);
        }
    }

    private static void spreadImmediate(ServerLevel level, BlockPos pos, BlockDecay.DecayType type, RandomSource random, boolean manualPropagation) {
        BlockState state = level.getBlockState(pos);
        if (state.is(MystBlocks.DECAY.get()) && state.getValue(BlockDecay.DECAY) == type) return;
        int difficulty = conversionDifficulty(level, pos, state, type);
        if (difficulty <= 1 || random.nextInt(difficulty) == 0) {
            BlockState replacement = MystBlocks.DECAY.get().defaultBlockState()
                    .setValue(BlockDecay.DECAY, type)
                    .setValue(BlockDecay.MANUAL, manualPropagation);
            level.setBlock(pos, replacement, Block.UPDATE_ALL);
        }
    }

    private static void spreadBatched(ServerLevel level, BlockPos pos, BlockDecay.DecayType type, RandomSource random, boolean manualPropagation) {
        BlockState state = LegacyInstabilityBlockMutationBatch.effectiveState(level, pos);
        if (state.is(MystBlocks.DECAY.get()) && state.getValue(BlockDecay.DECAY) == type) return;
        int difficulty = conversionDifficulty(level, pos, state, type);
        if (difficulty <= 1 || random.nextInt(difficulty) == 0) {
            BlockState replacement = MystBlocks.DECAY.get().defaultBlockState()
                    .setValue(BlockDecay.DECAY, type)
                    .setValue(BlockDecay.MANUAL, manualPropagation);
            LegacyInstabilityBlockMutationBatch.queue(level, pos, replacement);
        }
    }

    static int conversionDifficulty(ServerLevel level, BlockPos pos, BlockState state, BlockDecay.DecayType type) {
        // CP303: match the actual 0.13.7.06 JAR behavior, including two long-standing
        // IBlockState-vs-Block identity checks in Red/Purple/White which are always false
        // at runtime. Those quirks are player-visible: Red eats air much faster, Purple does
        // not get the intended generic Decay shortcut, and White converts every neighbour
        // deterministically once ticked.
        return switch (type) {
            case RED -> {
                float resistance = clampLegacy(legacyExplosionResistance(state));
                yield Math.max(1, (int) resistance);
            }
            case BLUE -> {
                if (state.isAir()) yield 20;
                float hardness = clampLegacy(legacyHardness(level, pos, state));
                yield Math.max(1, ((int) hardness) * 2);
            }
            case PURPLE -> {
                if (!state.getFluidState().isEmpty()) yield 3;
                float resistance = clampLegacy(legacyExplosionResistance(state));
                float hardness = clampLegacy(legacyHardness(level, pos, state)) * 2.0F;
                yield Math.max(1, (int) (hardness + resistance)) * 10;
            }
            case WHITE -> 1;
            default -> 1000;
        };
    }

    private static float legacyExplosionResistance(BlockState state) {
        if (state.is(MystBlocks.DECAY.get())) {
            return switch (state.getValue(BlockDecay.DECAY)) {
                case RED -> 10.0F;
                case BLUE -> 2.0F;
                case PURPLE, WHITE -> 100.0F;
            };
        }
        return state.getBlock().getExplosionResistance();
    }

    private static float legacyHardness(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(MystBlocks.DECAY.get())) {
            return switch (state.getValue(BlockDecay.DECAY)) {
                case RED -> 1.0F;
                case BLUE -> 5.0F;
                case PURPLE, WHITE -> 50.0F;
            };
        }
        return state.getDestroySpeed(level, pos);
    }

    private static float clampLegacy(float value) {
        if (value < 0.0F) return 1000.0F;
        return Math.min(1000.0F, value);
    }

    public static void whiteDecayContact(Level level, Entity entity) {
        if (level instanceof ServerLevel server && entity != null && !entity.isRemoved()) {
            boolean damaged = entity.hurt(server.damageSources().magic(), 1.0F);
            if (damaged) {
                if (entity instanceof net.minecraft.server.level.ServerPlayer) {
                }
            }
        }
    }

    private static int modernSurfaceToLegacy(ServerLevel level, int x, int z) {
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
        return modernYToLegacy(level, y);
    }

    private static int modernYToLegacy(ServerLevel level, int y) {
        int relative = Math.max(0, Math.min(level.getHeight() - 1, y - level.getMinBuildHeight()));
        return Math.max(0, Math.min(255, Math.floorDiv(relative * 256, Math.max(1, level.getHeight()))));
    }
}
