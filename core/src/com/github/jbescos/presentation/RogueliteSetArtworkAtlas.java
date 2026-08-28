package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.RogueliteSetDefinition;

/** Themed 3x3 artwork atlas for automatic set-bonus cards. */
public final class RogueliteSetArtworkAtlas {
    public static final String THEMED_RELATIVE_PATH =
            "roguelite/cards/set_art_atlas.png";
    public static final int COLUMNS = 3;
    public static final int ROWS = 3;

    private RogueliteSetArtworkAtlas() {
    }

    public static int indexFor(RogueliteSetDefinition definition) {
        return definition == null ? -1 : definition.getIconIndex();
    }
}
