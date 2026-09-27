package com.xcompwiz.mystcraft.world.worldgen;

import java.util.Random;

/**
 * Direct compatibility port of Mystcraft 0.13.7.06 WorldGenMystBigTree.
 *
 * <p>This intentionally retains the unusual split between a low root position (Y 4..11)
 * and a high foliage blob (Y 128..180), the approximate sqrt routine, 2x2 trunk,
 * branch/node equations, and 17x17 source-chunk scan inherited from MapGenAdvanced.</p>
 */
public final class LegacyWorldGenMystBigTreeKernel {
    private static final byte[] OTHER_COORD_PAIRS = {2, 0, 0, 1, 2, 1};
    private static final int[] COORD_MAXIMUMS = {16, 255, 16};
    private static final double SCALE_WIDTH = 0.2D;
    private static final int MAX_Y = 180;
    private static final int MIN_Y = 128;
    private static final int LEAF_DISTANCE_LIMIT = 3;
    private static final int TRUNK_SIZE = 2;
    private static final int RANGE = 8;

    private final long seed;
    private final Random rand = new Random();
    private final int[] blobPos = {0, 0, 0};
    private final int[] rootPos = {0, 0, 0};
    private int blobHeight;
    private int[][] leafNodes;

    public LegacyWorldGenMystBigTreeKernel(long seed) {
        this.seed = seed;
    }

    public synchronized void generateFromSource(
            int sourceChunkX,
            int sourceChunkZ,
            int targetChunkX,
            int targetChunkZ,
            LegacyBigTreeBuffer buffer) {

        if (Math.abs(sourceChunkX - targetChunkX) > RANGE
                || Math.abs(sourceChunkZ - targetChunkZ) > RANGE) {
            return;
        }

        rand.setSeed(seed);
        long xseed = rand.nextLong();
        long zseed = rand.nextLong();
        rand.setSeed(sourceChunkX * xseed ^ sourceChunkZ * zseed ^ seed);
        recursiveGenerate(sourceChunkX, sourceChunkZ, targetChunkX, targetChunkZ, buffer);
    }

    private void recursiveGenerate(
            int sourceChunkX,
            int sourceChunkZ,
            int targetChunkX,
            int targetChunkZ,
            LegacyBigTreeBuffer buffer) {

        if (rand.nextInt(2) != 0) return;

        int pX = sourceChunkX * 16 + rand.nextInt(16) - targetChunkX * 16;
        int pY = rand.nextInt(8) + 4;
        int pZ = sourceChunkZ * 16 + rand.nextInt(16) - targetChunkZ * 16;

        rootPos[0] = blobPos[0] = pX;
        rootPos[1] = blobPos[1] = pY;
        rootPos[2] = blobPos[2] = pZ;

        blobPos[1] = rand.nextInt(MAX_Y - MIN_Y + 1) + MIN_Y;
        blobHeight = rand.nextInt(30) + 40;

        generateLeafNodeList();
        generateLeaves(buffer);
        generateTrunk(buffer);
        generateRoots(buffer);
        generateLeafNodeBases(buffer);
    }

    private void generateLeafNodeList() {
        int nodesPerLayer = 4;
        int[][] nodes = new int[nodesPerLayer * blobHeight][4];
        int y = blobPos[1] + blobHeight - LEAF_DISTANCE_LIMIT;
        int count = 1;
        int trunkTop = (int) (blobPos[1] + blobHeight * 0.9D);
        int relativeY = y - blobPos[1];

        nodes[0][0] = blobPos[0];
        nodes[0][1] = y;
        nodes[0][2] = blobPos[2];
        nodes[0][3] = trunkTop;
        --y;

        int[] trunkBase = {blobPos[0], 0, blobPos[2]};
        final double centerOffset = 0.5D;

        while (relativeY >= 0) {
            float layerRadius = layerSize(relativeY);
            if (layerRadius < 0.0F) {
                --y;
                --relativeY;
                continue;
            }

            for (int n = 0; n < nodesPerLayer; ++n) {
                double limbLength = SCALE_WIDTH * (layerRadius * (rand.nextFloat() + 3.0D));
                double direction = rand.nextFloat() * 2D * 3.1415899999999999D;
                int nodeX = floor(limbLength * Math.sin(direction) + blobPos[0] + centerOffset);
                int nodeZ = floor(limbLength * Math.cos(direction) + blobPos[2] + centerOffset);
                trunkBase[1] = y - 2;

                nodes[count][0] = nodeX;
                nodes[count][1] = y;
                nodes[count][2] = nodeZ;
                nodes[count][3] = trunkBase[1];
                ++count;
            }

            --y;
            --relativeY;
        }

        leafNodes = new int[count][4];
        System.arraycopy(nodes, 0, leafNodes, 0, count);
    }

