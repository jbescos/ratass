package com.github.jbescos.presentation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HudPanelVisibilityTest {
    @Test
    public void cyclesRightBottomBothAndAll() {
        HudPanelVisibility bottomOnly = HudPanelVisibility.ALL.next();
        assertFalse(bottomOnly.isRightPanelVisible());
        assertTrue(bottomOnly.isBottomPanelVisible());

        HudPanelVisibility rightOnly = bottomOnly.next();
        assertTrue(rightOnly.isRightPanelVisible());
        assertFalse(rightOnly.isBottomPanelVisible());

        HudPanelVisibility none = rightOnly.next();
        assertFalse(none.isAnyPanelVisible());
        assertSame(HudPanelVisibility.ALL, none.next());
    }
}
