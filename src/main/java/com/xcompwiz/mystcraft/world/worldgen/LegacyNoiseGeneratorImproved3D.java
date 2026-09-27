package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Random;

/**
 * Legacy-compatible improved-gradient noise kernel used by Mystcraft terrain generation.
 *
 * <p>This compatibility boundary preserves the historical RNG consumption and numerical
 * output required by existing Mystcraft terrain kernels. It is isolated here so the rest of
 * the 1.21.1 port does not depend on obsolete Minecraft world-generator classes.</p>
 */
public final class LegacyNoiseGeneratorImproved3D {
    private final int[] permutations = new int[512];
    private final double xCoord;
    private final double yCoord;
    private final double zCoord;

    private static final double[] GRAD_X = {1,-1,1,-1,1,-1,1,-1,0,0,0,0,1,0,-1,0};
    private static final double[] GRAD_Y = {1,1,-1,-1,0,0,0,0,1,-1,1,-1,1,-1,1,-1};
    private static final double[] GRAD_Z = {0,0,0,0,1,1,-1,-1,1,1,-1,-1,0,1,0,-1};
    private static final double[] GRAD_2X = {1,-1,1,-1,1,-1,1,-1,0,0,0,0,1,0,-1,0};
    private static final double[] GRAD_2Z = {0,0,0,0,1,1,-1,-1,1,1,-1,-1,0,1,0,-1};

    public LegacyNoiseGeneratorImproved3D(Random random) {
        xCoord = random.nextDouble() * 256.0D;
        yCoord = random.nextDouble() * 256.0D;
        zCoord = random.nextDouble() * 256.0D;
        for (int i = 0; i < 256; permutations[i] = i++) { }
        for (int i = 0; i < 256; i++) {
            int swap = random.nextInt(256 - i) + i;
            int value = permutations[i];
            permutations[i] = permutations[swap];
            permutations[swap] = value;
            permutations[i + 256] = permutations[i];
        }
    }

    private static double lerp(double t, double a, double b) { return a + t * (b - a); }
    private static double fade(double v) { return v * v * v * (v * (v * 6.0D - 15.0D) + 10.0D); }
    private static int floor(double v) { int i = (int)v; return v < i ? i - 1 : i; }
    private static double grad2(int hash, double x, double z) {
        int i = hash & 15;
        return GRAD_2X[i] * x + GRAD_2Z[i] * z;
    }
    private static double grad(int hash, double x, double y, double z) {
        int i = hash & 15;
        return GRAD_X[i] * x + GRAD_Y[i] * y + GRAD_Z[i] * z;
    }

    public void populateNoiseArray(double[] out,
                                   double xOffset, double yOffset, double zOffset,
                                   int xSize, int ySize, int zSize,
                                   double xScale, double yScale, double zScale,
                                   double noiseScale) {
        if (ySize == 1) {
            int outIndex = 0;
            double inverseScale = 1.0D / noiseScale;
            for (int x = 0; x < xSize; x++) {
                double dx = xOffset + x * xScale + xCoord;
                int fx = floor(dx);
                int px = fx & 255;
                dx -= fx;
                double sx = fade(dx);
                for (int z = 0; z < zSize; z++) {
                    double dz = zOffset + z * zScale + zCoord;
                    int fz = floor(dz);
                    int pz = fz & 255;
                    dz -= fz;
                    double sz = fade(dz);
                    int a = permutations[px];
                    int aa = permutations[a] + pz;
                    int b = permutations[px + 1];
                    int ba = permutations[b] + pz;
                    double v0 = lerp(sx,
                            grad2(permutations[aa], dx, dz),
                            grad(permutations[ba], dx - 1.0D, 0.0D, dz));
                    double v1 = lerp(sx,
                            grad(permutations[aa + 1], dx, 0.0D, dz - 1.0D),
                            grad(permutations[ba + 1], dx - 1.0D, 0.0D, dz - 1.0D));
                    out[outIndex++] += lerp(sz, v0, v1) * inverseScale;
                }
            }
            return;
        }

        int outIndex = 0;
        double inverseScale = 1.0D / noiseScale;
        int previousYCell = -1;
        int a0 = 0, a1 = 0, b0 = 0, b1 = 0;
        double x00 = 0, x01 = 0, x10 = 0, x11 = 0;

        for (int x = 0; x < xSize; x++) {
            double dx = xOffset + x * xScale + xCoord;
            int fx = floor(dx);
            int px = fx & 255;
            dx -= fx;
            double sx = fade(dx);

            for (int z = 0; z < zSize; z++) {
                double dz = zOffset + z * zScale + zCoord;
                int fz = floor(dz);
                int pz = fz & 255;
                dz -= fz;
                double sz = fade(dz);

                for (int y = 0; y < ySize; y++) {
                    double dy = yOffset + y * yScale + yCoord;
                    int fy = floor(dy);
                    int py = fy & 255;
                    dy -= fy;
                    double sy = fade(dy);

                    if (y == 0 || py != previousYCell) {
                        previousYCell = py;
                        int pa = permutations[px] + py;
                        a0 = permutations[pa] + pz;
                        a1 = permutations[pa + 1] + pz;
                        int pb = permutations[px + 1] + py;
                        b0 = permutations[pb] + pz;
                        b1 = permutations[pb + 1] + pz;
                        x00 = lerp(sx, grad(permutations[a0], dx, dy, dz), grad(permutations[b0], dx - 1, dy, dz));
                        x01 = lerp(sx, grad(permutations[a1], dx, dy - 1, dz), grad(permutations[b1], dx - 1, dy - 1, dz));
                        x10 = lerp(sx, grad(permutations[a0 + 1], dx, dy, dz - 1), grad(permutations[b0 + 1], dx - 1, dy, dz - 1));
                        x11 = lerp(sx, grad(permutations[a1 + 1], dx, dy - 1, dz - 1), grad(permutations[b1 + 1], dx - 1, dy - 1, dz - 1));
                    }
                    double yz0 = lerp(sy, x00, x01);
                    double yz1 = lerp(sy, x10, x11);
                    out[outIndex++] += lerp(sz, yz0, yz1) * inverseScale;
                }
            }
        }
    }
}
