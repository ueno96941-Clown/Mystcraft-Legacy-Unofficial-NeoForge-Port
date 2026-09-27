package com.xcompwiz.mystcraft.world.dimension;

import com.xcompwiz.mystcraft.world.worldgen.AgeFeatureKind;
import com.xcompwiz.mystcraft.api.event.DenseOresEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.material.FluidState;

/** Runtime-only legacy Mystcraft population feature with per-Age material binding. */
public final class AgeLegacyPopulationFeature extends Feature<NoneFeatureConfiguration> {
    private final AgeFeatureKind kind;
    private final BlockState material;
    private final int densePages;
    private static final EntityType<?>[] LEGACY_DUNGEON_MOBS = {
            EntityType.SKELETON, EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.SPIDER
    };

    public AgeLegacyPopulationFeature(AgeFeatureKind kind, String blockId) { this(kind, blockId, 1); }

    public AgeLegacyPopulationFeature(AgeFeatureKind kind, String blockId, int densePages) {
        super(NoneFeatureConfiguration.CODEC);
        this.kind = kind;
        this.densePages = kind == AgeFeatureKind.DENSE_ORES ? Math.max(0, densePages) : 0;
        ResourceLocation id = ResourceLocation.parse(blockId);
        var block = BuiltInRegistries.BLOCK.get(id);
        this.material = (block == null ? Blocks.AIR : block).defaultBlockState();
    }

    AgeFeatureKind kind() {
        return kind;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int chunkX = Math.floorDiv(origin.getX(), 16) * 16;
        int chunkZ = Math.floorDiv(origin.getZ(), 16) * 16;

        return switch (kind) {
            case SPIKES -> placeSpikes(level, random, chunkX, chunkZ);
            case OBELISKS -> placeObelisk(level, random, chunkX, chunkZ);
            case CRYSTAL_FORMATION -> placeCrystal(level, random, chunkX, chunkZ);
            case DUNGEONS -> placeDungeons(context, chunkX, chunkZ);
            case DENSE_ORES -> placeDenseOres(level, random, chunkX, chunkZ);
            case DEEP_LAKES -> placeDeepLake(level, random, chunkX, chunkZ);
            case SURFACE_LAKES -> placeSurfaceLake(level, random, chunkX, chunkZ);
            default -> false;
        };
    }

    /**
     * Direct Mystcraft 0.13.7.06 / Minecraft 1.12 WorldGenDungeons population port.
     * SymbolDungeons performs exactly eight room-generation attempts per populated chunk.
     */
    private boolean placeDungeons(FeaturePlaceContext<NoneFeatureConfiguration> parent, int chunkX, int chunkZ) {
        RandomSource random = parent.random();
        boolean placed = false;
        for (int attempt = 0; attempt < 8; ++attempt) {
            int x = chunkX + random.nextInt(16) + 8;
            int minY = parent.level().getMinBuildHeight() + 1;
            int maxY = parent.level().getMaxBuildHeight() - 5;
            int y = minY + random.nextInt(Math.max(1, maxY - minY));
            int z = chunkZ + random.nextInt(16) + 8;
            placed |= generateLegacyDungeon(parent.level(), random, new BlockPos(x, y, z));
        }
        return placed;
    }

