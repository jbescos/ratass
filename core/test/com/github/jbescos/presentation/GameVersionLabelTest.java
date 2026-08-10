package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GameVersionLabelTest {
    @Test
    public void includesDatedVersionName() {
        assertEquals("v1.0-11082026-110423517", GameVersionLabel.format("1.0-11082026-110423517"));
    }

    @Test
    public void trimsVersionName() {
        assertEquals("v1.0-11082026-110423517", GameVersionLabel.format(" 1.0-11082026-110423517 "));
    }

    @Test
    public void fallsBackForMissingVersionName() {
        assertEquals("v1.0", GameVersionLabel.format("  "));
    }
}
