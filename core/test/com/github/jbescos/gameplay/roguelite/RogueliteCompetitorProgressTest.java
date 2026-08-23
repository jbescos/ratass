package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class RogueliteCompetitorProgressTest {
    @Test
    public void racecraftExperienceRemainsPendingUntilLapIsBanked() {
        RogueliteCompetitorProgress progress =
                new RogueliteCompetitorProgress("profile00");

        int gained =
                progress.awardRacecraftExperience(
                        RogueliteExperienceAwards.Reason.OVERTAKE,
                        2,
                        30);

        assertEquals(2, gained);
        assertEquals(0, progress.getExperience());
        assertEquals(2, progress.getLapExperience());
        assertEquals(0, progress.getRaceExperience());
        assertEquals(
                RogueliteExperienceAwards.Reason.OVERTAKE,
                progress.getLastExperienceReason());
        assertEquals(2, progress.getLastExperienceAmount());

        assertEquals(2, progress.bankLapExperience());
        assertEquals(2, progress.getExperience());
        assertEquals(0, progress.getLapExperience());
        assertEquals(2, progress.getRaceExperience());
    }

    @Test
    public void lapExperienceCannotLevelUpBeforeItIsBanked() {
        RogueliteCompetitorProgress progress =
                new RogueliteCompetitorProgress("profile00");
        progress.restore(1, 75, 0, false);

        progress.awardRacecraftExperience(
                RogueliteExperienceAwards.Reason.OVERTAKE,
                10,
                30);

        assertEquals(1, progress.getLevel());
        assertEquals(75, progress.getExperience());
        assertEquals(10, progress.getLapExperience());
        assertEquals(0, progress.getPendingRewards());

        assertEquals(10, progress.bankLapExperience());
        assertEquals(2, progress.getLevel());
        assertEquals(5, progress.getExperience());
        assertEquals(0, progress.getLapExperience());
        assertEquals(1, progress.getPendingRewards());
    }

    @Test
    public void lapExperienceMultiplierAppliesAfterTheLapCap() {
        RogueliteCompetitorProgress progress =
                new RogueliteCompetitorProgress("profile00");
        assertEquals(40, progress.awardRacecraftExperience(100, 40));

        assertEquals(80, progress.bankLapExperience(2f));

        assertEquals(0, progress.getLapExperience());
        assertEquals(80, progress.getRaceExperience());
        assertEquals(2, progress.getLevel());
        assertEquals(0, progress.getExperience());
        assertEquals(1, progress.getPendingRewards());
    }

    @Test
    public void lapExperienceMultiplierDoesNotAffectFinishExperience() {
        RogueliteCompetitorProgress progress =
                new RogueliteCompetitorProgress("profile00");
        assertEquals(10, progress.awardRacecraftExperience(10, 40));
        assertEquals(20, progress.bankLapExperience(2f));
        assertEquals(20, progress.getRaceBankedLapExperience());
        assertEquals(0, progress.getRaceFinishExperience());

        assertEquals(30, progress.awardRacePosition(10, 10));

        assertEquals(50, progress.getExperience());
        assertEquals(50, progress.getRaceExperience());
        assertEquals(20, progress.getRaceBankedLapExperience());
        assertEquals(30, progress.getRaceFinishExperience());
    }

    @Test
    public void lapResetDiscardsPendingExperienceButNotLastAward() {
        RogueliteCompetitorProgress progress =
                new RogueliteCompetitorProgress("profile00");
        progress.awardRacecraftExperience(
                RogueliteExperienceAwards.Reason.DRIFT,
                3,
                30);

        progress.resetLapExperience();

        assertEquals(0, progress.getLapExperience());
        assertEquals(0, progress.getRaceExperience());
        assertEquals(0, progress.getRaceBankedLapExperience());
        assertEquals(0, progress.getRaceFinishExperience());
        assertEquals(
                RogueliteExperienceAwards.Reason.DRIFT,
                progress.getLastExperienceReason());
        assertEquals(3, progress.getLastExperienceAmount());
    }

    @Test
    public void raceResetClearsOnlyTransientRaceState() {
        RogueliteCompetitorProgress progress =
                new RogueliteCompetitorProgress("profile00");
        progress.awardRacecraftExperience(
                RogueliteExperienceAwards.Reason.REVENGE,
                4,
                30);

        progress.resetRaceExperience();

        assertEquals(0, progress.getExperience());
        assertEquals(0, progress.getLapExperience());
        assertEquals(0, progress.getRaceExperience());
        assertNull(progress.getLastExperienceReason());
        assertEquals(0, progress.getLastExperienceAmount());
    }

    @Test
    public void competitorsNeverShareAwardNoticesOrCounters() {
        RogueliteCompetitorProgress first =
                new RogueliteCompetitorProgress("profile00");
        RogueliteCompetitorProgress second =
                new RogueliteCompetitorProgress("profile01");

        first.awardRacecraftExperience(
                RogueliteExperienceAwards.Reason.OVERTAKE,
                2,
                30);
        second.awardRacecraftExperience(
                RogueliteExperienceAwards.Reason.DRIFT,
                1,
                30);

        assertEquals(2, first.getLapExperience());
        assertEquals(
                RogueliteExperienceAwards.Reason.OVERTAKE,
                first.getLastExperienceReason());
        assertEquals(1, second.getLapExperience());
        assertEquals(
                RogueliteExperienceAwards.Reason.DRIFT,
                second.getLastExperienceReason());
    }

    @Test
    public void lapExperienceStealIsCappedByRecipientCapacity() {
        RogueliteCompetitorProgress offender =
                new RogueliteCompetitorProgress("profile00");
        RogueliteCompetitorProgress recipient =
                new RogueliteCompetitorProgress("profile01");
        offender.awardRacecraftExperience(40, 40);
        recipient.awardRacecraftExperience(20, 40);

        assertEquals(20, recipient.stealLapExperienceFrom(offender, 40));

        assertEquals(20, offender.getLapExperience());
        assertEquals(40, recipient.getLapExperience());
        assertEquals(0, offender.getExperience());
        assertEquals(0, recipient.getExperience());
    }

    @Test
    public void lapExperienceStealDoesNothingWithoutCapacityOrDistinctOffender() {
        RogueliteCompetitorProgress offender =
                new RogueliteCompetitorProgress("profile00");
        RogueliteCompetitorProgress recipient =
                new RogueliteCompetitorProgress("profile01");
        offender.awardRacecraftExperience(15, 40);
        recipient.awardRacecraftExperience(40, 40);

        assertEquals(0, recipient.stealLapExperienceFrom(offender, 40));
        assertEquals(0, offender.stealLapExperienceFrom(offender, 40));

        assertEquals(15, offender.getLapExperience());
        assertEquals(40, recipient.getLapExperience());
    }
}
