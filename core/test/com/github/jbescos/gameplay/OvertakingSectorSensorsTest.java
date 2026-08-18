package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OvertakingSectorSensorsTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void classifiesAllRouteRelativeDirections() {
        assertEquals(OvertakingSectorSensors.FRONT, sector(2f, 0f));
        assertEquals(OvertakingSectorSensors.FRONT_LEFT, sector(2f, 1f));
        assertEquals(OvertakingSectorSensors.LEFT, sector(0f, 1f));
        assertEquals(OvertakingSectorSensors.REAR_LEFT, sector(-2f, 1f));
        assertEquals(OvertakingSectorSensors.REAR, sector(-2f, 0f));
        assertEquals(OvertakingSectorSensors.REAR_RIGHT, sector(-2f, -1f));
        assertEquals(OvertakingSectorSensors.RIGHT, sector(0f, -1f));
        assertEquals(OvertakingSectorSensors.FRONT_RIGHT, sector(2f, -1f));
    }

    @Test
    public void preservesLateralLaneInformationAtLongRange() {
        assertEquals(OvertakingSectorSensors.FRONT_LEFT, sector(20f, 0.6f));
        assertEquals(OvertakingSectorSensors.FRONT, sector(20f, 0.4f));
        assertEquals(OvertakingSectorSensors.FRONT_RIGHT, sector(20f, -0.6f));
    }

    @Test
    public void normalizesProximityInsideRange() {
        assertEquals(1f, OvertakingSectorSensors.proximity(0f, 10f), EPSILON);
        assertEquals(0.5f, OvertakingSectorSensors.proximity(5f, 10f), EPSILON);
        assertEquals(0f, OvertakingSectorSensors.proximity(10f, 10f), EPSILON);
    }

    @Test
    public void extendsDetectionRangeWithClosingSpeed() {
        assertEquals(
                10f,
                OvertakingSectorSensors.detectionRange(10f, 30f, -5f, 1f),
                EPSILON);
        assertEquals(
                20f,
                OvertakingSectorSensors.detectionRange(10f, 30f, 10f, 1f),
                EPSILON);
        assertEquals(
                30f,
                OvertakingSectorSensors.detectionRange(10f, 30f, 50f, 1f),
                EPSILON);
    }

    @Test
    public void emphasizesRelativeClosingSpeedWithoutChangingItsSign() {
        assertEquals(
                -0.5f,
                OvertakingSectorSensors.normalizedRelativeSpeed(-10f, 20f),
                EPSILON);
        assertEquals(
                1f,
                OvertakingSectorSensors.normalizedRelativeSpeed(30f, 20f),
                EPSILON);
    }

    @Test
    public void collisionRiskRewardsEarlyLateralSeparation() {
        assertEquals(
                0.5f,
                OvertakingSectorSensors.closingCollisionRisk(
                        6f, 0f, 4f, 2f, 2f, 2f),
                EPSILON);
        assertEquals(
                0.25f,
                OvertakingSectorSensors.closingCollisionRisk(
                        6f, 1f, 4f, 2f, 2f, 2f),
                EPSILON);
        assertEquals(
                0f,
                OvertakingSectorSensors.closingCollisionRisk(
                        6f, 2f, 4f, 2f, 2f, 2f),
                EPSILON);
        assertEquals(
                0f,
                OvertakingSectorSensors.closingCollisionRisk(
                        6f, 0f, -1f, 2f, 2f, 2f),
                EPSILON);
    }

    @Test
    public void closingThreatRequiresCleanSafeLateralClearance() {
        assertEquals(
                true,
                OvertakingSectorSensors.closingThreatResolved(false, 0.01f, 1.5f, 1.5f));
        assertEquals(
                false,
                OvertakingSectorSensors.closingThreatResolved(true, 0f, 2f, 1.5f));
        assertEquals(
                false,
                OvertakingSectorSensors.closingThreatResolved(false, 0.1f, 2f, 1.5f));
        assertEquals(
                false,
                OvertakingSectorSensors.closingThreatResolved(false, 0f, 1.49f, 1.5f));
    }

    @Test
    public void rearCarsDoNotKeepSteeringAssistanceActiveAfterPass() {
        assertEquals(true, OvertakingSectorSensors.isRelevantSteeringThreat(2f, 1f));
        assertEquals(true, OvertakingSectorSensors.isRelevantSteeringThreat(-1f, 1f));
        assertEquals(false, OvertakingSectorSensors.isRelevantSteeringThreat(-1.01f, 1f));
    }

    private static int sector(float forward, float side) {
        return OvertakingSectorSensors.sectorFor(forward, side, 1f, 0.5f);
    }

}
