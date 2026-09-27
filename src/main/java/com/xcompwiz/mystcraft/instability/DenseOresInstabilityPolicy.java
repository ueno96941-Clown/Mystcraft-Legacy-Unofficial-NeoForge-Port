package com.xcompwiz.mystcraft.instability;

/**
 * CP291 fixed Dense Ores Instability tiers.
 *
 * <p>These values are intentionally page-count based rather than ore-count based.
 * The 2-page value remains close to direct Mystcraft 0.13.7.06 measurements. CP298
 * raises the first page to the controller minimum (500) so an unmitigated one-page Age can
 * produce a light Basic-deck penalty instead of always rounding to zero. The third page remains
 * the explicit forbidden tier, high enough to enter Decay-capable
 * instability decks even on normal Mystcraft difficulty.</p>
 */
public final class DenseOresInstabilityPolicy {
    public static final int ONE_PAGE = 500;
    public static final int TWO_PAGES = 2100;
    public static final int THREE_PAGES = 20000;

    private DenseOresInstabilityPolicy() {}

    public static int effectivePages(int authoredPages) {
        return Math.max(0, Math.min(3, authoredPages));
    }

    public static int totalForPages(int authoredPages) {
        return switch (effectivePages(authoredPages)) {
            case 0 -> 0;
            case 1 -> ONE_PAGE;
            case 2 -> TWO_PAGES;
            default -> THREE_PAGES;
        };
    }

    /**
     * IAgeSymbol.instabilityModifier(count) is incremental in legacy Mystcraft.
     * Return the delta contributed by the count-th copy so repeated pages sum to
     * the fixed tier total without changing the static calculator contract.
     */
    public static int incrementalForOccurrence(int occurrence) {
        return switch (occurrence) {
            case 1 -> ONE_PAGE;
            case 2 -> TWO_PAGES - ONE_PAGE;
            case 3 -> THREE_PAGES - TWO_PAGES;
            default -> 0;
        };
    }
}
