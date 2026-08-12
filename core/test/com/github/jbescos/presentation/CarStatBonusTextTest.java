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
    }

    @Test
    public void invalidValuesFallBackToNeutral() {
        assertEquals("0%", CarStatBonusText.format(Float.NaN));
        assertEquals("0%", CarStatBonusText.format(Float.POSITIVE_INFINITY));
    }
}
