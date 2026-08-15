package com.github.jbescos.presentation;

/** Visibility cycle for the two in-race HUD panels. */
public enum HudPanelVisibility {
    ALL(true, true),
    BOTTOM_ONLY(false, true),
    RIGHT_ONLY(true, false),
    NONE(false, false);

    private final boolean rightPanelVisible;
    private final boolean bottomPanelVisible;

    HudPanelVisibility(boolean rightPanelVisible, boolean bottomPanelVisible) {
        this.rightPanelVisible = rightPanelVisible;
        this.bottomPanelVisible = bottomPanelVisible;
    }

    public boolean isRightPanelVisible() {
        return rightPanelVisible;
    }

    public boolean isBottomPanelVisible() {
        return bottomPanelVisible;
    }

    public boolean isAnyPanelVisible() {
        return rightPanelVisible || bottomPanelVisible;
    }

    public HudPanelVisibility next() {
        switch (this) {
            case ALL:
                return BOTTOM_ONLY;
            case BOTTOM_ONLY:
                return RIGHT_ONLY;
            case RIGHT_ONLY:
                return NONE;
            default:
                return ALL;
        }
    }
}
