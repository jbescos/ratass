package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PresentationViewportLayoutTest {
    @Test
    public void letterboxesWideAndroidPhoneAndUsesLargerLogicalUi() {
        PresentationViewportLayout.Layout layout =
                PresentationViewportLayout.fit(2400, 1080, true);

        assertLayout(layout, 240, 0, 1920, 1080, 1280f, 720f);
    }

    @Test
    public void letterboxesAndroidTabletVertically() {
        PresentationViewportLayout.Layout layout =
                PresentationViewportLayout.fit(2560, 1600, true);

        assertLayout(layout, 0, 80, 2560, 1440, 1280f, 720f);
    }

    @Test
    public void preservesDesktopPixelSizedUiInsideUltrawideFrame() {
        PresentationViewportLayout.Layout layout =
                PresentationViewportLayout.fit(3440, 1440, false);

        assertLayout(layout, 440, 0, 2560, 1440, 2560f, 1440f);
    }

    @Test
    public void leavesSixteenByNineDesktopUnchanged() {
        PresentationViewportLayout.Layout layout =
                PresentationViewportLayout.fit(1920, 1080, false);

        assertLayout(layout, 0, 0, 1920, 1080, 1920f, 1080f);
    }

    private static void assertLayout(
            PresentationViewportLayout.Layout layout,
            int screenX,
            int screenY,
            int screenWidth,
            int screenHeight,
            float logicalWidth,
            float logicalHeight) {
        assertEquals(screenX, layout.screenX);
        assertEquals(screenY, layout.screenY);
        assertEquals(screenWidth, layout.screenWidth);
        assertEquals(screenHeight, layout.screenHeight);
        assertEquals(logicalWidth, layout.logicalWidth, 0.001f);
        assertEquals(logicalHeight, layout.logicalHeight, 0.001f);
    }
}
