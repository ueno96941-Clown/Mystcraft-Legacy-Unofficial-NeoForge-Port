package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Random;

/** Literal compatibility port of Mystcraft 0.13.7.06 MapGenRavineMyst. */
public final class LegacyMapGenRavineKernel extends LegacyMapGenAdvancedKernel {
    private final float[] field75046d = new float[1024];

    public LegacyMapGenRavineKernel(long seed) {
        super(seed, false);
    }

    private void generateRavine(
            long seed,
            int chunkX,
            int chunkZ,
            LegacyTerrainBuffer buffer,
            double baseX,
            double baseY,
            double baseZ,
            float par12,
            float par13,
            float par14,
            int par15,
            int par16,
            double par17) {

        Random random = new Random(seed);
        double chunkXmid = chunkX * 16 + 8;
        double chunkZmid = chunkZ * 16 + 8;
        int layers = 256;
        float var24 = 0.0F;
        float var25 = 0.0F;

        if (par16 <= 0) {
            int var26 = range * 16 - 16;
            par16 = var26 - random.nextInt(var26 / 4);
        }

        boolean flag1 = false;
        if (par15 == -1) {
            par15 = par16 / 2;
            flag1 = true;
        }

        float var27 = 1.0F;
        for (int var28 = 0; var28 < 128; ++var28) {
            if (var28 == 0 || random.nextInt(3) == 0) {
                var27 = 1.0F + random.nextFloat() * random.nextFloat();
            }
            field75046d[var28] = var27 * var27;
        }

        for (; par15 < par16; ++par15) {
            double var53 = 1.5D + LegacyMathHelper.sin(
                    par15 * (float) Math.PI / par16) * par12;
            double var30 = var53 * par17;
            var53 *= random.nextFloat() * 0.25D + 0.75D;
            var30 *= random.nextFloat() * 0.25D + 0.75D;
            float var32 = LegacyMathHelper.cos(par14);
            float var33 = LegacyMathHelper.sin(par14);
            baseX += LegacyMathHelper.cos(par13) * var32;
            baseY += var33;
            baseZ += LegacyMathHelper.sin(par13) * var32;
            par14 *= 0.7F;
            par14 += var25 * 0.05F;
            par13 += var24 * 0.05F;
            var25 *= 0.8F;
            var24 *= 0.5F;
            var25 += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            var24 += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;

            if (flag1 || random.nextInt(4) != 0) {
                double var34 = baseX - chunkXmid;
                double var36 = baseZ - chunkZmid;
                double var38 = par16 - par15;
                double var40 = par12 + 2.0F + 16.0F;

                if (var34 * var34 + var36 * var36 - var38 * var38 > var40 * var40) {
                    return;
                }

                if (baseX >= chunkXmid - 16.0D - var53 * 2.0D
                        && baseZ >= chunkZmid - 16.0D - var53 * 2.0D
                        && baseX <= chunkXmid + 16.0D + var53 * 2.0D
                        && baseZ <= chunkZmid + 16.0D + var53 * 2.0D) {

                    int minX = LegacyMathHelper.floor(baseX - var53) - chunkX * 16 - 1;
                    int maxX = LegacyMathHelper.floor(baseX + var53) - chunkX * 16 + 1;
                    int minY = LegacyMathHelper.floor(baseY - var30) - 1;
                    int maxY = LegacyMathHelper.floor(baseY + var30) + 1;
                    int minZ = LegacyMathHelper.floor(baseZ - var53) - chunkZ * 16 - 1;
                    int maxZ = LegacyMathHelper.floor(baseZ + var53) - chunkZ * 16 + 1;

                    if (minX < 0) minX = 0;
                    if (maxX > 16) maxX = 16;
                    if (minY < 1) minY = 1;
                    if (maxY > layers) maxY = layers;
                    if (minZ < 0) minZ = 0;
                    if (maxZ > 16) maxZ = 16;

                    boolean foundWater = false;
                    for (int localY = maxY + 1; !foundWater && localY >= minY - 1; --localY) {
                        for (int localZ = minZ; !foundWater && localZ < maxZ; ++localZ) {
                            for (int localX = minX; !foundWater && localX < maxX; ++localX) {
                                if (localY >= 0 && localY < layers) {
                                    if (buffer.isWater(localX, localY, localZ)) {
                                        foundWater = true;
                                    }
                                    if (localY != minY - 1
                                            && localX != minX
                                            && localX != maxX - 1
                                            && localZ != minZ
                                            && localZ != maxZ - 1) {
                                        localY = minY;
                                    }
                                }
                            }
                        }
                    }

                    if (!foundWater) {
                        // Preserve the original 0..minY loop literally.
                        for (int localY = 0; localY < minY; ++localY) {
                            double yfactor = (localY + 0.5D - baseY) / var30;
                            double yfactorSq = yfactor * yfactor;
                            for (int localZ = minZ; localZ < maxZ; ++localZ) {
                                double zfactor =
                                        ((localZ + chunkZ * 16) + 0.5D - baseZ) / var53;
                                double zfactorSq = zfactor * zfactor;
                                for (int localX = minX; localX < maxX; ++localX) {
                                    double xfactor =
                                            ((localX + chunkX * 16) + 0.5D - baseX) / var53;
                                    double xfactorSq = xfactor * xfactor;

                                    if (xfactorSq + zfactorSq < 1.0D
                                            && (xfactorSq + zfactorSq) * field75046d[localY]
                                                + yfactorSq / 6.0D < 1.0D) {
                                        placeBlock(buffer, localX, localY, localZ);
                                    }
                                }
                            }
                        }

                        if (flag1) break;
                    }
                }
            }
        }
    }

    @Override
    protected void recursiveGenerate(
            int sourceX, int sourceZ, int chunkX, int chunkZ, LegacyTerrainBuffer buffer) {

        if (rand.nextInt(50) == 0) {
            double x = sourceX * 16 + rand.nextInt(16);
            double y = rand.nextInt(rand.nextInt(40) + 8) + 20;
            double z = sourceZ * 16 + rand.nextInt(16);

            float angle = rand.nextFloat() * (float) Math.PI * 2.0F;
            float pitch = (rand.nextFloat() - 0.5F) * 2.0F / 8.0F;
            float size = (rand.nextFloat() * 2.0F + rand.nextFloat()) * 2.0F;

            generateRavine(rand.nextLong(), chunkX, chunkZ, buffer,
                    x, y, z, size, angle, pitch, 0, 0, 3.0D);
        }
    }
}
