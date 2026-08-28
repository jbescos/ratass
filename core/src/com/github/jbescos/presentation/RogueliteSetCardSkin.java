package com.github.jbescos.presentation;

/** Presentation constants for the dedicated set-bonus card shell. */
public final class RogueliteSetCardSkin {
    public static final String ASSET_PATH = "roguelite/cards/set_card_shell.png";

    private RogueliteSetCardSkin() {
    }

    public static float brightness(boolean empty, boolean selected) {
        return empty ? 0.78f : selected ? 1f : 0.94f;
    }
}
