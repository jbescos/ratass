package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RivalXpTransferVisualTest {
    @Test
    public void movesOnceFromTheOffenderToTheRecipientThenExpires() {
        RivalXpTransferVisual visual = new RivalXpTransferVisual(7, 3, 6);

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
        assertTrue(!visual.isActive());
    }

    @Test
    public void localAwardRemainsAttachedToOneCar() {
        RivalXpTransferVisual visual = RivalXpTransferVisual.award(4, 2);

        assertEquals(4, visual.getSourceVehicleId());
        assertEquals(4, visual.getDestinationVehicleId());
        assertEquals(2, visual.getAmount());
        assertTrue(!visual.isTransfer());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyTransfers() {
        new RivalXpTransferVisual(7, 3, 0);
    }
}
