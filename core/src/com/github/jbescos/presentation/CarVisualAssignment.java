package com.github.jbescos.presentation;

/** Builds a unique visual palette for the player and their opponents. */
public final class CarVisualAssignment {
    private CarVisualAssignment() {
    }

    public static boolean remainsValid(
            int assignedVisualCount,
            int assignedPlayerVisualIndex,
            int currentVisualCount,
            int currentPlayerVisualIndex) {
        return assignedVisualCount == currentVisualCount
                && assignedPlayerVisualIndex == currentPlayerVisualIndex;
    }

    public static int[] enemyCandidates(int visualCount, int playerVisualIndex) {
        if (visualCount <= 1) {
            return new int[] {0};
        }

        int clampedPlayerIndex = Math.max(0, Math.min(playerVisualIndex, visualCount - 1));
        int[] candidates = new int[visualCount - 1];
        int candidateIndex = 0;
        for (int visualIndex = 0; visualIndex < visualCount; visualIndex++) {
            if (visualIndex != clampedPlayerIndex) {
                candidates[candidateIndex++] = visualIndex;
            }
        }
        return candidates;
    }
}
