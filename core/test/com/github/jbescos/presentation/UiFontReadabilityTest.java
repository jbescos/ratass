package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class UiFontReadabilityTest {
    @Test
    public void desktopDoesNotReceiveMobileFontBoost() {
        assertEquals(
                1f,
                UiFontReadability.scaleForDisplay(false, 1920, 1080, 96f, 96f),
                0.0001f);
    }

    @Test
    public void phoneReceivesMaximumReadableFontBoost() {
        assertEquals(
                UiFontReadability.PHONE_SCALE,
                UiFontReadability.scaleForDisplay(true, 2400, 1080, 395f, 395f),
                0.0001f);
    }

    @Test
    public void tabletUsesSmallerBoostThanPhone() {
        float tabletScale =
                UiFontReadability.scaleForDisplay(true, 2560, 1600, 274f, 274f);

        assertTrue(tabletScale < UiFontReadability.PHONE_SCALE);
        assertTrue(tabletScale >= UiFontReadability.LARGE_SCREEN_SCALE);
    }

    @Test
    public void unknownMobileDensityUsesConservativeBoost() {
        float scale =
                UiFontReadability.scaleForDisplay(true, 1920, 1080, 0f, 0f);

        assertTrue(scale > 1f);
        assertTrue(scale < UiFontReadability.PHONE_SCALE);
    }
}
