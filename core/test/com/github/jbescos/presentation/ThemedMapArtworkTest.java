package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ThemedMapArtworkTest {
    @Test
    public void buildsPresentationPathWithoutMovingGameplayAssets() {
        assertEquals("maps/map015.png", ThemedMapArtwork.relativePath("map015"));
    }

    @Test
    public void rejectsMissingOrUnsafeMapIds() {
        assertEquals("", ThemedMapArtwork.relativePath(null));
        assertEquals("", ThemedMapArtwork.relativePath(" "));
        assertEquals("", ThemedMapArtwork.relativePath("../map015"));
        assertEquals("", ThemedMapArtwork.relativePath("maps/map015"));
    }
}
