package com.xcompwiz.mystcraft.validation;

import com.xcompwiz.mystcraft.world.dimension.DenseOresPolicy;

public final class DenseOresPolicyHarness {
    public static void main(String[] args) {
        expect(1L, DenseOresPolicy.targetMultiplier(0), "0 pages");
        expect(2L, DenseOresPolicy.targetMultiplier(1), "1 page");
        expect(3L, DenseOresPolicy.targetMultiplier(2), "2 pages");
        expect(5L, DenseOresPolicy.targetMultiplier(3), "3 pages forbidden tier");
        expect(5L, DenseOresPolicy.targetMultiplier(11), "11 pages capped at 3 effective pages");
        expect(10, DenseOresPolicy.extraBlocksForLayer(10, 1000, 1), "2x total extras");
        expect(20, DenseOresPolicy.extraBlocksForLayer(10, 1000, 2), "3x total extras");
        expect(40, DenseOresPolicy.extraBlocksForLayer(10, 1000, 3), "5x total extras");
        expect(40, DenseOresPolicy.extraBlocksForLayer(10, 1000, 11), "cap applies to authored overflow");
        expect(15, DenseOresPolicy.extraBlocksForLayer(10, 15, 3), "host saturation");
        System.out.println("DenseOresPolicyHarness PASS");
    }

    private static void expect(long expected, long actual, String label) {
        if (expected != actual) throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
    }
}
