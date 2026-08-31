package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.badlogic.gdx.math.Vector2;
import org.junit.Test;

public final class ManualDrivingCameraTest {
    @Test
    public void onlyManualCameraAddsHighSpeedEmphasis() {
        assertEquals(0f, ManualDrivingCamera.additionalZoom(false, 1f), 0.0001f);
        assertEquals(0f, ManualDrivingCamera.additionalLookAhead(false, 1f), 0.0001f);
        assertTrue(ManualDrivingCamera.additionalZoom(true, 1f) > 0f);
        assertTrue(ManualDrivingCamera.additionalLookAhead(true, 1f) > 0f);
    }

    @Test
    public void emphasisGrowsQuadraticallyAndClampsSpeed() {
        float halfSpeedZoom = ManualDrivingCamera.additionalZoom(true, 0.5f);
        float fullSpeedZoom = ManualDrivingCamera.additionalZoom(true, 1f);
        assertEquals(fullSpeedZoom * 0.25f, halfSpeedZoom, 0.0001f);
        assertEquals(fullSpeedZoom, ManualDrivingCamera.additionalZoom(true, 2f), 0.0001f);
    }

    @Test
    public void manualCameraKeepsTrackVisibleAheadEvenWhenStopped() {
        assertEquals(12.00f, ManualDrivingCamera.additionalLookAhead(true, 0f), 0.0001f);
        assertEquals(24.00f, ManualDrivingCamera.additionalLookAhead(true, 1f), 0.0001f);
        assertEquals(
                6.9264f,
                ManualDrivingCamera.maximumLookAhead(
                        true,
                        2.35f,
                        0f,
                        1f,
                        32f,
                        18f,
                        1.04f),
                0.0001f);
    }

    @Test
    public void manualCameraUsesMostOfTheVisibleDistanceForTheRoadAhead() {
        assertEquals(
                11.988f,
                ManualDrivingCamera.maximumLookAhead(
                        true,
                        2.35f,
                        0f,
                        1f,
                        32f,
                        18f,
                        1.8f),
                0.0001f);
        assertEquals(
                21.312f,
                ManualDrivingCamera.maximumLookAhead(
                        true,
                        2.35f,
                        1f,
                        0f,
                        32f,
                        18f,
                        1.8f),
                0.0001f);
    }

    @Test
    public void routeLeadScalesWithTheFinalCameraZoom() {
        float zoomedIn =
                ManualDrivingCamera.maximumLookAhead(
                        true,
                        2.35f,
                        0f,
                        1f,
                        32f,
                        18f,
                        0.72f);
        float zoomedOut =
                ManualDrivingCamera.maximumLookAhead(
                        true,
                        2.35f,
                        0f,
                        1f,
                        32f,
                        18f,
                        1.8f);

        assertEquals(4.7952f, zoomedIn, 0.0001f);
        assertEquals(11.988f, zoomedOut, 0.0001f);
        assertTrue(zoomedIn < zoomedOut);
    }

    @Test
    public void extremeZoomKeepsTheWholeCarBehindTheCameraTarget() {
        float visibleHalfDistance = 18f * 0.416f * 0.5f;
        float carLength = 1.58f;
        float lookAhead =
                ManualDrivingCamera.maximumLookAhead(
                        true,
                        2.35f,
                        0f,
                        1f,
                        32f,
                        18f,
                        0.416f,
                        carLength);

        assertEquals(carLength, visibleHalfDistance - lookAhead, 0.0001f);
    }

    @Test
    public void automaticCameraKeepsItsExistingMaximumLookAhead() {
        assertEquals(
                2.35f,
                ManualDrivingCamera.maximumLookAhead(
                        false,
                        2.35f,
                        1f,
                        0f,
                        32f,
                        18f,
                        1.8f),
                0.0001f);
    }

    @Test
    public void routePointControlsCameraDirectionInsteadOfCarAngle() {
        Vector2 output = new Vector2();

        assertTrue(
                ManualDrivingCamera.aimAtRoutePoint(
                        output,
                        new Vector2(4f, 3f),
                        new Vector2(4f, 13f),
                        8f));

        assertEquals(0f, output.x, 0.0001f);
        assertEquals(8f, output.y, 0.0001f);
    }

    @Test
    public void coincidentRoutePointDoesNotCreateInvalidCameraDirection() {
        Vector2 output = new Vector2(2f, 3f);
        Vector2 position = new Vector2(5f, 7f);

        assertFalse(ManualDrivingCamera.aimAtRoutePoint(output, position, position, 8f));
        assertEquals(0f, output.len2(), 0.0001f);
    }

    @Test
    public void routeLookAheadFollowsSignedMovement() {
        assertEquals(1f, ManualDrivingCamera.routeLookAheadSign(8f, -1f), 0.0001f);
        assertEquals(-1f, ManualDrivingCamera.routeLookAheadSign(-8f, 1f), 0.0001f);
    }

    @Test
    public void routeLookAheadUsesHeadingAtLowSpeed() {
        assertEquals(1f, ManualDrivingCamera.routeLookAheadSign(0f, 0.8f), 0.0001f);
        assertEquals(-1f, ManualDrivingCamera.routeLookAheadSign(0f, -0.8f), 0.0001f);
    }

    @Test
    public void directionReversalMovesLookAheadProgressivelyThroughCenter() {
        Vector2 current = new Vector2(0f, 12f);
        Vector2 target = new Vector2(0f, -12f);

        ManualDrivingCamera.smoothLookAhead(current, target, 1f / 30f);

        assertTrue(current.y < 12f);
        assertTrue(current.y > -12f);

        for (int i = 0; i < 90; i++) {
            ManualDrivingCamera.smoothLookAhead(current, target, 1f / 30f);
        }
        assertEquals(-12f, current.y, 0.001f);
    }
}