    private void genTreeLayer(
            LegacyBigTreeBuffer buffer,
            int x,
            int y,
            int z,
            float radiusF,
            byte axis0,
            boolean leaves) {

        int radius = (int) (radiusF + 0.61799999999999999D);
        float radiusSquared = radiusF * radiusF;
        byte axis1 = OTHER_COORD_PAIRS[axis0];
        byte axis2 = OTHER_COORD_PAIRS[axis0 + 3];
        int[] basePos = {x, y, z};
        int[] localPos = {0, 0, 0};

        localPos[axis0] = basePos[axis0];
        if (localPos[axis0] < 0 || localPos[axis0] >= COORD_MAXIMUMS[axis0]) return;

        for (int axis1Offset = -radius; axis1Offset <= radius; ++axis1Offset) {
            localPos[axis1] = basePos[axis1] + axis1Offset;
            if (localPos[axis1] < 0 || localPos[axis1] >= COORD_MAXIMUMS[axis1]) continue;

            for (int axis2Offset = -radius; axis2Offset <= radius; ++axis2Offset) {
                localPos[axis2] = basePos[axis2] + axis2Offset;
                if (localPos[axis2] < 0 || localPos[axis2] >= COORD_MAXIMUMS[axis2]) continue;

                if (Math.pow(axis1Offset + 0.5D, 2D)
                        + Math.pow(axis2Offset + 0.5D, 2D) < radiusSquared) {
                    place(buffer, localPos[0], localPos[1], localPos[2], leaves);
                }
            }
        }
    }

    private float layerSize(int i) {
        if (i < blobHeight * 0.75D) return -1.618F;

        float half = blobHeight * 0.5F;
        float delta = blobHeight * 0.5F - i;
        float result;
        if (delta == 0.0F) result = half;
        else if (Math.abs(delta) >= half) result = 0.0F;
        else result = (float) sqrt(Math.pow(half, 2D) - Math.pow(delta, 2D));
        return result * 0.5F;
    }

    /** Same bit-level sqrt approximation used by the 1.12 Mystcraft source. */
    public static double sqrt(final double a) {
        final long x = Double.doubleToLongBits(a) >> 32;
        return Double.longBitsToDouble((x + 1072632448) << 31);
    }

    private float leafSize(int i) {
        if (i < 0 || i >= LEAF_DISTANCE_LIMIT) return -1F;
        return i != 0 && i != LEAF_DISTANCE_LIMIT - 1 ? 3F : 2.0F;
    }

    private void generateLeafNode(LegacyBigTreeBuffer buffer, int x, int y, int z) {
        for (int yy = y; yy < y + LEAF_DISTANCE_LIMIT; ++yy) {
            float radius = leafSize(yy - y);
            if (radius >= 0) genTreeLayer(buffer, x, yy, z, radius, (byte) 1, true);
        }
    }

