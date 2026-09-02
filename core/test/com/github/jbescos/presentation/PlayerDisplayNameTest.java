package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PlayerDisplayNameTest {
    @Test
    public void defaultsWhenNameIsMissingOrBlank() {
        assertEquals("YOU", PlayerDisplayName.sanitize(null));
        assertEquals("YOU", PlayerDisplayName.sanitize("   "));
    }

    @Test
    public void trimsAndLimitsNameToNinePrintableCharacters() {
        assertEquals("Road King", PlayerDisplayName.sanitize("  Road King Extra Long  "));
        assertEquals("AB", PlayerDisplayName.sanitize("A\nB"));
    }

    @Test
    public void fallbackNameDoesNotBecomeUndeletableEditorText() {
        assertEquals("", PlayerDisplayName.editableValue(null));
        assertEquals("", PlayerDisplayName.editableValue("YOU"));
        assertEquals("RACER", PlayerDisplayName.editableValue("RACER"));
    }

    @Test
    public void raceLabelKeepsTheNameAndLocalizesOnlyTheLevelPrefix() {
        assertEquals("DrBolinga (Nv 1)", PlayerDisplayName.raceLabel("DrBolinga", 1, true));
        assertEquals("Overtake (Lv 3)", PlayerDisplayName.raceLabel("Overtake", 3, false));
        assertEquals(" (Nv 1)", PlayerDisplayName.raceLevelSuffix(1, true));
        assertEquals(" (Lv 3)", PlayerDisplayName.raceLevelSuffix(3, false));
    }
}
