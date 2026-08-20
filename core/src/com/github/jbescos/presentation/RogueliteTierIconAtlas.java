package com.github.jbescos.presentation;

/** Maps card tiers to cells in the shared tier icon atlas. */
public final class RogueliteTierIconAtlas {
    public static final int COLUMNS = 4;
    public static final int ROWS = 1;

    private RogueliteTierIconAtlas() {
    }

    public static int indexForTier(int tier) {
        return tier >= 1 && tier <= COLUMNS ? tier - 1 : -1;
    }
}
