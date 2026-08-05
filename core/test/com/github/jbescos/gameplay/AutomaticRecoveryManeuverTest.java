package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AutomaticRecoveryManeuverTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void gearSelectionUsesHysteresisAroundSidewaysTargets() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(4f, -0.8f);

        assertTrue(maneuver.isReversing());

        maneuver.update(0.1f, 3.9f, -0.20f);
        assertTrue(maneuver.isReversing());

        maneuver.update(0.1f, 3.8f, 0.20f);
        assertFalse(maneuver.isReversing());
    }

    @Test
    public void sidewaysTargetUsesLessThrottleWhileKeepingStrongSteering() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(4f, 1f);

        float alignedThrottle = maneuver.calculateThrottle(1f, 0.86f, 0.62f);
        float sidewaysThrottle = maneuver.calculateThrottle(0f, 0.86f, 0.62f);

        assertEquals(0.86f, alignedThrottle, EPSILON);
        assertTrue(sidewaysThrottle > 0f);
        assertTrue(sidewaysThrottle < alignedThrottle * 0.5f);
        assertEquals(-1f, maneuver.calculateTurn(1f), EPSILON);
    }

    @Test
    public void repeatedFailedManeuversEscalateToRelocation() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(4f, 0.2f);

        maneuver.update(0.90f, 4f, 0.2f);
        assertTrue(maneuver.consumeReplanRequest());
        assertFalse(maneuver.isRelocationRequested());

        maneuver.retarget(4f, 0.2f);
        maneuver.update(0.90f, 4f, 0.2f);
        assertTrue(maneuver.consumeReplanRequest());
        assertTrue(maneuver.isRelocationRequested());
    }

    @Test
    public void usefulDistanceProgressPreventsEscalation() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(4f, 0.8f);

        for (int i = 1; i <= 8; i++) {
            maneuver.update(0.25f, 4f - i * 0.15f, 0.8f);
        }

        assertFalse(maneuver.consumeReplanRequest());
        assertFalse(maneuver.isRelocationRequested());
    }
}
