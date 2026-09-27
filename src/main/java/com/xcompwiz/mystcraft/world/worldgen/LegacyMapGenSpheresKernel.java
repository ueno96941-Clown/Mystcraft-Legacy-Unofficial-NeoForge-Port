package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Random;

/**
 * Literal compatibility port of Mystcraft 0.13.7.06 MapGenSpheresMyst.
 *
 * <p>The legacy class is named "Spheres" but its generator is intentionally the original
 * short cave-node blob algorithm: 5% activation per source chunk, one wandering ellipsoid,
 * solid replacement material.</p>
 */
public final class LegacyMapGenSpheresKernel extends LegacyMapGenAdvancedKernel {
    public LegacyMapGenSpheresKernel(long seed) {
        super(seed, true);
    }

    private void generateCaveNode(
            long nodeSeed,
            int chunkX,
            int chunkZ,
            LegacyTerrainBuffer buffer,
            double baseX,
            double baseY,
            double baseZ,
            float angleA,
            float angleB,
            float angleC,
            int loopc,
            int maxLoops,
            double squash) {

        double chunkXmid = chunkX * 16 + 8;
        double chunkZmid = chunkZ * 16 + 8;
        int layers = 256;
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
                    + LegacyMathHelper.sin((loopc * (float) Math.PI) / maxLoops)
                    * angleA;
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
            f1 += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            f += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;

            if (!flag && loopc == j && angleA > 1.0F && maxLoops > 0) {
                generateCaveNode(
                        random.nextLong(), chunkX, chunkZ, buffer,
                        baseX, baseY, baseZ,
                        random.nextFloat() * 0.5F + 0.5F,
                        angleB - ((float) Math.PI / 2F),
                        angleC / 3F,
                        loopc, maxLoops, 1.0D);
                generateCaveNode(
                        random.nextLong(), chunkX, chunkZ, buffer,
                        baseX, baseY, baseZ,
                        random.nextFloat() * 0.5F + 0.5F,
                        angleB + ((float) Math.PI / 2F),
                        angleC / 3F,
                        loopc, maxLoops, 1.0D);
                return;
            }

            if (!flag && random.nextInt(4) == 0) {
                continue;
            }

            double xoffset = baseX - chunkXmid;
            double zoffset = baseZ - chunkZmid;
            double remaining = maxLoops - loopc;
            double d7 = angleA + 2.0F + 16.0F;

            if ((xoffset * xoffset + zoffset * zoffset)
                    - remaining * remaining > d7 * d7) {
                return;
            }

            if (baseX < chunkXmid - 16.0D - d2 * 2.0D
                    || baseZ < chunkZmid - 16.0D - d2 * 2.0D
                    || baseX > chunkXmid + 16.0D + d2 * 2.0D
                    || baseZ > chunkZmid + 16.0D + d2 * 2.0D) {
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
                        double xfactorSq = xfactor * xfactor;

                        if (xfactorSq + zfactorSq < 1.0D) {
                            double total = xfactorSq + yfactorSq + zfactorSq;
                            if (total < 1.0D) {
                                placeBlock(buffer, localX, localY, localZ);
                            }
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
            LegacyTerrainBuffer buffer) {

        float roll = rand.nextFloat();
        if (roll > 0.05F) return;

        double x = sourceChunkX * 16 + rand.nextInt(16);
        double y = rand.nextInt(rand.nextInt(192) + 1) + 32;
        double z = sourceChunkZ * 16 + rand.nextInt(16);

        generateCaveNode(
                rand.nextLong(),
                targetChunkX,
                targetChunkZ,
                buffer,
                x, y, z,
                1.0F + rand.nextFloat() * 4.0F,
                0.0F, 0.0F,
                -1, -1,
                1.0D);
    }
}
