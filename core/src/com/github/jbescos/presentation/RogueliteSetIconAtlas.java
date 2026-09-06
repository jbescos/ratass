package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.RogueliteSetDefinition;

/** Maps set recipes to the illustrated shared icon atlas. */
public final class RogueliteSetIconAtlas {
    public static final int COLUMNS = 5;
    public static final int ROWS = 2;

    private RogueliteSetIconAtlas() {
    }

    public static int indexFor(RogueliteSetDefinition definition) {
        return definition == null ? -1 : definition.getIconIndex();
    }
}
