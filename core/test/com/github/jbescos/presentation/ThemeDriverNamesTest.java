package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ThemeDriverNamesTest {
    @Test
    public void parsesProfileAliasesAndIgnoresCommentsAndMalformedLines() {
        ThemeDriverNames names = ThemeDriverNames.parse(
                "# Drivers\n"
                        + "profile00 = Luca Vero\n"
                        + "invalid\n"
                        + "profile10=Max Crown\n");

        assertEquals("Luca Vero", names.get("profile00"));
        assertEquals("Max Crown", names.get("profile10"));
        assertNull(names.get("invalid"));
    }

    @Test
    public void keepsFirstAliasWhenProfileIsDuplicated() {
        ThemeDriverNames names = ThemeDriverNames.parse(
                "profile08=Mara Voss\nprofile08=Other Name\n");

        assertEquals("Mara Voss", names.get("profile08"));
    }

    @Test
    public void emptyInputHasNoAliases() {
        assertNull(ThemeDriverNames.parse(null).get("profile00"));
        assertNull(ThemeDriverNames.parse("  \n# comment").get("profile00"));
    }
}
