package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.Test;

public class RogueliteRunTest {
    @Test
    public void customRulesFilterOffersAndChangeTheExperienceCurve() {
        RogueliteRun run = new RogueliteRun(9L);
        CustomGameRules rules = new CustomGameRules();
        rules.toggleCardType(RogueliteSlotType.DRIVER);
        rules.toggleCardType(RogueliteSlotType.TUNING);
        rules.toggleCardType(RogueliteSlotType.POWERUP);
        rules.toggleCardType(RogueliteSlotType.REVENGE);
        rules.toggleTier(1);
        rules.toggleTier(3);
        rules.setTierUnlockLevel(2, 1);
        rules.setLevelXpIncrement(100);
        run.configureGameRules(rules);
        run.reset();

        run.awardPlayerRacePosition(1, 10);
        List<RogueliteCardOffer> offers = run.createOffers(20);

        assertEquals(2, run.getUnlockedTier());
        assertFalse(offers.isEmpty());
        for (int i = 0; i < offers.size(); i++) {
            assertEquals(RogueliteSlotType.TECHNIQUE, offers.get(i).getSlotType());
            assertEquals(2, offers.get(i).getTier());
        }
        assertEquals(2, run.getPlayerProgress().getLevel());
        run.awardPlayerRacePosition(1, 10);
        assertEquals(2, run.getPlayerProgress().getLevel());
        assertEquals(20, run.getPlayerProgress().getExperience());
        assertEquals(180, run.getPlayerProgress().getExperienceForNextLevel());
    }

    @Test
    public void customRulesFilterCardTypesWithinTheUnlockedTier() {
        RogueliteRun run = new RogueliteRun(901L);
        CustomGameRules rules = new CustomGameRules();
        for (RogueliteSlotType type : RogueliteSlotType.values()) {
            if (type != RogueliteSlotType.TUNING) {
                assertTrue(rules.toggleTierCardType(1, type));
            }
        }
        run.configureGameRules(rules);
        run.awardPlayerRacePosition(1, 10);

        List<RogueliteCardOffer> offers = run.createOffers(20);

        assertFalse(offers.isEmpty());
        for (RogueliteCardOffer offer : offers) {
            assertEquals(1, offer.getTier());
            assertEquals(RogueliteSlotType.TUNING, offer.getSlotType());
        }
    }

    @Test
    public void customRacecraftCapAppliesIndependentlyToEveryCompetitor() {
        RogueliteRun run = new RogueliteRun(91L);
        CustomGameRules rules = new CustomGameRules();
        rules.setRacecraftXpPerLapCap(15);
        run.configureGameRules(rules);
        run.reset();

        assertEquals(15, run.getRacecraftXpPerLapCap());
        assertEquals(15, run.awardPlayerRacecraftExperience(30));
        assertEquals(15, run.awardRivalRacecraftExperience(7, 30));
        assertEquals(0, run.awardPlayerRacecraftExperience(1));
    }

    @Test
    public void loadoutHasOneDriverAndOneSlotPerModificationCategory() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");

        assertTrue(loadout.equip(RogueliteCardId.CLUB_TUNE));
        assertTrue(loadout.equip(RogueliteCardId.CORNER_FOCUS));
        assertTrue(loadout.equip(RogueliteCardId.NITRO_PULSE));
        assertTrue(loadout.equip(RogueliteCardId.DRAFT_MAGNET));
        assertTrue(loadout.isFull());
        assertFalse(loadout.equip(RogueliteCardId.CLUB_TUNE));

        assertTrue(loadout.equip(RogueliteCardId.SPORT_TUNE));
        assertEquals(4, loadout.getModifications().size());
        assertEquals(
                RogueliteCardId.SPORT_TUNE,
                loadout.get(RogueliteSlotType.TUNING));
        assertFalse(loadout.has(RogueliteCardId.CLUB_TUNE));
        assertEquals(
                RogueliteCardId.CORNER_FOCUS,
                loadout.get(RogueliteSlotType.TECHNIQUE));
        assertEquals(
                RogueliteCardId.NITRO_PULSE,
                loadout.get(RogueliteSlotType.POWERUP));
        assertEquals(
                RogueliteCardId.DRAFT_MAGNET,
                loadout.get(RogueliteSlotType.REVENGE));

