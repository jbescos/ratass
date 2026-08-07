package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
        assertEquals(1f, upgrades.getDraftMagnetRangeMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getDraftMagnetForceMultiplier(), EPSILON);
        assertFalse(upgrades.isPowerupReady());
        assertTrue(upgrades.getActiveCardIds().isEmpty());
        assertEquals(0f, upgrades.consumeForwardLaunchSpeedRatio(), EPSILON);
    }

    @Test
    public void everyCatalogCardHasAnEffectAndValidArtwork() {
        List<RogueliteCardDefinition> cards = RogueliteCardCatalog.all();
        Set<Integer> artworkIndexes = new HashSet<Integer>();
        for (int i = 0; i < cards.size(); i++) {
            RogueliteCardDefinition card = cards.get(i);
            RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
            upgrades.configure(loadout(card.getId()));
            assertTrue(upgrades.isEnabled());
            assertTrue(card.getArtworkIndex() >= 0);
            assertTrue(card.getArtworkIndex() < RogueliteCardDefinition.ARTWORK_CAPACITY);
            assertTrue(
                    "Duplicate card artwork for " + card.getId(),
                    artworkIndexes.add(card.getArtworkIndex()));
        }
    }

    @Test
    public void cardFaceCopyStaysConciseWhileDetailedCopyRemainsAvailable() {
        List<RogueliteCardDefinition> cards = RogueliteCardCatalog.all();
        for (int i = 0; i < cards.size(); i++) {
            RogueliteCardDefinition card = cards.get(i);
            assertTrue(card.getDescription().trim().length() > 0);
            String[] effectLines = card.getEffectText().split("\\n");
            assertTrue("Too many effect lines for " + card.getId(), effectLines.length <= 2);
            for (int line = 0; line < effectLines.length; line++) {
                assertTrue(
                        "Effect line is too long for " + card.getId(),
                        effectLines[line].length() <= 72);
            }
        }
    }

    @Test
    public void rivalPenalizingAbilitiesUseTheRevengeSlot() {
        assertEquals(
                RogueliteSlotType.REVENGE,
                RogueliteCardCatalog.get(RogueliteCardId.DRAFT_MAGNET).getSlotType());
        assertEquals(
                RogueliteSlotType.REVENGE,
                RogueliteCardCatalog.get(RogueliteCardId.RAM_REACTOR).getSlotType());
        assertEquals(
                1,
                RogueliteCardCatalog.get(RogueliteCardId.RAM_REACTOR).getTier());
        assertEquals(
                RogueliteSlotType.REVENGE,
                RogueliteCardCatalog.get(RogueliteCardId.CROWN_ENGINE).getSlotType());
        assertEquals(
                RogueliteSlotType.REVENGE,
                RogueliteCardCatalog.get(RogueliteCardId.RECOVERY_BEACON).getSlotType());
        assertEquals(
                2,
                RogueliteCardCatalog.get(RogueliteCardId.RECOVERY_BEACON).getTier());
        assertEquals(
                RogueliteSlotType.REVENGE,
                RogueliteCardCatalog.get(RogueliteCardId.DRAFT_VENDETTA).getSlotType());
        assertEquals(
                RogueliteSlotType.REVENGE,
                RogueliteCardCatalog.get(RogueliteCardId.PAYBACK_SHIELD).getSlotType());
        assertEquals(
                RogueliteSlotType.REVENGE,
                RogueliteCardCatalog.get(RogueliteCardId.REPULSOR_SURGE).getSlotType());
        assertEquals(
                RogueliteSlotType.REVENGE,
                RogueliteCardCatalog.get(RogueliteCardId.TAR_TETHER).getSlotType());
        assertEquals(
                RogueliteSlotType.REVENGE,
                RogueliteCardCatalog.get(RogueliteCardId.EMP_SNARE).getSlotType());
        assertEquals(
                RogueliteSlotType.REVENGE,
                RogueliteCardCatalog.get(RogueliteCardId.VOID_ANCHOR).getSlotType());
        assertEquals(
                RogueliteSlotType.POWERUP,
                RogueliteCardCatalog.get(RogueliteCardId.GRAVITY_WELL).getSlotType());
    }

    @Test
    public void tuningCardsExposeDistinctMassAndAerodynamicSetups() {
        RogueliteCarUpgrades lightweight = configured(RogueliteCardId.SPORT_TUNE);
        RogueliteCarUpgrades heavyweight = configured(RogueliteCardId.HEAVYWEIGHT_TUNE);
        RogueliteCarUpgrades aero = configured(RogueliteCardId.CHAMPIONSHIP_TUNE);
        RogueliteCarUpgrades streamline = configured(RogueliteCardId.AERO_TRIM);
        RogueliteCarUpgrades drift = configured(RogueliteCardId.DRIFT_DIFFERENTIAL);
        RogueliteCarUpgrades carbonPanels = configured(RogueliteCardId.CARBON_PANELS);
        RogueliteCarUpgrades carbonMonocoque = configured(RogueliteCardId.CARBON_MONOCOQUE);
        RogueliteCarUpgrades grapheneChassis = configured(RogueliteCardId.GRAPHENE_CHASSIS);

        assertEquals(0.98f, lightweight.getMassMultiplier(), EPSILON);
        assertEquals(1.10f, heavyweight.getMassMultiplier(), EPSILON);
        assertTrue(heavyweight.getFrontCollisionPushMultiplier() > 1f);
        assertEquals(1.07f, aero.getMassMultiplier(), EPSILON);
        assertEquals(0.86f, aero.getDragMultiplier(), EPSILON);
        assertTrue(aero.getMaxSpeedMultiplier() > heavyweight.getMaxSpeedMultiplier());
        assertTrue(streamline.getDragMultiplier() < 1f);
        assertTrue(streamline.getGripMultiplier(0f) > 1f);
        assertTrue(drift.getGripMultiplier(0f) > streamline.getGripMultiplier(0f));
        assertTrue(drift.getGripMultiplier(0.50f) < drift.getGripMultiplier(0f));
        assertTrue(drift.getSteeringMultiplier(0f) > 1f);
        assertEquals(0.94f, carbonPanels.getMassMultiplier(), EPSILON);
        assertEquals(0.92f, carbonMonocoque.getMassMultiplier(), EPSILON);
        assertEquals(0.84f, grapheneChassis.getMassMultiplier(), EPSILON);
        assertTrue(carbonMonocoque.getAccelerationMultiplier()
                > carbonPanels.getAccelerationMultiplier());
        assertTrue(grapheneChassis.getAccelerationMultiplier()
                > carbonMonocoque.getAccelerationMultiplier());
        assertEquals(1f, carbonPanels.getFrontCollisionRecoilMultiplier(), EPSILON);
        assertEquals(1f, grapheneChassis.getFrontCollisionPushMultiplier(), EPSILON);
    }

    @Test
    public void velocityShellAndTorqueVectoringHaveDistinctDrivingRoles() {
        RogueliteCarUpgrades velocity = configured(RogueliteCardId.VELOCITY_SHELL);
        RogueliteCarUpgrades vectoring = configured(RogueliteCardId.TORQUE_VECTORING);

        assertTrue(velocity.getMaxSpeedMultiplier() > vectoring.getMaxSpeedMultiplier());
        assertTrue(velocity.getDragMultiplier() < vectoring.getDragMultiplier());
        assertEquals(
                velocity.getGripMultiplier(0f),
                vectoring.getGripMultiplier(0f),
                EPSILON);
        assertTrue(vectoring.getSteeringMultiplier(0f) > velocity.getSteeringMultiplier(0f));
    }

    @Test
    public void everyTierHasFiveTuningChoices() {
        int[] counts = new int[4];
        for (RogueliteCardDefinition card : cardsForSlot(RogueliteSlotType.TUNING)) {
            counts[card.getTier()]++;
        }

        assertEquals(5, counts[1]);
        assertEquals(5, counts[2]);
        assertEquals(5, counts[3]);
    }

    @Test
    public void revengeCardsHaveUniqueArtwork() {
        Set<Integer> artworkIndexes = new HashSet<>();
        for (RogueliteCardDefinition card : cardsForSlot(RogueliteSlotType.REVENGE)) {
            assertTrue("Duplicate revenge artwork for " + card.getId(),
                    artworkIndexes.add(card.getArtworkIndex()));
            assertFalse(
                    "Revenge card advertises a cooldown: " + card.getId(),
                    card.getEffectText().contains("Cooldown"));
        }
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

        assertEquals(1.05f, equipped.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.21f, preview.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f / 0.88f, preview.getAerodynamicEfficiency(), EPSILON);
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
    public void liveCarStatsTrackActiveCardsAndTemporaryCarEffects() {
        RogueliteCarUpgrades upgrades =
                activateStraightPowerup(RogueliteCardId.NITRO_PULSE);
        RogueliteCarStatSnapshot passive =
                RogueliteCarStatSnapshot.from(
                        loadout(RogueliteCardId.NITRO_PULSE),
                        null);
        RogueliteCarStatSnapshot live =
                RogueliteCarStatSnapshot.fromLive(
                        upgrades,
                        0f,
                        0.80f,
                        0.75f,
                        0.70f,
                        0.90f,
                        1.20f,
                        1.15f);

        assertTrue(upgrades.getAccelerationMultiplier()
                > passive.getAccelerationMultiplier());
        assertEquals(
                upgrades.getAccelerationMultiplier() * 0.80f,
                live.getAccelerationMultiplier(),
                EPSILON);
        assertEquals(
                upgrades.getMaxSpeedMultiplier() * 0.75f,
                live.getMaxSpeedMultiplier(),
                EPSILON);
        assertEquals(upgrades.getGripMultiplier(0f) * 0.70f,
                live.getGripMultiplier(), EPSILON);
        assertEquals(upgrades.getSteeringMultiplier(0f) * 0.90f,
                live.getSteeringMultiplier(), EPSILON);
        assertEquals(upgrades.getMassMultiplier() * 1.20f,
                live.getMassMultiplier(), EPSILON);
        assertEquals(
                passive.getAerodynamicEfficiency() * 1.15f,
                live.getAerodynamicEfficiency(),
                EPSILON);
    }

    @Test
    public void liveCarStatsTrackAnActivatedTechniqueBoost() {
        RogueliteCarUpgrades upgrades =
                configured(RogueliteCardId.CORNER_EXIT);
        RogueliteCarStatSnapshot passive =
                RogueliteCarStatSnapshot.from(
                        loadout(RogueliteCardId.CORNER_EXIT),
                        null);

        update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0.20f, 0.5f, 0.2f, 0f, 0f);
        update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0.02f, 0.5f, 0.2f, 0f, 0f);
        RogueliteCarStatSnapshot active =
                RogueliteCarStatSnapshot.fromLive(
                        upgrades,
                        0f,
                        1f,
                        1f,
                        1f,
                        1f,
                        1f,
                        1f);

        assertTrue(upgrades.getActiveCardIds().contains(RogueliteCardId.CORNER_EXIT));
        assertEquals(RogueliteCardId.CORNER_EXIT, upgrades.getActiveTechniqueCardId());
        assertTrue(active.getAccelerationMultiplier()
                > passive.getAccelerationMultiplier());
        assertTrue(active.getMaxSpeedMultiplier()
                > passive.getMaxSpeedMultiplier());
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
        assertEquals(1.13f, driftRelease.getGripMultiplier(0.05f), EPSILON);
        assertEquals(1.10f, driftRelease.getGripMultiplier(0.50f), EPSILON);
    }

    @Test
    public void rlBenchmarkHookAcceptsTheRequestedCardSlotOnly() {
        RatassGame.RlTrainingConfig config = new RatassGame.RlTrainingConfig();

        assertNull(config.benchmarkCard);
        config.withBenchmarkTuningCard("AERO_TRIM");
        assertEquals(RogueliteCardId.AERO_TRIM, config.benchmarkCard);
        config.withBenchmarkTuningCard("");
        assertNull(config.benchmarkCard);
        config.withBenchmarkPowerupCard("NITRO_PULSE");
        assertEquals(RogueliteCardId.NITRO_PULSE, config.benchmarkCard);
        config.withBenchmarkTechniqueCard("CLEAN_MOMENTUM");
        assertEquals(RogueliteCardId.CLEAN_MOMENTUM, config.benchmarkCard);

        try {
            config.withBenchmarkTuningCard("NITRO_PULSE");
            fail("A non-tuning card must not be accepted by the tuning benchmark hook");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("not a tuning card"));
        }
        try {
            config.withBenchmarkPowerupCard("AERO_TRIM");
            fail("A non-powerup card must not be accepted by the powerup benchmark hook");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("not a powerup card"));
        }
        try {
            config.withBenchmarkTechniqueCard("AERO_TRIM");
            fail("A non-technique card must not be accepted by the technique benchmark hook");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("not a technique card"));
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
        assertEquals(1f / 1.03f, upgrades.getDragMultiplier(), EPSILON);

        update(upgrades, 0.1f, 1f, false, 0.01f, 0.7f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertEquals(1f, upgrades.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getDragMultiplier(), EPSILON);
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
    public void positionTechniquesScaleUsefulStatsWithoutChangingMassOrSteering() {
        assertPositionTechnique(RogueliteCardId.UNDERDOG_INSTINCT, 0.10f);
        assertPositionTechnique(RogueliteCardId.COMEBACK_DRIVE, 0.15f);
        assertPositionTechnique(RogueliteCardId.LAST_PLACE_FURY, 0.20f);
    }

    @Test
    public void nearbyRivalTechniquesBoostUsefulStatsWithoutChangingMassOrSteering() {
        assertNearbyRivalTechnique(RogueliteCardId.CLOSE_QUARTERS, 0.05f);
        assertNearbyRivalTechnique(RogueliteCardId.PACK_RACER, 0.10f);
        assertNearbyRivalTechnique(RogueliteCardId.TRAFFIC_DOMINANCE, 0.15f);
    }

    @Test
    public void nitroWaitsForAUsableStraightAndThenEntersCooldown() {
        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.NITRO_PULSE);

        update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0.25f, 0.1f, 0.3f, 0f, 0f);
        for (int i = 0; i < 30; i++) {
            update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0.25f, 0.1f, 0.3f, 0f, 0f);
        }
        assertTrue(upgrades.isPowerupReady());
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);

        update(upgrades, 0.1f, 1f, true, 0f, 0.35f, 0f, 0f, 0.8f, 0f, 0f, 0f);
        assertEquals(RogueliteCardId.NITRO_PULSE, upgrades.getActivePowerupCardId());
        assertEquals(1.20f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(0.18f, upgrades.consumeForwardLaunchSpeedRatio(), EPSILON);
        assertEquals(0f, upgrades.consumeForwardLaunchSpeedRatio(), EPSILON);

        update(upgrades, 1.5f, 1f, true, 0f, 0.65f, 0f, 0f, 0.8f, 0f, 0f, 0f);
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertFalse(upgrades.isPowerupReady());
        assertTrue(upgrades.getPowerupReadiness() > 0f);
    }

    @Test
    public void nitroWaitsWhileItsForwardLaneIsBlocked() {
        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.NITRO_PULSE);

        for (int i = 0; i < 50; i++) {
            updateStraightPowerup(upgrades, true);
        }
        assertTrue(upgrades.isPowerupReady());
        assertNull(upgrades.getActivePowerupCardId());

        updateStraightPowerup(upgrades, false);
        assertEquals(RogueliteCardId.NITRO_PULSE, upgrades.getActivePowerupCardId());
    }

    @Test
    public void straightLineLaunchesRespectSafeTargetSpeedWithoutReplacingStatBuffs() {
        RogueliteCarUpgrades nitro = activateStraightPowerup(RogueliteCardId.NITRO_PULSE);
        RogueliteCarUpgrades rocket = activateStraightPowerup(RogueliteCardId.ROCKET_EXHAUST);
        RogueliteCarUpgrades hyperdrive = activateStraightPowerup(RogueliteCardId.HYPERDRIVE);

        float nitroLaunch = nitro.consumeForwardLaunchSpeedRatio();
        float rocketLaunch = rocket.consumeForwardLaunchSpeedRatio();
        float hyperdriveLaunch = hyperdrive.consumeForwardLaunchSpeedRatio();

        assertEquals(0.18f, nitroLaunch, EPSILON);
        assertEquals(0.20f, rocketLaunch, EPSILON);
        assertEquals(0.19f, hyperdriveLaunch, EPSILON);
        assertTrue(rocketLaunch > nitroLaunch);
        assertTrue(hyperdriveLaunch > nitroLaunch);
        assertEquals(1.20f, nitro.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.32f, rocket.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.38f, hyperdrive.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.06f, nitro.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(1.11f, rocket.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(1.15f, hyperdrive.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(
                1.4f,
                nitro.getActiveTimeRemainingSeconds(RogueliteCardId.NITRO_PULSE),
                EPSILON);
        assertEquals(
                2f,
                rocket.getActiveTimeRemainingSeconds(RogueliteCardId.ROCKET_EXHAUST),
                EPSILON);
        assertEquals(
                3.3f,
                hyperdrive.getActiveTimeRemainingSeconds(RogueliteCardId.HYPERDRIVE),
                EPSILON);
        assertTrue(
                nitro.getCooldownTimeRemainingSeconds(RogueliteCardId.NITRO_PULSE)
                        > 0f);
    }

    @Test
    public void mirrorPowerupTiersUseTwoThreeAndFourCarsWithoutStatBoosts() {
        assertMirrorPowerup(RogueliteCardId.MIRROR_DUO, 2, 5f);
        assertMirrorPowerup(RogueliteCardId.MIRROR_TRIO, 3, 5f);
        assertMirrorPowerup(RogueliteCardId.OVERDRIVE_COIL, 4, 5f);
    }

    @Test
    public void mirrorPowerupCanActivateAtRacingSpeed() {
        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.MIRROR_DUO);

        for (int i = 0;
                i < 40 && upgrades.getActivePowerupCardId() == null;
                i++) {
            updateStraightPowerupAtSpeed(upgrades, 0.72f, true, 0.5f);
        }
        assertEquals(RogueliteCardId.MIRROR_DUO, upgrades.getActivePowerupCardId());
    }

    @Test
    public void mirrorPowerupWaitsForANearbyOpponent() {
        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.MIRROR_DUO);

        for (int i = 0; i < 80; i++) {
            updateStraightPowerupAtSpeed(upgrades, 0.72f, false, 0f);
        }
        assertTrue(upgrades.isPowerupReady());
        assertNull(upgrades.getActivePowerupCardId());

        updateStraightPowerupAtSpeed(upgrades, 0.72f, false, 0.5f);
        assertEquals(RogueliteCardId.MIRROR_DUO, upgrades.getActivePowerupCardId());
    }

    @Test
    public void cloakTiersUseTheirConfiguredDurationsAndWaitForClearanceToEnd() {
        assertCloakDuration(RogueliteCardId.GHOST_CLOAK, 3f);
        assertCloakDuration(RogueliteCardId.PHANTOM_CLOAK, 4f);
        assertCloakDuration(RogueliteCardId.VOID_CLOAK, 5f);

        RogueliteCarUpgrades held = activateCloak(RogueliteCardId.GHOST_CLOAK);
        held.deferInvisibilityExpiration();
        update(held, 3.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertTrue(held.isInvisible());
        assertEquals(0f, held.getCooldownTimeRemainingSeconds(RogueliteCardId.GHOST_CLOAK), EPSILON);

        held.deferInvisibilityExpiration();
        update(held, 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertTrue(held.isInvisible());

        update(held, 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertFalse(held.isInvisible());
        assertEquals(10f, held.getCooldownTimeRemainingSeconds(RogueliteCardId.GHOST_CLOAK), EPSILON);
    }

    @Test
    public void cloakWaitsForANearbyOpponent() {
        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.GHOST_CLOAK);

        for (int i = 0; i < 80; i++) {
            update(upgrades, 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        }
        assertTrue(upgrades.isPowerupReady());
        assertFalse(upgrades.isInvisible());

        update(upgrades, 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0.5f);
        assertTrue(upgrades.isInvisible());
    }

    @Test
    public void activeCloakBlocksHostileEffects() {
        RogueliteCarUpgrades upgrades = activateCloak(RogueliteCardId.GHOST_CLOAK);

        assertTrue(upgrades.isInvisible());
        assertTrue(upgrades.blocksHostileEffects());

        update(upgrades, 3.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertFalse(upgrades.isInvisible());
        assertFalse(upgrades.blocksHostileEffects());
    }

    @Test
    public void draftMagnetBecomesReadyOnlyAfterAQualifiedRivalHit() {
        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.DRAFT_MAGNET);

        update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertFalse(upgrades.isDraftMagnetActive());
        assertFalse(upgrades.isRevengeArmed());

        update(upgrades, 0.1f, 0f, false, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0.80f);
        assertFalse(upgrades.isDraftMagnetActive());
        assertFalse(upgrades.isRevengeArmed());

        upgrades.onHitBy(42, 12f);
        assertTrue(upgrades.isRevengeArmed());
        assertFalse(upgrades.isDraftMagnetActive());
        assertEquals(
                0f,
                upgrades.getCooldownTimeRemainingSeconds(RogueliteCardId.DRAFT_MAGNET),
                EPSILON);

        update(upgrades, 0.1f, 0f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertTrue(upgrades.isRevengeArmed());
        assertFalse(upgrades.isDraftMagnetActive());

        update(upgrades, 0.1f, 0f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0.80f);
        assertEquals(RogueliteCardId.DRAFT_MAGNET, upgrades.getActiveAbilityCardId());
        assertTrue(upgrades.isDraftMagnetActive());
        assertTrue(
                upgrades.getActiveTimeRemainingSeconds(RogueliteCardId.DRAFT_MAGNET)
                        > 0f);
        assertEquals(
                0f,
                upgrades.getCooldownTimeRemainingSeconds(RogueliteCardId.DRAFT_MAGNET),
                EPSILON);

        update(upgrades, 0.1f, 0f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0.80f);
        assertTrue(upgrades.isDraftMagnetActive());

        for (int i = 0; i < 20; i++) {
            update(upgrades, 0.1f, 0f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0.80f);
        }
        assertNull(upgrades.getActiveAbilityCardId());
        assertFalse(upgrades.isDraftMagnetActive());
        assertEquals(
                0f,
                upgrades.getActiveTimeRemainingSeconds(RogueliteCardId.DRAFT_MAGNET),
                EPSILON);
        assertFalse(upgrades.isRevengeArmed());
    }

    @Test
    public void impactReversalWaitsForACollisionAndRearmsAfterItsActiveEffect() {
        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.RAM_REACTOR);

        update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0.8f, 0.8f);
        assertFalse(upgrades.isRamChargeActive());
        assertFalse(upgrades.isImpactCounterReady());

        upgrades.onHitBy(42, 12f);
        assertTrue(upgrades.isRevengeArmed());
        assertTrue(upgrades.isImpactCounterReady());
        update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0.8f, 0.8f);
        assertTrue(upgrades.isImpactCounterReady());
        assertFalse(upgrades.isRamChargeActive());

        for (int i = 0; i < 130; i++) {
            update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        }
        assertTrue(upgrades.isImpactCounterReady());

        upgrades.consumeImpactCounter();
        assertFalse(upgrades.isImpactCounterReady());
        assertFalse(upgrades.isRevengeArmed());
        assertEquals(RogueliteCardId.RAM_REACTOR, upgrades.getActiveAbilityCardId());
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getGripMultiplier(0f), EPSILON);

        upgrades.onHitBy(42, 12f);
        for (int i = 0; i < 20; i++) {
            update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0.8f, 0.8f);
        }
        assertFalse(upgrades.isImpactCounterReady());
        assertFalse(upgrades.isRevengeArmed());

        upgrades.onHitBy(7, 12f);
        assertTrue(upgrades.isImpactCounterReady());
        assertTrue(upgrades.isRevengeArmed());
        assertEquals(
                0f,
                upgrades.getCooldownTimeRemainingSeconds(RogueliteCardId.RAM_REACTOR),
                EPSILON);
    }

    @Test
    public void reflectedCounterImpactCannotArmAnotherRevengeCard() {
        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.RAM_REACTOR);

        upgrades.onHitBy(42, 12f, false);

        assertFalse(upgrades.isImpactCounterReady());
        assertFalse(upgrades.isRevengeArmed());

        upgrades.onHitBy(42, 12f, true);
        assertTrue(upgrades.isImpactCounterReady());
        assertTrue(upgrades.isRevengeArmed());
    }

    @Test
    public void crownBreakerBoostsUntilItRamsTheRecordedOffender() {
        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.CROWN_ENGINE);

        assertFalse(upgrades.isRevengeReady());
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getFrontCollisionRecoilMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getFrontCollisionPushMultiplier(), EPSILON);

        upgrades.onHitBy(42, 12f);
        assertTrue(upgrades.isRevengeReady());
        assertTrue(upgrades.isRevengeArmed());
        assertFalse(upgrades.isImpactCounterReady());
        assertEquals(42, upgrades.getRevengeTargetVehicleId());
        assertTrue(upgrades.getAccelerationMultiplier() > 1.50f);
        assertTrue(upgrades.getMaxSpeedMultiplier() > 1.20f);
        assertEquals(1f, upgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(1f, upgrades.getSteeringMultiplier(0f), EPSILON);
        upgrades.onHitBy(7, 12f);
        assertEquals(42, upgrades.getRevengeTargetVehicleId());

        for (int i = 0; i < 130; i++) {
            update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        }
        assertTrue(upgrades.isRevengeArmed());
        assertNull(upgrades.tryActivateOffenderStrike(7, 1f));
        assertNull(upgrades.tryActivateOffenderStrike(42, 4f));

        RogueliteRevengeStrike strike = upgrades.tryActivateOffenderStrike(42, 3f);
        assertNotNull(strike);
        assertTrue(strike.isHardImpact());
        assertEquals(RogueliteCardId.CROWN_ENGINE, strike.getCardId());
        assertTrue(strike.getAttackerLaunchSpeedRatio() >= 0.45f);
        assertTrue(strike.getTargetPushSpeedRatio() >= 0.70f);
        assertFalse(upgrades.isRevengeArmed());
        assertNull(upgrades.getActiveAbilityCardId());
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(
                0f,
                upgrades.getCooldownTimeRemainingSeconds(RogueliteCardId.CROWN_ENGINE),
                EPSILON);

        upgrades.onHitBy(42, 12f);
        upgrades.onHitBy(42, 12f);
        assertFalse(upgrades.isRevengeArmed());
        assertEquals(-1, upgrades.getRevengeTargetVehicleId());

        upgrades.onContactEnded(42);
        upgrades.onHitBy(42, 12f);
        assertTrue(upgrades.isRevengeArmed());
        assertEquals(42, upgrades.getRevengeTargetVehicleId());
    }

    @Test
    public void targetedRevengeCardsEmitDistinctActionsAgainstTheRecordedOffender() {
        RogueliteCarUpgrades positionSwap = configured(RogueliteCardId.RECOVERY_BEACON);
        armRevenge(positionSwap);
        positionSwap.onHitBy(7, 12f);
        assertEquals(42, positionSwap.getRevengeTargetVehicleId());
        assertFalse(positionSwap.isRevengeReady());
        update(positionSwap, 2.9f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertNull(positionSwap.tryActivateOffenderStrike(42, 4f, true));
        update(positionSwap, 0.2f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertTrue(positionSwap.isRevengeReady());
        assertNull(positionSwap.tryActivateOffenderStrike(42, 4f, false));
        assertTrue(positionSwap.isRevengeArmed());
        assertEquals(42, positionSwap.getRevengeTargetVehicleId());
        assertNull(positionSwap.tryActivateOffenderStrike(42, 2f, true));
        RogueliteRevengeStrike swapStrike =
                positionSwap.tryActivateOffenderStrike(42, 4f, true);
        assertNotNull(swapStrike);
        assertEquals(RogueliteRevengeStrike.Action.POSITION_SWAP, swapStrike.getAction());

        RogueliteCarUpgrades redline = configured(RogueliteCardId.DRAFT_VENDETTA);
        armRevenge(redline);
        assertNull(redline.tryActivateOffenderStrike(7, 4f));
        RogueliteRevengeStrike throttleStrike =
                redline.tryActivateOffenderStrike(42, 1f);
        assertNotNull(throttleStrike);
        assertEquals(RogueliteRevengeStrike.Action.FORCE_THROTTLE, throttleStrike.getAction());
        assertEquals(5f, throttleStrike.getDurationSeconds(), EPSILON);

        RogueliteCarUpgrades hook = configured(RogueliteCardId.PAYBACK_SHIELD);
        armRevenge(hook);
        assertNull(hook.tryActivateOffenderStrike(42, 2f));
        RogueliteRevengeStrike hookStrike = hook.tryActivateOffenderStrike(42, 6f);
        assertNotNull(hookStrike);
        assertEquals(RogueliteRevengeStrike.Action.HOOK, hookStrike.getAction());
        assertTrue(hookStrike.getAttackerLaunchSpeedRatio() > 0.40f);
        assertEquals(20f, hookStrike.getDurationSeconds(), EPSILON);
        assertEquals(RogueliteCardId.PAYBACK_SHIELD, hook.getActiveAbilityCardId());
        update(hook, 19.9f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertEquals(RogueliteCardId.PAYBACK_SHIELD, hook.getActiveAbilityCardId());
        update(hook, 0.2f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertNull(hook.getActiveAbilityCardId());

        RogueliteCarUpgrades repulsor = configured(RogueliteCardId.REPULSOR_SURGE);
        armRevenge(repulsor);
        update(repulsor, 0.1f, 0f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0.20f);
        assertEquals(RogueliteCardId.REPULSOR_SURGE, repulsor.getActiveAbilityCardId());
        assertTrue(repulsor.isDraftMagnetActive());
        assertEquals(1.35f, repulsor.getDraftMagnetRangeMultiplier(), EPSILON);
        assertEquals(1.55f, repulsor.getDraftMagnetForceMultiplier(), EPSILON);
    }

    @Test
    public void targetedRevengeKeepsTheFirstOffenderUntilItExecutes() {
        RogueliteCardId[] targetedCards = {
                RogueliteCardId.RECOVERY_BEACON,
                RogueliteCardId.DRAFT_VENDETTA,
                RogueliteCardId.PAYBACK_SHIELD,
                RogueliteCardId.TAR_TETHER,
                RogueliteCardId.EMP_SNARE,
                RogueliteCardId.VOID_ANCHOR
        };
        for (int i = 0; i < targetedCards.length; i++) {
            RogueliteCarUpgrades upgrades = configured(targetedCards[i]);

            upgrades.onHitBy(42, 12f);
            upgrades.onHitBy(7, 18f);

            assertTrue(upgrades.isRevengeArmed());
            assertEquals(42, upgrades.getRevengeTargetVehicleId());
        }
    }

    @Test
    public void offenderSlowdownWaitsForTheExactAttackerAndAppliesItsTierEffect() {
        RogueliteCarUpgrades tar = configured(RogueliteCardId.TAR_TETHER);
        tar.onHitBy(42, 12f);
        assertTrue(tar.isRevengeArmed());
        assertEquals(42, tar.getRevengeTargetVehicleId());

        for (int i = 0; i < 200; i++) {
            update(tar, 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        }
        assertTrue(tar.isRevengeArmed());
        assertNull(tar.tryActivateOffenderStrike(7, 2f));

        RogueliteRevengeStrike tarStrike = tar.tryActivateOffenderStrike(42, 100f);
        assertNotNull(tarStrike);
        assertEquals(RogueliteCardId.TAR_TETHER, tarStrike.getCardId());
        assertEquals(RogueliteRevengeStrike.Action.DEBUFF, tarStrike.getAction());
        assertEquals(1f, tarStrike.getSpeedMultiplier(), EPSILON);
        assertEquals(0f, tarStrike.getGripMultiplier(), EPSILON);
        assertEquals(2f, tarStrike.getDurationSeconds(), EPSILON);
        assertFalse(tar.isRevengeArmed());
        assertEquals(RogueliteCardId.TAR_TETHER, tar.getActiveAbilityCardId());
        update(tar, 1.9f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertEquals(RogueliteCardId.TAR_TETHER, tar.getActiveAbilityCardId());
        update(tar, 0.2f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertNull(tar.getActiveAbilityCardId());

        RogueliteCarUpgrades emp = configured(RogueliteCardId.EMP_SNARE);
        emp.onHitBy(42, 12f);
        RogueliteRevengeStrike empStrike = emp.tryActivateOffenderStrike(42, 100f);
        assertNotNull(empStrike);
        assertEquals(RogueliteRevengeStrike.Action.FORCE_BRAKE, empStrike.getAction());
        assertEquals(2f, empStrike.getDurationSeconds(), EPSILON);

        RogueliteCarUpgrades anchor = configured(RogueliteCardId.VOID_ANCHOR);
        anchor.onHitBy(42, 12f);
        RogueliteRevengeStrike anchorStrike =
                anchor.tryActivateOffenderStrike(42, 100f);
        assertNotNull(anchorStrike);
        assertEquals(RogueliteRevengeStrike.Action.FORCE_BRAKE, anchorStrike.getAction());
        assertEquals(3f, anchorStrike.getDurationSeconds(), EPSILON);
    }

    @Test
    public void raceBlackoutTriggersImmediatelyAtItsTierDurationWithoutCooldown() {
        assertRaceBlackoutDuration(RogueliteCardId.SENSOR_JAMMER, 10f);
        assertRaceBlackoutDuration(RogueliteCardId.GRID_BLACKOUT, 20f);
        assertRaceBlackoutDuration(RogueliteCardId.TOTAL_BLACKOUT, 30f);
    }

    @Test
    public void everyCardCombinationRemainsStableThroughASimulatedRace() {
        List<RogueliteCardDefinition> tuning = cardsForSlot(RogueliteSlotType.TUNING);
        List<RogueliteCardDefinition> techniques = cardsForSlot(RogueliteSlotType.TECHNIQUE);
        List<RogueliteCardDefinition> powerups = cardsForSlot(RogueliteSlotType.POWERUP);
        List<RogueliteCardDefinition> revenge = cardsForSlot(RogueliteSlotType.REVENGE);

        for (int t = 0; t < tuning.size(); t++) {
            for (int technique = 0; technique < techniques.size(); technique++) {
                for (int powerup = 0; powerup < powerups.size(); powerup++) {
                    for (int retaliation = 0; retaliation < revenge.size(); retaliation++) {
                        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
                        upgrades.configure(
                                loadout(
                                        tuning.get(t).getId(),
                                        techniques.get(technique).getId(),
                                        powerups.get(powerup).getId(),
                                        revenge.get(retaliation).getId()),
                                (t * 1000 + technique * 100 + powerup * 10 + retaliation)
                                        / 12999f);
                        simulateRace(upgrades);
                    }
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
                upgrades.onHitBy(42, 12f);
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
            assertMultiplier(upgrades.getPowerupReadiness(), 0f, 1f);
            assertMultiplier(upgrades.getRevengeReadiness(), 0f, 1f);
        }
    }

    private static void armRevenge(RogueliteCarUpgrades upgrades) {
        upgrades.onHitBy(42, 12f);
        assertTrue(upgrades.isRevengeArmed());
    }

    private static void assertPositionTechnique(
            RogueliteCardId cardId,
            float maximumBonus) {
        RogueliteCarUpgrades upgrades = configured(cardId);

        updateRacePosition(upgrades, 0f);
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getDragMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getGripMultiplier(0f), EPSILON);
        assertFalse(upgrades.getActiveCardIds().contains(cardId));

        updateRacePosition(upgrades, 0.5f);
        float midpointBonus = maximumBonus * 0.5f;
        assertEquals(1f + midpointBonus, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f + midpointBonus, upgrades.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(1f / (1f + midpointBonus), upgrades.getDragMultiplier(), EPSILON);
        assertEquals(1f + midpointBonus, upgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(1f, upgrades.getMassMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getSteeringMultiplier(0f), EPSILON);
        assertTrue(upgrades.getActiveCardIds().contains(cardId));

        updateRacePosition(upgrades, 1f);
        assertEquals(1f + maximumBonus, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f + maximumBonus, upgrades.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(1f / (1f + maximumBonus), upgrades.getDragMultiplier(), EPSILON);
        assertEquals(1f + maximumBonus, upgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(1f, upgrades.getMassMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getSteeringMultiplier(0f), EPSILON);
    }

    private static void updateRacePosition(
            RogueliteCarUpgrades upgrades,
            float racePositionFactor) {
        upgrades.update(
                0.1f,
                1f,
                true,
                false,
                false,
                0f,
                0.5f,
                0f,
                10f,
                100f,
                2f,
                0f,
                1f,
                0f,
                0f,
                0f,
                false,
                racePositionFactor);
    }

    private static void assertNearbyRivalTechnique(
            RogueliteCardId cardId,
            float bonus) {
        RogueliteCarUpgrades upgrades = configured(cardId);

        update(upgrades, 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertFalse(upgrades.getActiveCardIds().contains(cardId));

        update(upgrades, 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0.65f);
        assertEquals(1f + bonus, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f + bonus, upgrades.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(1f / (1f + bonus), upgrades.getDragMultiplier(), EPSILON);
        assertEquals(1f + bonus, upgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(1f, upgrades.getMassMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getSteeringMultiplier(0f), EPSILON);
        assertTrue(upgrades.getActiveCardIds().contains(cardId));

        update(upgrades, 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getDragMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getGripMultiplier(0f), EPSILON);
        assertFalse(upgrades.getActiveCardIds().contains(cardId));
    }

    private static RogueliteCarUpgrades configured(RogueliteCardId cardId) {
        return configured(new RogueliteCardId[] {cardId});
    }

    private static void assertRaceBlackoutDuration(
            RogueliteCardId cardId,
            float durationSeconds) {
        RogueliteCarUpgrades upgrades = configured(cardId);
        upgrades.onHitBy(42, 12f);

        assertEquals(cardId, upgrades.getActiveAbilityCardId());
        assertEquals(durationSeconds, upgrades.consumeRaceBlackoutSeconds(), EPSILON);
        assertEquals(0f, upgrades.consumeRaceBlackoutSeconds(), EPSILON);

        update(upgrades, durationSeconds * 0.5f, 1f, true, 0f, 0.5f,
                0f, 0f, 1f, 0f, 0f, 0f);
        upgrades.onHitBy(7, 8f);
        assertEquals(durationSeconds, upgrades.consumeRaceBlackoutSeconds(), EPSILON);
        assertEquals(durationSeconds, upgrades.getActiveTimeRemainingSeconds(cardId), EPSILON);

        update(upgrades, durationSeconds + 0.1f, 1f, true, 0f, 0.5f,
                0f, 0f, 1f, 0f, 0f, 0f);
        assertNull(upgrades.getActiveAbilityCardId());
    }

    private static RogueliteCarUpgrades configured(RogueliteCardId... cardIds) {
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(loadout(cardIds));
        return upgrades;
    }

    private static RogueliteCarUpgrades activateStraightPowerup(RogueliteCardId cardId) {
        RogueliteCarUpgrades upgrades = configured(cardId);
        update(upgrades, 0.1f, 1f, true, 0f, 0.35f, 0f, 0f, 0.8f, 0f, 0f, 0f);
        for (int i = 0; i < 50 && !upgrades.isPowerupReady(); i++) {
            update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0.25f, 0.1f, 0.3f, 0f, 0f);
        }
        assertTrue(upgrades.isPowerupReady());
        update(upgrades, 0.1f, 1f, true, 0f, 0.35f, 0f, 0f, 0.8f, 0f, 0f, 0f);
        return upgrades;
    }

    private static RogueliteCarUpgrades activateCloak(RogueliteCardId cardId) {
        RogueliteCarUpgrades upgrades = configured(cardId);
        update(upgrades, 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0.5f);
        for (int i = 0; i < 60 && !upgrades.isInvisible(); i++) {
            update(upgrades, 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0.5f);
        }
        assertTrue(upgrades.isInvisible());
        return upgrades;
    }

    private static void assertCloakDuration(RogueliteCardId cardId, float duration) {
        RogueliteCarUpgrades upgrades = activateCloak(cardId);
        assertEquals(duration, upgrades.getActiveTimeRemainingSeconds(cardId), EPSILON);
        update(upgrades, duration + 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertFalse(upgrades.isInvisible());
        assertEquals(10f, upgrades.getCooldownTimeRemainingSeconds(cardId), EPSILON);
    }

    private static void assertMirrorPowerup(
            RogueliteCardId cardId,
            int totalVehicleCount,
            float duration) {
        RogueliteCarUpgrades upgrades = activateMirrorPowerup(cardId);

        assertTrue(MirrorPowerupSpec.isMirrorCard(cardId));
        assertEquals(totalVehicleCount, MirrorPowerupSpec.totalVehicleCount(cardId));
        assertEquals(duration, upgrades.getActiveTimeRemainingSeconds(cardId), EPSILON);
        assertEquals(MirrorPowerupSpec.COOLDOWN_SECONDS,
                upgrades.getCooldownTimeRemainingSeconds(cardId), EPSILON);
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(0f, upgrades.consumeForwardLaunchSpeedRatio(), EPSILON);
    }

    private static void updateStraightPowerup(
            RogueliteCarUpgrades upgrades,
            boolean forwardLaneBlocked) {
        updateStraightPowerupAtSpeed(upgrades, 0.35f, forwardLaneBlocked);
    }

    private static void updateStraightPowerupAtSpeed(
            RogueliteCarUpgrades upgrades,
            float speedRatio,
            boolean forwardLaneBlocked) {
        updateStraightPowerupAtSpeed(upgrades, speedRatio, forwardLaneBlocked, 0f);
    }

    private static void updateStraightPowerupAtSpeed(
            RogueliteCarUpgrades upgrades,
            float speedRatio,
            boolean forwardLaneBlocked,
            float nearbyOpponentProximity) {
        upgrades.update(
                0.1f,
                1f,
                true,
                false,
                false,
                0f,
                speedRatio,
                0f,
                0f,
                100f,
                0f,
                0f,
                0.8f,
                0f,
                0f,
                nearbyOpponentProximity,
                forwardLaneBlocked);
    }

    private static RogueliteCarUpgrades activateMirrorPowerup(RogueliteCardId cardId) {
        RogueliteCarUpgrades upgrades = configured(cardId);
        updateStraightPowerupAtSpeed(upgrades, 0.35f, false, 0.5f);
        for (int i = 0; i < 80 && upgrades.getActivePowerupCardId() == null; i++) {
            updateStraightPowerupAtSpeed(upgrades, 0.35f, false, 0.5f);
        }
        assertEquals(cardId, upgrades.getActivePowerupCardId());
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
