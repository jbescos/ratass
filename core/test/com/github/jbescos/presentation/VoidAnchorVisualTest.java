package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VoidAnchorVisualTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void anchorDeploysAndRemainsActiveForTheTargetDebuffDuration() {
        VoidAnchorVisual visual = new VoidAnchorVisual();

        visual.start(3f);
        visual.update(0.18f);

        assertTrue(visual.isActive());
        assertEquals(1f, visual.getDeployment(), EPSILON);
        assertTrue(visual.getPulse() >= 0f);
        assertTrue(visual.getPulse() <= 1f);

        visual.update(1f);
        visual.update(1f);
        visual.update(0.80f);
        assertTrue(visual.isActive());
        visual.update(0.02f);
        assertFalse(visual.isActive());
    }

    @Test
    public void resetAndInvalidDurationsClearTheVisual() {
        VoidAnchorVisual visual = new VoidAnchorVisual();
        visual.start(2f);

        visual.reset();
        assertFalse(visual.isActive());
        assertEquals(0f, visual.getDeployment(), EPSILON);

        visual.start(Float.NaN);
        assertFalse(visual.isActive());
    }
}
