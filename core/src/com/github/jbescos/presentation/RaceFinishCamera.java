package com.github.jbescos.presentation;

/** Presentation-only rules for the event camera's winner finish shot. */
public final class RaceFinishCamera {
    private static final float WINNER_ZOOM_SCALE = 0.72f;

    private RaceFinishCamera() {
    }

    public static boolean shouldFocusWinner(
            boolean eventCameraSelected,
            boolean finishingRace,
            boolean roundOver,
            boolean winnerAvailable) {
        return eventCameraSelected
                && (finishingRace || roundOver)
                && winnerAvailable;
    }

    public static float focusedZoom(float regularZoom, float minimumZoom) {
        return Math.max(minimumZoom, regularZoom * WINNER_ZOOM_SCALE);
    }
}