    private boolean generateLegacyDungeon(WorldGenLevel level, RandomSource random, BlockPos position) {
        int radiusX = random.nextInt(2) + 2;
        int minX = -radiusX - 1;
        int maxX = radiusX + 1;
        int radiusZ = random.nextInt(2) + 2;
        int minZ = -radiusZ - 1;
        int maxZ = radiusZ + 1;
        int openings = 0;

        for (int dx = minX; dx <= maxX; ++dx) {
            for (int dy = -1; dy <= 4; ++dy) {
                for (int dz = minZ; dz <= maxZ; ++dz) {
                    BlockPos at = position.offset(dx, dy, dz);
                    boolean solid = level.getBlockState(at).isSolid();
                    if (dy == -1 && !solid) return false;
                    if (dy == 4 && !solid) return false;
                    if ((dx == minX || dx == maxX || dz == minZ || dz == maxZ)
                            && dy == 0 && level.isEmptyBlock(at) && level.isEmptyBlock(at.above())) {
                        ++openings;
                    }
                }
            }
        }

        if (openings < 1 || openings > 5) return false;

        for (int dx = minX; dx <= maxX; ++dx) {
            for (int dy = 3; dy >= -1; --dy) {
                for (int dz = minZ; dz <= maxZ; ++dz) {
                    BlockPos at = position.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(at);
                    boolean boundary = dx == minX || dy == -1 || dz == minZ
                            || dx == maxX || dy == 4 || dz == maxZ;
                    if (!boundary) {
                        if (!state.is(Blocks.CHEST)) level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
                    } else if (at.getY() >= 0 && !level.getBlockState(at.below()).isSolid()) {
                        level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
                    } else if (state.isSolid() && !state.is(Blocks.CHEST)) {
                        level.setBlock(at,
                                dy == -1 && random.nextInt(4) != 0
                                        ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                                        : Blocks.COBBLESTONE.defaultBlockState(),
                                2);
                    }
                }
            }
        }

        for (int chest = 0; chest < 2; ++chest) {
            for (int tryIndex = 0; tryIndex < 3; ++tryIndex) {
                int x = position.getX() + random.nextInt(radiusX * 2 + 1) - radiusX;
                int z = position.getZ() + random.nextInt(radiusZ * 2 + 1) - radiusZ;
                BlockPos chestPos = new BlockPos(x, position.getY(), z);
                if (!level.isEmptyBlock(chestPos)) continue;

                int solidSides = 0;
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    if (level.getBlockState(chestPos.relative(direction)).isSolid()) ++solidSides;
                }
                if (solidSides == 1) {
                    level.setBlock(
                            chestPos,
                            StructurePiece.reorient(level, chestPos, Blocks.CHEST.defaultBlockState()),
                            2);
                    RandomizableContainer.setBlockEntityLootTable(
                            level, random, chestPos, BuiltInLootTables.SIMPLE_DUNGEON);
                    break;
                }
            }
        }

