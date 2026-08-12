package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class TriadCoupOrderTest {
    private static final int ME = 2;
    private static final int A = 1;
    private static final int B = 3;

    @Test
    public void putsTheSourceFirstAndReversesTheTwoRivalsFromEveryInitialOrder() {
        assertResolved(Arrays.asList(A, ME, B));
        assertResolved(Arrays.asList(A, B, ME));
        assertResolved(Arrays.asList(ME, A, B));
    }

    @Test
    public void fallsBackToTwoCarsWhenTheThirdCarIsMissingOrDuplicated() {
        assertEquals(
                Arrays.asList(ME, A),
                TriadCoupOrder.resolve(Arrays.asList(A, ME, B), ME, A, -1));
        assertEquals(
                Arrays.asList(ME, A),
                TriadCoupOrder.resolve(Arrays.asList(A, ME, B), ME, A, A));
    }

    @Test
    public void rejectsAnInvalidSourceOrOffender() {
        assertEquals(
                Collections.emptyList(),
                TriadCoupOrder.resolve(Arrays.asList(A, ME, B), 99, A, B));
        assertEquals(
                Collections.emptyList(),
                TriadCoupOrder.resolve(Arrays.asList(A, ME, B), ME, ME, B));
    }

    private static void assertResolved(List<Integer> raceOrder) {
        List<Integer> resolved = TriadCoupOrder.resolve(raceOrder, ME, A, B);
        assertEquals(Arrays.asList(ME, B, A), resolved);
        assertEquals(
                Arrays.asList(raceOrder.get(0), raceOrder.get(1), raceOrder.get(2)),
                TriadCoupOrder.selectedPositions(raceOrder, resolved));
    }
}
