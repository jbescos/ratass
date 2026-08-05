package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MirrorFormationLayoutTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void quartetUsesThreeDistinctRoadWidthPositions() {
        float[] offsets = new float[3];

        int count = MirrorFormationLayout.fillMirrorOffsets(
                0f,
                4f,
                4f,
                1.14f,
                4,
                offsets);

        assertEquals(3, count);
        for (int i = 0; i < count; i++) {
            assertTrue(Math.abs(offsets[i]) >= 1.14f - EPSILON);
            assertTrue(offsets[i] < 4f);
            assertTrue(offsets[i] > -4f);
            for (int j = i + 1; j < count; j++) {
                assertTrue(Math.abs(offsets[i] - offsets[j]) >= 1.14f - EPSILON);
            }
        }
    }

    @Test
    public void formationUsesTheSideWithAvailableRoad() {
        float[] offsets = new float[3];

        int count = MirrorFormationLayout.fillMirrorOffsets(
                2.5f,
                3.2f,
                6f,
                1.14f,
                3,
                offsets);

        assertEquals(2, count);
        assertTrue(offsets[0] < 2.5f);
        assertTrue(offsets[1] < offsets[0]);
    }
}
