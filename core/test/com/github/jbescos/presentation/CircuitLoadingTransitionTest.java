package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CircuitLoadingTransitionTest {
    @Test
    public void startsOnlyForPresentationMapChanges() {
        assertTrue(CircuitLoadingTransition.shouldStart(true, false, 2));
        assertFalse(CircuitLoadingTransition.shouldStart(false, false, 2));
        assertFalse(CircuitLoadingTransition.shouldStart(true, true, 2));
        assertFalse(CircuitLoadingTransition.shouldStart(true, false, 1));
    }

    @Test
    public void reportsTheNextCircuitAndWrapsAtTheEnd() {
        assertEquals(2, CircuitLoadingTransition.nextCircuitNumber(1, 5));
        assertEquals(1, CircuitLoadingTransition.nextCircuitNumber(5, 5));
        assertEquals("Loading circuit 2 / 5", CircuitLoadingTransition.status(1, 5));
        assertEquals("Loading circuit", CircuitLoadingTransition.status(0, 0));
    }
}
