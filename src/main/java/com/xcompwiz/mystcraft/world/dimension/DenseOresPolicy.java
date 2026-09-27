package com.xcompwiz.mystcraft.world.dimension;

/**
 * CP291 Dense Ores stacking policy.
 *
 * <p>The first two tiers keep the measured/original linear behavior. The third
 * authored page is intentionally a port-only forbidden tier: resource output jumps
 * to 5x total and its Instability score is handled separately by
 * DenseOresInstabilityPolicy.</p>
 *
 * <pre>
 * 0 pages = 1x total
 * 1 page  = 2x total
 * 2 pages = 3x total
 * 3 pages = 5x total
 * 4+ pages = 5x total (three effective pages maximum)
 * </pre>
 */
public final class DenseOresPolicy {
    public static final int MAX_EFFECTIVE_PAGES = 3;

    private DenseOresPolicy() {}

    public static int effectivePages(int authoredPages) {
        return Math.max(0, Math.min(MAX_EFFECTIVE_PAGES, authoredPages));
    }

    public static long targetMultiplier(int authoredPages) {
        return switch (effectivePages(authoredPages)) {
            case 0 -> 1L;
            case 1 -> 2L;
            case 2 -> 3L;
            default -> 5L;
        };
    }

    /** Number of extra baseline copies added on top of normal world generation. */
    public static int extraCopies(int authoredPages) {
        return (int) targetMultiplier(authoredPages) - 1;
    }

    public static int extraBlocksForLayer(int baselineCount, int hostCapacity, int authoredPages) {
        if (baselineCount <= 0 || hostCapacity <= 0) return 0;
        int copies = extraCopies(authoredPages);
        if (copies <= 0) return 0;
        long requested = (long) baselineCount * copies;
        return (int) Math.min((long) hostCapacity, requested);
    }
}
