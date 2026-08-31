package com.github.jbescos.presentation;

public final class RogueliteResponsiveCardLayout {
    private static final float SHORT_LANDSCAPE_HEIGHT = 560f;

    private RogueliteResponsiveCardLayout() {}

    public static boolean isShortLandscape(float width, float height) {
        return width > height && height < SHORT_LANDSCAPE_HEIGHT;
    }

    public static int rewardSectionColumns(
            float width,
            float height,
            int itemCount,
            int wideColumns) {
        if (itemCount <= 0 || wideColumns <= 0) {
            return 0;
        }
        boolean constrained =
                isShortLandscape(width, height)
                        || (height > width && width < 900f);
        int columns = constrained ? 3 : wideColumns;
        return Math.min(itemCount, columns);
    }

    public static boolean showCarStats(float height) {
        return height >= 520f;
    }

    public static int collectionPageCapacity(
            float width,
            float height,
            int maximumCards) {
        return Math.max(0, Math.min(3, maximumCards));
    }

    public static int equippedLoadoutPageCapacity(int maximumCards) {
        return Math.max(0, maximumCards);
    }

    public static int equippedLoadoutColumns(
            float width,
            float height,
            int cardCount) {
        if (cardCount <= 0) {
            return 0;
        }
        if (width < 560f) {
            return 1;
        }
        if (height > width && width < 900f) {
            return Math.min(2, cardCount);
        }
        if (isShortLandscape(width, height)) {
            return Math.min(3, cardCount);
        }
        return cardCount;
    }

    public static float minimumTouchTarget(float width, float height) {
        float shortSide = Math.max(1f, Math.min(width, height));
        return Math.max(56f, Math.min(72f, shortSide * 0.085f));
    }

    public static float cardsButtonSize(float width, float height) {
        float shortSide = Math.max(1f, Math.min(width, height));
        return Math.max(88f, Math.min(120f, shortSide * 0.14f));
    }

    public static float mainMenuButtonHeight(float screenHeight) {
        return Math.max(60f, Math.min(84f, screenHeight * 0.105f));
    }

    public static float rewardActionButtonHeight(float width, float height) {
        float shortSide = Math.max(1f, Math.min(width, height));
        return Math.max(72f, Math.min(96f, shortSide * 0.13f));
    }

    public static float rewardActionButtonMaximumWidth(float width) {
        return Math.max(200f, Math.min(260f, Math.max(1f, width) * 0.22f));
    }

    public static float modeControlHeight(float width, float height) {
        float shortSide = Math.max(1f, Math.min(width, height));
        return Math.max(32f, Math.min(40f, shortSide * 0.05f));
    }

    public static float modeControlGap(float width, float height) {
        float shortSide = Math.max(1f, Math.min(width, height));
        return Math.max(4f, Math.min(7f, shortSide * 0.01f));
    }

    public static float modeControlBottom(
            float cardBottom,
            float controlHeight,
            float gap) {
        return cardBottom - Math.max(0f, gap) - Math.max(0f, controlHeight);
    }

    public static float inspectionCardWidth(
            float width,
            float height,
            float cardAspect) {
        float safeWidth = Math.max(1f, width);
        float safeHeight = Math.max(1f, height);
        float aspect = Math.max(0.1f, cardAspect);
        float horizontalMargin = Math.max(18f, Math.min(110f, safeWidth * 0.08f));
        float verticalMargin = Math.max(16f, Math.min(48f, safeHeight * 0.06f));
        float availableWidth = Math.max(1f, safeWidth - horizontalMargin * 2f);
        float availableHeight = Math.max(1f, safeHeight - verticalMargin * 2f);
        return Math.max(
                1f,
                Math.min(430f, Math.min(availableWidth, availableHeight / aspect)));
    }

    public static float centeredTextBaseline(
            float bottom,
            float height,
            float capHeight) {
        return bottom
                + (Math.max(0f, height) + Math.max(0f, capHeight)) * 0.5f;
    }
}
