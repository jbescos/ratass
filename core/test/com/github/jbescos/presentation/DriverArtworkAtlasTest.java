package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DriverArtworkAtlasTest {
    @Test
    public void mapsDriverProfilesToRowMajorArtworkCells() {
        assertEquals(0, DriverArtworkAtlas.indexForProfile("profile00"));
        assertEquals(4, DriverArtworkAtlas.indexForProfile("profile04"));
        assertEquals(5, DriverArtworkAtlas.indexForProfile("profile05"));
        assertEquals(9, DriverArtworkAtlas.indexForProfile("profile09"));
    }

    @Test
    public void rejectsProfilesWithoutArtworkCells() {
        assertEquals(-1, DriverArtworkAtlas.indexForProfile(null));
        assertEquals(-1, DriverArtworkAtlas.indexForProfile("profile0"));
        assertEquals(-1, DriverArtworkAtlas.indexForProfile("profile10"));
        assertEquals(-1, DriverArtworkAtlas.indexForProfile("legacy"));
    }
}
