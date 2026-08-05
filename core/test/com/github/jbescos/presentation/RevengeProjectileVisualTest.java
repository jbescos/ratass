package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RevengeProjectileVisualTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void projectileAdvancesAndExpiresWithoutGameplayState() {
        RevengeProjectileVisual visual = new RevengeProjectileVisual();

        assertFalse(visual.isActive());
        visual.start(2);
        assertTrue(visual.isActive());
        assertEquals(2, visual.getTier());
        assertEquals(0f, visual.getProgress(), EPSILON);

        visual.update(0.275f);
        assertEquals(0.5f, visual.getProgress(), EPSILON);
        assertEquals(1f, visual.getAlpha(), EPSILON);

        visual.update(1f);
        assertFalse(visual.isActive());
        assertEquals(1f, visual.getProgress(), EPSILON);
        assertEquals(0f, visual.getAlpha(), EPSILON);
    }

    @Test
    public void tierIsClampedToSupportedVisualStrengths() {
        RevengeProjectileVisual visual = new RevengeProjectileVisual();

        visual.start(8);
        assertEquals(3, visual.getTier());
        visual.reset();
        assertFalse(visual.isActive());
        assertEquals(0, visual.getTier());
    }

    @Test
    public void restartingProjectileStartsACompleteNewThrow() {
        RevengeProjectileVisual visual = new RevengeProjectileVisual();

        visual.start(1);
        visual.update(0.4f);
        assertTrue(visual.getProgress() > 0.5f);

        visual.start(3);
        assertTrue(visual.isActive());
        assertEquals(0f, visual.getProgress(), EPSILON);
        assertEquals(3, visual.getTier());
    }

    @Test
    public void tetherReachesItsTargetAndRemainsConnectedForItsDuration() {
        RevengeProjectileVisual visual = new RevengeProjectileVisual();

        visual.startTether(2, 0.9f);
        assertTrue(visual.isTether());
        assertEquals(0f, visual.getTetherReach(), EPSILON);

        visual.update(0.225f);
        assertEquals(1f, visual.getTetherReach(), EPSILON);
        visual.update(0.4f);
        assertTrue(visual.isActive());
        assertEquals(1f, visual.getTetherReach(), EPSILON);

        visual.update(0.3f);
        assertFalse(visual.isActive());
        visual.reset();
        assertFalse(visual.isTether());
    }
}
