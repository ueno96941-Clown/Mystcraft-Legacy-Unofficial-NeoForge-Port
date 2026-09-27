package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Random;

/**
 * Geometry portion of Mystcraft 0.13.7.06 MapGenFloatingIslands.
 *
 * <p>Exact legacy constants/seed behavior are retained. Surface/filler conversion and
 * biome-palette finalization consume the modified-column mask in the follow-up pass.</p>
 */
public final class LegacyMapGenFloatingIslandsKernel extends LegacyMapGenAdvancedKernel {
    private static final int RATE = 192;
    private final LegacyNoiseGeneratorOctaves2D noiseGen4;
    private double[] stoneNoise;

    public LegacyMapGenFloatingIslandsKernel(long seed) {
        super(seed, true);
        this.range = 5;
        // Deliberately constructed from the superclass Random before generate() reseeds it,
        // matching the original MapGenFloatingIslands constructor order.
        this.noiseGen4 = new LegacyNoiseGeneratorOctaves2D(rand, 4);
    }

    private void generateNode(
            long nodeSeed,
            int chunkX,
            int chunkZ,
            LegacyFloatingIslandBuffer buffer,
            double baseX,
            double baseY,
            double baseZ,
            float scalar,
            float angleB,
            float angleC,
            int loopc,
            int maxLoops,
            double squash) {

        double chunkXmid = chunkX * 16 + 8;
        double chunkZmid = chunkZ * 16 + 8;
        final int layers = 256;
        float f = 0.0F;
        float f1 = 0.0F;
        Random random = new Random(nodeSeed);

        if (maxLoops <= 0) {
            int i = range * 16 - 16;
            maxLoops = i - random.nextInt(i / 4);
        }

        boolean flag = false;
        if (loopc == -1) {
            loopc = maxLoops / 2;
            flag = true;
        }

        int j = random.nextInt(maxLoops / 2) + maxLoops / 4;
        boolean flag1 = random.nextInt(6) == 0;

        for (; loopc < maxLoops; ++loopc) {
            double d2 = 1.5D
                    + LegacyMathHelper.sin((loopc * (float) Math.PI) / maxLoops) * scalar;
            double d3 = d2 * squash;
            float f2 = LegacyMathHelper.cos(angleC);
            float f3 = LegacyMathHelper.sin(angleC);

            baseX += LegacyMathHelper.cos(angleB) * f2;
            baseY += f3;
            baseZ += LegacyMathHelper.sin(angleB) * f2;

            if (flag1) angleC *= 0.92F;
            else angleC *= 0.7F;

            angleC += f1 * 0.1F;
            angleB += f * 0.1F;
            f1 *= 0.9F;
            f *= 0.75F;
            // Floating islands deliberately use 1.0F here; normal caves use 2.0F.
            f1 += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 1.0F;
            f += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;

            if (!flag && loopc == j && scalar > 1.0F && maxLoops > 0) {
                generateNode(
                        random.nextLong(), chunkX, chunkZ, buffer,
                        baseX, baseY, baseZ,
                        random.nextFloat() * 0.5F + 0.5F,
                        angleB - ((float) Math.PI / 2F),
                        angleC / 3F, loopc, maxLoops, squash);
                generateNode(
                        random.nextLong(), chunkX, chunkZ, buffer,
                        baseX, baseY, baseZ,
                        random.nextFloat() * 0.5F + 0.5F,
                        angleB + ((float) Math.PI / 2F),
                        angleC / 3F, loopc, maxLoops, squash);
                return;
            }

            if (!flag && random.nextInt(4) == 0) continue;

            double xoffset = baseX - chunkXmid;
            double zoffset = baseZ - chunkZmid;
            double remaining = maxLoops - loopc;
            double d7 = scalar + 2.0F + 16.0F;

            if ((xoffset * xoffset + zoffset * zoffset)
                    - remaining * remaining > d7 * d7) {
                return;
            }

            if (baseX < chunkXmid - 16D - d2 * 2D
                    || baseZ < chunkZmid - 16D - d2 * 2D
                    || baseX > chunkXmid + 16D + d2 * 2D
                    || baseZ > chunkZmid + 16D + d2 * 2D) {
                continue;
            }

            int minX = LegacyMathHelper.floor(baseX - d2) - chunkX * 16 - 1;
            int maxX = LegacyMathHelper.floor(baseX + d2) - chunkX * 16 + 1;
            int minY = LegacyMathHelper.floor(baseY - d3) - 1;
            int maxY = LegacyMathHelper.floor(baseY + d3) + 1;
            int minZ = LegacyMathHelper.floor(baseZ - d2) - chunkZ * 16 - 1;
            int maxZ = LegacyMathHelper.floor(baseZ + d2) - chunkZ * 16 + 1;

            if (minX < 0) minX = 0;
            if (maxX > 16) maxX = 16;
            if (minY < 1) minY = 1;
            if (maxY > layers) maxY = layers;
            if (minZ < 0) minZ = 0;
            if (maxZ > 16) maxZ = 16;

            for (int localY = minY; localY < maxY; ++localY) {
                double yfactor = ((localY + 0.5D) - baseY) / d3;
                double yfactorSq = yfactor * yfactor;

                for (int localZ = minZ; localZ < maxZ; ++localZ) {
                    double zfactor =
                            (((localZ + chunkZ * 16) + 0.5D) - baseZ) / d2;
                    double zfactorSq = zfactor * zfactor;

                    for (int localX = minX; localX < maxX; ++localX) {
                        double xfactor =
                                (((localX + chunkX * 16) + 0.5D) - baseX) / d2;
                        double total = xfactor * xfactor + yfactorSq + zfactorSq;

                        if (total < 1.0D
                                && placeBlock(buffer, localX, localY, localZ)) {
                            buffer.markModifiedColumn(localX, localZ);
                        }
                    }
                }
            }

            if (flag) break;
        }
    }

