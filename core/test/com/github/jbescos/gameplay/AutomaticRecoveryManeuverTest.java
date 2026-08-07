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
    public void repeatedFailedManeuversKeepReplanningWithOppositeGear() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(4f, -0.2f);

        maneuver.update(0.90f, 4f, -0.2f);
        assertTrue(maneuver.consumeReplanRequest());
        assertTrue(maneuver.isReversing());

        maneuver.retarget(4f, -0.2f);
        maneuver.update(0.90f, 4f, -0.2f);
        assertTrue(maneuver.consumeReplanRequest());
        assertFalse(maneuver.isReversing());
    }

    @Test
    public void continuousReverseCommitsToATurnaroundUsingActualMotionDirection() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(6f, -1f);

        for (int i = 1; i <= 45; i++) {
            maneuver.update(0.05f, 6f - i * 0.03f, -1f);
        }

        assertFalse(maneuver.isReversing());
        assertTrue(maneuver.calculateTurn(0.2f, -1f) > 0f);
        assertTrue(maneuver.calculateTurn(0.2f, 1f) < 0f);
    }

    @Test
    public void usefulDistanceProgressPreventsEscalation() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(4f, 0.8f);

        for (int i = 1; i <= 8; i++) {
            maneuver.update(0.25f, 4f - i * 0.15f, 0.8f);
        }

        assertFalse(maneuver.consumeReplanRequest());
    }

    @Test
    public void fastApproachBrakesBeforeCrossingTheTarget() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();

        assertEquals(
                -1f,
                maneuver.limitApproachThrottle(
                        0.8f,
                        9f,
                        9f,
                        3f,
                        4f,
                        0.8f,
                        6f,
                        8f),
                EPSILON);
        assertEquals(
                1f,
                maneuver.limitApproachThrottle(
                        -0.6f,
                        -9f,
                        9f,
                        3f,
                        4f,
                        0.8f,
                        6f,
                        8f),
                EPSILON);
    }

    @Test
    public void controlledApproachTapersThrottleNearTheTarget() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();

        float farThrottle =
                maneuver.limitApproachThrottle(
                        0.8f,
                        2f,
                        2f,
                        8f,
                        0.4f,
                        0.8f,
                        6f,
                        8f);
        float nearThrottle =
                maneuver.limitApproachThrottle(
                        0.8f,
                        2f,
                        2f,
                        2f,
                        0.4f,
                        0.8f,
                        6f,
                        8f);

        assertEquals(0.8f, farThrottle, EPSILON);
        assertTrue(nearThrottle > 0f);
        assertTrue(nearThrottle < farThrottle);
    }

    @Test
    public void contactRecoveryStopsWhenTheBlockingConditionClears() {
        assertTrue(AutomaticRecoveryManeuver.requiresContactRecovery(1, false));
        assertFalse(AutomaticRecoveryManeuver.requiresContactRecovery(0, false));
        assertFalse(AutomaticRecoveryManeuver.requiresContactRecovery(1, true));
    }

    @Test
    public void forwardNudgeTakesTheShortestTurnTowardTheRoute() {
        assertEquals(
                0f,
                AutomaticRecoveryManeuver.calculateShortestForwardTurn(1f, 0f),
                EPSILON);
        assertTrue(
                AutomaticRecoveryManeuver.calculateShortestForwardTurn(0.5f, 0.5f) < 0f);
        assertTrue(
                AutomaticRecoveryManeuver.calculateShortestForwardTurn(0.5f, -0.5f) > 0f);
        assertEquals(
                -1f,
                AutomaticRecoveryManeuver.calculateShortestForwardTurn(-0.5f, 0.5f),
                EPSILON);
        assertEquals(
                1f,
                AutomaticRecoveryManeuver.calculateShortestForwardTurn(-0.5f, -0.5f),
                EPSILON);
    }

    @Test
    public void forwardNudgeIgnoresInvalidRouteDirections() {
        assertEquals(
                0f,
                AutomaticRecoveryManeuver.calculateShortestForwardTurn(Float.NaN, 0f),
                EPSILON);
        assertEquals(
                0f,
                AutomaticRecoveryManeuver.calculateShortestForwardTurn(0f, Float.POSITIVE_INFINITY),
                EPSILON);
    }

    @Test
    public void offRoadRecoveryStopsOnlyAtASafeOnRoadHandoff() {
        assertTrue(AutomaticRecoveryManeuver.requiresOffRoadRecovery(true, false));
        assertTrue(AutomaticRecoveryManeuver.requiresOffRoadRecovery(true, true));
        assertTrue(AutomaticRecoveryManeuver.requiresOffRoadRecovery(false, false));
        assertFalse(AutomaticRecoveryManeuver.requiresOffRoadRecovery(false, true));
    }

    @Test
    public void handoffRequiresRoadMarginControlledSpeedAndForwardAlignment() {
        assertTrue(safeHandoff(0.95f, 4f, 2f));
        assertFalse(safeHandoff(0.70f, 4f, 2f));
        assertFalse(safeHandoff(0.95f, 9f, 2f));
        assertFalse(safeHandoff(0.95f, 4f, -0.2f));
        assertFalse(
                AutomaticRecoveryManeuver.isSafeDirectionalHandoff(
                        false,
                        2f,
                        1f,
                        4f,
                        6f,
                        0.95f,
                        0.86f,
                        2f,
                        0.5f));
    }

    private static boolean safeHandoff(
            float routeAlignment, float speed, float routeForwardSpeed) {
        return AutomaticRecoveryManeuver.isSafeDirectionalHandoff(
                true,
                2f,
                1f,
                speed,
                6f,
                routeAlignment,
                0.86f,
                routeForwardSpeed,
                0.5f);
    }
}