    private void placeBlockLine(
            LegacyBigTreeBuffer buffer,
            int[] startPoint,
            int[] endPoint,
            boolean leaves) {

        int[] delta = {0, 0, 0};
        int dominant = 0;
        for (byte axis = 0; axis < 3; ++axis) {
            delta[axis] = endPoint[axis] - startPoint[axis];
            if (Math.abs(delta[axis]) > Math.abs(delta[dominant])) dominant = axis;
        }
        if (delta[dominant] == 0) return;

        byte axis1 = OTHER_COORD_PAIRS[dominant];
        byte axis2 = OTHER_COORD_PAIRS[dominant + 3];
        byte step = (byte) (delta[dominant] > 0 ? 1 : -1);
        double slope1 = (double) delta[axis1] / (double) delta[dominant];
        double slope2 = (double) delta[axis2] / (double) delta[dominant];
        int[] pos = {0, 0, 0};

        for (int k = 0, end = delta[dominant] + step; k != end; k += step) {
            pos[dominant] = floor(startPoint[dominant] + k + 0.5D);
            pos[axis1] = floor(startPoint[axis1] + k * slope1 + 0.5D);
            pos[axis2] = floor(startPoint[axis2] + k * slope2 + 0.5D);
            if (pos[0] < 0 || pos[0] >= 16) continue;
            if (pos[1] < 0 || pos[1] >= 255) continue;
            if (pos[2] < 0 || pos[2] >= 16) continue;
            place(buffer, pos[0], pos[1], pos[2], leaves);
        }
    }

    private void generateLeaves(LegacyBigTreeBuffer buffer) {
        for (int[] node : leafNodes) {
            generateLeafNode(buffer, node[0], node[1], node[2]);
        }
    }

    private boolean leafNodeNeedsBase(int i) {
        return i >= blobHeight * 0.20000000000000001D;
    }

    private void generateTrunk(LegacyBigTreeBuffer buffer) {
        int x = rootPos[0];
        int y = rootPos[1];
        int topY = (int) (blobPos[1] + blobHeight * 0.9D);
        int z = rootPos[2];
        int[] start = {x, y, z};
        int[] end = {x, topY, z};
        placeBlockLine(buffer, start, end, false);

        if (TRUNK_SIZE == 2) {
            start[0]++; end[0]++;
            if (validXZ(start)) placeBlockLine(buffer, start, end, false);
            start[2]++; end[2]++;
            if (validXZ(start)) placeBlockLine(buffer, start, end, false);
            start[0]--; end[0]--;
            if (validXZ(start)) placeBlockLine(buffer, start, end, false);
        }
    }

    private static boolean validXZ(int[] p) {
        return p[0] >= 0 && p[0] < COORD_MAXIMUMS[0]
                && p[2] >= 0 && p[2] < COORD_MAXIMUMS[2];
    }

    private void generateRoots(LegacyBigTreeBuffer buffer) {
        int x = rootPos[0];
        int y = rootPos[1];
        int z = rootPos[2];
        int[] start = {x, y + 1, z};
        int[] end = {x, y, z};
        int range = rootPos[1];
        int count = blobHeight >> 2;

        for (int c = 0; c < count; ++c) {
            start[0] = x + c % 2;
            start[2] = z + (c > 2 ? 1 : 0);
            end[0] = x + rand.nextInt(13) - 6;
            end[1] = y - rand.nextInt(range + 1) - 3;
            end[2] = z + rand.nextInt(13) - 6;
            placeBlockLine(buffer, start, end, false);
        }
    }

    private void generateLeafNodeBases(LegacyBigTreeBuffer buffer) {
        int[] trunk = {blobPos[0], blobPos[1], blobPos[2]};
        for (int[] node : leafNodes) {
            int[] nodePos = {node[0], node[1], node[2]};
            trunk[1] = node[3];
            int relative = trunk[1] - blobPos[1];
            if (leafNodeNeedsBase(relative)) {
                placeBlockLine(buffer, trunk, nodePos, false);
            }
        }
    }

    private static int floor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private static void place(
            LegacyBigTreeBuffer buffer,
            int x,
            int y,
            int z,
            boolean leaves) {
        if (buffer.isBedrock(x, y, z)) return;
        if (leaves) buffer.setLeaves(x, y, z);
        else buffer.setLog(x, y, z);
    }
}
