package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ManualShortcutGuardTest {
    @Test
    public void acceptsSmallForwardMovementWhileOffRoad() {
        ManualShortcutGuard guard = new ManualShortcutGuard();

        assertFalse(guard.update(true, true, 25f, 100f));
        assertFalse(guard.update(true, false, 26f, 100f));
        assertFalse(guard.update(true, true, 27.9f, 100f));
        assertEquals(27.9f, guard.getTrustedProgress(), 0.0001f);
    }

    @Test
    public void rejectsLargeForwardShortcut() {
        ManualShortcutGuard guard = new ManualShortcutGuard();

        assertFalse(guard.update(true, true, 25f, 100f));
        assertFalse(guard.update(true, false, 30f, 100f));
        assertTrue(guard.update(true, true, 35f, 100f));
        assertEquals(25f, guard.getTrustedProgress(), 0.0001f);
    }

    @Test
    public void acceptsSmallMovementAcrossRouteWrap() {
        ManualShortcutGuard guard = new ManualShortcutGuard();

        assertFalse(guard.update(true, true, 98f, 100f));
        assertFalse(guard.update(true, false, 99f, 100f));
        assertFalse(guard.update(true, true, 0.5f, 100f));
    }

    @Test
    public void automaticReentryStillRejectsPendingManualShortcut() {
        ManualShortcutGuard guard = new ManualShortcutGuard();

        assertFalse(guard.update(true, true, 25f, 100f));
        assertFalse(guard.update(true, false, 26f, 100f));
        assertTrue(guard.isReentryPending());
        assertFalse(guard.update(false, false, 60f, 100f));
        assertTrue(guard.isReentryPending());
        assertTrue(guard.update(false, true, 80f, 100f));
        assertFalse(guard.isReentryPending());
        assertEquals(25f, guard.getTrustedProgress(), 0.0001f);
    }

    @Test
    public void automaticDrivingWithoutManualExcursionIsNotGuarded() {
        ManualShortcutGuard guard = new ManualShortcutGuard();

        assertFalse(guard.update(false, true, 25f, 100f));
        assertFalse(guard.update(false, false, 40f, 100f));
        assertFalse(guard.update(false, true, 80f, 100f));
        assertFalse(guard.isReentryPending());
    }

    @Test
    public void detectsLargeBackwardCrossingAtRouteStart() {
        assertTrue(ManualShortcutGuard.isLargeBackwardStartCrossing(
                1f, 99f, 0f, 100f));
    }

    @Test
    public void detectsLargeBackwardCrossingAtOffsetRaceStart() {
        assertTrue(ManualShortcutGuard.isLargeBackwardStartCrossing(
                21f, 19f, 20f, 100f));
    }

    @Test
    public void ignoresOrdinaryBackwardProgress() {
        assertFalse(ManualShortcutGuard.isLargeBackwardStartCrossing(
                45f, 40f, 20f, 100f));
    }
}
