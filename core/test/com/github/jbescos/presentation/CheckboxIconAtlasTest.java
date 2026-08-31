package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CheckboxIconAtlasTest {
    @Test
    public void mapsCheckboxStatesToAtlasRows() {
        assertEquals(0, CheckboxIconAtlas.indexFor(false, true));
        assertEquals(1, CheckboxIconAtlas.indexFor(true, true));
        assertEquals(2, CheckboxIconAtlas.indexFor(false, false));
        assertEquals(3, CheckboxIconAtlas.indexFor(true, false));
        assertEquals(4, CheckboxIconAtlas.ROWS);
    }
}
