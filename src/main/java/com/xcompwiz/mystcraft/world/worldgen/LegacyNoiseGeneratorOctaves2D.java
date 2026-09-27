package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Arrays;
import java.util.Random;

/**
 * Independent compatibility implementation of the legacy improved-noise octave stream used by
 * Mystcraft floating-island stone-thickness generation. Constructor RNG consumption, coordinate
 * wrapping and output ordering are retained as compatibility behavior.
 */
public final class LegacyNoiseGeneratorOctaves2D {
    private final Improved[] octaves;

    public LegacyNoiseGeneratorOctaves2D(Random random, int count) {
        octaves = new Improved[count];
        for (int i = 0; i < count; i++) {
            octaves[i] = new Improved(random);
        }
    }

    public double[] generate(
            double[] output,
            int startX,
            int startZ,
            int sizeX,
            int sizeZ,
            double scaleX,
            double scaleZ) {

        int length = sizeX * sizeZ;
        if (output == null || output.length < length) {
            output = new double[length];
        } else {
            Arrays.fill(output, 0, length, 0.0D);
        }

        double octaveScale = 1.0D;
        for (Improved octave : octaves) {
            double xBase = startX * octaveScale * scaleX;
            double zBase = startZ * octaveScale * scaleZ;

            long xFloor = floorLong(xBase);
            long zFloor = floorLong(zBase);
            xBase -= xFloor;
            zBase -= zFloor;
            xFloor %= 16777216L;
            zFloor %= 16777216L;
            xBase += xFloor;
            zBase += zFloor;

            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    double sampleX = xBase + x * scaleX * octaveScale;
                    double sampleZ = zBase + z * scaleZ * octaveScale;
                    output[z + x * sizeZ] += octave.sample(sampleX, sampleZ) / octaveScale;
                }
            }

            octaveScale /= 2.0D;
        }

        return output;
    }

    private static long floorLong(double value) {
        long l = (long) value;
        return value < l ? l - 1L : l;
    }

    private static final class Improved {
        private final int[] permutations = new int[512];
        private final double xCoord;
        private final double yCoord;
        private final double zCoord;

        Improved(Random random) {
            xCoord = random.nextDouble() * 256.0D;
            yCoord = random.nextDouble() * 256.0D;
            zCoord = random.nextDouble() * 256.0D;

            for (int i = 0; i < 256; i++) {
                permutations[i] = i;
            }
            for (int i = 0; i < 256; i++) {
                int j = random.nextInt(256 - i) + i;
                int swap = permutations[i];
                permutations[i] = permutations[j];
                permutations[j] = swap;
                permutations[i + 256] = permutations[i];
            }
        }

        double sample(double x, double z) {
            double px = x + xCoord;
            double py = yCoord;
            double pz = z + zCoord;

            int ix = floor(px);
            int iy = floor(py);
            int iz = floor(pz);

            double fx = px - ix;
            double fy = py - iy;
            double fz = pz - iz;

            int X = ix & 255;
            int Y = iy & 255;
            int Z = iz & 255;

            double u = fade(fx);
            double v = fade(fy);
            double w = fade(fz);

            int A = permutations[X] + Y;
            int AA = permutations[A] + Z;
            int AB = permutations[A + 1] + Z;
            int B = permutations[X + 1] + Y;
            int BA = permutations[B] + Z;
            int BB = permutations[B + 1] + Z;

            return lerp(w,
                    lerp(v,
                            lerp(u, grad(permutations[AA], fx, fy, fz),
                                    grad(permutations[BA], fx - 1, fy, fz)),
                            lerp(u, grad(permutations[AB], fx, fy - 1, fz),
                                    grad(permutations[BB], fx - 1, fy - 1, fz))),
                    lerp(v,
                            lerp(u, grad(permutations[AA + 1], fx, fy, fz - 1),
                                    grad(permutations[BA + 1], fx - 1, fy, fz - 1)),
                            lerp(u, grad(permutations[AB + 1], fx, fy - 1, fz - 1),
                                    grad(permutations[BB + 1], fx - 1, fy - 1, fz - 1))));
        }

        private static int floor(double value) {
            int i = (int) value;
            return value < i ? i - 1 : i;
        }

        private static double fade(double t) {
            return t * t * t * (t * (t * 6 - 15) + 10);
        }

        private static double lerp(double t, double a, double b) {
            return a + t * (b - a);
        }

        private static double grad(int hash, double x, double y, double z) {
            int h = hash & 15;
            double u = h < 8 ? x : y;
            double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
            return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
        }
    }
}
