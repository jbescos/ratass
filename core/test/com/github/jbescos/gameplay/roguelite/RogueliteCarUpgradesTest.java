package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.github.jbescos.RatassGame;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class RogueliteCarUpgradesTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void nullLoadoutKeepsEveryModifierNeutral() {
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(null);

        assertFalse(upgrades.isEnabled());
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getDragMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getMassMultiplier(), EPSILON);
        assertEquals(0.58f, upgrades.adjustSurfaceGrip(0.58f), EPSILON);
        assertFalse(upgrades.isGadgetReady());
        assertTrue(upgrades.getActiveCardIds().isEmpty());
        assertEquals(0f, upgrades.consumeForwardLaunchSpeedRatio(), EPSILON);
    }

    @Test
    public void everyCatalogCardHasAnEffectAndUniqueArtwork() {
        List<RogueliteCardDefinition> cards = RogueliteCardCatalog.all();
        Set<Integer> artworkIndexes = new HashSet<Integer>();
        for (int i = 0; i < cards.size(); i++) {
            RogueliteCardDefinition card = cards.get(i);
            RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
            upgrades.configure(loadout(card.getId()));
            assertTrue(upgrades.isEnabled());
            assertTrue(artworkIndexes.add(Integer.valueOf(card.getArtworkIndex())));
        }
        assertEquals(cards.size(), artworkIndexes.size());
    }

    @Test
    public void tuningCardsExposeDistinctMassAndAerodynamicSetups() {
        RogueliteCarUpgrades lightweight = configured(RogueliteCardId.SPORT_TUNE);
        RogueliteCarUpgrades heavyweight = configured(RogueliteCardId.HEAVYWEIGHT_TUNE);
        RogueliteCarUpgrades aero = configured(RogueliteCardId.CHAMPIONSHIP_TUNE);
        RogueliteCarUpgrades streamline = configured(RogueliteCardId.AERO_TRIM);
        RogueliteCarUpgrades drift = configured(RogueliteCardId.DRIFT_DIFFERENTIAL);

        assertEquals(0.98f, lightweight.getMassMultiplier(), EPSILON);
        assertEquals(1.16f, heavyweight.getMassMultiplier(), EPSILON);
        assertTrue(heavyweight.getFrontCollisionPushMultiplier() > 1f);
        assertEquals(1.10f, aero.getMassMultiplier(), EPSILON);
        assertEquals(0.80f, aero.getDragMultiplier(), EPSILON);
        assertTrue(aero.getMaxSpeedMultiplier() > heavyweight.getMaxSpeedMultiplier());
        assertTrue(streamline.getDragMultiplier() < 1f);
        assertTrue(streamline.getGripMultiplier(0f) > 1f);
        assertTrue(drift.getGripMultiplier(0f) > streamline.getGripMultiplier(0f));
        assertTrue(drift.getGripMultiplier(0.50f) < drift.getGripMultiplier(0f));
        assertTrue(drift.getSteeringMultiplier(0f) > 1f);
    }

    @Test
    public void everyTierHasFourTuningChoices() {
        int[] counts = new int[4];
        for (RogueliteCardDefinition card : cardsForSlot(RogueliteSlotType.TUNING)) {
            counts[card.getTier()]++;
        }

        assertEquals(4, counts[1]);
        assertEquals(4, counts[2]);
        assertEquals(4, counts[3]);
    }

    @Test
    public void carStatPreviewUsesTheOfferedCardAsASlotReplacement() {
        RogueliteLoadout loadout = loadout(RogueliteCardId.CLUB_TUNE);

        RogueliteCarStatSnapshot equipped =
                RogueliteCarStatSnapshot.from(loadout, null);
        RogueliteCarStatSnapshot preview =
                RogueliteCarStatSnapshot.from(
                        loadout,
                        RogueliteCardId.LOW_DRAG_BODY);

        assertEquals(1.06f, equipped.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.38f, preview.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f / 0.78f, preview.getAerodynamicEfficiency(), EPSILON);
        assertTrue(preview.getMaxSpeedMultiplier() > equipped.getMaxSpeedMultiplier());
    }

    @Test
    public void conditionalCardsDoNotMisrepresentPassiveCarStats() {
        RogueliteLoadout loadout = loadout(RogueliteCardId.AERO_TRIM);

        RogueliteCarStatSnapshot tuning =
                RogueliteCarStatSnapshot.from(loadout, null);
        RogueliteCarStatSnapshot withTechniquePreview =
                RogueliteCarStatSnapshot.from(
                        loadout,
                        RogueliteCardId.DRIFT_SLINGSHOT);

        assertEquals(
                tuning.getAccelerationMultiplier(),
                withTechniquePreview.getAccelerationMultiplier(),
                EPSILON);
        assertEquals(
                tuning.getAerodynamicEfficiency(),
                withTechniquePreview.getAerodynamicEfficiency(),
                EPSILON);
    }

    @Test
    public void tuningAndTechniqueEffectsComposeWithoutPairSpecificRules() {
        RogueliteCarUpgrades streamlinedDraft = configured(
                RogueliteCardId.AERO_TRIM,
                RogueliteCardId.DRAFT_HUNTER);
        RogueliteCarUpgrades driftRelease = configured(
                RogueliteCardId.DRIFT_DIFFERENTIAL,
                RogueliteCardId.DRIFT_SLINGSHOT);

        update(streamlinedDraft, 0.1f, 1f, true, 0.02f, 0.7f, 0.4f, 0f, 1f, 0f, 0.8f, 0f);
        assertEquals(0.92f, streamlinedDraft.getDragMultiplier(), EPSILON);
        assertEquals(1.25f, streamlinedDraft.getSlipstreamStrengthMultiplier(), EPSILON);

        for (int i = 0; i < 15; i++) {
            update(driftRelease, 0.1f, 1f, true, 0.24f, 0.7f, 0f, 0.2f, 0.2f, 0.2f, 0f, 0f);
        }
        update(driftRelease, 0.1f, 1f, true, 0.05f, 0.7f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertTrue(driftRelease.getAccelerationMultiplier() > 1.20f);
        assertEquals(1.20f, driftRelease.getGripMultiplier(0.05f), EPSILON);
        assertEquals(1.16f, driftRelease.getGripMultiplier(0.50f), EPSILON);
    }

    @Test
    public void rlBenchmarkHookAcceptsOnlyTuningCards() {
        RatassGame.RlTrainingConfig config = new RatassGame.RlTrainingConfig();

        assertNull(config.benchmarkTuningCard);
        config.withBenchmarkTuningCard("AERO_TRIM");
        assertEquals(RogueliteCardId.AERO_TRIM, config.benchmarkTuningCard);
        config.withBenchmarkTuningCard("");
        assertNull(config.benchmarkTuningCard);

        try {
            config.withBenchmarkTuningCard("NITRO_PULSE");
            fail("A non-tuning card must not be accepted by the tuning benchmark hook");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("not a tuning card"));
        }
    }

    @Test
    public void cornerExitTriggersOnlyAfterLeavingAnOnRoadCorner() {
        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.CORNER_EXIT);

        update(upgrades, 0.1f, 1f, true, 0.02f, 0.65f, 0f, 0.20f, 0.2f, 0.2f, 0f, 0f);
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);

        update(upgrades, 0.1f, 1f, true, 0.02f, 0.65f, 0f, 0f, 0.8f, 0f, 0f, 0f);
        assertEquals(1.12f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertTrue(upgrades.getActiveCardIds().contains(RogueliteCardId.CORNER_EXIT));
    }

    @Test
    public void cleanMomentumBuildsOnRoadAndResetsOffRoad() {
        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.CLEAN_MOMENTUM);
        for (int i = 0; i < 60; i++) {
            update(upgrades, 0.1f, 1f, true, 0.01f, 0.7f, 0f, 0f, 1f, 0f, 0f, 0f);
        }
        assertEquals(1.07f, upgrades.getMaxSpeedMultiplier(), EPSILON);

        update(upgrades, 0.1f, 1f, false, 0.01f, 0.7f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertEquals(1f, upgrades.getMaxSpeedMultiplier(), EPSILON);
    }

    @Test
    public void overtakingCreatesThenExpiresAnAccelerationBurst() {
        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.OVERTAKE_SURGE);

        assertTrue(upgrades.hasOvertakeInjector());
        upgrades.onRacePositionImproved(1, 0f);
        assertEquals(1.24f, upgrades.getAccelerationMultiplier(), EPSILON);

        update(upgrades, 2.1f, 1f, true, 0f, 0.8f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);
    }

    @Test
    public void nitroWaitsForAUsableStraightAndThenEntersCooldown() {
        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.NITRO_PULSE);

        update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0.25f, 0.1f, 0.3f, 0f, 0f);
        for (int i = 0; i < 30; i++) {
            update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0.25f, 0.1f, 0.3f, 0f, 0f);
        }
        assertTrue(upgrades.isGadgetReady());
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);

        update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 0.8f, 0f, 0f, 0f);
        assertEquals(RogueliteCardId.NITRO_PULSE, upgrades.getActiveGadgetCardId());
        assertEquals(1.20f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(0.18f, upgrades.consumeForwardLaunchSpeedRatio(), EPSILON);
        assertEquals(0f, upgrades.consumeForwardLaunchSpeedRatio(), EPSILON);

        update(upgrades, 1.5f, 1f, true, 0f, 0.65f, 0f, 0f, 0.8f, 0f, 0f, 0f);
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertFalse(upgrades.isGadgetReady());
        assertTrue(upgrades.getGadgetReadiness() > 0f);
    }

    @Test
    public void straightLineLaunchesScaleByGadgetTierWithoutReplacingStatBuffs() {
        RogueliteCarUpgrades nitro = activateStraightGadget(RogueliteCardId.NITRO_PULSE);
        RogueliteCarUpgrades rocket = activateStraightGadget(RogueliteCardId.ROCKET_EXHAUST);
        RogueliteCarUpgrades hyperdrive = activateStraightGadget(RogueliteCardId.HYPERDRIVE);

        float nitroLaunch = nitro.consumeForwardLaunchSpeedRatio();
        float rocketLaunch = rocket.consumeForwardLaunchSpeedRatio();
        float hyperdriveLaunch = hyperdrive.consumeForwardLaunchSpeedRatio();

        assertEquals(0.18f, nitroLaunch, EPSILON);
        assertEquals(0.27f, rocketLaunch, EPSILON);
        assertEquals(0.40f, hyperdriveLaunch, EPSILON);
        assertTrue(rocketLaunch > nitroLaunch);
        assertTrue(hyperdriveLaunch > rocketLaunch);
        assertEquals(1.20f, nitro.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.28f, rocket.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.38f, hyperdrive.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.06f, nitro.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(1.09f, rocket.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(1.15f, hyperdrive.getMaxSpeedMultiplier(), EPSILON);
    }

    @Test
    public void draftMagnetPulsesOnceWhenAnyRivalIsCloseThenWaitsForCooldown() {
        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.DRAFT_MAGNET);

        update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        for (int i = 0; i < 30; i++) {
            update(upgrades, 0.1f, 0f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        }
        assertTrue(upgrades.isGadgetReady());
        assertFalse(upgrades.consumeDraftMagnetPulse());

        update(upgrades, 0.1f, 0f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0.80f);
        assertEquals(RogueliteCardId.DRAFT_MAGNET, upgrades.getActiveGadgetCardId());
        assertTrue(upgrades.consumeDraftMagnetPulse());
        assertFalse(upgrades.consumeDraftMagnetPulse());

        for (int i = 0; i < 30; i++) {
            update(upgrades, 0.1f, 0f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0.80f);
            assertFalse(upgrades.consumeDraftMagnetPulse());
        }

        boolean pulsedAgain = false;
        for (int i = 0; i < 80; i++) {
            update(upgrades, 0.1f, 0f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0.80f);
            pulsedAgain |= upgrades.consumeDraftMagnetPulse();
        }
        assertTrue(pulsedAgain);
    }

    @Test
    public void ramArmsNearARivalAndCannotRearmDuringCooldown() {
        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.RAM_REACTOR);

        update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        for (int i = 0; i < 30; i++) {
            update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        }
        assertTrue(upgrades.isGadgetReady());
        assertFalse(upgrades.isRamGadgetActive());

        update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0.8f, 0.8f);
        assertTrue(upgrades.isRamGadgetActive());
        upgrades.consumeRamGadgetCharge();
        assertFalse(upgrades.isRamGadgetActive());

        for (int i = 0; i < 20; i++) {
            update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0.8f, 0.8f);
        }
        assertFalse(upgrades.isRamGadgetActive());
    }

    @Test
    public void everyCardCombinationRemainsStableThroughASimulatedRace() {
        List<RogueliteCardDefinition> tuning = cardsForSlot(RogueliteSlotType.TUNING);
        List<RogueliteCardDefinition> techniques = cardsForSlot(RogueliteSlotType.TECHNIQUE);
        List<RogueliteCardDefinition> gadgets = cardsForSlot(RogueliteSlotType.GADGET);

        for (int t = 0; t < tuning.size(); t++) {
            for (int technique = 0; technique < techniques.size(); technique++) {
                for (int gadget = 0; gadget < gadgets.size(); gadget++) {
                    RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
                    upgrades.configure(
                            loadout(
                                    tuning.get(t).getId(),
                                    techniques.get(technique).getId(),
                                    gadgets.get(gadget).getId()),
                            (t * 100 + technique * 10 + gadget) / 499f);
                    simulateRace(upgrades);
                }
            }
        }
    }

    private static void simulateRace(RogueliteCarUpgrades upgrades) {
        for (int step = 0; step < 240; step++) {
            int phase = step % 120;
            float cornerSeverity = phase < 40 ? 0f : phase < 85 ? 0.30f : 0.04f;
            float nextCornerDistance = phase < 30 ? 0.80f : phase < 70 ? 0.18f : 0.65f;
            float nextCornerSeverity = phase < 70 ? 0.32f : 0.04f;
            boolean onRoad = step % 137 != 0;
            float slip = phase >= 50 && phase < 80 ? 0.24f : 0.03f;
            float opponentAhead = phase < 35 ? 0.72f : 0f;
            float opponentNearby = phase < 20 ? 0.65f : 0f;

            upgrades.update(
                    1f / 30f,
                    1f,
                    onRoad,
                    step % 181 < 30,
                    step % 89 == 0,
                    slip,
                    0.65f,
                    phase < 20 ? 0.35f : 0f,
                    step,
                    360f,
                    2f,
                    cornerSeverity,
                    nextCornerDistance,
                    nextCornerSeverity,
                    opponentAhead,
                    opponentNearby);
            if (step % 89 == 0) {
                upgrades.onCollision(12f);
            }
            if (step % 101 == 0) {
                upgrades.onRacePositionImproved(1, 0.35f);
            }

            assertMultiplier(upgrades.getAccelerationMultiplier(), 0.80f, 1.85f);
            assertMultiplier(upgrades.getMassMultiplier(), 0.75f, 1.35f);
            assertMultiplier(upgrades.getMaxSpeedMultiplier(), 0.80f, 1.35f);
            assertMultiplier(upgrades.getDragMultiplier(), 0.50f, 1.50f);
            assertMultiplier(upgrades.getGripMultiplier(slip), 0.65f, 2f);
            assertMultiplier(upgrades.getSteeringMultiplier(slip), 0.70f, 2f);
            assertMultiplier(upgrades.getSlipstreamRangeMultiplier(), 1f, 2f);
            assertMultiplier(upgrades.getSlipstreamStrengthMultiplier(), 1f, 2f);
            assertMultiplier(upgrades.getFrontCollisionRecoilMultiplier(), 0f, 1f);
            assertMultiplier(upgrades.getFrontCollisionPushMultiplier(), 1f, 3f);
            assertMultiplier(upgrades.adjustSurfaceGrip(0.58f), 0f, 1f);
            assertMultiplier(upgrades.getGadgetReadiness(), 0f, 1f);
        }
    }

    private static RogueliteCarUpgrades configured(RogueliteCardId cardId) {
        return configured(new RogueliteCardId[] {cardId});
    }

    private static RogueliteCarUpgrades configured(RogueliteCardId... cardIds) {
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(loadout(cardIds));
        return upgrades;
    }

    private static RogueliteCarUpgrades activateStraightGadget(RogueliteCardId cardId) {
        RogueliteCarUpgrades upgrades = configured(cardId);
        update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 0.8f, 0f, 0f, 0f);
        for (int i = 0; i < 50 && !upgrades.isGadgetReady(); i++) {
            update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0.25f, 0.1f, 0.3f, 0f, 0f);
        }
        assertTrue(upgrades.isGadgetReady());
        update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 0.8f, 0f, 0f, 0f);
        return upgrades;
    }

    private static void update(
            RogueliteCarUpgrades upgrades,
            float delta,
            float throttle,
            boolean onRoad,
            float slip,
            float speedRatio,
            float slipstreamBoost,
            float cornerSeverity,
            float nextCornerDistance,
            float nextCornerSeverity,
            float opponentAhead,
            float opponentNearby) {
        upgrades.update(
                delta,
                throttle,
                onRoad,
                false,
                false,
                slip,
                speedRatio,
                slipstreamBoost,
                10f,
                100f,
                2f,
                cornerSeverity,
                nextCornerDistance,
                nextCornerSeverity,
                opponentAhead,
                opponentNearby);
    }

    private static List<RogueliteCardDefinition> cardsForSlot(
            RogueliteSlotType slotType) {
        List<RogueliteCardDefinition> matches = new ArrayList<RogueliteCardDefinition>();
        List<RogueliteCardDefinition> cards = RogueliteCardCatalog.all();
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).getSlotType() == slotType) {
                matches.add(cards.get(i));
            }
        }
        return matches;
    }

    private static void assertMultiplier(float value, float minimum, float maximum) {
        assertFalse(Float.isNaN(value));
        assertFalse(Float.isInfinite(value));
        assertTrue(value >= minimum - EPSILON);
        assertTrue(value <= maximum + EPSILON);
    }

    private static RogueliteLoadout loadout(RogueliteCardId... cards) {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        for (int i = 0; i < cards.length; i++) {
            assertTrue(loadout.equip(cards[i]));
        }
        return loadout;
    }
}
