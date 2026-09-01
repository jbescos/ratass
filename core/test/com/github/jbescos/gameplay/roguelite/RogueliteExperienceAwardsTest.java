package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RogueliteExperienceAwardsTest {
    @Test
    public void everyGainedRacePositionAwardsExperience() {
        assertEquals(6, RogueliteExperienceAwards.forPositionsGained(1));
        assertEquals(18, RogueliteExperienceAwards.forPositionsGained(3));
    }

    @Test
    public void unchangedOrWorsePositionDoesNotAwardExperience() {
        assertEquals(0, RogueliteExperienceAwards.forPositionsGained(0));
        assertEquals(0, RogueliteExperienceAwards.forPositionsGained(-2));
    }

    @Test
    public void customOvertakeValueAppliesToEveryPosition() {
        assertEquals(12, RogueliteExperienceAwards.forPositionsGained(3, 4));
        assertEquals(0, RogueliteExperienceAwards.forPositionsGained(3, -1));
    }

    @Test
    public void driftTimeAwardsEveryAccumulatedSecondAndKeepsTheRemainder() {
        assertEquals(0, RogueliteExperienceAwards.completedDriftSeconds(0.9f));
        assertEquals(1, RogueliteExperienceAwards.completedDriftSeconds(1f));
        assertEquals(2, RogueliteExperienceAwards.completedDriftSeconds(2.4f));
        assertEquals(
                0.4f,
                RogueliteExperienceAwards.remainingDriftSeconds(2.4f),
                0.0001f);
    }

    @Test
    public void noticeDescribesOnlyPositiveAwards() {
        assertEquals(
                "Drift +2 XP",
                RogueliteExperienceAwards.formatNotice(
                        RogueliteExperienceAwards.Reason.DRIFT,
                        2));
        assertEquals(
                "",
                RogueliteExperienceAwards.formatNotice(
                        RogueliteExperienceAwards.Reason.DRIFT,
                        0));
        assertEquals(
                "Fastest lap +6 XP",
                RogueliteExperienceAwards.formatNotice(
                        RogueliteExperienceAwards.Reason.FASTEST_LAP,
                        RogueliteExperienceAwards.FASTEST_LAP));
        assertEquals(
                "Lap complete +5 XP",
                RogueliteExperienceAwards.formatNotice(
                        RogueliteExperienceAwards.Reason.LAP_COMPLETE,
                        RogueliteExperienceAwards.LAP_COMPLETE));
    }

    @Test
    public void visibleDriftUsesHysteresisWithoutDroppingOnMinorSlipChanges() {
        assertFalse(RogueliteExperienceAwards.isDrifting(false, true, 0.30f, 0.31f));
        assertTrue(RogueliteExperienceAwards.isDrifting(false, true, 0.30f, 0.32f));
        assertTrue(RogueliteExperienceAwards.isDrifting(true, true, 0.30f, 0.25f));
        assertFalse(RogueliteExperienceAwards.isDrifting(true, true, 0.30f, 0.24f));
    }

    @Test
    public void driftRequiresVisibleSpeedAndBeingOnRoad() {
        assertFalse(RogueliteExperienceAwards.isDrifting(false, true, 0.17f, 0.80f));
        assertFalse(RogueliteExperienceAwards.isDrifting(false, false, 0.80f, 0.80f));
        assertTrue(RogueliteExperienceAwards.isDrifting(false, true, 0.18f, 0.32f));
    }
}
