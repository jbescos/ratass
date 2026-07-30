package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class RogueliteRunTest {
    @Test
    public void loadoutHasOneDriverAndThreeReplaceableModificationSlots() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");

        assertTrue(loadout.equip(RogueliteCardId.TURBOCHARGER, -1));
        assertTrue(loadout.equip(RogueliteCardId.AERODYNAMIC_KIT, -1));
        assertTrue(loadout.equip(RogueliteCardId.STORM_TIRES, -1));
        assertTrue(loadout.isFull());
        assertFalse(loadout.equip(RogueliteCardId.CLEAN_MOMENTUM, -1));
        assertFalse(loadout.equip(RogueliteCardId.TURBOCHARGER, 0));

        assertTrue(loadout.equip(RogueliteCardId.CLEAN_MOMENTUM, 1));
        assertEquals(3, loadout.getModifications().size());
        assertEquals(
                RogueliteCardId.CLEAN_MOMENTUM,
                loadout.getModifications().get(1));

        loadout.setDriverProfileId("profile01");
        assertEquals("profile01", loadout.getDriverProfileId());
    }

    @Test
    public void oneCardTierUnlocksForEachChampionship() {
        RogueliteRun run = new RogueliteRun(17L);

        assertEquals(1, run.getUnlockedTier());
        run.advanceChampionship();
        assertEquals(2, run.getUnlockedTier());
        run.advanceChampionship();
        assertEquals(3, run.getUnlockedTier());
        run.advanceChampionship();
        assertEquals(4, run.getUnlockedTier());
        run.advanceChampionship();
        assertEquals(5, run.getUnlockedTier());
        for (int i = 0; i < 3; i++) {
            run.advanceChampionship();
        }
        assertEquals(5, run.getUnlockedTier());
    }

    @Test
    public void worstBenchmarkedDriverIsDefaultAndFirstOffersStayInTierOne() {
        DriverProfileCatalog catalog =
                new DriverProfileCatalog(Arrays.asList(
                        metadata("profile00", 72f),
                        metadata("profile01", 18f),
                        metadata("profile02", 44f),
                        metadata("profile03", 91f)));
        RogueliteRun run = new RogueliteRun(31L, catalog);

        assertEquals(
                "profile01",
                run.getPlayerLoadout().getDriverProfileId());
        run.awardPlayerRacePosition(1, 10);
        run.awardPlayerRacePosition(1, 10);
        List<RogueliteCardOffer> offers = run.createOffers(3);

        assertFalse(offers.isEmpty());
        Set<String> ids = new HashSet<String>();
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer offer = offers.get(i);
            assertTrue(ids.add(offer.getOfferId()));
            assertTrue(offer.getTier() <= 1);
            if (offer.isDriver()) {
                assertEquals("profile02", offer.getDriver().getProfileId());
            }
        }
    }

    @Test
    public void finishingPositionControlsExperienceAndLevelUpRewards() {
        RogueliteRun run = new RogueliteRun(43L);

        assertEquals(100, run.awardPlayerRacePosition(1, 10));
        assertEquals(30, run.awardRivalRacePosition(7, 10, 10));
        assertTrue(
                run.getPlayerProgress().getExperience()
                        > run.getRivalProgress(7).getExperience());
        assertFalse(run.getPlayerProgress().hasPendingReward());

        run.awardPlayerRacePosition(1, 10);
        assertEquals(2, run.getPlayerProgress().getLevel());
        assertEquals(20, run.getPlayerProgress().getExperience());
        assertEquals(1, run.getPlayerProgress().getPendingRewards());
    }

    @Test
    public void selectingAnOfferConsumesOnePendingRewardWithoutCardLevels() {
        RogueliteRun run = new RogueliteRun(47L);
        levelUpPlayer(run);
        List<RogueliteCardOffer> offers = run.createOffers(3);
        RogueliteCardOffer modification = firstModification(offers);

        assertTrue(run.select(modification));
        assertTrue(
                run.getPlayerLoadout().has(
                        modification.getCard().getId()));
        assertEquals(0, run.getPlayerProgress().getPendingRewards());
        assertTrue(run.createOffers(3).isEmpty());
    }

    @Test
    public void equippedCardsStayOutOfOffersButReplacedCardsCanReturn() {
        RogueliteRun run = new RogueliteRun(49L);

        earnOneReward(run);
        RogueliteCardOffer first = firstModification(run.createOffers(20));
        assertTrue(run.select(first));

        earnOneReward(run);
        assertFalse(containsOffer(run.createOffers(20), first.getOfferId()));

        while (!run.getPlayerLoadout().isFull()) {
            RogueliteCardOffer next = firstModification(run.createOffers(20));
            assertTrue(run.select(next));
            if (!run.getPlayerLoadout().isFull()) {
                earnOneReward(run);
            }
        }

        RogueliteCardId replaced =
                run.getPlayerLoadout().getModifications().get(0);
        run.advanceChampionship();
        earnOneReward(run);
        RogueliteCardOffer replacement =
                firstModification(run.createOffers(20));
        assertTrue(run.select(replacement, 0));

        earnOneReward(run);
        List<RogueliteCardOffer> laterOffers = run.createOffers(20);
        assertTrue(
                containsOffer(
                        laterOffers,
                        "card:" + replaced.name()));
        assertFalse(containsOffer(laterOffers, replacement.getOfferId()));
    }

    @Test
    public void currentDriverStaysOutOfOffersButPreviousDriverCanReturn() {
        RogueliteRun run = new RogueliteRun(51L);
        String defaultDriver = run.getPlayerLoadout().getDriverProfileId();

        earnOneReward(run);
        RogueliteCardOffer selectedDriver =
                firstDriver(run.createOffers(20));
        assertTrue(run.select(selectedDriver));

        earnOneReward(run);
        List<RogueliteCardOffer> laterOffers = run.createOffers(20);
        assertFalse(containsOffer(laterOffers, selectedDriver.getOfferId()));
        assertTrue(
                containsOffer(
                        laterOffers,
                        "driver:" + defaultDriver));
    }

    @Test
    public void rivalsOnlyChooseCardsAfterTheyEarnALevel() {
        RogueliteRun run = new RogueliteRun(53L);
        run.awardRivalRacePosition(2, 10, 10);
        run.resolveRivalRewards(Arrays.asList(Integer.valueOf(2)));
        assertTrue(run.getRivalLoadout(2).getModifications().isEmpty());

        run.awardRivalRacePosition(2, 1, 10);
        run.awardRivalRacePosition(2, 1, 10);
        run.resolveRivalRewards(Arrays.asList(Integer.valueOf(2)));

        RogueliteCompetitorProgress rival = run.getRivalProgress(2);
        assertEquals(0, rival.getPendingRewards());
        assertTrue(
                !rival.getLoadout().getModifications().isEmpty()
                        || !"profile00".equals(
                                rival.getLoadout().getDriverProfileId()));
    }

    @Test
    public void snapshotRestoresLoadoutsProgressAndOfferSequence() {
        RogueliteRun original = new RogueliteRun(59L);
        levelUpPlayer(original);
        RogueliteCardOffer selected =
                firstModification(original.createOffers(3));
        assertTrue(original.select(selected));
        original.awardPlayerRacePosition(1, 10);
        original.awardPlayerRacePosition(1, 10);
        original.awardPlayerRacePosition(1, 10);
        original.awardRivalRacePosition(8, 1, 10);

        RogueliteRun restored = new RogueliteRun(999L);
        assertTrue(restored.restore(original.snapshot()));

        assertEquals(
                original.getPlayerLoadout().getModifications(),
                restored.getPlayerLoadout().getModifications());
        assertEquals(
                original.getPlayerProgress().getLevel(),
                restored.getPlayerProgress().getLevel());
        assertNotEquals(
                original.getPlayerProgress(),
                restored.getPlayerProgress());
        assertEquals(
                offerIds(original.createOffers(3)),
                offerIds(restored.createOffers(3)));
    }

    private static void levelUpPlayer(RogueliteRun run) {
        run.awardPlayerRacePosition(1, 10);
        run.awardPlayerRacePosition(1, 10);
    }

    private static void earnOneReward(RogueliteRun run) {
        while (!run.getPlayerProgress().hasPendingReward()) {
            run.awardPlayerRacePosition(1, 10);
        }
    }

    private static RogueliteCardOffer firstModification(
            List<RogueliteCardOffer> offers) {
        for (int i = 0; i < offers.size(); i++) {
            if (!offers.get(i).isDriver()) {
                return offers.get(i);
            }
        }
        throw new AssertionError("Expected a modification offer.");
    }

    private static RogueliteCardOffer firstDriver(
            List<RogueliteCardOffer> offers) {
        for (int i = 0; i < offers.size(); i++) {
            if (offers.get(i).isDriver()) {
                return offers.get(i);
            }
        }
        throw new AssertionError("Expected a driver offer.");
    }

    private static boolean containsOffer(
            List<RogueliteCardOffer> offers,
            String offerId) {
        for (int i = 0; i < offers.size(); i++) {
            if (offerId.equals(offers.get(i).getOfferId())) {
                return true;
            }
        }
        return false;
    }

    private static DriverProfileMetadata metadata(String id, float rating) {
        return new DriverProfileMetadata(
                id,
                "",
                "test",
                rating,
                rating,
                rating,
                rating,
                1f,
                30f,
                31f,
                2f);
    }

    private static List<String> offerIds(List<RogueliteCardOffer> offers) {
        List<String> ids = new java.util.ArrayList<String>();
        for (int i = 0; i < offers.size(); i++) {
            ids.add(offers.get(i).getOfferId());
        }
        return ids;
    }
}
