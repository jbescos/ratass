package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MenuButtonSkinAtlasTest {
    @Test
    public void mapsStatesToAtlasRows() {
        MenuButtonSkinAtlas.State[] states = MenuButtonSkinAtlas.State.values();
        assertEquals(MenuButtonSkinAtlas.ROWS, states.length);
        for (int i = 0; i < states.length; i++) {
            assertEquals(i, states[i].getArtworkIndex());
        }
    }
}
