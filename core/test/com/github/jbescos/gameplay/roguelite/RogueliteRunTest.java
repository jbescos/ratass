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
    public void loadoutHasOneDriverAndOneSlotPerModificationCategory() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");

        assertTrue(loadout.equip(RogueliteCardId.CLUB_TUNE));
        assertTrue(loadout.equip(RogueliteCardId.CORNER_EXIT));
        assertTrue(loadout.equip(RogueliteCardId.NITRO_PULSE));
        assertTrue(loadout.isFull());
        assertFalse(loadout.equip(RogueliteCardId.CLUB_TUNE));

        assertTrue(loadout.equip(RogueliteCardId.SPORT_TUNE));
        assertEquals(3, loadout.getModifications().size());
        assertEquals(
                RogueliteCardId.SPORT_TUNE,
                loadout.get(RogueliteSlotType.TUNING));
        assertFalse(loadout.has(RogueliteCardId.CLUB_TUNE));
        assertEquals(
                RogueliteCardId.CORNER_EXIT,
                loadout.get(RogueliteSlotType.TECHNIQUE));
        assertEquals(
                RogueliteCardId.NITRO_PULSE,
                loadout.get(RogueliteSlotType.GADGET));

        loadout.setDriverProfileId("profile01");
        assertEquals("profile01", loadout.getDriverProfileId());
    }

    @Test
    public void oneCardTierUnlocksForEachOfThreeChampionships() {
        RogueliteRun run = new RogueliteRun(17L);

        assertEquals(1, run.getUnlockedTier());
        run.advanceChampionship();
        assertEquals(2, run.getUnlockedTier());
        run.advanceChampionship();
        assertEquals(3, run.getUnlockedTier());
        for (int i = 0; i < 3; i++) {
            run.advanceChampionship();
        }
        assertEquals(3, run.getUnlockedTier());
    }

    @Test
    public void selectedStartingTierOffsetsProgressionAndCapsAtTierThree() {
        RogueliteRun run = new RogueliteRun(18L);

        run.reset(2);
        assertEquals(2, run.getStartingTier());
        assertEquals(2, run.getUnlockedTier());
        run.advanceChampionship();
        assertEquals(3, run.getUnlockedTier());
        run.advanceChampionship();
        assertEquals(3, run.getUnlockedTier());
        run.advanceChampionship();
        assertEquals(3, run.getUnlockedTier());
        assertEquals(4, run.getChampionshipNumber());
    }

    @Test
    public void selectedStartingTierControlsOffersAndSurvivesRestore() {
        RogueliteRun original = new RogueliteRun(19L);
        original.reset(3);
        earnOneReward(original);
        RogueliteRun.Snapshot snapshot = original.snapshot();

        List<RogueliteCardOffer> offers = original.createOffers(20);

        assertFalse(offers.isEmpty());
        for (int i = 0; i < offers.size(); i++) {
            assertEquals(3, offers.get(i).getTier());
        }

        RogueliteRun restored = new RogueliteRun(190L);
        assertTrue(restored.restore(snapshot));
        assertEquals(3, restored.getStartingTier());
        assertEquals(3, restored.getUnlockedTier());
        assertEquals(offerIds(offers), offerIds(restored.createOffers(20)));
    }

    @Test
    public void legacyModificationIdsRestoreIntoTheNewTypedSlots() {
        RogueliteRun original = new RogueliteRun(20L);
        RogueliteRun.Snapshot snapshot = original.snapshot();
        snapshot.player.modificationCardIds = Arrays.asList(
                "TURBOCHARGER",
                "COUNTERSTEER_SERVO",
                "FRONT_SPLITTER");

        RogueliteRun restored = new RogueliteRun(21L);

        assertTrue(restored.restore(snapshot));
        assertEquals(
                RogueliteCardId.CLUB_TUNE,
                restored.getPlayerLoadout().get(RogueliteSlotType.TUNING));
        assertEquals(
                RogueliteCardId.CORNER_EXIT,
                restored.getPlayerLoadout().get(RogueliteSlotType.TECHNIQUE));
        assertEquals(
                RogueliteCardId.GRIP_FAN,
                restored.getPlayerLoadout().get(RogueliteSlotType.GADGET));
    }

    @Test
    public void catalogCardsHaveCompleteIndependentMetadata() {
        List<RogueliteCardDefinition> cards = RogueliteCardCatalog.all();
        Set<Integer> artworkIndexes = new HashSet<Integer>();
        for (int i = 0; i < cards.size(); i++) {
            RogueliteCardDefinition card = cards.get(i);
            assertFalse(card.getSlotType().isDriver());
            assertFalse(card.getEffectText().isEmpty());
            assertTrue(artworkIndexes.add(Integer.valueOf(card.getArtworkIndex())));
        }
    }

    @Test
    public void everyTierOffersEveryModificationSlotType() {
        List<RogueliteCardDefinition> cards = RogueliteCardCatalog.all();

        for (int tier = 1; tier <= DriverProfileCatalog.MAX_TIER; tier++) {
            for (RogueliteSlotType slotType : RogueliteSlotType.modificationSlots()) {
                boolean found = false;
                for (int i = 0; i < cards.size(); i++) {
                    RogueliteCardDefinition card = cards.get(i);
                    if (card.getTier() == tier && card.getSlotType() == slotType) {
                        found = true;
                        break;
                    }
                }
                assertTrue(
                        "Missing " + slotType + " card in tier " + tier,
                        found);
            }
        }
    }

    @Test
    public void worstBenchmarkedDriverIsDefaultAndFirstOffersStayInTierOne() {
        DriverProfileCatalog catalog =
                new DriverProfileCatalog(Arrays.asList(
                        metadata("profile00", 31f),
                        metadata("profile01", 40f),
                        metadata("profile02", 35f),
                        metadata("profile03", 30f)));
        RogueliteRun run = new RogueliteRun(31L, catalog);

        assertEquals(
                "profile01",
                run.getPlayerLoadout().getDriverProfileId());
        run.awardPlayerRacePosition(1, 10);
        List<RogueliteCardOffer> offers = run.createOffers(3);

        assertFalse(offers.isEmpty());
        Set<String> ids = new HashSet<String>();
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer offer = offers.get(i);
            assertTrue(ids.add(offer.getOfferId()));
            assertEquals(1, offer.getTier());
            if (offer.isDriver()) {
                assertEquals("profile02", offer.getDriver().getProfileId());
            }
        }
    }

    @Test
    public void finishingPositionControlsExperienceAndLevelUpRewards() {
        RogueliteRun first = new RogueliteRun(43L);
        RogueliteRun second = new RogueliteRun(44L);
        RogueliteRun third = new RogueliteRun(45L);
        RogueliteRun fourth = new RogueliteRun(46L);

        assertEquals(100, first.awardPlayerRacePosition(1, 10));
        assertEquals(92, second.awardPlayerRacePosition(2, 10));
        assertEquals(84, third.awardPlayerRacePosition(3, 10));
        assertEquals(77, fourth.awardPlayerRacePosition(4, 10));

        assertEquals(2, first.getPlayerProgress().getLevel());
        assertEquals(20, first.getPlayerProgress().getExperience());
        assertEquals(120, first.getPlayerProgress().getExperienceForNextLevel());
        assertEquals(2, second.getPlayerProgress().getLevel());
        assertEquals(2, third.getPlayerProgress().getLevel());
        assertEquals(1, fourth.getPlayerProgress().getLevel());
        assertEquals(1, first.getPlayerProgress().getPendingRewards());
        assertFalse(fourth.getPlayerProgress().hasPendingReward());
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
    public void deferredRewardsUnlockAtTheNextLevelAndRemainQueued() {
        RogueliteRun run = new RogueliteRun(48L);
        levelUpPlayer(run);
        List<RogueliteCardOffer> deferredOffers = run.createOffers(3);

        assertFalse(deferredOffers.isEmpty());
        assertEquals(1, run.getPlayerProgress().getPendingRewards());
        assertTrue(run.deferPlayerRewardsUntilNextLevel());
        assertFalse(run.getPlayerProgress().hasOfferableReward());
        assertTrue(run.createOffers(3).isEmpty());
        assertFalse(run.select(deferredOffers.get(0)));

        RogueliteRun restored = new RogueliteRun(480L);
        assertTrue(restored.restore(run.snapshot()));
        assertEquals(1, restored.getPlayerProgress().getPendingRewards());
        assertFalse(restored.getPlayerProgress().hasOfferableReward());

        restored.awardPlayerRacePosition(1, 10);

        assertTrue(restored.getPlayerProgress().hasOfferableReward());
        assertEquals(2, restored.getPlayerProgress().getPendingRewards());
        assertTrue(restored.select(restored.createOffers(3).get(0)));
        assertEquals(1, restored.getPlayerProgress().getPendingRewards());
        assertTrue(restored.select(restored.createOffers(3).get(0)));
        assertEquals(0, restored.getPlayerProgress().getPendingRewards());
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
            RogueliteCardOffer next =
                    firstModificationForEmptySlot(
                            run.getPlayerLoadout(),
                            run.createOffers(20));
            assertTrue(run.select(next));
            if (!run.getPlayerLoadout().isFull()) {
                earnOneReward(run);
            }
        }

        earnOneReward(run);
        RogueliteCardOffer replacement =
                firstModificationForOccupiedSlot(
                        run.getPlayerLoadout(),
                        run.createOffers(20));
        RogueliteCardId replaced =
                run.getPlayerLoadout().get(replacement.getSlotType());
        assertTrue(run.select(replacement));

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
        run.resolveRivalRewards(Arrays.asList(Integer.valueOf(2)));

        RogueliteCompetitorProgress rival = run.getRivalProgress(2);
        assertEquals(0, rival.getPendingRewards());
        assertTrue(
                !rival.getLoadout().getModifications().isEmpty()
                        || !"profile00".equals(
                        rival.getLoadout().getDriverProfileId()));
    }

    @Test
    public void rivalsFillEmptyCategoriesInsteadOfChoosingAWorseDriver() {
        RogueliteRun run = new RogueliteRun(57L);
        RogueliteLoadout loadout = run.getRivalLoadout(2);
        loadout.setDriverProfileId("profile01");
        assertTrue(loadout.equip(RogueliteCardId.CLUB_TUNE));

        run.awardRivalRacePosition(2, 1, 10);
        run.resolveRivalRewards(Arrays.asList(Integer.valueOf(2)));

        assertEquals("profile01", loadout.getDriverProfileId());
        assertTrue(loadout.has(RogueliteCardId.CLUB_TUNE));
        assertEquals(2, loadout.getModifications().size());
    }

    @Test
    public void rivalsImproveEmptySlotsBeforeRepeatedDriverUpgrades() {
        RogueliteRun run = new RogueliteRun(572L);
        run.reset(3);
        RogueliteLoadout loadout = run.getRivalLoadout(2);
        String defaultDriver = loadout.getDriverProfileId();

        run.awardRivalRacePosition(2, 1, 10);
        run.resolveRivalRewards(Arrays.asList(Integer.valueOf(2)));

        assertEquals(defaultDriver, loadout.getDriverProfileId());
        assertEquals(1, loadout.getModifications().size());
    }

    @Test
    public void tierThreeRivalSimulationBuildsABalancedCompetitiveLoadout() {
        RogueliteRun run = new RogueliteRun(573L);
        run.reset(3);
        RogueliteLoadout loadout = run.getRivalLoadout(2);
        String defaultDriver = loadout.getDriverProfileId();

        for (int race = 0; race < 18; race++) {
            run.awardRivalRacePosition(2, 1, 10);
            run.resolveRivalRewards(Arrays.asList(Integer.valueOf(2)));
        }

        assertTrue(loadout.isFull());
        assertEquals(3, run.getDriverTier(loadout.getDriverProfileId()));
        for (int i = 0; i < loadout.getModifications().size(); i++) {
            assertEquals(
                    3,
                    RogueliteCardCatalog.get(loadout.getModifications().get(i)).getTier());
        }
        assertFalse(defaultDriver.equals(loadout.getDriverProfileId()));
    }

    @Test
    public void rivalsPreferHigherTierEquipmentAfterEveryCategoryIsFilled() {
        RogueliteRun run = new RogueliteRun(571L);
        RogueliteLoadout loadout = run.getRivalLoadout(2);
        loadout.setDriverProfileId("profile03");
        assertTrue(loadout.equip(RogueliteCardId.CLUB_TUNE));
        assertTrue(loadout.equip(RogueliteCardId.CORNER_EXIT));
        assertTrue(loadout.equip(RogueliteCardId.NITRO_PULSE));
        run.advanceChampionship();

        run.awardRivalRacePosition(2, 1, 10);
        run.resolveRivalRewards(Arrays.asList(Integer.valueOf(2)));

        int tierTwoSlots =
                run.getDriverTier(loadout.getDriverProfileId()) == 2 ? 1 : 0;
        List<RogueliteCardId> cards = loadout.getModifications();
        for (int i = 0; i < cards.size(); i++) {
            if (RogueliteCardCatalog.get(cards.get(i)).getTier() == 2) {
                tierTwoSlots++;
            }
        }
        assertEquals(1, tierTwoSlots);
    }

    @Test
    public void offersContainOnlyTheCurrentChampionshipTier() {
        RogueliteRun run = new RogueliteRun(58L);
        run.advanceChampionship();
        earnOneReward(run);

        List<RogueliteCardOffer> offers = run.createOffers(20);

        assertFalse(offers.isEmpty());
        for (int i = 0; i < offers.size(); i++) {
            assertEquals(2, offers.get(i).getTier());
        }
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

    private static RogueliteCardOffer firstModificationForEmptySlot(
            RogueliteLoadout loadout,
            List<RogueliteCardOffer> offers) {
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer offer = offers.get(i);
            if (!offer.isDriver()
                    && !loadout.hasCardIn(offer.getSlotType())) {
                return offer;
            }
        }
        throw new AssertionError("Expected a modification for an empty slot.");
    }

    private static RogueliteCardOffer firstModificationForOccupiedSlot(
            RogueliteLoadout loadout,
            List<RogueliteCardOffer> offers) {
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer offer = offers.get(i);
            if (!offer.isDriver()
                    && loadout.hasCardIn(offer.getSlotType())) {
                return offer;
            }
        }
        throw new AssertionError("Expected a modification for an occupied slot.");
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

    private static DriverProfileMetadata metadata(String id, float averageLapSeconds) {
        return new DriverProfileMetadata(
                id,
                "",
                "test",
                50f,
                50f,
                50f,
                1f,
                averageLapSeconds - 1f,
                averageLapSeconds,
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
