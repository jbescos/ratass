package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LongStraightThrottleAssistTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void holdsFullThrottleOnAlignedLongStraight() {
        assertTrue(
                LongStraightThrottleAssist.shouldForceFullThrottle(
                        0.995f,
                        0.01f,
                        40f,
                        0f,
                        0.02f,
                        2.35f));
    }

    @Test
    public void yieldsBeforeCornerRequiresBraking() {
        assertFalse(
                LongStraightThrottleAssist.shouldForceFullThrottle(
                        0.995f,
                        0.01f,
                        20f,
                        0.3f,
                        0.02f,
                        2.35f));
    }

    @Test
    public void yieldsWhenCarIsTurningOrSliding() {
        assertFalse(
                LongStraightThrottleAssist.shouldForceFullThrottle(
                        0.95f,
                        0.01f,
                        40f,
                        0f,
                        0.02f,
                        2.35f));
        assertFalse(
                LongStraightThrottleAssist.shouldForceFullThrottle(
                        0.995f,
                        0.08f,
                        40f,
                        0f,
                        0.02f,
                        2.35f));
        assertFalse(
                LongStraightThrottleAssist.shouldForceFullThrottle(
                        0.995f,
                        0.01f,
                        40f,
                        0f,
                        0.20f,
                        2.35f));
    }

    @Test
    public void ignoresBrakeDemandWhenCornerDistanceIsSaturated() {
        assertEquals(
                0f,
                LongStraightThrottleAssist.effectiveBrakeDemand(44f, 44f, 0.8f),
                EPSILON);
        assertEquals(
                0.8f,
                LongStraightThrottleAssist.effectiveBrakeDemand(30f, 44f, 0.8f),
                EPSILON);
    }
}
