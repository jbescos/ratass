package com.github.jbescos.presentation;

/** Stable row mapping for the generated menu-button skin atlas. */
public final class MenuButtonSkinAtlas {
    public static final int COLUMNS = 1;
    public static final int ROWS = 4;

    public enum State {
        NORMAL(0),
        SELECTED(1),
        DISABLED(2),
        PRIMARY(3);

        private final int artworkIndex;

        State(int artworkIndex) {
            this.artworkIndex = artworkIndex;
        }

        public int getArtworkIndex() {
            return artworkIndex;
        }
    }

    private MenuButtonSkinAtlas() {
    }
}
