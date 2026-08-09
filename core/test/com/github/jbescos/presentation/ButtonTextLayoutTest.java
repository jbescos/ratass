package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ButtonTextLayoutTest {
    @Test
    public void preservesAQuietCenterInWideButtons() {
        assertTrue(ButtonTextLayout.contentWidth(420f, 76f) >= 350f);
        assertTrue(ButtonTextLayout.contentHeight(76f) >= 52f);
    }

    @Test
    public void preservesReadableSpaceInSmallTouchButtons() {
        assertTrue(ButtonTextLayout.contentWidth(108f, 56f) >= 84f);
        assertEquals(40f, ButtonTextLayout.contentWidth(56f, 56f), 0.001f);
    }

    @Test
    public void keepsTextScaleWithinLegibleBounds() {
        assertEquals(1.05f, ButtonTextLayout.preferredTextScale(32f), 0.001f);
        assertEquals(1.30f, ButtonTextLayout.preferredTextScale(56f), 0.001f);
        assertEquals(1.42f, ButtonTextLayout.preferredTextScale(84f), 0.001f);
    }

    @Test
    public void preservesReadableSpaceInCompactStepButtons() {
        assertTrue(ButtonTextLayout.compactContentWidth(34f, 34f) >= 24f);
        assertTrue(ButtonTextLayout.compactContentHeight(34f) >= 22f);
        assertTrue(ButtonTextLayout.compactTextScale(34f) >= 1.25f);
    }

    @Test
    public void centersSingleLineTextByItsVisibleCapHeight() {
        assertEquals(
                37.75f,
                ButtonTextLayout.centeredBaseline(0f, 56f, 15f, 1.30f),
                0.001f);
        assertEquals(
                50f,
                ButtonTextLayout.centeredBaseline(0f, 76f, 16f, 1.50f),
                0.001f);
    }

    @Test
    public void emptyBoundsHaveNoContentArea() {
        assertEquals(0f, ButtonTextLayout.contentWidth(0f, 56f), 0.001f);
        assertEquals(0f, ButtonTextLayout.contentHeight(0f), 0.001f);
    }
}
