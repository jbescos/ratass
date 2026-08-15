package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.badlogic.gdx.math.Vector2;
import org.junit.Test;

public final class HybridPlayerControlTest {
    @Test
    public void oppositeDirectionsNeutralizeOnlyTheRequestedAxis() {
        assertEquals(1f, HybridPlayerControl.digitalAxis(true, false), 0.0001f);
        assertEquals(-1f, HybridPlayerControl.digitalAxis(false, true), 0.0001f);
        assertEquals(0f, HybridPlayerControl.digitalAxis(true, true), 0.0001f);
        assertEquals(0f, HybridPlayerControl.digitalAxis(false, false), 0.0001f);
    }

    @Test
    public void activePlayerAxisReplacesAutomaticAxis() {
        assertEquals(
                -0.75f,
                HybridPlayerControl.overrideAxis(0.6f, -0.75f, true, true),
                0.0001f);
        assertEquals(
                0f,
                HybridPlayerControl.overrideAxis(0.6f, 0f, true, true),
                0.0001f);
        assertEquals(
                0.6f,
                HybridPlayerControl.overrideAxis(0.6f, 0f, false, true),
                0.0001f);
    }

    @Test
    public void disallowedTakeoverKeepsAutomaticAction() {
        assertEquals(
                0.6f,
                HybridPlayerControl.overrideAxis(0.6f, -1f, true, false),
                0.0001f);
    }

    @Test
    public void enablesTakeoverForNormalRaceAndAutomaticSandbox() {
        assertEquals(true, HybridPlayerControl.isSteeringTakeoverModeEnabled(false, false));
        assertEquals(true, HybridPlayerControl.isSteeringTakeoverModeEnabled(true, true));
        assertEquals(false, HybridPlayerControl.isSteeringTakeoverModeEnabled(true, false));
    }

    @Test
    public void rejectsTakeoverOffRoadAndInsideIntersectionHints() {
        ArenaMap map =
                ArenaMap.builder("intersection", "Intersection")
                        .solid(ArenaShape.rectangle(0f, 0f, 20f, 20f))
                        .spawn(new SpawnPoint(0f, 0f, 0f))
                        .manualSteeringRestrictedZone(2f, 3f, 2f)
                        .build();

        assertTrue(
                HybridPlayerControl.isSteeringTakeoverAllowed(
                        true, map, new Vector2(-5f, -5f)));
        assertFalse(
                HybridPlayerControl.isSteeringTakeoverAllowed(
                        true, map, new Vector2(2f, 3f)));
        assertFalse(
                HybridPlayerControl.isSteeringTakeoverAllowed(
                        true, map, new Vector2(20f, 20f)));
    }
}
