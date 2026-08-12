package com.github.jbescos.presentation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SandboxDebugGuidesTest {
    @Test
    public void drawsOnlyForVisibleSandboxPresentation() {
        assertTrue(SandboxDebugGuides.shouldDraw(true, true, true));
        assertFalse(SandboxDebugGuides.shouldDraw(false, true, true));
        assertFalse(SandboxDebugGuides.shouldDraw(true, false, true));
        assertFalse(SandboxDebugGuides.shouldDraw(true, true, false));
    }
}
