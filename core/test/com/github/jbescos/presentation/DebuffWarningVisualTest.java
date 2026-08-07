package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DebuffWarningVisualTest {
    @Test
    public void warningRemainsVisibleAndPulsesForTheWholeDebuff() {
        DebuffWarningVisual visual = new DebuffWarningVisual();

        visual.update(0.1f, DebuffWarningVisual.Reason.BLIND_ENEMIES);
        float initialPulse = visual.getPulse();
        visual.update(0.2f, DebuffWarningVisual.Reason.BLIND_ENEMIES);

        assertTrue(visual.isActive());
        assertEquals("BLIND ENEMIES", visual.getReasonLabel());
        assertTrue(visual.getPulse() >= 0f);
        assertTrue(visual.getPulse() <= 1f);
        assertTrue(initialPulse != visual.getPulse());
    }

    @Test
    public void warningClearsAsSoonAsTheDebuffEnds() {
        DebuffWarningVisual visual = new DebuffWarningVisual();

        visual.update(0.1f, DebuffWarningVisual.Reason.BRAKED);
        visual.update(0.1f, DebuffWarningVisual.Reason.NONE);

        assertFalse(visual.isActive());
        assertEquals("", visual.getReasonLabel());
        assertEquals(0f, visual.getPulse(), 0f);
    }

    @Test
    public void resetClearsPresentationState() {
        DebuffWarningVisual visual = new DebuffWarningVisual();
        visual.update(0.1f, DebuffWarningVisual.Reason.NO_GRIP);

        visual.reset();

        assertFalse(visual.isActive());
        assertEquals(0f, visual.getPulse(), 0f);
    }
}
