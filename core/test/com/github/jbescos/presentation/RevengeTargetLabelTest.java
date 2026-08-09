package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RevengeTargetLabelTest {
    @Test
    public void labelRequiresAVisibleSkullAndActiveNamedTarget() {
        assertTrue(RevengeTargetLabel.shouldDraw(true, 7, true, "CAR 7"));
        assertFalse(RevengeTargetLabel.shouldDraw(false, 7, true, "CAR 7"));
        assertFalse(RevengeTargetLabel.shouldDraw(true, -1, true, "CAR 7"));
        assertFalse(RevengeTargetLabel.shouldDraw(true, 7, false, "CAR 7"));
        assertFalse(RevengeTargetLabel.shouldDraw(true, 7, true, "  "));
    }

    @Test
    public void labelStaysCenteredBelowTheSkullAndInsideThePlayfield() {
        assertEquals(80f, RevengeTargetLabel.centeredX(100f, 40f, 10f, 190f), 0.001f);
        assertEquals(10f, RevengeTargetLabel.centeredX(15f, 40f, 10f, 190f), 0.001f);
        assertEquals(150f, RevengeTargetLabel.centeredX(185f, 40f, 10f, 190f), 0.001f);
        assertEquals(88.48f, RevengeTargetLabel.baseline(100f, 16f, 20f, 180f), 0.001f);
        assertEquals(20f, RevengeTargetLabel.baseline(22f, 16f, 20f, 180f), 0.001f);
    }

    @Test
    public void labelNamesTheRevengeCardAndItsTarget() {
        assertEquals("Doom Hex on Blitz", RevengeTargetLabel.buildText("Doom Hex", "Blitz"));
        assertEquals("Blitz", RevengeTargetLabel.buildText(null, " Blitz "));
    }
}
