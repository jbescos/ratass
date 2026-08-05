package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RogueliteOpponentGeometryTest {
    @Test
    public void centeredOpponentBlocksStraightPowerup() {
        assertTrue(blocks(0.1f, 2f));
    }

    @Test
    public void parallelOpponentDoesNotBlockStraightPowerup() {
        assertFalse(blocks(1.1f, 0.2f));
    }

    @Test
    public void opponentBehindOrBeyondActivationRangeDoesNotBlock() {
        assertFalse(blocks(0f, -1f));
        assertFalse(blocks(0f, 7.1f));
    }

    private static boolean blocks(float deltaX, float deltaY) {
        return RogueliteOpponentGeometry.blocksStraightPowerup(
                deltaX,
                deltaY,
                0f,
                1f,
                1f,
                0f,
                1f,
                2f,
                1f,
                2f);
    }
}
