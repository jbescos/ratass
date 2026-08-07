package com.github.jbescos.gameplay;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RecoverySeparationPlanTest {
    private static final float TARGET_DISTANCE = 2f;

    @Test
    public void escapeTargetsPointAwayFromBlockersAtEverySide() {
        assertTargetPointsAway(0f, 1f);
        assertTargetPointsAway(0f, -1f);
        assertTargetPointsAway(1f, 0f);
        assertTargetPointsAway(-1f, 0f);
        assertTargetPointsAway(0.8f, 0.8f);
        assertTargetPointsAway(-0.8f, -0.8f);
    }

    @Test
    public void movementMustBeAwayRatherThanMerelyLarge() {
        RecoverySeparationPlan plan = createPlan(0f, 1f);

        assertFalse(plan.hasMoved(2f, 0f, 1f));
        assertFalse(plan.hasMoved(0f, 0.5f, 1f));
        assertTrue(plan.hasMoved(0f, -1.1f, 1f));
    }

    @Test
    public void clearanceUsesCurrentBlockerPosition() {
        RecoverySeparationPlan plan = createPlan(0f, 1f);

        assertFalse(plan.hasClearance(0f, -0.2f, 0f, 1f, 1.5f));
        assertTrue(plan.hasClearance(0f, -0.6f, 0f, 1f, 1.5f));
        assertFalse(plan.hasClearance(0f, -0.6f, 0f, 0.7f, 1.5f));
    }

    @Test
    public void exactOverlapFallsBackOppositeTheCarsHeading() {
        RecoverySeparationPlan plan = new RecoverySeparationPlan();
        plan.begin(2f, 3f, 2f, 3f, 0f, 1f, TARGET_DISTANCE);

        assertTrue(plan.getTargetY() < 3f);
    }

    private static void assertTargetPointsAway(float blockerX, float blockerY) {
        RecoverySeparationPlan plan = createPlan(blockerX, blockerY);
        float escapeX = plan.getTargetX();
        float escapeY = plan.getTargetY();
        float towardBlockerX = blockerX;
        float towardBlockerY = blockerY;

        assertTrue(escapeX * towardBlockerX + escapeY * towardBlockerY < 0f);
    }

    private static RecoverySeparationPlan createPlan(float blockerX, float blockerY) {
        RecoverySeparationPlan plan = new RecoverySeparationPlan();
        plan.begin(0f, 0f, blockerX, blockerY, 0f, 1f, TARGET_DISTANCE);
        return plan;
    }
}
