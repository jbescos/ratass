package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CameraTargetTransitionTest {
    @Test
    public void startsMovingOnTheFirstRenderedFrame() {
        assertEquals(0f, CameraTargetTransition.nextEasedProgress(0.8f, 0.8f, 0f), 0f);
        assertTrue(CameraTargetTransition.nextEasedProgress(0.8f, 0.8f, 1f / 60f) > 0f);
    }

    @Test
    public void isHalfwayAtHalfTheDuration() {
        assertEquals(
                0.5f,
                CameraTargetTransition.nextEasedProgress(0.5f, 1f, 0f),
                0.0001f);
    }

    @Test
    public void landsExactlyOnTheLiveTargetAtTheEnd() {
        assertEquals(
                1f,
                CameraTargetTransition.nextEasedProgress(0.01f, 0.8f, 0.02f),
                0f);
        assertEquals(1f, CameraTargetTransition.nextEasedProgress(0f, 0.8f, 0f), 0f);
        assertEquals(1f, CameraTargetTransition.nextEasedProgress(1f, 0f, 0f), 0f);
    }

    @Test
    public void negativeFrameTimeCannotMoveTheTransitionBackwards() {
        assertEquals(
                0.5f,
                CameraTargetTransition.nextEasedProgress(0.5f, 1f, -1f),
                0.0001f);
    }
}
