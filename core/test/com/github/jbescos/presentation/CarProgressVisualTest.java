package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CarProgressVisualTest {
    @Test
    public void movesOnceFromTheOffenderToTheRecipientThenExpires() {
        CarProgressVisual visual = CarProgressVisual.experienceTransfer(7, 3, 6);

        assertEquals(CarProgressVisual.Kind.EXPERIENCE, visual.getKind());
        assertEquals(7, visual.getSourceVehicleId());
        assertEquals(3, visual.getDestinationVehicleId());
        assertEquals(6, visual.getAmount());
        assertTrue(visual.isTransfer());
        assertEquals(0f, visual.getProgress(), 0.0001f);
        assertTrue(visual.isActive());

        visual.update(0.65f);
        assertEquals(0.5f, visual.getProgress(), 0.0001f);
        assertEquals(1f, visual.getAlpha(), 0.0001f);
        visual.update(0.65f);
        assertEquals(1f, visual.getProgress(), 0.0001f);
        assertTrue(visual.isActive());
        visual.update(0.7f);
        assertFalse(visual.isActive());
    }

    @Test
    public void localAwardRemainsAttachedToOneCar() {
        CarProgressVisual visual = CarProgressVisual.experienceAward(4, 2);

        assertEquals(CarProgressVisual.Kind.EXPERIENCE, visual.getKind());
        assertEquals(4, visual.getSourceVehicleId());
        assertEquals(4, visual.getDestinationVehicleId());
        assertEquals(2, visual.getAmount());
        assertFalse(visual.isTransfer());
    }

    @Test
    public void levelUpRemainsAttachedAndCarriesNoExperienceAmount() {
        CarProgressVisual visual = CarProgressVisual.levelUp(5);

        assertEquals(CarProgressVisual.Kind.LEVEL_UP, visual.getKind());
        assertEquals(5, visual.getSourceVehicleId());
        assertEquals(5, visual.getDestinationVehicleId());
        assertEquals(0, visual.getAmount());
        assertFalse(visual.isTransfer());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyExperienceTransfers() {
        CarProgressVisual.experienceTransfer(7, 3, 0);
    }
}
