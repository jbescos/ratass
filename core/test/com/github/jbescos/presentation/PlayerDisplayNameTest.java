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
    public void trimsAndLimitsNameToFifteenPrintableCharacters() {
        assertEquals("Road King Extra", PlayerDisplayName.sanitize("  Road King Extra Long  "));
        assertEquals("AB", PlayerDisplayName.sanitize("A\nB"));
    }

    @Test
    public void fallbackNameDoesNotBecomeUndeletableEditorText() {
        assertEquals("", PlayerDisplayName.editableValue(null));
        assertEquals("", PlayerDisplayName.editableValue("YOU"));
        assertEquals("RACER", PlayerDisplayName.editableValue("RACER"));
    }
}
