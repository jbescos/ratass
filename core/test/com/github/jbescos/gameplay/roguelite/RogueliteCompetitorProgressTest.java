package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class RogueliteCompetitorProgressTest {
    @Test
    public void competitorOwnsLevelLapRaceAndLastAwardState() {
        RogueliteCompetitorProgress progress =
                new RogueliteCompetitorProgress("profile00");

        int gained =
                progress.awardRacecraftExperience(
                        RogueliteExperienceAwards.Reason.OVERTAKE,
                        2,
                        30);

        assertEquals(2, gained);
        assertEquals(2, progress.getExperience());
        assertEquals(2, progress.getLapExperience());
        assertEquals(2, progress.getRaceExperience());
        assertEquals(
                RogueliteExperienceAwards.Reason.OVERTAKE,
                progress.getLastExperienceReason());
        assertEquals(2, progress.getLastExperienceAmount());
    }

    @Test
    public void lapResetDoesNotEraseRaceTotalOrLastAward() {
        RogueliteCompetitorProgress progress =
                new RogueliteCompetitorProgress("profile00");
        progress.awardRacecraftExperience(
                RogueliteExperienceAwards.Reason.DRIFT,
                3,
                30);

        progress.resetLapExperience();

        assertEquals(0, progress.getLapExperience());
        assertEquals(3, progress.getRaceExperience());
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

        assertEquals(4, progress.getExperience());
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
}
