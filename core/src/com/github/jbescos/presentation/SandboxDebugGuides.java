package com.github.jbescos.presentation;

/** Keeps sandbox debug rendering out of headless and normal gameplay paths. */
public final class SandboxDebugGuides {
    private SandboxDebugGuides() {
    }

    public static boolean shouldDraw(
            boolean presentationEnabled,
            boolean sandboxMode,
            boolean guidesVisible) {
        return presentationEnabled && sandboxMode && guidesVisible;
    }
}
