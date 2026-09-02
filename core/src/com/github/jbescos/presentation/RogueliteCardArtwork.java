package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.RogueliteCardDefinition;

/** Resolves independently loadable, theme-specific card artwork. */
public final class RogueliteCardArtwork {
    public static final String THEMED_DIRECTORY_PATH = "roguelite/cards/artwork";

    private static final String[] PATHS = createPaths();

    private RogueliteCardArtwork() {
    }

    public static String pathForIndex(int artworkIndex) {
        return artworkIndex >= 0 && artworkIndex < PATHS.length
                ? PATHS[artworkIndex]
                : null;
    }

    private static String[] createPaths() {
        String[] paths = new String[RogueliteCardDefinition.ARTWORK_CAPACITY];
        for (int index = 0; index < paths.length; index++) {
            paths[index] = THEMED_DIRECTORY_PATH + "/" + threeDigits(index) + ".png";
        }
        return paths;
    }

    private static String threeDigits(int value) {
        if (value < 10) {
            return "00" + value;
        }
        if (value < 100) {
            return "0" + value;
        }
        return Integer.toString(value);
    }
}
