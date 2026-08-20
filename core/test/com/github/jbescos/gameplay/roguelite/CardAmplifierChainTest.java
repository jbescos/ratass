package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class CardAmplifierChainTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void directlyMultipliesConnectedAmplifiers() {
        assertEquals(4f, CardAmplifierChain.combine(2f, 2f), EPSILON);
        assertEquals(1.5625f, CardAmplifierChain.combine(1.25f, 1.25f), EPSILON);
    }

    @Test
    public void upstreamAmplifierDoesNotCreateAMissingDownstreamEffect() {
        assertEquals(1f, CardAmplifierChain.combine(1f, 2f), EPSILON);
    }
}