    @Override
    protected void recursiveGenerate(
            int sourceChunkX,
            int sourceChunkZ,
            int targetChunkX,
            int targetChunkZ,
            LegacyTerrainBuffer genericBuffer) {

        if (!(genericBuffer instanceof LegacyFloatingIslandBuffer buffer)) {
            throw new IllegalArgumentException(
                    "Floating islands require LegacyFloatingIslandBuffer");
        }

        if (rand.nextInt(RATE) != 0) return;

        double dx = sourceChunkX * 16 + rand.nextInt(16);
        double dy = rand.nextInt(rand.nextInt(50) + 50) + 150;
        double dz = sourceChunkZ * 16 + rand.nextInt(16);

        generateNode(
                rand.nextLong(),
                targetChunkX,
                targetChunkZ,
                buffer,
                dx, dy, dz,
                12.0F,
                0.0F, 0.0F,
                -1, -1,
                0.2D);

        int subelements = rand.nextInt(12) + 40;
        for (int i = 0; i < subelements; ++i) {
            double subx = dx + (rand.nextDouble() - rand.nextDouble()) * 20.0D;
            double suby = dy + (rand.nextDouble() - rand.nextDouble()) * 10.0D;
            double subz = dz + (rand.nextDouble() - rand.nextDouble()) * 20.0D;
            float scale = rand.nextFloat() * 3.0F + 1.0F;

            generateNode(
                    rand.nextLong(),
                    targetChunkX,
                    targetChunkZ,
                    buffer,
                    subx, suby, subz,
                    scale,
                    0.0F, 0.0F,
                    -1, -1,
                    0.4D);
        }
    }

    @Override
    protected void afterRecursiveGenerate(
            int sourceChunkX,
            int sourceChunkZ,
            int targetChunkX,
            int targetChunkZ,
            LegacyTerrainBuffer genericBuffer) {

        if (!(genericBuffer instanceof LegacyFloatingIslandSurfaceBuffer buffer)) {
            return;
        }

        boolean any = false;
        boolean[] modified = buffer instanceof LegacyFloatingIslandMaskView view
                ? view.copyModifiedColumns()
                : null;
        if (modified != null) {
            for (boolean value : modified) {
                if (value) {
                    any = true;
                    break;
                }
            }
        }
        if (!any) return;

        replaceBlocksForBiome(targetChunkX, targetChunkZ, buffer, modified);
    }

    private void replaceBlocksForBiome(
            int chunkX,
            int chunkZ,
            LegacyFloatingIslandSurfaceBuffer buffer,
            boolean[] modified) {

        double noiseFactor = 0.03125D;
        stoneNoise = noiseGen4.generate(
                stoneNoise,
                chunkX * 16,
                chunkZ * 16,
                16,
                16,
                noiseFactor * 2.0D,
                noiseFactor * 2.0D);

        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                if (!modified[x + z * 16]) continue;

                int stoneNoiseVal =
                        (int) (stoneNoise[z + x * 16] / 3.0D
                                + 3.0D
                                + rand.nextDouble() * 0.25D);
                int counter = -1;
                boolean fillerIsSand = buffer.fillerIsSand();
                boolean usingSandstone = false;

                for (int y = 255; y >= 0; --y) {
                    if (!buffer.isStone(x, y, z)) {
                        continue;
                    }

                    if (counter == -1) {
                        counter = stoneNoiseVal;
                        buffer.setTop(x, y, z);
                        continue;
                    }

                    if (counter <= 0) {
                        continue;
                    }

                    --counter;
                    if (usingSandstone) {
                        buffer.setSandstone(x, y, z);
                    } else {
                        buffer.setFiller(x, y, z);
                    }

                    if (counter == 0 && fillerIsSand && !usingSandstone) {
                        counter = rand.nextInt(4);
                        usingSandstone = true;
                    }
                }
            }
        }
    }

}
