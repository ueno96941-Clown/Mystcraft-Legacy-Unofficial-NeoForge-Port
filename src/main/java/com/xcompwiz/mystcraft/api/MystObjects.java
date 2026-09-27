package com.xcompwiz.mystcraft.api;

/** Stable registry-name constants from the 0.13.7.06 API. */
public final class MystObjects {
    public static APIInstanceProvider.EntryPoint entryPoint;
    public static final String MystcraftModId = "mystcraft";
    public static final String MYST_TREASURE = "mystcraftTreasure";
    public static final class Blocks {
        public static final String portal="linkportal", crystal="blockcrystal", crystal_receptacle="blockbookreceptacle",
                decay="blockdecay", bookstand="blockbookstand", book_lectern="blocklectern", writing_desk_block="writingdesk",
                bookbinder="blockbookbinder", inkmixer="blockinkmixer", star_fissure="blockstarfissure",
                link_modifer="blocklinkmodifier", fluidblock_black_ink="fluidblockblackink";
        private Blocks() {}
    }
    public static final class Items {
        public static final String writing_desk="writingdesk", page="page", descriptive_book="agebook",
                linkbook_unlinked="unlinkedbook", linkbook="linkbook", folder="folder", booster="booster", inkvial="vial", portfolio="portfolio";
        private Items() {}
    }
    public static final class Fluids { public static final String black_ink="myst.ink.black"; private Fluids() {} }
    private MystObjects() {}
}
