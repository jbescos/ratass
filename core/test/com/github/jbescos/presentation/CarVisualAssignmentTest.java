package com.github.jbescos.presentation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class CarVisualAssignmentTest {
    @Test
    public void providesEveryOpponentWithADifferentVisual() {
        int playerVisualIndex = 3;
        int[] candidates = CarVisualAssignment.enemyCandidates(10, playerVisualIndex);
        Set<Integer> uniqueIndices = new HashSet<Integer>();

        assertTrue(candidates.length == 9);
        for (int i = 0; i < candidates.length; i++) {
            assertTrue(candidates[i] != playerVisualIndex);
            assertTrue(uniqueIndices.add(Integer.valueOf(candidates[i])));
        }
    }

    @Test
    public void changingThePlayerVisualInvalidatesTheEnemyPalette() {
        assertTrue(CarVisualAssignment.remainsValid(10, 2, 10, 2));
        assertFalse(CarVisualAssignment.remainsValid(10, 2, 10, 7));
        assertFalse(CarVisualAssignment.remainsValid(10, 2, 8, 2));
    }
}
