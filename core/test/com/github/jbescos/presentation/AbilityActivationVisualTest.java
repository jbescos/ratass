package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import org.junit.Test;

public class AbilityActivationVisualTest {
    @Test
    public void activeCardTracksTransitions() {
        AbilityActivationVisual visual = new AbilityActivationVisual();

        visual.update(0.1f, null);
        visual.update(0.1f, RogueliteCardId.NITRO_PULSE);
        assertTrue(visual.isActive());
        assertEquals(RogueliteCardId.NITRO_PULSE, visual.getActiveCardId());

        visual.update(0.1f, RogueliteCardId.NITRO_PULSE);
        visual.update(0.1f, null);
        assertFalse(visual.isActive());
    }

    @Test
    public void projectileRevengeCardsDoNotDrawCarCenteredFields() {
        AbilityActivationVisual visual = new AbilityActivationVisual();

        visual.update(0.1f, RogueliteCardId.DRAFT_VENDETTA);
        assertFalse(visual.hasCarCenteredEffect());

        visual.update(0.1f, RogueliteCardId.TAR_TETHER);
        assertFalse(visual.hasCarCenteredEffect());

        visual.update(0.1f, RogueliteCardId.EMP_SNARE);
        assertFalse(visual.hasCarCenteredEffect());

        visual.update(0.1f, RogueliteCardId.DRAFT_MAGNET);
        assertTrue(visual.hasCarCenteredEffect());
    }

    @Test
    public void driverHotlinesDrawCarCenteredArtworkWhileActive() {
        AbilityActivationVisual visual = new AbilityActivationVisual();

        visual.update(0.1f, RogueliteCardId.ACE_HOTLINE);
        assertTrue(visual.hasCarCenteredEffect());

        visual.update(0.1f, RogueliteCardId.PRIORITY_HOTLINE);
        assertTrue(visual.hasCarCenteredEffect());
    }

    @Test
    public void resetClearsAllPresentationState() {
        AbilityActivationVisual visual = new AbilityActivationVisual();
        visual.update(0f, RogueliteCardId.DRAFT_MAGNET, true);

        visual.reset();

        assertFalse(visual.isActive());
        assertFalse(visual.isRevengeArmed());
        assertFalse(visual.isTechniqueActive());
        assertFalse(visual.isPowerupActive());
    }

    @Test
    public void armedRevengeHasPersistentSkullPulse() {
        AbilityActivationVisual visual = new AbilityActivationVisual();

        visual.update(0.1f, null, true);
        float initialPulse = visual.getRevengeArmedPulse();
        visual.update(0.2f, null, true);

        assertTrue(visual.isRevengeArmed());
        assertTrue(visual.getRevengeArmedPulse() >= 0f);
        assertTrue(visual.getRevengeArmedPulse() <= 1f);
        assertTrue(initialPulse != visual.getRevengeArmedPulse());

        visual.update(0.1f, null, false);
        assertFalse(visual.isRevengeArmed());
        assertEquals(0f, visual.getRevengeArmedPulse(), 0f);
    }

    @Test
    public void techniqueAndPowerupActivityHaveIndependentPulses() {
        AbilityActivationVisual visual = new AbilityActivationVisual();

        visual.update(0.1f, null, false, true, false);
        float initialTechniquePulse = visual.getTechniquePulse();
        visual.update(0.2f, null, false, true, true);

        assertTrue(visual.isTechniqueActive());
        assertTrue(visual.isPowerupActive());
        assertTrue(initialTechniquePulse != visual.getTechniquePulse());
        assertTrue(visual.getPowerupPulse() >= 0f);
        assertTrue(visual.getPowerupPulse() <= 1f);

        visual.update(0.1f, null, false, false, false);
        assertFalse(visual.isTechniqueActive());
        assertFalse(visual.isPowerupActive());
        assertEquals(0f, visual.getTechniquePulse(), 0f);
        assertEquals(0f, visual.getPowerupPulse(), 0f);
    }

}
