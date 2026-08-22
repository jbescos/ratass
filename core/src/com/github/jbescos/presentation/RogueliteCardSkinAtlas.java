package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;

/** Maps roguelite slot types to the filled and empty card-shell atlas cells. */
public final class RogueliteCardSkinAtlas {
    public static final int COLUMNS = 5;
    public static final int ROWS = 2;
    private static final float MINIMUM_ARTWORK_SIZE = 24f;
    private static final float COMPACT_CARD_HEIGHT = 260f;
    private static final float COMPACT_CARD_ARTWORK_RATIO = 0.34f;
    private static final float STANDARD_CARD_ARTWORK_RATIO = 0.38f;
    private static final float COMPACT_DRIVER_ARTWORK_RATIO = 0.34f;
    private static final float STANDARD_DRIVER_ARTWORK_RATIO = 0.38f;
    private static final float HEADER_BADGE_ICON_WIDTH_RATIO = 0.15f;
    private static final float HEADER_BADGE_ICON_HEIGHT_RATIO = 0.112f;
    private static final float TYPE_ICON_CENTER_X_RATIO = 0.125f;
    private static final float TIER_ICON_CENTER_X_RATIO = 0.875f;
    private static final float HEADER_ICON_CENTER_Y_RATIO = 0.90625f;
    private static final float HEADER_TITLE_LEFT_RATIO = 0.23f;
    private static final float HEADER_TITLE_WIDTH_RATIO = 0.54f;
    private static final float HEADER_TITLE_BOTTOM_RATIO = 0.855f;
    private static final float HEADER_TITLE_HEIGHT_RATIO = 0.11f;
    private static final float ARTWORK_WINDOW_BOTTOM_RATIO = 0.425f;
    private static final float ARTWORK_WINDOW_TOP_RATIO = 0.825f;
    private static final float EXTENDED_ARTWORK_SIDE_INSET_RATIO = 0.025f;
    private static final float EXTENDED_ARTWORK_BOTTOM_RATIO = 0.405f;
    private static final float EXTENDED_ARTWORK_TOP_RATIO = 0.985f;
    private static final float INFORMATION_PANEL_BOTTOM_RATIO = 0.083f;
    private static final float INFORMATION_PANEL_TOP_RATIO = 0.39f;
    private static final float FOOTER_LABEL_LEFT_RATIO = 0.328125f;
    private static final float FOOTER_LABEL_WIDTH_RATIO = 0.34375f;
    private static final float FOOTER_LABEL_BOTTOM_RATIO = 0.018f;
    private static final float FOOTER_LABEL_HEIGHT_RATIO = 0.052f;

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
            case POWERUP:
                column = 3;
                break;
            case REVENGE:
                column = 4;
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

    public static float extendedArtworkSideInset(float cardWidth) {
        return Math.max(0f, cardWidth * EXTENDED_ARTWORK_SIDE_INSET_RATIO);
    }

    public static float extendedArtworkBottom(float cardHeight) {
        return Math.max(0f, cardHeight * EXTENDED_ARTWORK_BOTTOM_RATIO);
    }

    public static float extendedArtworkTop(float cardHeight) {
        return Math.max(0f, cardHeight * EXTENDED_ARTWORK_TOP_RATIO);
    }

    public static float informationPanelBottom(float cardHeight) {
        return Math.max(0f, cardHeight * INFORMATION_PANEL_BOTTOM_RATIO);
    }

    public static float informationPanelTop(float cardHeight) {
        return Math.max(0f, cardHeight * INFORMATION_PANEL_TOP_RATIO);
    }

    public static float headerBadgeIconSize(float cardWidth, float cardHeight) {
        return Math.max(
                0f,
                Math.min(
                        cardWidth * HEADER_BADGE_ICON_WIDTH_RATIO,
                        cardHeight * HEADER_BADGE_ICON_HEIGHT_RATIO));
    }

    public static float typeIconCenterX(float cardWidth) {
        return Math.max(0f, cardWidth * TYPE_ICON_CENTER_X_RATIO);
    }

    public static float tierIconCenterX(float cardWidth) {
        return Math.max(0f, cardWidth * TIER_ICON_CENTER_X_RATIO);
    }

    public static float headerIconCenterY(float cardHeight) {
        return Math.max(0f, cardHeight * HEADER_ICON_CENTER_Y_RATIO);
    }

    public static float headerTitleLeft(float cardWidth) {
        return Math.max(0f, cardWidth * HEADER_TITLE_LEFT_RATIO);
    }

    public static float headerTitleWidth(float cardWidth) {
        return Math.max(0f, cardWidth * HEADER_TITLE_WIDTH_RATIO);
    }

    public static float headerTitleBottom(float cardHeight) {
        return Math.max(0f, cardHeight * HEADER_TITLE_BOTTOM_RATIO);
    }

    public static float headerTitleHeight(float cardHeight) {
        return Math.max(0f, cardHeight * HEADER_TITLE_HEIGHT_RATIO);
    }

    public static float footerLabelLeft(float cardWidth) {
        return Math.max(0f, cardWidth * FOOTER_LABEL_LEFT_RATIO);
    }

    public static float footerLabelWidth(float cardWidth) {
        return Math.max(0f, cardWidth * FOOTER_LABEL_WIDTH_RATIO);
    }

    public static float footerLabelBottom(float cardHeight) {
        return Math.max(0f, cardHeight * FOOTER_LABEL_BOTTOM_RATIO);
    }

    public static float footerLabelHeight(float cardHeight) {
        return Math.max(0f, cardHeight * FOOTER_LABEL_HEIGHT_RATIO);
    }
}
