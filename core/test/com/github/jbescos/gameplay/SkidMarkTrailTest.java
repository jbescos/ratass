package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SkidMarkTrailTest {
    @Test
    public void emitsOnlyAfterHighSlipAndDoesNotBridgeSeparateDrifts() {
        SkidMarkTrail trail = new SkidMarkTrail();

        update(trail, 0f, 0f, 0.31f);
        assertEquals(0, trail.getSegmentCount());

        update(trail, 0f, 0f, 0.50f);
        update(trail, 0.04f, 0.4f, 0.50f);
        assertEquals(1, trail.getSegmentCount());

        update(trail, 0.08f, 0.8f, 0.20f);
        update(trail, 0.12f, 10f, 0.50f);
        assertEquals(1, trail.getSegmentCount());

        update(trail, 0.16f, 10.4f, 0.50f);
        assertEquals(2, trail.getSegmentCount());
    }

    @Test
    public void expiresAndClearsSegments() {
        SkidMarkTrail trail = new SkidMarkTrail();
        update(trail, 0f, 0f, 0.50f);
        update(trail, 0.04f, 0.4f, 0.50f);

        trail.prune(0.04f + SkidMarkTrail.LIFETIME);
        assertEquals(0, trail.getSegmentCount());

        update(trail, 15f, 0.8f, 0.50f);
        assertEquals(1, trail.getSegmentCount());
        trail.clear();
        assertEquals(0, trail.getSegmentCount());
    }

    private static void update(
            SkidMarkTrail trail, float now, float centerX, float slip) {
        trail.updateEmitter(
                7,
                0.04f,
                now,
                centerX,
                0f,
                0f,
                1.14f,
                1.58f,
                0.45f,
                slip);
    }
}
