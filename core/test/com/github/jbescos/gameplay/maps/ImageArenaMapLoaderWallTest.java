package com.github.jbescos.gameplay.maps;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ImageArenaMapLoaderWallTest {
    @Test
    public void recognizesOrangeWallPixelsIncludingAntialiasing() {
        assertTrue(ImageArenaMapLoader.isWallMarker(rgba8888(233, 111, 0, 255)));
        assertTrue(ImageArenaMapLoader.isWallMarker(rgba8888(47, 22, 0, 255)));
    }

    @Test
    public void doesNotTreatRoadOrGridMarkersAsWalls() {
        assertFalse(ImageArenaMapLoader.isWallMarker(rgba8888(255, 255, 255, 255)));
        assertFalse(ImageArenaMapLoader.isWallMarker(rgba8888(0, 0, 0, 255)));
        assertFalse(ImageArenaMapLoader.isWallMarker(rgba8888(225, 28, 34, 255)));
        assertFalse(ImageArenaMapLoader.isWallMarker(rgba8888(0, 220, 50, 255)));
    }

    private static int rgba8888(int red, int green, int blue, int alpha) {
        return ((red & 0xff) << 24)
                | ((green & 0xff) << 16)
                | ((blue & 0xff) << 8)
                | (alpha & 0xff);
    }
}