        loadout.setDriverProfileId("profile01");
        assertEquals("profile01", loadout.getDriverProfileId());
    }

    @Test
    public void cardTiersUnlockAtBalancedProgressionLevels() {
        RogueliteRun run = new RogueliteRun(17L);

        assertEquals(1, run.getUnlockedTier());
        run.getPlayerProgress().restore(
                RogueliteRun.TIER_TWO_LEVEL - 1, 0, 0);
        assertEquals(1, run.getUnlockedTier());
        run.getPlayerProgress().restore(RogueliteRun.TIER_TWO_LEVEL, 0, 0);
        assertEquals(2, run.getUnlockedTier());
        run.getPlayerProgress().restore(
                RogueliteRun.TIER_THREE_LEVEL - 1, 0, 0);
        assertEquals(2, run.getUnlockedTier());
        run.getPlayerProgress().restore(RogueliteRun.TIER_THREE_LEVEL, 0, 0);
        assertEquals(3, run.getUnlockedTier());
    }

    @Test
    public void championshipProgressDoesNotGrantLevelsOrChangeCardTier() {
        RogueliteRun run = new RogueliteRun(171L);
        RogueliteCompetitorProgress player = run.getPlayerProgress();
        RogueliteCompetitorProgress rival = run.getRivalProgress(4);

        run.advanceProgression();
        assertEquals(1, player.getLevel());
        assertEquals(0, player.getPendingRewards());
        assertEquals(0, player.getExperience());
        assertEquals(1, rival.getLevel());
        assertEquals(0, rival.getPendingRewards());
        assertEquals(0, rival.getExperience());
        assertEquals(1, run.getUnlockedTier());
    }

    @Test
    public void restartingAfterTheFinalKeepsProgressAndLoadout() {
        RogueliteRun run = new RogueliteRun(172L);
        run.awardPlayerRacePosition(1, 10);
        RogueliteCardOffer selected = firstModification(run.createOffers(20));
        assertTrue(run.select(selected));
        int level = run.getPlayerProgress().getLevel();
        int experience = run.getPlayerProgress().getExperience();

        run.advanceProgression();
        run.restartChampionship();

        assertEquals(1, run.getChampionshipNumber());
        assertEquals(level, run.getPlayerProgress().getLevel());
        assertEquals(experience, run.getPlayerProgress().getExperience());
        assertTrue(run.getPlayerLoadout().has(selected.getCard().getId()));
    }

    @Test
    public void rivalsUnlockTiersFromTheirOwnLevels() {
        RogueliteRun run = new RogueliteRun(173L);
        run.getRivalProgress(4).restore(RogueliteRun.TIER_TWO_LEVEL, 0, 0);

        assertEquals(1, run.getUnlockedTier());
        assertEquals(2, run.getRivalUnlockedTier(4));
    }

    @Test
    public void selectedStartingTierSetsTheMinimumTierAcrossChampionships() {
        RogueliteRun run = new RogueliteRun(18L);

        run.reset(2);
        assertEquals(2, run.getStartingTier());
        assertEquals(2, run.getUnlockedTier());
        run.advanceChampionship();
        assertEquals(2, run.getUnlockedTier());
        run.advanceChampionship();
        assertEquals(2, run.getUnlockedTier());
        run.advanceChampionship();
        assertEquals(2, run.getUnlockedTier());
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
                RogueliteCardId.CORNER_FOCUS,
                restored.getPlayerLoadout().get(RogueliteSlotType.TECHNIQUE));
        assertEquals(
                RogueliteCardId.GRIP_FAN,
                restored.getPlayerLoadout().get(RogueliteSlotType.POWERUP));
    }

    @Test
    public void catalogCardsHaveCompleteIndependentMetadata() {
        List<RogueliteCardDefinition> cards = RogueliteCardCatalog.all();
        for (int i = 0; i < cards.size(); i++) {
            RogueliteCardDefinition card = cards.get(i);
            assertFalse(card.getSlotType().isDriver());
            assertFalse(card.getEffectText().isEmpty());
            String playerFacingText =
                    (card.getTitle() + " " + card.getDescription() + " " + card.getEffectText())
                            .toLowerCase(Locale.ROOT);
            assertFalse(playerFacingText.contains("drag"));
            assertTrue(card.getArtworkIndex() >= 0);
            assertTrue(card.getArtworkIndex() < RogueliteCardDefinition.ARTWORK_CAPACITY);
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
    public void randomTierOneDriverStartsTheRunAndFirstOffersStayInTierOne() {
        DriverProfileCatalog catalog =
                new DriverProfileCatalog(Arrays.asList(
                        metadata("profile00", 31f),
                        metadata("profile01", 40f),
                        metadata("profile02", 35f),
                        metadata("profile03", 30f)));
        RogueliteRun run = new RogueliteRun(31L, catalog);
        String startingDriver =
                run.getPlayerLoadout().getDriverProfileId();

        assertEquals(1, run.getDriverTier(startingDriver));
        run.awardPlayerRacePosition(1, 10);
        List<RogueliteCardOffer> offers = run.createOffers(3);

        assertFalse(offers.isEmpty());
        Set<String> ids = new HashSet<String>();
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer offer = offers.get(i);
            assertTrue(ids.add(offer.getOfferId()));
            assertEquals(1, offer.getTier());
            if (offer.isDriver()) {
                assertNotEquals(startingDriver, offer.getDriver().getProfileId());
                assertEquals(1, run.getDriverTier(offer.getDriver().getProfileId()));
            }
        }
    }

    @Test
    public void driverIsNotReservedAsOneOfTheThreeOffers() {
        boolean foundOfferSetWithoutDriver = false;
        for (long seed = 1L; seed <= 100L; seed++) {
            RogueliteRun run = new RogueliteRun(seed);
            earnOneReward(run);
            List<RogueliteCardOffer> offers = run.createOffers(3);
            boolean hasDriver = false;
            for (int i = 0; i < offers.size(); i++) {
                hasDriver |= offers.get(i).isDriver();
            }
            if (!hasDriver) {
                foundOfferSetWithoutDriver = true;
                break;
            }
        }
        assertTrue(foundOfferSetWithoutDriver);
    }

    @Test
    public void everyStartingTierStillBeginsWithARandomTierOneDriver() {
        DriverProfileCatalog catalog =
                new DriverProfileCatalog(Arrays.asList(
                        metadata("profile00", 31f),
                        metadata("profile01", 40f),
                        metadata("profile02", 35f),
                        metadata("profile03", 30f)));
        Set<String> selectedDrivers = new HashSet<String>();

        for (long seed = 1L; seed <= 32L; seed++) {
            RogueliteRun run = new RogueliteRun(seed, catalog);
            selectedDrivers.add(run.getPlayerLoadout().getDriverProfileId());
            assertEquals(
                    1,
                    run.getDriverTier(
                            run.getPlayerLoadout().getDriverProfileId()));
            for (int startingTier = 1;
                    startingTier <= DriverProfileCatalog.MAX_TIER;
                    startingTier++) {
                run.reset(startingTier);
                assertEquals(
                        1,
                        run.getDriverTier(
                                run.getPlayerLoadout().getDriverProfileId()));
            }
        }

        assertEquals(2, selectedDrivers.size());
    }

    @Test
    public void everyRivalBeginsWithAnIndependentRandomTierOneDriver() {
        DriverProfileCatalog catalog =
                new DriverProfileCatalog(Arrays.asList(
                        metadata("profile00", 31f),
                        metadata("profile01", 40f),
                        metadata("profile02", 35f),
                        metadata("profile03", 30f)));
        Set<String> selectedDrivers = new HashSet<String>();

        for (long seed = 1L; seed <= 32L; seed++) {
            RogueliteRun run = new RogueliteRun(seed, catalog);
            for (int vehicleId = 1; vehicleId <= 4; vehicleId++) {
                String driver =
                        run.getRivalLoadout(vehicleId).getDriverProfileId();
                selectedDrivers.add(driver);
                assertEquals(1, run.getDriverTier(driver));
            }
        }

        assertEquals(2, selectedDrivers.size());
    }

    @Test
    public void loadedBenchmarksRepairAFallbackDriverOutsideTheRealTierOne() {
        DriverProfileCatalog benchmarkCatalog =
                new DriverProfileCatalog(Arrays.asList(
                        metadata("profile00", 31f),
                        metadata("profile01", 40f),
                        metadata("profile02", 35f),
                        metadata("profile03", 30f)));

        for (long seed = 1L; seed <= 32L; seed++) {
            RogueliteRun run = new RogueliteRun(seed);
            run.reset(1);
            run.configureDriverCatalog(benchmarkCatalog);

            assertEquals(
                    1,
                    run.getDriverTier(
                            run.getPlayerLoadout().getDriverProfileId()));
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
        assertEquals(82, first.getPlayerProgress().getExperienceForNextLevel());
        assertEquals(2, second.getPlayerProgress().getLevel());
        assertEquals(2, third.getPlayerProgress().getLevel());
        assertEquals(1, fourth.getPlayerProgress().getLevel());
        assertEquals(1, first.getPlayerProgress().getPendingRewards());
        assertFalse(fourth.getPlayerProgress().hasPendingReward());
    }

    @Test
    public void finishingPositionExperienceIsIndependentFromTheLapCap() {
        RogueliteRun run = new RogueliteRun(431L);
        CustomGameRules rules = new CustomGameRules();
        rules.setRacecraftXpPerLapCap(
                CustomGameRules.MIN_RACECRAFT_XP_PER_LAP_CAP);
        run.configureGameRules(rules);

        assertEquals(100, run.awardPlayerRacePosition(1, 10));
        assertEquals(0, run.getPlayerProgress().getLapExperience());
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
    public void skippingConsumesTheRewardAndAllowsTheNextLevel() {
        RogueliteRun run = new RogueliteRun(471L);
        levelUpPlayer(run);

        assertTrue(run.skipPlayerReward());
        assertFalse(run.getPlayerProgress().hasPendingReward());
        assertTrue(run.getPlayerLoadout().getModifications().isEmpty());
        assertTrue(run.createOffers(3).isEmpty());

        assertEquals(61, run.awardPlayerExperience(61));
        assertEquals(2, run.getPlayerProgress().getLevel());
        assertEquals(1, run.awardPlayerExperience(1));
        assertEquals(3, run.getPlayerProgress().getLevel());
        assertTrue(run.getPlayerProgress().hasPendingReward());
    }

    @Test
    public void unresolvedRewardPausesXpAndLegacyQueuesRestoreAsOneChoice() {
        RogueliteRun run = new RogueliteRun(48L);
        levelUpPlayer(run);
        RogueliteRun.Snapshot snapshot = run.snapshot();
        snapshot.player.pendingRewards = 4;
        snapshot.player.rewardDeferredUntilLevel = 99;

        RogueliteRun restored = new RogueliteRun(480L);
        assertTrue(restored.restore(snapshot));
        assertEquals(1, restored.getPlayerProgress().getPendingRewards());
        assertTrue(restored.getPlayerProgress().hasOfferableReward());
        assertFalse(restored.createOffers(3).isEmpty());

        assertEquals(0, restored.awardPlayerRacePosition(1, 10));
        assertEquals(2, restored.getPlayerProgress().getLevel());
        assertEquals(20, restored.getPlayerProgress().getExperience());
        assertEquals(1, restored.getPlayerProgress().getPendingRewards());

        assertTrue(restored.select(restored.createOffers(3).get(0)));
        assertEquals(0, restored.getPlayerProgress().getPendingRewards());
        assertEquals(100, restored.awardPlayerRacePosition(1, 10));
        assertEquals(3, restored.getPlayerProgress().getLevel());
        assertEquals(1, restored.getPlayerProgress().getPendingRewards());
    }

    @Test
    public void selectedCardsNeverReturnAfterReplacementOrRestore() {
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
        RogueliteRun restored = new RogueliteRun(50L);
        assertTrue(restored.restore(run.snapshot()));
        for (int attempt = 0; attempt < 10; attempt++) {
            List<RogueliteCardOffer> laterOffers = restored.createOffers(20);
            assertFalse(containsOffer(laterOffers, "card:" + replaced.name()));
            assertFalse(containsOffer(laterOffers, replacement.getOfferId()));
        }
    }

    @Test
    public void selectedDriversNeverReturnAfterReplacementOrRestore() {
        RogueliteRun run = new RogueliteRun(51L);
        CustomGameRules driverOnlyRules = new CustomGameRules();
        for (RogueliteSlotType type : RogueliteSlotType.values()) {
            if (type != RogueliteSlotType.DRIVER) {
                assertTrue(driverOnlyRules.toggleCardType(type));
            }
        }
        run.configureGameRules(driverOnlyRules);
        String defaultDriver = run.getPlayerLoadout().getDriverProfileId();

        earnOneReward(run);
        RogueliteCardOffer selectedDriver =
                firstDriver(run.createOffers(20));
        assertTrue(run.select(selectedDriver));

        earnOneReward(run);
        RogueliteRun restored = new RogueliteRun(52L);
        restored.configureGameRules(driverOnlyRules);
        assertTrue(restored.restore(run.snapshot()));
        List<RogueliteCardOffer> laterOffers = restored.createOffers(20);
        assertFalse(containsOffer(laterOffers, selectedDriver.getOfferId()));
        assertFalse(containsOffer(laterOffers, "driver:" + defaultDriver));
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
    public void rivalsEquipRevengeAfterEarningEnoughTierOneRewards() {
        RogueliteRun run = new RogueliteRun(574L);
        RogueliteLoadout loadout = run.getRivalLoadout(2);

        for (int race = 0; race < 8 && !loadout.isFull(); race++) {
            run.awardRivalRacePosition(2, 1, 10);
            run.resolveRivalRewards(Arrays.asList(Integer.valueOf(2)));
        }

        assertTrue(loadout.isFull());
        RogueliteCardId revengeCard =
                loadout.get(RogueliteSlotType.REVENGE);
        assertTrue(revengeCard != null);
        assertEquals(
                RogueliteSlotType.REVENGE,
                RogueliteCardCatalog.get(revengeCard).getSlotType());
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
        assertTrue(loadout.equip(RogueliteCardId.CORNER_FOCUS));
        assertTrue(loadout.equip(RogueliteCardId.NITRO_PULSE));
        run.getRivalProgress(2).restore(RogueliteRun.TIER_TWO_LEVEL, 0, 1);
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
    public void rivalsFillAnEmptySlotBeforeReplacingEquippedCards() {
        RogueliteRun run = new RogueliteRun(575L);
        RogueliteCompetitorProgress rival = run.getRivalProgress(2);
        RogueliteLoadout loadout = rival.getLoadout();
        assertTrue(loadout.equip(RogueliteCardId.AERO_TRIM));
        assertTrue(loadout.equip(RogueliteCardId.CORNER_FOCUS));
        assertTrue(loadout.equip(RogueliteCardId.NITRO_PULSE));

        RogueliteCardOffer selected = run.chooseRivalOffer(
                rival,
                Arrays.asList(
                        modificationOffer(RogueliteCardId.STRAIGHT_FOCUS),
                        modificationOffer(RogueliteCardId.SPORT_TUNE),
                        modificationOffer(RogueliteCardId.DRAFT_MAGNET)));

        assertEquals(RogueliteSlotType.REVENGE, selected.getSlotType());
    }

    @Test
    public void rivalsUpgradeTheOnlyLowerTierSlotBeforeConsideringSynergy() {
        RogueliteRun run = new RogueliteRun(576L);
        RogueliteCompetitorProgress rival = run.getRivalProgress(2);
        RogueliteLoadout loadout = rival.getLoadout();
        assertTrue(loadout.equip(RogueliteCardId.RACE_TUNE));
        assertTrue(loadout.equip(RogueliteCardId.CORNER_EXPERT));
        assertTrue(loadout.equip(RogueliteCardId.PHASE_SHIELD));
        assertTrue(loadout.equip(RogueliteCardId.DRAFT_MAGNET));

        RogueliteCardOffer selected = run.chooseRivalOffer(
                rival,
                Arrays.asList(
                        modificationOffer(RogueliteCardId.STRAIGHT_EXPERT),
                        modificationOffer(RogueliteCardId.LOW_DRAG_BODY),
                        modificationOffer(RogueliteCardId.EMP_SNARE)));

        assertEquals(RogueliteCardId.EMP_SNARE, selected.getCard().getId());
    }

    @Test
    public void rivalsUsuallyChooseTheStrongestSameTierSynergyButStillExplore() {
        RogueliteRun run = new RogueliteRun(577L);
        RogueliteCompetitorProgress rival = run.getRivalProgress(2);
        RogueliteLoadout loadout = rival.getLoadout();
        assertTrue(loadout.equip(RogueliteCardId.AERO_TRIM));
        assertTrue(loadout.equip(RogueliteCardId.CORNER_FOCUS));
        assertTrue(loadout.equip(RogueliteCardId.NITRO_PULSE));
        assertTrue(loadout.equip(RogueliteCardId.DRAFT_MAGNET));
        List<RogueliteCardOffer> offers = Arrays.asList(
                modificationOffer(RogueliteCardId.STRAIGHT_FOCUS),
                modificationOffer(RogueliteCardId.DRIFT_FOCUS),
                modificationOffer(RogueliteCardId.SPORT_TUNE));

        int strongestSelections = 0;
        int exploratorySelections = 0;
        for (int selection = 0; selection < 1000; selection++) {
            RogueliteCardOffer selected = run.chooseRivalOffer(rival, offers);
            if (selected.getCard().getId() == RogueliteCardId.STRAIGHT_FOCUS) {
                strongestSelections++;
            } else {
                exploratorySelections++;
            }
        }

        assertTrue(strongestSelections >= 850);
        assertTrue(exploratorySelections > 0);
    }

    @Test
    public void rivalsFallBackToRevengeWhenNoStatSynergyImproves() {
        RogueliteRun run = new RogueliteRun(578L);
        RogueliteCompetitorProgress rival = run.getRivalProgress(2);
        RogueliteLoadout loadout = rival.getLoadout();
        assertTrue(loadout.equip(RogueliteCardId.AERO_TRIM));
        assertTrue(loadout.equip(RogueliteCardId.STRAIGHT_FOCUS));
        assertTrue(loadout.equip(RogueliteCardId.NITRO_PULSE));
        assertTrue(loadout.equip(RogueliteCardId.DRAFT_MAGNET));

        RogueliteCardOffer selected = run.chooseRivalOffer(
                rival,
                Arrays.asList(
                        modificationOffer(RogueliteCardId.DRIFT_FOCUS),
                        modificationOffer(RogueliteCardId.MIRROR_DUO),
                        modificationOffer(RogueliteCardId.TAR_TETHER)));

        assertEquals(RogueliteCardId.TAR_TETHER, selected.getCard().getId());
    }

    @Test
    public void offersContainOnlyTheTierUnlockedByTheCompetitorLevel() {
        RogueliteRun run = new RogueliteRun(58L);
        run.getPlayerProgress().restore(RogueliteRun.TIER_TWO_LEVEL, 0, 1);

        List<RogueliteCardOffer> offers = run.createOffers(20);

        assertFalse(offers.isEmpty());
        for (int i = 0; i < offers.size(); i++) {
            assertEquals(2, offers.get(i).getTier());
        }
    }

    @Test
    public void representativeSingleChampionshipProgressionReachesTierThree() {
        RogueliteRun run = new RogueliteRun(581L);
        int tierTwoCircuit = 0;
        int tierThreeCircuit = 0;

        for (int circuit = 1; circuit <= 19; circuit++) {
            for (int lap = 0; lap < CustomGameRules.DEFAULT_LAPS; lap++) {
                awardRacecraftAndResolve(run, RogueliteExperienceAwards.PASS_RIVAL);
                awardRacecraftAndResolve(run, RogueliteExperienceAwards.FASTEST_LAP);
                if (lap == 0) {
                    awardRacecraftAndResolve(run, RogueliteExperienceAwards.REVENGE);
                }
                if (circuit % 2 == 0 && lap == 1) {
                    awardRacecraftAndResolve(
                            run,
                            RogueliteExperienceAwards.PUSH_RIVAL_OFF_ROAD);
                }
                int driftSeconds = lap == CustomGameRules.DEFAULT_LAPS - 1 ? 4 : 3;
                for (int second = 0; second < driftSeconds; second++) {
                    awardRacecraftAndResolve(
                            run,
                            RogueliteExperienceAwards.DRIFT_SECOND);
                }
                run.resetPlayerLapExperience();
            }
            run.awardPlayerRacePosition(3, 10);
            resolvePlayerReward(run);

            if (tierTwoCircuit == 0 && run.getUnlockedTier() >= 2) {
                tierTwoCircuit = circuit;
            }
            if (tierThreeCircuit == 0 && run.getUnlockedTier() >= 3) {
                tierThreeCircuit = circuit;
            }
        }

        assertTrue(tierTwoCircuit > 0 && tierTwoCircuit <= 10);
        assertTrue(tierThreeCircuit > 0 && tierThreeCircuit <= 19);
        assertTrue(
                run.getPlayerProgress().getLevel()
                        >= RogueliteRun.TIER_THREE_LEVEL);
    }

    @Test
    public void onlyAnEventHeavyLapReachesTheRacecraftCap() {
        RogueliteRun run = new RogueliteRun(582L);

        for (int overtake = 0; overtake < 3; overtake++) {
            awardRacecraftAndResolve(run, RogueliteExperienceAwards.PASS_RIVAL);
        }
        awardRacecraftAndResolve(run, RogueliteExperienceAwards.FASTEST_LAP);
        awardRacecraftAndResolve(run, RogueliteExperienceAwards.REVENGE);
        awardRacecraftAndResolve(run, RogueliteExperienceAwards.PUSH_RIVAL_OFF_ROAD);
        for (int second = 0; second < 4; second++) {
            awardRacecraftAndResolve(run, RogueliteExperienceAwards.DRIFT_SECOND);
        }

        assertEquals(26, run.getPlayerProgress().getLapExperience());
        assertEquals(1, run.getPlayerProgress().getLevel());
        awardRacecraftAndResolve(run, RogueliteExperienceAwards.PASS_RIVAL);
        awardRacecraftAndResolve(run, RogueliteExperienceAwards.PASS_RIVAL);
        assertEquals(
                RogueliteExperienceAwards.MAX_RACECRAFT_XP_PER_LAP,
                run.getPlayerProgress().getLapExperience());
        assertTrue(
                RogueliteExperienceAwards.MAX_RACECRAFT_XP_PER_LAP * 2
                        < run.getPlayerProgress().getExperienceForNextLevel());
    }

    @Test
    public void racecraftExperienceIsCappedPerCompetitorAndRestored() {
        RogueliteRun run = new RogueliteRun(583L);

        assertEquals(30, run.awardPlayerRacecraftExperience(100));
        assertEquals(0, run.awardPlayerRacecraftExperience(1));
        assertEquals(
                RogueliteExperienceAwards.MAX_RACECRAFT_XP_PER_LAP,
                run.getPlayerProgress().getLapExperience());
        assertEquals(30, run.awardRivalRacecraftExperience(7, 200));

        RogueliteRun restored = new RogueliteRun(584L);
        assertTrue(restored.restore(run.snapshot()));
        assertEquals(30, restored.getPlayerProgress().getLapExperience());
        assertEquals(30, restored.getRivalProgress(7).getLapExperience());

        restored.resetPlayerLapExperience();
        assertEquals(0, restored.getPlayerProgress().getLapExperience());
        assertEquals(30, restored.getRivalProgress(7).getLapExperience());
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

    private static void awardAndResolve(RogueliteRun run, int experience) {
        run.awardPlayerExperience(experience);
        resolvePlayerReward(run);
    }

    private static void awardRacecraftAndResolve(
            RogueliteRun run,
            int experience) {
        run.awardPlayerRacecraftExperience(experience);
        resolvePlayerReward(run);
    }

    private static void resolvePlayerReward(RogueliteRun run) {
        if (!run.getPlayerProgress().hasPendingReward()) {
            return;
        }
        List<RogueliteCardOffer> offers = run.createOffers(3);
        assertFalse(offers.isEmpty());
        assertTrue(run.select(offers.get(0)));
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

    private static RogueliteCardOffer modificationOffer(RogueliteCardId cardId) {
        return RogueliteCardOffer.modification(RogueliteCardCatalog.get(cardId));
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