        level.setBlock(position, Blocks.SPAWNER.defaultBlockState(), 2);
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (blockEntity instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(LEGACY_DUNGEON_MOBS[random.nextInt(LEGACY_DUNGEON_MOBS.length)], random);
        }
        return true;
    }

    private boolean placeSpikes(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        // SymbolSpikes.Populator: !flag && rand.nextInt(18)==0. Village short-circuiting
        // is handled at the ordered batch level, so reaching this method implies flag=false.
        if (random.nextInt(18) != 0) return false;
        int x = chunkX + random.nextInt(16);
        int z = chunkZ + random.nextInt(16);
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        BlockPos base = new BlockPos(x, y, z);
        if (!level.isEmptyBlock(base)) return false;

        int height = random.nextInt(32) + 6;
        int width = random.nextInt(4) + 1;
        for (int px = x - width; px <= x + width; ++px) {
            for (int pz = z - width; pz <= z + width; ++pz) {
                int dx = px - x;
                int dz = pz - z;
                if (dx * dx + dz * dz <= width * width + 1
                        && level.isEmptyBlock(new BlockPos(px, y - 1, pz))) {
                    return false;
                }
            }
        }

        int maxY = level.getMaxBuildHeight();
        for (int px = x - width; px <= x + width; ++px) {
            for (int pz = z - width; pz <= z + width; ++pz) {
                int localMax = y + random.nextInt(random.nextInt(height) + 1) + 1;
                for (int py = y; py < localMax && py < maxY; ++py) {
                    int dx = px - x;
                    int dz = pz - z;
                    if (dx * dx + dz * dz <= width * width + 1) {
                        level.setBlock(new BlockPos(px, py, pz), material, 2);
                    }
                }
            }
        }
        return true;
    }

    private boolean placeObelisk(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(128) != 0) return false;
        int x = chunkX + random.nextInt(16);
        int z = chunkZ + random.nextInt(16);
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        if (y == 0) return false;

        int height = 12;
        int width = 4;
        int maxDeep = 5;
        BlockPos base = new BlockPos(x, y + 1, z);
        boolean foundBase = false;
        for (int py = base.getY(); py > base.getY() - maxDeep && !foundBase; --py) {
            foundBase = true;
            for (int dz = 0; dz < width; ++dz) {
                for (int dx = 0; dx < width; ++dx) {
                    BlockPos at = new BlockPos(base.getX() + dx, py, base.getZ() + dz);
                    level.setBlock(at, material, 2);
                    BlockState below = level.getBlockState(at.below());
                    FluidState fluid = below.getFluidState();
                    if (level.isEmptyBlock(at.below()) || below.is(Blocks.SNOW)
                            || (!fluid.isEmpty())) {
                        foundBase = false;
                    }
                }
            }
        }

        base = base.offset(1, 0, 1);
        for (int dy = 0; dy < height; ++dy) {
            for (int dz = 0; dz < 2; ++dz) {
                for (int dx = 0; dx < 2; ++dx) {
                    level.setBlock(base.offset(dx, dy, dz), material, 2);
                }
            }
        }
        return true;
    }

    private boolean placeCrystal(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(15) != 0) return false;
        int x = chunkX + random.nextInt(16) + 8;
        int z = chunkZ + random.nextInt(16) + 8;
        BlockPos crystal = findCrystalBase(level, x, z);
        if (crystal == null) return false;

        int count = random.nextInt(3) + 1;
        for (int i = 0; i < count; ++i) generateCrystalLine(level, random, crystal);
        return true;
    }

    private BlockPos findCrystalBase(WorldGenLevel level, int x, int z) {
        int max = level.getMaxBuildHeight() - 1;
        int y = level.getMinBuildHeight();
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        while (!level.isEmptyBlock(pos)) {
            y++;
            if (y > max) return null;
            pos = new BlockPos(x, y, z);
            state = level.getBlockState(pos);
        }
        // Preserve the legacy second loop literally. It is normally a no-op because the
        // first loop exits on AIR, but keeping it documents/retains 0.13.7.06 semantics.
        // Legacy 0.13.7.06: descend while the block is non-liquid AND non-air.
        while (state.getFluidState().isEmpty() && !level.isEmptyBlock(pos)) {
            y++;
            if (y > max) return null;
            pos = new BlockPos(x, y, z);
            state = level.getBlockState(pos);
        }
        pos = pos.below(2);
        return level.isEmptyBlock(pos) ? null : pos;
    }

    private void generateCrystalLine(WorldGenLevel level, RandomSource random, BlockPos startPos) {
        float angle = random.nextFloat() * 140.0F + 15.0F;
        int length = random.nextInt(7) + 6;
        int[] start = {startPos.getX(), startPos.getY(), startPos.getZ()};
        int[] end = {
                startPos.getX() + (int) (length * Math.cos(angle * Math.PI / 180.0D)),
                startPos.getY() + (int) (length * Math.sin(angle * Math.PI / 180.0D)),
                startPos.getZ() + random.nextInt(7) - 3
        };
        placeCrystalLine(level, start, end);
    }

    private void placeCrystalLine(WorldGenLevel level, int[] start, int[] end) {
        final byte[] pairs = {2, 0, 0, 1, 2, 1};
        int[] delta = {0, 0, 0};
        int major = 0;
        for (byte axis = 0; axis < 3; ++axis) {
            delta[axis] = end[axis] - start[axis];
            if (Math.abs(delta[axis]) > Math.abs(delta[major])) major = axis;
        }
        if (delta[major] == 0) return;
        byte a = pairs[major];
        byte b = pairs[major + 3];
        byte step = (byte) (delta[major] > 0 ? 1 : -1);
        double da = (double) delta[a] / (double) delta[major];
        double db = (double) delta[b] / (double) delta[major];
        int[] p = {0, 0, 0};
        int stop = delta[major] + step;
        for (int k = 0; k != stop; k += step) {
            p[major] = (int) Math.floor(start[major] + k + 0.5D);
            p[a] = (int) Math.floor(start[a] + k * da + 0.5D);
            p[b] = (int) Math.floor(start[b] + k * db + 0.5D);
            drawCrystalSphere(level, new BlockPos(p[0], p[1], p[2]));
        }
    }

    private void drawCrystalSphere(WorldGenLevel level, BlockPos pos) {
        setCrystal(level, pos);
        setCrystal(level, pos.above());
        setCrystal(level, pos.below());
        setCrystal(level, pos.north());
        setCrystal(level, pos.south());
        setCrystal(level, pos.west());
        setCrystal(level, pos.east());
    }

    private void setCrystal(WorldGenLevel level, BlockPos pos) {
        // CP270: a long Legacy crystal can reach the second neighbouring chunk. Do not read
        // through WorldGenRegion#getBlockState when that chunk is outside the active cache.
        // Crystal lines originate above the terrain surface, so an unavailable target cannot
        // legitimately be bedrock; preserve the authored write and let the deferred-write
        // bridge replay it when the target chunk becomes legal.
        if (!worldgenReadable(level, pos)) {
            AgeLegacyDeferredBlockWrites.setBlock(level, pos, material, 3);
            return;
        }
        if (!level.getBlockState(pos).is(Blocks.BEDROCK)) {
            // Legacy crystal origins are chunk+8..23 and each line can extend another
            // 12 blocks plus the one-block sphere shell. Along X this can reach the
            // second neighboring chunk, beyond 1.21.1 FEATURES' write radius of 1.
            // Preserve the authored BlockPos exactly and defer only those illegal writes.
            AgeLegacyDeferredBlockWrites.setBlock(level, pos, material, 3);
        }
    }

    private boolean placeSurfaceLake(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        if (random.nextInt(4) != 0) return false;
        int x = chunkX + random.nextInt(16) + 8;
        int minY = level.getMinBuildHeight() + 8;
        int maxY = level.getMaxBuildHeight() - 8;
        int y = minY + random.nextInt(Math.max(1, maxY - minY));
        int z = chunkZ + random.nextInt(16) + 8;
        return generateLegacyLake(level, random, new BlockPos(x, y, z));
    }

    private boolean placeDeepLake(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        // 0.13.7.06 SymbolLakesDeep:
        //   rand.nextInt(8)==0; y = rand.nextInt(rand.nextInt(248)+8);
        //   generate when y < seaLevel, otherwise retain the legacy extra 1/10 chance.
        //
        // The previous 1.21.1 adapter incorrectly capped the nested Y sample at sea level.
        // That pushed almost every attempt into the new negative-Y floor and also deleted the
        // original above-sea branch.  Preserve the same nested distribution across the complete
        // modern build height instead; the actual Age sea level still decides the deep/1-in-10 gate.
        if (random.nextInt(8) != 0) return false;
        int x = chunkX + random.nextInt(16) + 8;
        int minY = level.getMinBuildHeight();
        int height = Math.max(9, level.getMaxBuildHeight() - minY);
        int inner = random.nextInt(Math.max(1, height - 8)) + 8;
        int y = minY + random.nextInt(inner);
        int z = chunkZ + random.nextInt(16) + 8;
        if (y >= level.getSeaLevel() && random.nextInt(10) != 0) return false;

        BlockPos start = new BlockPos(x, y, z);
        return generateLegacyLake(level, random, start);
    }

    /** Direct port of Mystcraft 0.13.7.06 WorldGenLakesAdv. */
    private boolean generateLegacyLake(WorldGenLevel level, RandomSource random, BlockPos start) {
        BlockPos pos = start.below(8).offset(0, 0, -8);
        int bottomGuard = level.getMinBuildHeight() + 5;
        while (pos.getY() > bottomGuard && level.isEmptyBlock(pos)) pos = pos.below();
        if (pos.getY() <= bottomGuard - 1) return false;
        pos = pos.below(4);

        // WorldGenLakesAdv reads the complete 16x16 footprint before it writes anything.
        // With Legacy's +8..23 origin that footprint can touch a second neighbour. Modern
        // WorldGenRegion must not be asked to read a chunk outside its finite cache.
        if (!worldgenReadableBox(level, pos, 15, 15)) return false;

        boolean[] mask = new boolean[2048];
        int blobs = random.nextInt(4) + 4;
        for (int n = 0; n < blobs; ++n) {
            double sx = random.nextDouble() * 6.0D + 3.0D;
            double sy = random.nextDouble() * 4.0D + 2.0D;
            double sz = random.nextDouble() * 6.0D + 3.0D;
            double cx = random.nextDouble() * (16.0D - sx - 2.0D) + 1.0D + sx / 2.0D;
            double cy = random.nextDouble() * (8.0D - sy - 4.0D) + 2.0D + sy / 2.0D;
            double cz = random.nextDouble() * (16.0D - sz - 2.0D) + 1.0D + sz / 2.0D;
            for (int x = 1; x < 15; ++x) {
                for (int z = 1; z < 15; ++z) {
                    for (int y = 1; y < 7; ++y) {
                        double dx = (x - cx) / (sx / 2.0D);
                        double dy = (y - cy) / (sy / 2.0D);
                        double dz = (z - cz) / (sz / 2.0D);
                        if (dx * dx + dy * dy + dz * dz < 1.0D) {
                            mask[(x * 16 + z) * 8 + y] = true;
                        }
                    }
                }
            }
        }

        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = 0; y < 8; ++y) {
                    int idx = (x * 16 + z) * 8 + y;
                    boolean edge = !mask[idx] && (
                            x < 15 && mask[((x + 1) * 16 + z) * 8 + y]
                                    || x > 0 && mask[((x - 1) * 16 + z) * 8 + y]
                                    || z < 15 && mask[(x * 16 + z + 1) * 8 + y]
                                    || z > 0 && mask[(x * 16 + z - 1) * 8 + y]
                                    || y < 7 && mask[(x * 16 + z) * 8 + y + 1]
                                    || y > 0 && mask[(x * 16 + z) * 8 + y - 1]);
                    if (!edge) continue;
                    BlockPos at = pos.offset(x, y, z);
                    BlockState state = level.getBlockState(at);
                    boolean liquid = !state.getFluidState().isEmpty();
                    if (y >= 4 && liquid) return false;
                    if (y < 4 && !state.isSolid() && !state.equals(material)) return false;
                }
            }
        }

        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = 0; y < 8; ++y) {
                    if (!mask[(x * 16 + z) * 8 + y]) continue;
                    AgeLegacyDeferredBlockWrites.setBlock(
                            level, pos.offset(x, y, z),
                            y >= 4 ? Blocks.AIR.defaultBlockState() : material, 2);
                }
            }
        }
        return true;
    }

    private boolean placeDenseOres(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        if (densePages <= 0) return false;

        // Snapshot the base/mod ore population and all currently replaceable hosts once.
        // CP291: the first two pages retain measured legacy 2x/3x behavior; the third
        // page is the explicit 5x forbidden tier. MOD ores share the same classifier.
        java.util.Map<Integer, java.util.ArrayList<OreSample>> oresByY = new java.util.HashMap<>();
        java.util.Map<Integer, java.util.ArrayList<BlockPos>> hostsByY = new java.util.HashMap<>();
        snapshotDenseOreLayerData(level, chunkX, chunkZ, oresByY, hostsByY);

        boolean placed = false;
        for (var entry : oresByY.entrySet()) {
            int y = entry.getKey();
            java.util.ArrayList<OreSample> ores = entry.getValue();
            java.util.ArrayList<BlockPos> hosts = hostsByY.get(y);
            if (hosts == null || hosts.isEmpty()) continue;

            int additions = DenseOresPolicy.extraBlocksForLayer(ores.size(), hosts.size(), densePages);
            if (additions <= 0) continue;

            // Partial Fisher-Yates: choose host blocks without replacement, then copy a
            // baseline ore from the exact same Y. This keeps the observed vertical
            // distribution and ore mix while making extreme stacks saturate deterministically.
            for (int i = 0; i < additions; ++i) {
                int pick = i + random.nextInt(hosts.size() - i);
                BlockPos chosen = hosts.get(pick);
                hosts.set(pick, hosts.get(i));
                hosts.set(i, chosen);
                OreSample sample = ores.get(random.nextInt(ores.size()));
                level.setBlock(chosen, sample.state(), 2);
                placed = true;
            }
        }

        // Compatibility signal: Legacy posted once per Dense Ores populator. Post once per
        // effective authored page; CP291 caps authored effect at three pages.
        ServerLevel serverLevel = level instanceof WorldGenRegion region ? region.getLevel()
                : level instanceof ServerLevel direct ? direct : null;
        if (serverLevel != null) {
            for (int page = 0; page < densePages; ++page) {
                NeoForge.EVENT_BUS.post(new DenseOresEvent(
                        serverLevel, new RandomSourceBridge(random), chunkX, chunkZ));
            }
        }
        return placed;
    }

    private static boolean worldgenReadable(WorldGenLevel level, BlockPos pos) {
        if (!(level instanceof WorldGenRegion region)) return true;
        return region.hasChunk(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16));
    }

    private static boolean worldgenReadableBox(WorldGenLevel level, BlockPos min, int sizeX, int sizeZ) {
        if (!(level instanceof WorldGenRegion region)) return true;
        int minChunkX = Math.floorDiv(min.getX(), 16);
        int maxChunkX = Math.floorDiv(min.getX() + sizeX, 16);
        int minChunkZ = Math.floorDiv(min.getZ(), 16);
        int maxChunkZ = Math.floorDiv(min.getZ() + sizeZ, 16);
        for (int cx = minChunkX; cx <= maxChunkX; ++cx) {
            for (int cz = minChunkZ; cz <= maxChunkZ; ++cz) {
                if (!region.hasChunk(cx, cz)) return false;
            }
        }
        return true;
    }

    private record OreSample(BlockState state, int y) {}

    private void snapshotDenseOreLayerData(
            WorldGenLevel level, int chunkX, int chunkZ,
            java.util.Map<Integer, java.util.ArrayList<OreSample>> oresByY,
            java.util.Map<Integer, java.util.ArrayList<BlockPos>> hostsByY) {
        for (int x = chunkX; x < chunkX + 16; ++x) {
            for (int z = chunkZ; z < chunkZ + 16; ++z) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); ++y) {
                    BlockPos at = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(at);
                    if (DenseOreResourceClassifier.isOre(state)) {
                        oresByY.computeIfAbsent(y, ignored -> new java.util.ArrayList<>())
                                .add(new OreSample(state, y));
                    } else if (genericOreHost(state)) {
                        hostsByY.computeIfAbsent(y, ignored -> new java.util.ArrayList<>()).add(at);
                    }
                }
            }
        }
    }

    /** java.util.Random compatibility facade backed by the exact worldgen RandomSource stream. */
    private static final class RandomSourceBridge extends java.util.Random {
        private final RandomSource source;
        RandomSourceBridge(RandomSource source) { this.source = source; }
        @Override protected int next(int bits) { return source.nextInt() >>> (32 - bits); }
        @Override public void setSeed(long seed) {
            // java.util.Random invokes setSeed from its constructor before source is assigned.
            if (source != null) source.setSeed(seed);
        }
    }

    private boolean genericOreHost(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.STONE || block == Blocks.DEEPSLATE || block == Blocks.GRANITE || block == Blocks.DIORITE
                || block == Blocks.ANDESITE || block == Blocks.TUFF || block == Blocks.NETHERRACK || block == Blocks.END_STONE;
    }

    private boolean generateOrePass(WorldGenLevel level, RandomSource random, BlockState ore,
                                    int veinSize, int times, int chunkX, int chunkZ,
                                    int minY, int maxY, boolean netherrackOnly) {
        boolean placed = false;
        for (int n = 0; n < times; ++n) {
            int x = chunkX + random.nextInt(16);
            int y = random.nextInt(maxY - minY) + minY;
            int z = chunkZ + random.nextInt(16);
            placed |= generateLegacyOreVein(level, random, new BlockPos(x, y, z), ore, veinSize, netherrackOnly);
        }
        return placed;
    }

    /** 1.12 WorldGenMinable geometry with the legacy target predicates. */
    private boolean generateLegacyOreVein(WorldGenLevel level, RandomSource random, BlockPos pos,
                                          BlockState ore, int size, boolean netherrackOnly) {
        float angle = random.nextFloat() * (float) Math.PI;
        double x1 = pos.getX() + 8 + Math.sin(angle) * size / 8.0F;
        double x2 = pos.getX() + 8 - Math.sin(angle) * size / 8.0F;
        double z1 = pos.getZ() + 8 + Math.cos(angle) * size / 8.0F;
        double z2 = pos.getZ() + 8 - Math.cos(angle) * size / 8.0F;
        double y1 = pos.getY() + random.nextInt(3) - 2;
        double y2 = pos.getY() + random.nextInt(3) - 2;
        boolean placed = false;

        for (int step = 0; step < size; ++step) {
            float t = (float) step / (float) size;
            double cx = x1 + (x2 - x1) * t;
            double cy = y1 + (y2 - y1) * t;
            double cz = z1 + (z2 - z1) * t;
            double fuzz = random.nextDouble() * size / 16.0D;
            double rx = (Math.sin(Math.PI * t) + 1.0D) * fuzz + 1.0D;
            double ry = (Math.sin(Math.PI * t) + 1.0D) * fuzz + 1.0D;
            int minX = (int) Math.floor(cx - rx / 2.0D);
            int minY = (int) Math.floor(cy - ry / 2.0D);
            int minZ = (int) Math.floor(cz - rx / 2.0D);
            int maxX = (int) Math.floor(cx + rx / 2.0D);
            int maxY = (int) Math.floor(cy + ry / 2.0D);
            int maxZ = (int) Math.floor(cz + rx / 2.0D);

            for (int x = minX; x <= maxX; ++x) {
                double nx = (x + 0.5D - cx) / (rx / 2.0D);
                if (nx * nx >= 1.0D) continue;
                for (int y = minY; y <= maxY; ++y) {
                    if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) continue;
                    double ny = (y + 0.5D - cy) / (ry / 2.0D);
                    if (nx * nx + ny * ny >= 1.0D) continue;
                    for (int z = minZ; z <= maxZ; ++z) {
                        double nz = (z + 0.5D - cz) / (rx / 2.0D);
                        if (nx * nx + ny * ny + nz * nz >= 1.0D) continue;
                        BlockPos at = new BlockPos(x, y, z);
                        BlockState old = level.getBlockState(at);
                        if (!legacyOreTarget(old, netherrackOnly)) continue;
                        level.setBlock(at, ore, 2);
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }

    private boolean legacyOreTarget(BlockState state, boolean netherrackOnly) {
        if (netherrackOnly) return state.is(Blocks.NETHERRACK);
        // In 1.12 granite/diorite/andesite were metadata variants of Blocks.STONE,
        // so BlockMatcher.forBlock(Blocks.STONE) matched all four modern blocks.
        Block block = state.getBlock();
        return block == Blocks.STONE || block == Blocks.GRANITE
                || block == Blocks.DIORITE || block == Blocks.ANDESITE;
    }

}
