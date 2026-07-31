package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;

/** Maps roguelite slot types to the filled and empty card-shell atlas cells. */
public final class RogueliteCardSkinAtlas {
    public static final int COLUMNS = 4;
    public static final int ROWS = 2;
    private static final float MINIMUM_ARTWORK_SIZE = 24f;
    private static final float COMPACT_CARD_HEIGHT = 260f;
    private static final float COMPACT_CARD_ARTWORK_RATIO = 0.30f;
    private static final float STANDARD_CARD_ARTWORK_RATIO = 0.35f;
    private static final float COMPACT_DRIVER_ARTWORK_RATIO = 0.30f;
    private static final float STANDARD_DRIVER_ARTWORK_RATIO = 0.35f;
    private static final float ARTWORK_WINDOW_BOTTOM_RATIO = 0.41f;
    private static final float ARTWORK_WINDOW_TOP_RATIO = 0.79f;
    private static final float INFORMATION_PANEL_BOTTOM_RATIO = 0.075f;
    private static final float INFORMATION_PANEL_TOP_RATIO = 0.39f;

    private RogueliteCardSkinAtlas() {
    }

    public static int indexFor(RogueliteSlotType slotType, boolean empty) {
        if (slotType == null) {
            return -1;
        }
        int column;
        switch (slotType) {
            case DRIVER:
                column = 0;
                break;
            case TUNING:
                column = 1;
                break;
            case TECHNIQUE:
                column = 2;
                break;
            case GADGET:
                column = 3;
                break;
            default:
                return -1;
        }
        return column + (empty ? COLUMNS : 0);
    }

    public static float fitSquareArtwork(float availableWidth, float preferredHeight) {
        return Math.max(0f, Math.min(availableWidth, preferredHeight));
    }

    public static float preferredArtworkSize(float cardHeight, boolean driver) {
        if (cardHeight <= 0f) {
            return 0f;
        }
        float ratio;
        if (driver) {
            ratio = cardHeight < COMPACT_CARD_HEIGHT
                    ? COMPACT_DRIVER_ARTWORK_RATIO
                    : STANDARD_DRIVER_ARTWORK_RATIO;
        } else {
            ratio = cardHeight < COMPACT_CARD_HEIGHT
                    ? COMPACT_CARD_ARTWORK_RATIO
                    : STANDARD_CARD_ARTWORK_RATIO;
        }
        return Math.max(MINIMUM_ARTWORK_SIZE, cardHeight * ratio);
    }

    public static float artworkWindowBottom(float cardHeight) {
        return Math.max(0f, cardHeight * ARTWORK_WINDOW_BOTTOM_RATIO);
    }

    public static float artworkWindowTop(float cardHeight) {
        return Math.max(0f, cardHeight * ARTWORK_WINDOW_TOP_RATIO);
    }

    public static float informationPanelBottom(float cardHeight) {
        return Math.max(0f, cardHeight * INFORMATION_PANEL_BOTTOM_RATIO);
    }

    public static float informationPanelTop(float cardHeight) {
        return Math.max(0f, cardHeight * INFORMATION_PANEL_TOP_RATIO);
    }
}
