package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.badlogic.gdx.math.Vector2;
import org.junit.Test;

public class ImpactReversalBounceTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void sendsTheAttackerStronglyAwayFromTheProtectedCar() {
        Vector2 reflected = ImpactReversalBounce.calculateVelocity(
                new Vector2(),
                new Vector2(0f, -18f),
                new Vector2(0f, 1f),
                1f,
                18f,
                18f);

        assertEquals(0f, reflected.x, EPSILON);
        assertEquals(27f, reflected.y, EPSILON);
    }

    @Test
    public void heavilyReducesVelocityAcrossTheImpactNormal() {
        Vector2 reflected = ImpactReversalBounce.calculateVelocity(
                new Vector2(),
                new Vector2(20f, -10f),
                new Vector2(0f, 1f),
                1f,
                10f,
                10f);

        assertEquals(4f, reflected.x, EPSILON);
        assertEquals(15f, reflected.y, EPSILON);
    }

    @Test
    public void respectsTheOppositeContactNormalDirection() {
        Vector2 reflected = ImpactReversalBounce.calculateVelocity(
                new Vector2(),
                new Vector2(0f, 18f),
                new Vector2(0f, 1f),
                -1f,
                18f,
                18f);

        assertEquals(0f, reflected.x, EPSILON);
        assertEquals(-27f, reflected.y, EPSILON);
    }

    @Test
    public void hasAStrongMinimumAndABoundedMaximum() {
        assertEquals(
                ImpactReversalBounce.MIN_REBOUND_SPEED,
                ImpactReversalBounce.reboundSpeed(0.5f, 0.5f),
                EPSILON);
        assertEquals(
                ImpactReversalBounce.MAX_REBOUND_SPEED,
                ImpactReversalBounce.reboundSpeed(100f, 100f),
                EPSILON);
        assertTrue(ImpactReversalBounce.MAX_REBOUND_SPEED
                > ImpactReversalBounce.MIN_REBOUND_SPEED * 2f);
    }
}
