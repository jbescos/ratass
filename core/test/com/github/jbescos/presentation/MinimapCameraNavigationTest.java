package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import org.junit.Test;

public class MinimapCameraNavigationTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void wideMapIsCenteredVerticallyInPanel() {
        Rectangle fitted = new Rectangle();

        assertTrue(MinimapCameraNavigation.fitMap(
                new Rectangle(10f, 20f, 220f, 120f),
                10f,
                new Rectangle(-100f, -50f, 400f, 100f),
                fitted));

        assertEquals(20f, fitted.x, EPSILON);
        assertEquals(55f, fitted.y, EPSILON);
        assertEquals(200f, fitted.width, EPSILON);
        assertEquals(50f, fitted.height, EPSILON);
    }

    @Test
    public void minimapPointMapsToWorldCoordinates() {
        Rectangle fitted = new Rectangle(20f, 65f, 200f, 50f);
        Rectangle world = new Rectangle(-100f, -50f, 400f, 100f);
        Vector2 target = new Vector2();

        assertTrue(MinimapCameraNavigation.worldPositionAt(
                170f, 77.5f, fitted, world, target));

        assertEquals(200f, target.x, EPSILON);
        assertEquals(-25f, target.y, EPSILON);
    }

    @Test
    public void letterboxedPanelAreaIsNotClickable() {
        Rectangle fitted = new Rectangle(20f, 65f, 200f, 50f);

        assertFalse(MinimapCameraNavigation.worldPositionAt(
                100f,
                40f,
                fitted,
                new Rectangle(-100f, -50f, 400f, 100f),
                new Vector2()));
    }
}
