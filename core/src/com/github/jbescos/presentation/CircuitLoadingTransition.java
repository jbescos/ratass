package com.github.jbescos.presentation;

public final class CircuitLoadingTransition {
    private CircuitLoadingTransition() {}

    public static boolean shouldStart(
            boolean presentationEnabled,
            boolean loadingAlreadyInProgress,
            int mapCount) {
        return presentationEnabled && !loadingAlreadyInProgress && mapCount > 1;
    }

    public static int nextCircuitNumber(int currentCircuitNumber, int mapCount) {
        if (mapCount <= 0) {
            return 0;
        }
        int current = Math.max(1, Math.min(currentCircuitNumber, mapCount));
        return current >= mapCount ? 1 : current + 1;
    }

    public static String status(int currentCircuitNumber, int mapCount) {
        int next = nextCircuitNumber(currentCircuitNumber, mapCount);
        return next <= 0
                ? "Loading circuit"
                : "Loading circuit " + next + " / " + mapCount;
    }
}
