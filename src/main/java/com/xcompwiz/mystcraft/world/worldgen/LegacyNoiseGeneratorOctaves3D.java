package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Arrays;
import java.util.Random;

/** Mystcraft legacy-compatible octave composition over the independent gradient-noise kernel. */
public final class LegacyNoiseGeneratorOctaves3D {
    private static final long LEGACY_COORDINATE_PERIOD = 16_777_216L;
    private final LegacyNoiseGeneratorImproved3D[] layers;

    public LegacyNoiseGeneratorOctaves3D(Random random, int octaveCount) {
        layers = new LegacyNoiseGeneratorImproved3D[octaveCount];
        for (int i = 0; i < octaveCount; i++) layers[i] = new LegacyNoiseGeneratorImproved3D(random);
    }

    public double[] generate(double[] target,
                             int xOffset, int yOffset, int zOffset,
                             int xSize, int ySize, int zSize,
                             double xScale, double yScale, double zScale) {
        int samples = xSize * ySize * zSize;
        if (target == null || target.length < samples) target = new double[samples];
        else Arrays.fill(target, 0, samples, 0.0D);

        double frequency = 1.0D;
        for (LegacyNoiseGeneratorImproved3D layer : layers) {
            double xBase = wrapLegacyCoordinate(xOffset * xScale * frequency);
            double zBase = wrapLegacyCoordinate(zOffset * zScale * frequency);
            double yBase = yOffset * yScale * frequency;
            layer.populateNoiseArray(target, xBase, yBase, zBase,
                    xSize, ySize, zSize,
                    xScale * frequency, yScale * frequency, zScale * frequency,
                    frequency);
            frequency *= 0.5D;
        }
        return target;
    }

    public double[] generate2D(double[] target, int xOffset, int zOffset,
                               int xSize, int zSize, double xScale, double zScale) {
        return generate(target, xOffset, 10, zOffset, xSize, 1, zSize, xScale, 1.0D, zScale);
    }

    private static double wrapLegacyCoordinate(double coordinate) {
        long whole = floorLong(coordinate);
        return coordinate - whole + whole % LEGACY_COORDINATE_PERIOD;
    }

    private static long floorLong(double value) {
        long truncated = (long) value;
        return value < truncated ? truncated - 1L : truncated;
    }
}
