package com.github.jbescos.gameplay;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FaceToFaceDeadlockDetectorTest {
    @Test
    public void sustainedNoseToNoseContactTriggersExplosion() {
        FaceToFaceDeadlockDetector detector =
                new FaceToFaceDeadlockDetector(2f, 0.75f, 2f);

        assertTrue(detector.isDeadlocked(0.2f, 0.3f, -0.9f, 0.9f, 0.8f));
        assertFalse(detector.update(0.30f, 4, true));
        assertFalse(detector.update(0.30f, 4, true));
        assertTrue(detector.update(0.15f, 4, true));
    }

    @Test
    public void movementOrNonOpposingContactDoesNotCountAsDeadlock() {
        FaceToFaceDeadlockDetector detector =
                new FaceToFaceDeadlockDetector(2f, 0.75f, 2f);

        assertFalse(detector.isDeadlocked(2.1f, 0.2f, -0.9f, 0.9f, 0.9f));
        assertFalse(detector.isDeadlocked(0.2f, 0.2f, 0.1f, 0.9f, 0.9f));
        assertFalse(detector.isDeadlocked(0.2f, 0.2f, -0.9f, -0.9f, 0.9f));
        assertFalse(detector.update(1f, -1, false));
    }

    @Test
    public void changingOpponentResetsTimerAndCooldownPreventsRetrigger() {
        FaceToFaceDeadlockDetector detector =
                new FaceToFaceDeadlockDetector(2f, 0.50f, 1f);

        assertFalse(detector.update(0.30f, 2, true));
        assertFalse(detector.update(0.30f, 3, true));
        assertTrue(detector.update(0.20f, 3, true));
        assertFalse(detector.update(0.60f, 3, true));
        assertFalse(detector.update(0.40f, 3, true));
        assertFalse(detector.update(0.30f, 3, true));
        assertTrue(detector.update(0.20f, 3, true));
    }
}
