package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AutomaticRecoveryManeuverTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void recoveryRunsTurnDriveAlignInOrder() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(12f);

        assertEquals(
                AutomaticRecoveryManeuver.Phase.TURN_TO_TARGET,
                maneuver.getPhase());

        update(maneuver, 12f, 0.99f, 0f);
        assertEquals(
                AutomaticRecoveryManeuver.Phase.DRIVE_TO_TARGET,
                maneuver.getPhase());

        update(maneuver, 0.5f, 1f, 0f);
        assertEquals(
                AutomaticRecoveryManeuver.Phase.ALIGN_TO_ROUTE,
                maneuver.getPhase());

        update(maneuver, 0.5f, 1f, 0.99f);
        assertEquals(AutomaticRecoveryManeuver.Phase.IDLE, maneuver.getPhase());
        assertFalse(maneuver.isActive());
    }

    @Test
    public void driveProgressPreventsExplosion() {
        AutomaticRecoveryManeuver maneuver = drivingManeuver(8f);

        for (int i = 1; i <= 20; i++) {
            maneuver.update(0.15f, 8f - i * 0.12f, 1f, 0f, 0.8f, 1.5f);
        }

        assertFalse(maneuver.consumeExplosionRequest());
        assertEquals(
                AutomaticRecoveryManeuver.Phase.DRIVE_TO_TARGET,
                maneuver.getPhase());
    }

    @Test
    public void blockedDriveRequestsOneExplosionPerTimeout() {
        AutomaticRecoveryManeuver maneuver = drivingManeuver(8f);

        maneuver.update(1.49f, 8f, 1f, 0f, 0.8f, 1.5f);
        assertFalse(maneuver.consumeExplosionRequest());

        maneuver.update(0.02f, 8f, 1f, 0f, 0.8f, 1.5f);
        assertTrue(maneuver.consumeExplosionRequest());
        assertFalse(maneuver.consumeExplosionRequest());
    }

    @Test
    public void stalledTurningAndAlignmentRequestExplosion() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(8f);
        maneuver.update(0f, 8f, 0f, 0f, 0.8f, 1.5f);
        maneuver.update(10f, 8f, 0f, 0f, 0.8f, 1.5f);
        assertTrue(maneuver.consumeExplosionRequest());

        update(maneuver, 8f, 1f, 0f);
        update(maneuver, 0.5f, 1f, 0f);
        maneuver.update(10f, 0.5f, 1f, 0f, 0.8f, 1.5f);
        assertTrue(maneuver.consumeExplosionRequest());
    }

    @Test
    public void targetIsTwoAndHalfPercentAheadOnTheRoute() {
        assertEquals(
                35f,
                AutomaticRecoveryManeuver.targetRouteProgress(10f, 1000f),
                EPSILON);
        assertEquals(
                10f,
                AutomaticRecoveryManeuver.targetRouteProgress(10f, -1000f),
                EPSILON);
    }

    @Test
    public void recoverySteeringUsesNormalLeftAndRightControlInputs() {
        assertEquals(-1f, AutomaticRecoveryManeuver.steeringToward(0f, 1f), EPSILON);
        assertEquals(1f, AutomaticRecoveryManeuver.steeringToward(0f, -1f), EPSILON);
        assertEquals(0f, AutomaticRecoveryManeuver.steeringToward(1f, 0f), EPSILON);
        assertEquals(-1f, AutomaticRecoveryManeuver.steeringToward(-1f, 0f), EPSILON);
        assertEquals(0f,
                AutomaticRecoveryManeuver.steeringToward(Float.NaN, 0f),
                EPSILON);
    }

    @Test
    public void recoveryYieldsToTheModelAsSoonAsAssistanceReachesSafeRoad() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(8f);

        assertFalse(maneuver.beginModelHandoffIfReady(0.5f, false, 1f, 0.35f));
        assertTrue(maneuver.beginModelHandoffIfReady(0f, true, 1f, 0.35f));
        assertTrue(maneuver.isModelHandoff());
        assertEquals(
                AutomaticRecoveryManeuver.Phase.MODEL_HANDOFF,
                maneuver.getPhase());
    }

    @Test
    public void recoveryKeepsControlOutsideTheSeventyDegreeRouteCone() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(8f);

        float alignmentAtSeventyDegrees =
                (float) Math.cos(Math.toRadians(70f));
        float alignmentAtSeventyOneDegrees =
                (float) Math.cos(Math.toRadians(71f));

        assertFalse(maneuver.beginModelHandoffIfReady(
                0.35f,
                true,
                alignmentAtSeventyOneDegrees,
                0.35f));
        assertFalse(maneuver.isModelHandoff());
        assertTrue(maneuver.beginModelHandoffIfReady(
                0f,
                true,
                alignmentAtSeventyDegrees,
                0.35f));
        assertTrue(maneuver.isModelHandoff());
    }

    @Test
    public void invalidRouteAlignmentCannotStartModelHandoff() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(8f);

        assertFalse(maneuver.beginModelHandoffIfReady(
                0.35f,
                true,
                Float.NaN,
                0.35f));
        assertFalse(maneuver.isModelHandoff());
    }

    @Test
    public void deliberateDebuffStopDisablesAutomaticRecovery() {
        assertFalse(AutomaticRecoveryManeuver.isControlAllowed(true, true));
        assertTrue(AutomaticRecoveryManeuver.isControlAllowed(true, false));
        assertFalse(AutomaticRecoveryManeuver.isControlAllowed(false, false));
    }

    @Test
    public void failedModelHandoffResumesAssistanceUntilTheModelMakesProgress() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(8f);
        assertTrue(maneuver.beginModelHandoffIfReady(0.35f, true, 1f, 0.35f));

        assertEquals(
                AutomaticRecoveryManeuver.ModelHandoffResult.YIELDING,
                maneuver.updateModelHandoff(1.4f, false, true, 1.5f));
        assertEquals(
                AutomaticRecoveryManeuver.ModelHandoffResult.RESUME_ASSISTANCE,
                maneuver.updateModelHandoff(0.2f, false, true, 1.5f));
        assertFalse(maneuver.isActive());

        maneuver.begin(6f);
        assertTrue(maneuver.beginModelHandoffIfReady(0.35f, true, 1f, 0.35f));
        assertEquals(
                AutomaticRecoveryManeuver.ModelHandoffResult.COMPLETED,
                maneuver.updateModelHandoff(0.1f, true, true, 1.5f));
        assertFalse(maneuver.isActive());
    }

    @Test
    public void modelHandoffResumesAssistanceImmediatelyAfterLeavingTheRoad() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(8f);
        assertTrue(maneuver.beginModelHandoffIfReady(0.35f, true, 1f, 0.35f));

        assertEquals(
                AutomaticRecoveryManeuver.ModelHandoffResult.RESUME_ASSISTANCE,
                maneuver.updateModelHandoff(0.1f, false, false, 1.5f));
        assertFalse(maneuver.isActive());
    }

    @Test
    public void recoveryPolicyFallsBackAfterMakingNoUsefulProgress() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(8f);
        maneuver.beginPolicyAttempt(8f, 0f);

        assertFalse(maneuver.shouldFallbackFromPolicy(
                2.9f, 8f, 0f, 0.2f, 0.03f, 3f, 10f));
        assertTrue(maneuver.shouldFallbackFromPolicy(
                0.11f, 8f, 0f, 0.2f, 0.03f, 3f, 10f));
    }

    @Test
    public void usefulPolicyProgressRestartsTheStallWindow() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(8f);
        maneuver.beginPolicyAttempt(8f, 0f);

        assertFalse(maneuver.shouldFallbackFromPolicy(
                2f, 8f, 0f, 0.2f, 0.03f, 3f, 10f));
        assertFalse(maneuver.shouldFallbackFromPolicy(
                0.1f, 7.7f, 0f, 0.2f, 0.03f, 3f, 10f));
        assertFalse(maneuver.shouldFallbackFromPolicy(
                2.9f, 7.7f, 0f, 0.2f, 0.03f, 3f, 10f));
        assertTrue(maneuver.shouldFallbackFromPolicy(
                0.11f, 7.7f, 0f, 0.2f, 0.03f, 3f, 10f));
    }

    @Test
    public void recoveryPolicyHasAnAbsoluteAttemptLimit() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(20f);
        maneuver.beginPolicyAttempt(20f, -1f);

        for (int i = 0; i < 9; i++) {
            assertFalse(maneuver.shouldFallbackFromPolicy(
                    1f,
                    20f - (i + 1) * 0.3f,
                    -1f + (i + 1) * 0.04f,
                    0.2f,
                    0.03f,
                    3f,
                    10f));
        }
        assertTrue(maneuver.shouldFallbackFromPolicy(
                1f, 17f, -0.6f, 0.2f, 0.03f, 3f, 10f));
    }

    @Test
    public void scriptedRecoveryRetriesPolicyAfterImprovingDistance() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(8f);
        maneuver.beginScriptedPolicyRetry(8f, 0f);

        assertFalse(maneuver.shouldRetryPolicyAfterScriptedAssist(
                1f, 6f, 0f, 1.25f, 1f, 0.15f));
        assertTrue(maneuver.shouldRetryPolicyAfterScriptedAssist(
                0.3f, 6f, 0f, 1.25f, 1f, 0.15f));
    }

    @Test
    public void scriptedRecoveryRetriesPolicyAfterImprovingHeading() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(8f);
        maneuver.beginScriptedPolicyRetry(8f, -0.8f);

        assertTrue(maneuver.shouldRetryPolicyAfterScriptedAssist(
                1.3f, 8f, -0.6f, 1.25f, 1f, 0.15f));
    }

    @Test
    public void scriptedRecoveryKeepsControlWithoutUsefulImprovement() {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(8f);
        maneuver.beginScriptedPolicyRetry(8f, 0f);

        assertFalse(maneuver.shouldRetryPolicyAfterScriptedAssist(
                10f, 7.5f, 0.1f, 1.25f, 1f, 0.15f));
    }

    private static AutomaticRecoveryManeuver drivingManeuver(float distance) {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        maneuver.begin(distance);
        update(maneuver, distance, 1f, 0f);
        return maneuver;
    }

    private static void update(
            AutomaticRecoveryManeuver maneuver,
            float targetDistance,
            float targetAlignment,
            float routeAlignment) {
        maneuver.update(
                0.05f,
                targetDistance,
                targetAlignment,
                routeAlignment,
                0.8f,
                1.5f);
    }
}
