package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import org.junit.Test;

public class GadgetActivationVisualTest {
    @Test
    public void eachInactiveToActiveTransitionCreatesOneCallout() {
        GadgetActivationVisual visual = new GadgetActivationVisual();

        visual.update(0.1f, null);
        visual.update(0.1f, RogueliteCardId.NITRO_PULSE);
        assertTrue(visual.isActive());
        assertTrue(visual.isCalloutVisible());
        assertEquals(1, visual.getActivationCount());

        visual.update(0.1f, RogueliteCardId.NITRO_PULSE);
        assertEquals(1, visual.getActivationCount());
        visual.update(0.1f, null);
        visual.update(0.1f, RogueliteCardId.NITRO_PULSE);
        assertEquals(2, visual.getActivationCount());
    }

    @Test
    public void resetClearsAllPresentationState() {
        GadgetActivationVisual visual = new GadgetActivationVisual();
        visual.update(0f, RogueliteCardId.RAM_REACTOR);

        visual.reset();

        assertFalse(visual.isActive());
        assertFalse(visual.isCalloutVisible());
        assertEquals(0, visual.getActivationCount());
    }
}
