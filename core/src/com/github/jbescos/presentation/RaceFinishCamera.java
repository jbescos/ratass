package com.github.jbescos.presentation;

/** Presentation-only rules for the event camera's winner finish shot. */
public final class RaceFinishCamera {
    private static final float WINNER_HOLD_SECONDS = 3f;
    private static final float WINNER_ZOOM_SCALE = 0.72f;

    private RaceFinishCamera() {
    }

    public static boolean shouldFocusWinner(
            boolean eventCameraSelected,
            boolean finishingRace,
            boolean roundOver,
            boolean winnerAvailable,
            float finishingElapsedSeconds) {
        return eventCameraSelected
                && winnerAvailable
                && (roundOver
                        || (finishingRace
                                && Math.max(0f, finishingElapsedSeconds)
                                        < WINNER_HOLD_SECONDS));
    }

    public static float focusedZoom(float regularZoom, float minimumZoom) {
        return Math.max(minimumZoom, regularZoom * WINNER_ZOOM_SCALE);
    }
}
