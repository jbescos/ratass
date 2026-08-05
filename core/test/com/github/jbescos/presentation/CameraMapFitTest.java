package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CameraMapFitTest {
    @Test
    public void fitsWideMapByWidth() {
        assertEquals(2.0f, CameraMapFit.calculateZoom(200f, 50f, 100f, 100f, 1f), 0.0001f);
    }

    @Test
    public void fitsTallMapByHeight() {
        assertEquals(3.0f, CameraMapFit.calculateZoom(80f, 300f, 100f, 100f, 1f), 0.0001f);
    }

    @Test
    public void addsWholeMapPadding() {
        assertEquals(2.1f, CameraMapFit.calculateZoom(200f, 50f, 100f, 100f, 1.05f), 0.0001f);
    }
}
