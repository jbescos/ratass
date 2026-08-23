package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CarStatBonusTextTest {
    @Test
    public void formatsMultipliersRelativeToNeutral() {
        assertEquals("0%", CarStatBonusText.format(1f));
        assertEquals("+5%", CarStatBonusText.format(1.05f));
        assertEquals("-5%", CarStatBonusText.format(0.95f));
        assertEquals("+7.5%", CarStatBonusText.format(1.075f));
        assertEquals("-0.5%", CarStatBonusText.format(0.995f));
        assertEquals("-90%", CarStatBonusText.format(0.10f));
    }

    @Test
    public void invalidValuesFallBackToNeutral() {
        assertEquals("0%", CarStatBonusText.format(Float.NaN));
        assertEquals("0%", CarStatBonusText.format(Float.POSITIVE_INFINITY));
    }

    @Test
    public void formatsExplicitMultipliers() {
        assertEquals("x1", CarStatBonusText.formatMultiplier(1f));
        assertEquals("x1.25", CarStatBonusText.formatMultiplier(1.25f));
        assertEquals("x1.5", CarStatBonusText.formatMultiplier(1.5f));
        assertEquals("x2", CarStatBonusText.formatMultiplier(2f));
    }
}
