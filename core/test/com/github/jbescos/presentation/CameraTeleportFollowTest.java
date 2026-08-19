package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.badlogic.gdx.math.Vector2;
import org.junit.Test;

public class CameraTeleportFollowTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void movesDirectlyToTeleportedCarWithVelocityLookAhead() {
        Vector2 output = new Vector2(-10f, -20f);

        assertTrue(CameraTeleportFollow.snapTarget(
                true,
                false,
                true,
                output,
                new Vector2(30f, 40f),
                new Vector2(4f, -2f),
                0.5f,
                10f));

        assertEquals(32f, output.x, EPSILON);
        assertEquals(39f, output.y, EPSILON);
    }

    @Test
    public void clampsVelocityLookAheadWithoutChangingItsDirection() {
        Vector2 output = new Vector2();

        assertTrue(CameraTeleportFollow.snapTarget(
                true,
                false,
                true,
                output,
                new Vector2(10f, 20f),
                new Vector2(6f, 8f),
                1f,
                5f));

        assertEquals(13f, output.x, EPSILON);
        assertEquals(24f, output.y, EPSILON);
    }

    @Test
    public void leavesCameraAloneOutsideFollowPresentation() {
        Vector2 output = new Vector2(7f, 9f);
        Vector2 position = new Vector2(30f, 40f);
        Vector2 velocity = new Vector2(1f, 2f);

        assertFalse(CameraTeleportFollow.snapTarget(
                false, false, true, output, position, velocity, 1f, 10f));
        assertFalse(CameraTeleportFollow.snapTarget(
                true, true, true, output, position, velocity, 1f, 10f));
        assertFalse(CameraTeleportFollow.snapTarget(
                true, false, false, output, position, velocity, 1f, 10f));

        assertEquals(7f, output.x, EPSILON);
        assertEquals(9f, output.y, EPSILON);
    }

    @Test
    public void rejectsNonFiniteCarTransforms() {
        Vector2 output = new Vector2(7f, 9f);

        assertFalse(CameraTeleportFollow.snapTarget(
                true,
                false,
                true,
                output,
                new Vector2(Float.NaN, 2f),
                new Vector2(1f, 2f),
                1f,
                10f));

        assertEquals(7f, output.x, EPSILON);
        assertEquals(9f, output.y, EPSILON);
    }
}
