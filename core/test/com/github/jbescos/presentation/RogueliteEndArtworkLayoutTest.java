package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.badlogic.gdx.math.Rectangle;
import org.junit.Test;

public class RogueliteEndArtworkLayoutTest {
    @Test
    public void defeatArtworkFitsAboveTheOutcomeTextOnDesktop() {
        Rectangle panel = new Rectangle(100f, 40f, 820f, 740f);
        Rectangle bounds = new Rectangle();

        RogueliteEndArtworkLayout.updateDefeatArtworkBounds(
                bounds,
                panel,
                1254,
                1254);

        assertEquals(148f, bounds.width, 0.001f);
        assertEquals(148f, bounds.height, 0.001f);
        assertEquals(panel.x + panel.width * 0.5f, bounds.x + bounds.width * 0.5f, 0.001f);
        assertTrue(bounds.y >= panel.y + panel.height * 0.78f);
        assertTrue(bounds.y + bounds.height <= panel.y + panel.height);
    }

    @Test
    public void defeatArtworkRemainsInsideAShortMobilePanel() {
        Rectangle panel = new Rectangle(12f, 8f, 400f, 430f);
        Rectangle bounds = new Rectangle();

        RogueliteEndArtworkLayout.updateDefeatArtworkBounds(
                bounds,
                panel,
                1600,
                900);

        assertTrue(bounds.width <= panel.width * 0.22f + 0.001f);
        assertTrue(bounds.height <= panel.height * 0.20f + 0.001f);
        assertTrue(bounds.x >= panel.x);
        assertTrue(bounds.x + bounds.width <= panel.x + panel.width);
        assertTrue(bounds.y + bounds.height <= panel.y + panel.height);
    }
}
