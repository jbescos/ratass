package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.github.jbescos.RatassGame;
import java.util.ArrayList;
import java.util.Collections;
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
        assertEquals(1f, upgrades.getDriveForceLimitMultiplier(), EPSILON);
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
    public void apexAscensionAddsItsFixedStatsWithoutChangingSteering() {
        RogueliteSetDefinition set = RogueliteSetCatalog.tierFourSet();
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(set.getTuningCardId());
        loadout.equip(set.getTechniqueCardId());
        loadout.equip(set.getPowerupCardId());
        loadout.equip(set.getRevengeCardId());
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();

        upgrades.configure(loadout, 0f, set);

        assertEquals(set, upgrades.getConfiguredSetBonus());
        assertFalse(upgrades.isTimeDilationActive());
        assertEquals(1.8f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(2f, upgrades.getAerodynamicEfficiencyMultiplier(1f), EPSILON);
        assertEquals(1.2f, upgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(0.7f, upgrades.getMassMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getSteeringMultiplier(0f), EPSILON);
        assertEquals(1f / 0.595f, upgrades.getSteeringTorqueMultiplier(), EPSILON);
        assertEquals(1.8f / 0.7f, upgrades.getDriveForceLimitMultiplier(), EPSILON);

        upgrades.reconfigurePreservingCardState(loadout, 0f, null);

        assertNull(upgrades.getConfiguredSetBonus());
        assertFalse(upgrades.isTimeDilationActive());
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getAerodynamicEfficiencyMultiplier(1f), EPSILON);
        assertEquals(1f, upgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(1f, upgrades.getMassMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getSteeringMultiplier(0f), EPSILON);
        assertEquals(1f, upgrades.getSteeringTorqueMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getDriveForceLimitMultiplier(), EPSILON);
    }

    @Test
    public void chaosCircuitKeepsTechniqueActiveUntilSetBreaks() {
        RogueliteSetDefinition set =
                RogueliteSetCatalog.get(RogueliteSetId.CHAOS_CIRCUIT);
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(set.getTuningCardId());
        loadout.equip(set.getTechniqueCardId());
        loadout.equip(set.getPowerupCardId());
        loadout.equip(set.getRevengeCardId());
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();

        upgrades.configure(loadout, 0f, set);
        update(upgrades, 0.1f, 1f, true, 0f, 0.7f, 0f, 0f, 1f, 0f, 0f, 0f);

        assertEquals(RogueliteCardId.DRAFT_MASTER, upgrades.getActiveTechniqueCardId());
        float setPower = upgrades.getAccelerationMultiplier();
        float setAero = upgrades.getAerodynamicEfficiencyMultiplier(1f);

        upgrades.reconfigurePreservingCardState(loadout, 0f, null);
        update(upgrades, 0.1f, 1f, true, 0f, 0.7f, 0f, 0f, 1f, 0f, 0f, 0f);

        assertNull(upgrades.getActiveTechniqueCardId());
        assertTrue(setPower > upgrades.getAccelerationMultiplier());
        assertTrue(setAero > upgrades.getAerodynamicEfficiencyMultiplier(1f));
    }

    @Test
    public void buildSuppressionDisablesTuningAndTechniqueUntilCleared() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        assertTrue(loadout.equip(RogueliteCardId.AERO_TRIM));
        assertTrue(loadout.equip(RogueliteCardId.CORNER_EXPERT));
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(loadout);
        update(upgrades, 0.1f, 1f, true, 0f, 0.5f, 0f, 0.7f, 1f, 0f, 0f, 0f);

        float activePower = upgrades.getAccelerationMultiplier();
        float activeAero = upgrades.getAerodynamicEfficiencyMultiplier(1f);
        assertTrue(activePower != 1f || activeAero != 1f);
        assertTrue(upgrades.getActiveCardIds().contains(RogueliteCardId.CORNER_EXPERT));

        upgrades.setBuildCardsSuppressed(true);
        assertTrue(upgrades.areBuildCardsSuppressed());
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getAerodynamicEfficiencyMultiplier(1f), EPSILON);
        assertFalse(upgrades.getActiveCardIds().contains(RogueliteCardId.CORNER_EXPERT));

        upgrades.setBuildCardsSuppressed(false);
        assertEquals(activePower, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(activeAero, upgrades.getAerodynamicEfficiencyMultiplier(1f), EPSILON);
    }

    @Test
    public void nullTechniqueCardHasNoAmplifiedStats() {
        assertEquals(0, RaceTechniqueEffect.amplifiedStatMask(null));
    }

    @Test
    public void timeDilationPowerupsShareDurationAndReduceCooldownByTier() {
        assertTimeDilationPowerup(RogueliteCardId.TIME_RIPPLE, 60f);
        assertTimeDilationPowerup(RogueliteCardId.CHRONO_SHIFT, 40f);
        assertTimeDilationPowerup(RogueliteCardId.TEMPORAL_DOMINION, 30f);
    }

    @Test
    public void liveLoadoutReconfigurationPreservesEachCarsOwnCardCooldown() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(RogueliteCardId.CHRONO_SHIFT);
        RogueliteCarUpgrades firstCar = new RogueliteCarUpgrades();
        RogueliteCarUpgrades secondCar = new RogueliteCarUpgrades();
        firstCar.configure(loadout, 0f);
        secondCar.configure(loadout, 0f);

        update(firstCar, 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        update(secondCar, 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        update(firstCar, 1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        float firstCooldown =
                firstCar.getCooldownTimeRemainingSeconds(RogueliteCardId.CHRONO_SHIFT);
        float secondCooldown =
                secondCar.getCooldownTimeRemainingSeconds(RogueliteCardId.CHRONO_SHIFT);
        assertTrue(firstCooldown < secondCooldown);

        loadout.equip(RogueliteCardId.AERO_TRIM);
        firstCar.reconfigurePreservingCardState(loadout, 0f);

        assertEquals(
                firstCooldown,
                firstCar.getCooldownTimeRemainingSeconds(RogueliteCardId.CHRONO_SHIFT),
                EPSILON);
        assertEquals(
                secondCooldown,
                secondCar.getCooldownTimeRemainingSeconds(RogueliteCardId.CHRONO_SHIFT),
                EPSILON);

        update(firstCar, 0.5f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertEquals(
                firstCooldown - 0.5f,
                firstCar.getCooldownTimeRemainingSeconds(RogueliteCardId.CHRONO_SHIFT),
                EPSILON);
        assertEquals(
                secondCooldown,
                secondCar.getCooldownTimeRemainingSeconds(RogueliteCardId.CHRONO_SHIFT),
                EPSILON);
    }

    @Test
    public void externalAerodynamicPenaltyIncreasesDrag() {
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();

        assertEquals(1f / 0.95f, upgrades.getDragMultiplier(0.95f), EPSILON);
        assertEquals(1f / 0.80f, upgrades.getDragMultiplier(0.80f), EPSILON);
        assertEquals(1f / 0.50f, upgrades.getDragMultiplier(0.50f), EPSILON);
    }

    @Test
    public void massMultiplierKeepsOnlyAPositiveTenPercentFloor() {
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();

        assertEquals(0.25f, upgrades.getMassMultiplier(0.25f), EPSILON);
        assertEquals(0.10f, upgrades.getMassMultiplier(0.05f), EPSILON);
    }

    @Test
    public void effectiveCarStatsCannotFallBelowTenPercent() {
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();

        assertEquals(0.10f, upgrades.getAccelerationMultiplier(0f), EPSILON);
        assertEquals(
                0.10f,
                upgrades.getAerodynamicEfficiencyMultiplier(0f),
                EPSILON);
        assertEquals(0.10f, upgrades.getGripMultiplier(0f, 0f, 0f), EPSILON);
        assertEquals(0.10f, upgrades.getMassMultiplier(0f), EPSILON);
    }

    @Test
    public void effectiveCarStatsDoNotHaveUpperCaps() {
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();

        assertEquals(3f, upgrades.getAccelerationMultiplier(3f), EPSILON);
        assertEquals(3f, upgrades.getAerodynamicEfficiencyMultiplier(3f), EPSILON);
        assertEquals(3f, upgrades.getGripMultiplier(0f, 1f, 3f), EPSILON);
        assertEquals(3f, upgrades.getMassMultiplier(3f), EPSILON);
        assertEquals(
                (float) Math.cbrt(9f),
                upgrades.getMaxSpeedMultiplier(3f, 3f),
                EPSILON);
    }

    @Test
    public void topSpeedIsDerivedFromPowerAndAeroWithOnlyALowerBound() {
        assertEquals(
                (float) Math.cbrt(1.20f * 1.10f),
                RogueliteCarUpgrades.deriveMaxSpeedMultiplier(1.20f, 1.10f),
                EPSILON);
        assertEquals(
                0.65f,
                RogueliteCarUpgrades.deriveMaxSpeedMultiplier(0f, 1f),
                EPSILON);
        assertEquals(
                (float) Math.cbrt(9f),
                RogueliteCarUpgrades.deriveMaxSpeedMultiplier(3f, 3f),
                EPSILON);
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
            int maximumLines =
                    card.getSlotType() == RogueliteSlotType.TECHNIQUE
                            ? 5
                            : card.getSlotType() == RogueliteSlotType.TUNING ? 3 : 2;
            assertTrue(
                    "Too many effect lines for " + card.getId(),
                    effectLines.length <= maximumLines);
            for (int line = 0; line < effectLines.length; line++) {
                assertTrue(
                        "Effect line is too long for " + card.getId(),
                        effectLines[line].length() <= 72);
            }
        }
    }

    @Test
    public void cloakCardsExplainCancellationWithoutClaimingToClearDebuffs() {
        RogueliteCardId[] cloakCards = {
                RogueliteCardId.GHOST_CLOAK,
                RogueliteCardId.PHANTOM_CLOAK,
                RogueliteCardId.VOID_CLOAK
        };
        for (int i = 0; i < cloakCards.length; i++) {
            String effectText = RogueliteCardCatalog.get(cloakCards[i]).getEffectText();
            assertTrue(effectText.contains("Cancels targeting Revenge"));
            assertTrue(effectText.contains("Debuffs remain"));
        }
    }

    @Test
    public void quantumCardsExplainIndependentDrivingAndSharedRevenge() {
        RogueliteCardId[] quantumCards = {
                RogueliteCardId.MIRROR_DUO,
                RogueliteCardId.MIRROR_TRIO,
                RogueliteCardId.OVERDRIVE_COIL
        };
        for (int i = 0; i < quantumCards.length; i++) {
            RogueliteCardDefinition card = RogueliteCardCatalog.get(quantumCards[i]);
            assertTrue(card.getDescription().contains("drives independently"));
            assertTrue(card.getDescription().contains("shares the same cards"));
            assertTrue(card.getDescription().contains("executes Revenge with the group"));
            assertTrue(card.getDescription().contains("hit to any copy arms it"));
            assertTrue(card.getEffectText().contains("Shared cards and Revenge"));
        }
    }

    @Test
    public void techniqueCatalogKeepsThirteenStandardChoicesAndOneTierFourAmplifier() {
        List<RogueliteCardDefinition> techniques = cardsForSlot(RogueliteSlotType.TECHNIQUE);
        assertEquals(40, techniques.size());
        for (int tier = 1; tier <= 3; tier++) {
            int cardsInTier = 0;
            for (int i = 0; i < techniques.size(); i++) {
                if (techniques.get(i).getTier() == tier) {
                    cardsInTier++;
                }
            }
            assertEquals("Technique cards in Tier " + tier, 13, cardsInTier);
        }
        assertEquals(4, RogueliteCardCatalog.get(RogueliteCardId.POWERUP_NEXUS).getTier());
    }

    @Test
    public void timedTechniquesUseTwoStatsAndCoverEveryPairInEveryTier() {
        Set<RogueliteCardId> passiveExceptions = new HashSet<RogueliteCardId>();
        Collections.addAll(
                passiveExceptions,
                RogueliteCardId.UNDERDOG_INSTINCT,
                RogueliteCardId.COMEBACK_DRIVE,
                RogueliteCardId.LAST_PLACE_FURY,
                RogueliteCardId.CLOSE_QUARTERS,
                RogueliteCardId.PACK_RACER,
                RogueliteCardId.TRAFFIC_DOMINANCE,
                RogueliteCardId.LAP_DIVIDEND,
                RogueliteCardId.LAP_BOOSTER,
                RogueliteCardId.LAP_DOUBLER,
                RogueliteCardId.POWERUP_LINK,
                RogueliteCardId.POWERUP_MATRIX,
                RogueliteCardId.POWERUP_NEXUS);
        Set<RogueliteCardId> rallyExceptions = new HashSet<RogueliteCardId>();
        Collections.addAll(
                rallyExceptions,
                RogueliteCardId.RALLY_FOCUS,
                RogueliteCardId.RALLY_EXPERT,
                RogueliteCardId.RALLY_MASTER);
        List<Set<Integer>> pairsByTier = new ArrayList<Set<Integer>>();
        for (int tier = 0; tier <= 3; tier++) {
            pairsByTier.add(new HashSet<Integer>());
        }

        List<RogueliteCardDefinition> techniques =
                cardsForSlot(RogueliteSlotType.TECHNIQUE);
        for (int i = 0; i < techniques.size(); i++) {
            RogueliteCardDefinition card = techniques.get(i);
            int statMask = RaceTechniqueEffect.amplifiedStatMask(card.getId());
            assertEquals("Unknown stat bit for " + card.getId(), statMask, statMask & 0x0f);
            if (passiveExceptions.contains(card.getId())) {
                assertEquals("Passive card has multiplier targets: " + card.getId(), 0, statMask);
                continue;
            }
            if (rallyExceptions.contains(card.getId())) {
                assertEquals("Rally must amplify all stats", 4, Integer.bitCount(statMask));
                continue;
            }

            assertEquals(
                    "Timed Technique must amplify two stats: " + card.getId(),
                    2,
                    Integer.bitCount(statMask));
            String[] effectLines = card.getEffectText().split("\\n");
            assertEquals("Technique text must list two multipliers: " + card.getId(), 3, effectLines.length);
            assertTrue(effectLines[1].contains(" x"));
            assertTrue(effectLines[2].contains(" x"));
            pairsByTier.get(card.getTier()).add(statMask);
        }

        for (int tier = 1; tier <= 3; tier++) {
            assertEquals(
                    "Missing two-stat Technique combination in Tier " + tier,
                    6,
                    pairsByTier.get(tier).size());
        }
    }

    @Test
    public void rivalPenalizingAbilitiesUseTheRevengeSlot() {
        assertEquals(
                RogueliteSlotType.REVENGE,
                RogueliteCardCatalog.get(RogueliteCardId.DRAFT_MAGNET).getSlotType());
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
                RogueliteCardCatalog.get(RogueliteCardId.REPULSOR_WAVE).getSlotType());
        assertEquals(
                2,
                RogueliteCardCatalog.get(RogueliteCardId.REPULSOR_WAVE).getTier());
        assertEquals(
                RogueliteSlotType.REVENGE,
                RogueliteCardCatalog.get(RogueliteCardId.HUNTER_BARRAGE).getSlotType());
        assertEquals(
                2,
                RogueliteCardCatalog.get(RogueliteCardId.HUNTER_BARRAGE).getTier());
        assertEquals(
                RogueliteSlotType.REVENGE,
                RogueliteCardCatalog.get(RogueliteCardId.HUNTER_STORM).getSlotType());
        assertEquals(
                3,
                RogueliteCardCatalog.get(RogueliteCardId.HUNTER_STORM).getTier());
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
        RogueliteCarUpgrades lightweight = configured(RogueliteCardId.AGILE_CHASSIS);
        RogueliteCarUpgrades heavyweight = configured(RogueliteCardId.HEAVYWEIGHT_TUNE);
        RogueliteCarUpgrades aero = configured(RogueliteCardId.CHAMPIONSHIP_TUNE);
        RogueliteCarUpgrades streamline = configured(RogueliteCardId.AERO_TRIM);
        RogueliteCarUpgrades carbonPanels = configured(RogueliteCardId.CARBON_PANELS);
        RogueliteCarUpgrades carbonMonocoque = configured(RogueliteCardId.CARBON_MONOCOQUE);
        RogueliteCarUpgrades grapheneChassis = configured(RogueliteCardId.GRAPHENE_CHASSIS);

        assertEquals(0.93f, lightweight.getMassMultiplier(), EPSILON);
        assertEquals(1.06f, heavyweight.getMassMultiplier(), EPSILON);
        assertEquals(1f, heavyweight.getFrontCollisionPushMultiplier(), EPSILON);
        assertEquals(1f, aero.getMassMultiplier(), EPSILON);
        assertEquals(1f / 1.55f, aero.getDragMultiplier(), EPSILON);
        assertEquals(1.28f, aero.getAccelerationMultiplier(), EPSILON);
        assertTrue(streamline.getDragMultiplier() < 1f);
        assertTrue(streamline.getGripMultiplier(0f) < 1f);
        assertEquals(streamline.getGripMultiplier(0f), streamline.getGripMultiplier(0.50f), EPSILON);
        assertEquals(1f, streamline.getSteeringMultiplier(0f), EPSILON);
        assertEquals(0.96f, carbonPanels.getMassMultiplier(), EPSILON);
        assertEquals(0.88f, carbonMonocoque.getMassMultiplier(), EPSILON);
        assertEquals(0.97f, grapheneChassis.getMassMultiplier(), EPSILON);
        assertTrue(carbonMonocoque.getAccelerationMultiplier()
                > carbonPanels.getAccelerationMultiplier());
        assertTrue(grapheneChassis.getGripMultiplier(0f)
                > carbonMonocoque.getGripMultiplier(0f));
        assertEquals(1f, carbonPanels.getFrontCollisionRecoilMultiplier(), EPSILON);
        assertEquals(1f, grapheneChassis.getFrontCollisionPushMultiplier(), EPSILON);
    }

    @Test
    public void velocityShellAndPowerMonocoqueBiasDifferentStatsInTheSamePair() {
        RogueliteCarUpgrades velocity = configured(RogueliteCardId.VELOCITY_SHELL);
        RogueliteCarUpgrades vectoring = configured(RogueliteCardId.TORQUE_VECTORING);

        assertTrue(velocity.getAccelerationMultiplier() > vectoring.getAccelerationMultiplier());
        assertTrue(vectoring.getGripMultiplier(0f) > velocity.getGripMultiplier(0f));
        assertEquals(velocity.getDragMultiplier(), vectoring.getDragMultiplier(), EPSILON);
        assertEquals(velocity.getMassMultiplier(), vectoring.getMassMultiplier(), EPSILON);
        assertEquals(velocity.getSteeringMultiplier(0f), vectoring.getSteeringMultiplier(0f), EPSILON);
    }

    @Test
    public void strengthenedUnderselectedTuningCardsKeepTheirOriginalStatCombinations() {
        RogueliteCarUpgrades agile = configured(RogueliteCardId.AGILE_CHASSIS);
        assertEquals(1.08f, agile.getGripMultiplier(0f), EPSILON);
        assertEquals(0.93f, agile.getMassMultiplier(), EPSILON);
        assertEquals(1f / 0.93f, agile.getDragMultiplier(), EPSILON);

        RogueliteCarUpgrades raceTune = configured(RogueliteCardId.RACE_TUNE);
        assertEquals(1.18f, raceTune.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.08f, raceTune.getGripMultiplier(0f), EPSILON);
        assertEquals(1f / 0.92f, raceTune.getDragMultiplier(), EPSILON);

        RogueliteCarUpgrades carbon = configured(RogueliteCardId.CARBON_MONOCOQUE);
        assertEquals(1.19f, carbon.getAccelerationMultiplier(), EPSILON);
        assertEquals(0.97f, carbon.getGripMultiplier(0f), EPSILON);
        assertEquals(0.88f, carbon.getMassMultiplier(), EPSILON);

        RogueliteCarUpgrades magnesium = configured(RogueliteCardId.MAGNESIUM_SUSPENSION);
        assertEquals(0.95f, magnesium.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.12f, magnesium.getGripMultiplier(0f), EPSILON);
        assertEquals(0.86f, magnesium.getMassMultiplier(), EPSILON);

        RogueliteCarUpgrades aeroAgile = configured(RogueliteCardId.AERO_AGILE_CHASSIS);
        assertEquals(1.11f, aeroAgile.getGripMultiplier(0f), EPSILON);
        assertEquals(0.88f, aeroAgile.getMassMultiplier(), EPSILON);
        assertEquals(1f / 0.95f, aeroAgile.getDragMultiplier(), EPSILON);

        RogueliteCarUpgrades powerMonocoque = configured(RogueliteCardId.TORQUE_VECTORING);
        assertEquals(1.05f, powerMonocoque.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.20f, powerMonocoque.getGripMultiplier(0f), EPSILON);

        RogueliteCarUpgrades carbonPrototype = configured(RogueliteCardId.CARBON_PROTOTYPE);
        assertEquals(1.09f, carbonPrototype.getAccelerationMultiplier(), EPSILON);
        assertEquals(0.75f, carbonPrototype.getMassMultiplier(), EPSILON);
    }

    @Test
    public void tierThreeDuplicateAttributePairsHaveOppositeBiases() {
        assertOppositeBias(
                RogueliteCardId.CHAMPIONSHIP_TUNE,
                RogueliteCardId.HYPERCAR_CORE,
                StatBias.AERO,
                StatBias.POWER);
        assertOppositeBias(
                RogueliteCardId.GROUND_EFFECT,
                RogueliteCardId.TRACK_VACUUM,
                StatBias.GRIP,
                StatBias.AERO);
        assertOppositeBias(
                RogueliteCardId.VELOCITY_SHELL,
                RogueliteCardId.TORQUE_VECTORING,
                StatBias.POWER,
                StatBias.GRIP);
        assertOppositeBias(
                RogueliteCardId.GRAPHENE_CHASSIS,
                RogueliteCardId.FEATHERWEIGHT_GROUND_EFFECT,
                StatBias.GRIP,
                StatBias.MASS);
        assertOppositeBias(
                RogueliteCardId.TITANIUM_SKELETON,
                RogueliteCardId.CARBON_PROTOTYPE,
                StatBias.POWER,
                StatBias.MASS);
        assertOppositeBias(
                RogueliteCardId.ACTIVE_AERO_SHELL,
                RogueliteCardId.WING_CAR,
                StatBias.AERO,
                StatBias.MASS);
    }

    @Test
    public void standardTiersHaveTwelveTuningChoicesAndTierFourHasTheAmplifier() {
        int[] counts = new int[RogueliteCardCatalog.MAX_CARD_TIER + 1];
        for (RogueliteCardDefinition card : cardsForSlot(RogueliteSlotType.TUNING)) {
            counts[card.getTier()]++;
        }

        assertEquals(12, counts[1]);
        assertEquals(12, counts[2]);
        assertEquals(12, counts[3]);
        assertEquals(1, counts[4]);
    }

    @Test
    public void amplifierChainCompoundsTechniquePowerupAndRevengeDeviations() {
        RogueliteCarUpgrades upgrades = configured(
                RogueliteCardId.TECHNIQUE_COUPLER,
                RogueliteCardId.POWERUP_LINK,
                RogueliteCardId.GRUDGE_SPARK);

        // Each connected amplifier directly multiplies the next card.
        assertEquals(1.953125f, upgrades.getRevengeEffectMultiplier(), EPSILON);
    }

    @Test
    public void tierFourAmplifierChainMultipliesRevengeByEight() {
        RogueliteCarUpgrades upgrades = configured(
                RogueliteCardId.TECHNIQUE_SINGULARITY,
                RogueliteCardId.POWERUP_NEXUS,
                RogueliteCardId.NEMESIS_ENGINE);

        assertEquals(8f, upgrades.getRevengeEffectMultiplier(), EPSILON);
    }

    @Test
    public void amplifierChainExtendsPowerupEffectsAndAcceleratesCooldownRecovery() {
        RogueliteCarUpgrades baseline = configured(RogueliteCardId.TIME_RIPPLE);
        RogueliteCarUpgrades amplified = configured(
                RogueliteCardId.TECHNIQUE_COUPLER,
                RogueliteCardId.POWERUP_LINK,
                RogueliteCardId.TIME_RIPPLE);

        update(baseline, 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        update(amplified, 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        int baselineSteps = 0;
        int amplifiedSteps = 0;
        while (!baseline.isTimeDilationActive() && baselineSteps < 200) {
            update(baseline, 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
            baselineSteps++;
        }
        while (!amplified.isTimeDilationActive() && amplifiedSteps < 200) {
            update(amplified, 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
            amplifiedSteps++;
        }

        assertTrue(amplifiedSteps < baselineSteps);
        assertEquals(
                2f * 1.5625f,
                amplified.getActiveTimeRemainingSeconds(RogueliteCardId.TIME_RIPPLE),
                EPSILON);
    }

    @Test
    public void tuningCardsHaveUniqueStatSetups() {
        Set<String> setups = new HashSet<String>();
        for (RogueliteCardDefinition card : cardsForSlot(RogueliteSlotType.TUNING)) {
            if (isTechniqueAmplifierTuning(card.getId())) {
                continue;
            }
            RogueliteCarUpgrades upgrades = configured(card.getId());
            String setup = upgrades.getAccelerationMultiplier()
                    + "|" + upgrades.getGripMultiplier(0f)
                    + "|" + upgrades.getDragMultiplier()
                    + "|" + upgrades.getMassMultiplier();

            assertTrue("Duplicate tuning setup for " + card.getId(), setups.add(setup));
        }
    }

    @Test
    public void tuningCardsHaveUniqueSignedStatCombinationsWithinLowerTiers() {
        List<RogueliteCardDefinition> tuning = cardsForSlot(RogueliteSlotType.TUNING);
        for (int tier = 1; tier <= 2; tier++) {
            Set<String> signatures = new HashSet<String>();
            for (int i = 0; i < tuning.size(); i++) {
                RogueliteCardDefinition card = tuning.get(i);
                if (card.getTier() == tier && !isTechniqueAmplifierTuning(card.getId())) {
                    String signature = signedStatSignature(card);
                    assertTrue(
                            "Duplicate Tier " + tier + " tuning signature "
                                    + signature + " on " + card.getId(),
                            signatures.add(signature));
                }
            }
        }
    }

    @Test
    public void techniqueCardsDoNotDuplicateAnActivationAndStatCombinationWithinEachTier() {
        List<RogueliteCardDefinition> techniques =
                cardsForSlot(RogueliteSlotType.TECHNIQUE);
        for (int tier = 1; tier <= 3; tier++) {
            Set<String> signatures = new HashSet<String>();
            for (int i = 0; i < techniques.size(); i++) {
                RogueliteCardDefinition card = techniques.get(i);
                if (card.getTier() == tier) {
                    String signature =
                            techniqueActivationSignature(card)
                                    + "|"
                                    + signedStatSignature(card);
                    assertTrue(
                            "Duplicate Tier " + tier + " Technique activation and stats "
                                    + signature + " on " + card.getId(),
                            signatures.add(signature));
                }
            }
        }
    }

    @Test
    public void tuningCardsOnlyModifyPowerGripAeroAndMass() {
        for (RogueliteCardDefinition card : cardsForSlot(RogueliteSlotType.TUNING)) {
            RogueliteCarUpgrades upgrades = configured(card.getId());

            assertEquals(card.getId().name(), 1f, upgrades.getSteeringMultiplier(0f), EPSILON);
            assertEquals(
                    card.getId().name(),
                    upgrades.getGripMultiplier(0f),
                    upgrades.getGripMultiplier(0.50f),
                    EPSILON);
            assertEquals(
                    card.getId().name(),
                    1f,
                    upgrades.getFrontCollisionRecoilMultiplier(),
                    EPSILON);
            assertEquals(
                    card.getId().name(),
                    1f,
                    upgrades.getFrontCollisionPushMultiplier(),
                    EPSILON);
        }
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

        assertEquals(1.07f, equipped.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.22f, preview.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.20f, preview.getAerodynamicEfficiency(), EPSILON);
        assertTrue(preview.getMaxSpeedMultiplier() > equipped.getMaxSpeedMultiplier());
    }

    @Test
    public void lapExperienceTechniquesAndTechniqueAmplifierCompose() {
        RogueliteCarUpgrades tierOne = configured(RogueliteCardId.LAP_DIVIDEND);
        RogueliteCarUpgrades tierTwo = configured(RogueliteCardId.LAP_BOOSTER);
        RogueliteCarUpgrades amplified = configured(
                RogueliteCardId.TECHNIQUE_SINGULARITY,
                RogueliteCardId.LAP_DOUBLER);

        assertEquals(1.25f, tierOne.getLapExperienceBankMultiplier(), EPSILON);
        assertEquals(1.5f, tierTwo.getLapExperienceBankMultiplier(), EPSILON);
        assertEquals(4f, amplified.getLapExperienceBankMultiplier(), EPSILON);
        assertEquals(
                2f,
                RogueliteCarStatSnapshot.from(
                                loadout(RogueliteCardId.LAP_DOUBLER),
                                null)
                        .getLapExperienceBankMultiplier(),
                EPSILON);
    }

    @Test
    public void conditionalCardsDoNotMisrepresentPassiveCarStats() {
        RogueliteLoadout loadout = loadout(RogueliteCardId.AERO_TRIM);

        RogueliteCarStatSnapshot tuning =
                RogueliteCarStatSnapshot.from(loadout, null);
        RogueliteCarStatSnapshot withTechniquePreview =
                RogueliteCarStatSnapshot.from(
                        loadout,
                        RogueliteCardId.DRIFT_FOCUS);

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
                        1f,
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
                (float) Math.cbrt(
                        live.getAccelerationMultiplier()
                                * live.getAerodynamicEfficiency()),
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
                configured(
                        RogueliteCardId.TRACK_WING,
                        RogueliteCardId.CORNER_FOCUS);
        RogueliteCarStatSnapshot passive =
                RogueliteCarStatSnapshot.from(
                        loadout(
                                RogueliteCardId.TRACK_WING,
                                RogueliteCardId.CORNER_FOCUS),
                        null);

        update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0.20f, 0.5f, 0.2f, 0f, 0f);
        RogueliteCarStatSnapshot active =
                RogueliteCarStatSnapshot.fromLive(
                        upgrades,
                        0f,
                        1f,
                        0.58f,
                        1f,
                        1f,
                        1f,
                        1f);

        assertTrue(upgrades.getActiveCardIds().contains(RogueliteCardId.CORNER_FOCUS));
        assertEquals(RogueliteCardId.CORNER_FOCUS, upgrades.getActiveTechniqueCardId());
        assertEquals(
                passive.getAccelerationMultiplier(),
                active.getAccelerationMultiplier(),
                EPSILON);
        assertEquals(
                upgrades.getGripMultiplier(0f) * 0.58f,
                active.getGripMultiplier(),
                EPSILON);
        assertTrue(upgrades.getGripMultiplier(0f) > passive.getGripMultiplier());
        assertTrue(active.getAerodynamicEfficiency()
                > passive.getAerodynamicEfficiency());
    }

    @Test
    public void tuningAndTechniqueEffectsComposeWithoutPairSpecificRules() {
        RogueliteCarUpgrades streamlinedDraft = configured(
                RogueliteCardId.AERO_TRIM,
                RogueliteCardId.DRAFT_FOCUS);
        RogueliteCarUpgrades activeDrift = configured(
                RogueliteCardId.AERO_TRIM,
                RogueliteCardId.DRIFT_EXPERT);

        update(streamlinedDraft, 0.1f, 1f, true, 0.02f, 0.7f, 0.4f, 0f, 1f, 0f, 0.8f, 0f);
        assertEquals(1.24f, streamlinedDraft.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f / 1.28f, streamlinedDraft.getDragMultiplier(), EPSILON);
        assertEquals(1f, streamlinedDraft.getSlipstreamStrengthMultiplier(), EPSILON);

        update(activeDrift, 0.1f, 1f, true, 0.24f, 0.7f, 0f, 0.2f, 0.2f, 0.2f, 0f, 0f);
        assertEquals(1.24f, activeDrift.getAccelerationMultiplier(), EPSILON);
        assertEquals(0.98f, activeDrift.getGripMultiplier(0.05f), EPSILON);
    }

    @Test
    public void rlBenchmarkHookAcceptsTheRequestedCardSlotOnly() {
        RatassGame.RlTrainingConfig config = new RatassGame.RlTrainingConfig();

        assertNull(config.benchmarkCard);
        assertEquals(1f, config.benchmarkTuningEffectMultiplier, EPSILON);
        config.withBenchmarkTuningCard("AERO_TRIM");
        assertEquals(RogueliteCardId.AERO_TRIM, config.benchmarkCard);
        config.withBenchmarkTuningEffectMultiplier(3f);
        assertEquals(3f, config.benchmarkTuningEffectMultiplier, EPSILON);
        config.withBenchmarkTuningCard("");
        assertNull(config.benchmarkCard);
        config.withBenchmarkPowerupCard("NITRO_PULSE");
        assertEquals(RogueliteCardId.NITRO_PULSE, config.benchmarkCard);
        config.withBenchmarkTechniqueCard("STRAIGHT_FOCUS");
        assertEquals(RogueliteCardId.STRAIGHT_FOCUS, config.benchmarkCard);

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
        try {
            config.withBenchmarkTuningEffectMultiplier(0f);
            fail("A non-positive Tuning benchmark multiplier must not be accepted");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("must be positive"));
        }
    }

    @Test
    public void tuningBenchmarkMultiplierScalesOnlyTheTuningDeviation() {
        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.AERO_TRIM);

        upgrades.setBenchmarkTuningEffectMultiplier(3f);

        assertEquals(1.36f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(0.98f, upgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(1f / 1.42f, upgrades.getDragMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getMassMultiplier(), EPSILON);
    }

    @Test
    public void cornerTechniqueRunsItsFullWindowAndSamplesAgainWhenItExpires() {
        RogueliteCarUpgrades upgrades = configured(
                RogueliteCardId.AERO_TRIM,
                RogueliteCardId.CORNER_FOCUS);

        update(upgrades, 0.1f, 1f, true, 0.02f, 0.65f, 0f, 0.20f, 0.2f, 0.2f, 0f, 0f);
        assertEquals(1.12f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(0.98f, upgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(1f / 1.21f, upgrades.getDragMultiplier(), EPSILON);
        assertTrue(upgrades.getActiveCardIds().contains(RogueliteCardId.CORNER_FOCUS));
        assertEquals(2f,
                upgrades.getActiveTimeRemainingSeconds(RogueliteCardId.CORNER_FOCUS),
                EPSILON);

        update(upgrades, 0.5f, 1f, true, 0.02f, 0.65f, 0f, 0f, 0.8f, 0f, 0f, 0f);
        assertEquals(1.5f,
                upgrades.getActiveTimeRemainingSeconds(RogueliteCardId.CORNER_FOCUS),
                EPSILON);

        update(upgrades, 0.1f, 1f, true, 0.02f, 0.65f, 0f, 0.20f, 0.2f, 0.2f, 0f, 0f);
        assertEquals(1.4f,
                upgrades.getActiveTimeRemainingSeconds(RogueliteCardId.CORNER_FOCUS),
                EPSILON);

        update(upgrades, 1.5f, 1f, true, 0.02f, 0.65f, 0f, 0.20f, 0.2f, 0.2f, 0f, 0f);
        assertTrue(upgrades.getActiveCardIds().contains(RogueliteCardId.CORNER_FOCUS));
        assertEquals(2f,
                upgrades.getActiveTimeRemainingSeconds(RogueliteCardId.CORNER_FOCUS),
                EPSILON);

        update(upgrades, 0.1f, 1f, true, 0.02f, 0.65f, 0f, 0f, 0.8f, 0f, 0f, 0f);
        assertTrue(upgrades.getActiveCardIds().contains(RogueliteCardId.CORNER_FOCUS));
        assertEquals(1.9f,
                upgrades.getActiveTimeRemainingSeconds(RogueliteCardId.CORNER_FOCUS),
                EPSILON);

        update(upgrades, 2f, 1f, true, 0.02f, 0.65f, 0f, 0f, 0.8f, 0f, 0f, 0f);
        assertFalse(upgrades.getActiveCardIds().contains(RogueliteCardId.CORNER_FOCUS));
        assertEquals(0.98f, upgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(1f / 1.14f, upgrades.getDragMultiplier(), EPSILON);
    }

    @Test
    public void cornerMasterToleratesCurvatureNoiseAndRearmsItsFourSecondWindow() {
        RogueliteCarUpgrades upgrades = configured(
                RogueliteCardId.AERO_TRIM,
                RogueliteCardId.CORNER_MASTER);

        update(upgrades, 0.1f, 1f, true, 0.02f, 0.65f, 0f, 0.12f, 0.2f, 0.2f, 0f, 0f);
        assertEquals(4f,
                upgrades.getActiveTimeRemainingSeconds(RogueliteCardId.CORNER_MASTER),
                EPSILON);

        update(upgrades, 3.9f, 1f, true, 0.02f, 0.65f, 0f, 0.08f, 0.2f, 0.2f, 0f, 0f);
        assertTrue(upgrades.getActiveCardIds().contains(RogueliteCardId.CORNER_MASTER));
        assertEquals(0.1f,
                upgrades.getActiveTimeRemainingSeconds(RogueliteCardId.CORNER_MASTER),
                EPSILON);

        update(upgrades, 0.2f, 1f, true, 0.02f, 0.65f, 0f, 0.08f, 0.2f, 0.2f, 0f, 0f);
        assertTrue(upgrades.getActiveCardIds().contains(RogueliteCardId.CORNER_MASTER));
        assertEquals(4f,
                upgrades.getActiveTimeRemainingSeconds(RogueliteCardId.CORNER_MASTER),
                EPSILON);

        update(upgrades, 0.1f, 1f, true, 0.02f, 0.65f, 0f, 0.05f, 0.2f, 0.2f, 0f, 0f);
        assertTrue(upgrades.getActiveCardIds().contains(RogueliteCardId.CORNER_MASTER));
        assertEquals(3.9f,
                upgrades.getActiveTimeRemainingSeconds(RogueliteCardId.CORNER_MASTER),
                EPSILON);

        update(upgrades, 4f, 1f, true, 0.02f, 0.65f, 0f, 0.05f, 0.2f, 0.2f, 0f, 0f);
        assertFalse(upgrades.getActiveCardIds().contains(RogueliteCardId.CORNER_MASTER));
        assertEquals(0f,
                upgrades.getActiveTimeRemainingSeconds(RogueliteCardId.CORNER_MASTER),
                EPSILON);
    }

    @Test
    public void cornerTechniquesLastOneSecondLessThanDriftTechniques() {
        RogueliteCardId[] cornerCards = {
            RogueliteCardId.CORNER_FOCUS,
            RogueliteCardId.CORNER_EXPERT,
            RogueliteCardId.CORNER_MASTER
        };
        RogueliteCardId[] driftCards = {
            RogueliteCardId.DRIFT_FOCUS,
            RogueliteCardId.DRIFT_EXPERT,
            RogueliteCardId.DRIFT_MASTER
        };
        float[] expectedCornerDurations = {2f, 3f, 4f};
        float[] expectedDriftDurations = {3f, 4f, 5f};

        for (int i = 0; i < cornerCards.length; i++) {
            RogueliteCarUpgrades corner = configured(RogueliteCardId.AERO_TRIM, cornerCards[i]);
            update(corner, 0.1f, 1f, true, 0.02f, 0.65f, 0f, 0.20f, 0.2f, 0.2f, 0f, 0f);
            assertEquals(expectedCornerDurations[i],
                    corner.getActiveTimeRemainingSeconds(cornerCards[i]), EPSILON);

            RogueliteCarUpgrades drift = configured(RogueliteCardId.AERO_TRIM, driftCards[i]);
            update(drift, 0.1f, 1f, true, 0.24f, 0.65f, 0f, 0f, 0.2f, 0.2f, 0f, 0f);
            assertEquals(expectedDriftDurations[i],
                    drift.getActiveTimeRemainingSeconds(driftCards[i]), EPSILON);
        }
    }

    @Test
    public void timedTechniquesSampleTheirConditionsOnlyAtWindowBoundaries() {
        assertTechniqueWindow(
                RogueliteCardId.DRAFT_FOCUS,
                10f,
                techniqueFrame(true, 0f, 0.65f, 0.20f, false),
                techniqueFrame(true, 0f, 0.65f, 0f, false));
        assertTechniqueWindow(
                RogueliteCardId.STRAIGHT_FOCUS,
                3f,
                techniqueFrame(true, 0f, 0.65f, 0f, true),
                techniqueFrame(true, 0f, 0.65f, 0f, false));
        assertTechniqueWindow(
                RogueliteCardId.DRIFT_FOCUS,
                3f,
                techniqueFrame(true, 0.24f, 0.65f, 0f, false),
                techniqueFrame(true, 0f, 0.65f, 0f, false));
        assertTechniqueWindow(
                RogueliteCardId.RALLY_FOCUS,
                10f,
                techniqueFrame(false, 0f, 0.20f, 0f, false),
                techniqueFrame(true, 0f, 0.20f, 0f, false));
    }

    @Test
    public void quantumCopyTechniqueConditionActivatesTheSharedFamilyEffect() {
        RogueliteCarUpgrades upgrades = configured(
                RogueliteCardId.SPORT_TUNE,
                RogueliteCardId.RALLY_FOCUS);

        update(upgrades, 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertFalse(upgrades.getActiveCardIds().contains(RogueliteCardId.RALLY_FOCUS));

        upgrades.observeTechniqueConditions(false, 0f, 0.5f, 0f, 0f, false);

        assertTrue(upgrades.getActiveCardIds().contains(RogueliteCardId.RALLY_FOCUS));
        assertEquals(
                10f,
                upgrades.getActiveTimeRemainingSeconds(RogueliteCardId.RALLY_FOCUS),
                EPSILON);

        upgrades.observeTechniqueConditions(false, 0f, 0.5f, 0f, 0f, false);
        assertEquals(
                10f,
                upgrades.getActiveTimeRemainingSeconds(RogueliteCardId.RALLY_FOCUS),
                EPSILON);
    }

    @Test
    public void everyTimedTechniqueTriggerTargetsItsExpectedStats() {
        RogueliteCarUpgrades draft = configured(
                RogueliteCardId.AERO_TRIM,
                RogueliteCardId.DRAFT_FOCUS);
        update(draft, 0.1f, 1f, true, 0.02f, 0.7f, 0.3f, 0f, 1f, 0f, 0f, 0f);
        assertEquals(1.24f, draft.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f / 1.28f, draft.getDragMultiplier(), EPSILON);

        RogueliteCarUpgrades straight = configured(
                RogueliteCardId.SHORT_GEARING,
                RogueliteCardId.STRAIGHT_EXPERT);
        update(straight, 0.1f, 1f, true, 0.02f, 0.7f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertEquals(1.26f, straight.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f / 1.26f, straight.getDragMultiplier(), EPSILON);
        assertEquals(1.05f, straight.getMassMultiplier(), EPSILON);

        RogueliteCarUpgrades drift = configured(
                RogueliteCardId.CARBON_PROTOTYPE,
                RogueliteCardId.DRIFT_MASTER);
        update(drift, 0.1f, 1f, true, 0.24f, 0.7f, 0f, 0.2f, 0.2f, 0.2f, 0f, 0f);
        assertEquals(1.27f, drift.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f, drift.getGripMultiplier(0f), EPSILON);
        assertEquals(0.25f, drift.getMassMultiplier(), EPSILON);
        assertEquals(1f, drift.getDragMultiplier(), EPSILON);

        RogueliteCarUpgrades offRoad = configured(
                RogueliteCardId.SPORT_TUNE,
                RogueliteCardId.RALLY_EXPERT);
        update(offRoad, 0.1f, 1f, false, 0.02f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertEquals(1.08f, offRoad.getGripMultiplier(0f), EPSILON);
        assertEquals(1.16f, offRoad.getMassMultiplier(), EPSILON);
        assertEquals(1.592f, offRoad.getMassMultiplier(1.20f), EPSILON);
    }

    @Test
    public void uncommonDraftUsesStrongerScalingWhileOffRoadUsesStandardTierScaling() {
        RogueliteCardId[] draftCards = {
            RogueliteCardId.DRAFT_FOCUS,
            RogueliteCardId.DRAFT_EXPERT,
            RogueliteCardId.DRAFT_MASTER
        };
        RogueliteCardId[] offRoadCards = {
            RogueliteCardId.RALLY_FOCUS,
            RogueliteCardId.RALLY_EXPERT,
            RogueliteCardId.RALLY_MASTER
        };
        float[] expectedDraftPower = {1.24f, 1.36f, 1.48f};
        float[] expectedOffRoadPower = {1.15f, 1.20f, 1.30f};
        float[] expectedGrip = {1.06f, 1.08f, 1.12f};
        float[] expectedMass = {1.12f, 1.16f, 1.24f};
        float[] expectedDraftScale = {2f, 3f, 4f};
        float[] expectedOffRoadScale = {1.5f, 2f, 3f};
        float[] expectedDuration = {10f, 10f, 10f};

        for (int i = 0; i < expectedDraftPower.length; i++) {
            RogueliteCarUpgrades draft = configured(
                    RogueliteCardId.AERO_TRIM,
                    draftCards[i]);
            update(draft, 0.1f, 1f, true, 0.02f, 0.7f, 0.3f, 0f, 1f, 0f, 0f, 0f);
            assertEquals(expectedDraftPower[i], draft.getAccelerationMultiplier(), EPSILON);
            assertEquals(
                    expectedDuration[i],
                    draft.getActiveTimeRemainingSeconds(draftCards[i]),
                    EPSILON);

            RaceTechniqueEffect draftEffect = new RaceTechniqueEffect(draftCards[i]);
            draftEffect.advance(0.1f, 0.1f,
                    techniqueFrame(true, 0f, 0.7f, 0.3f, false));
            assertEquals(expectedDraftScale[i], draftEffect.powerDeviationScale(), EPSILON);
            assertEquals(expectedDraftScale[i], draftEffect.aeroDeviationScale(), EPSILON);

            RogueliteCarUpgrades offRoad = configured(
                    RogueliteCardId.SPORT_TUNE,
                    offRoadCards[i]);
            update(offRoad, 0.1f, 1f, false, 0.02f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
            assertEquals(expectedOffRoadPower[i],
                    offRoad.getAccelerationMultiplier(), EPSILON);
            assertEquals(expectedGrip[i], offRoad.getGripMultiplier(0f), EPSILON);
            assertEquals(expectedMass[i], offRoad.getMassMultiplier(), EPSILON);
            assertEquals(
                    expectedDuration[i],
                    offRoad.getActiveTimeRemainingSeconds(offRoadCards[i]),
                    EPSILON);

            RaceTechniqueEffect effect = new RaceTechniqueEffect(offRoadCards[i]);
            effect.advance(0.1f, 0.1f,
                    techniqueFrame(false, 0f, 0.5f, 0f, false));
            assertEquals(expectedOffRoadScale[i], effect.powerDeviationScale(), EPSILON);
            assertEquals(expectedOffRoadScale[i], effect.gripDeviationScale(), EPSILON);
            assertEquals(expectedOffRoadScale[i], effect.aeroDeviationScale(), EPSILON);
            assertEquals(expectedOffRoadScale[i], effect.massDeviationScale(), EPSILON);
        }
    }

    @Test
    public void alternateTechniqueFamiliesTargetTheirOwnStatPairs() {
        RogueliteCarUpgrades apex = configured(
                RogueliteCardId.GROUNDED_AERO,
                RogueliteCardId.APEX_FOCUS);
        update(apex, 0.1f, 1f, true, 0.02f, 0.65f, 0f, 0.20f, 0.2f, 0.2f, 0f, 0f);
        assertEquals(1f, apex.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.06f, apex.getGripMultiplier(0f), EPSILON);
        assertEquals(1f / 1.27f, apex.getDragMultiplier(), EPSILON);
        assertEquals(1.03f, apex.getMassMultiplier(), EPSILON);

        RogueliteCarUpgrades sprint = configured(
                RogueliteCardId.CARBON_PROTOTYPE,
                RogueliteCardId.SPRINT_EXPERT);
        update(sprint, 0.1f, 1f, true, 0.02f, 0.7f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertEquals(1.18f, sprint.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f, sprint.getDragMultiplier(), EPSILON);
        assertEquals(0.50f, sprint.getMassMultiplier(), EPSILON);

        RogueliteCarUpgrades slide = configured(
                RogueliteCardId.CARBON_PROTOTYPE,
                RogueliteCardId.SLIDE_MASTER);
        update(slide, 0.1f, 1f, true, 0.24f, 0.7f, 0f, 0.2f, 0.2f, 0.2f, 0f, 0f);
        assertEquals(1.09f, slide.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f, slide.getGripMultiplier(0f), EPSILON);
        assertEquals(1f, slide.getDragMultiplier(), EPSILON);
        assertEquals(0.25f, slide.getMassMultiplier(), EPSILON);

        assertEquals(2f, apex.getActiveTimeRemainingSeconds(RogueliteCardId.APEX_FOCUS), EPSILON);
        assertEquals(4f, sprint.getActiveTimeRemainingSeconds(RogueliteCardId.SPRINT_EXPERT), EPSILON);
        assertEquals(5f, slide.getActiveTimeRemainingSeconds(RogueliteCardId.SLIDE_MASTER), EPSILON);
    }

    @Test
    public void newCornerTechniqueFamiliesTargetPowerGripAndGripMass() {
        RogueliteCarUpgrades traction = configured(
                RogueliteCardId.VELOCITY_SHELL,
                RogueliteCardId.TRACTION_FOCUS);
        update(traction, 0.1f, 1f, true, 0.02f, 0.65f, 0f, 0.20f, 0.2f, 0.2f, 0f, 0f);
        assertEquals(1.39f, traction.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.03f, traction.getGripMultiplier(0f), EPSILON);
        assertEquals(1f, traction.getDragMultiplier(), EPSILON);
        assertEquals(1f, traction.getMassMultiplier(), EPSILON);

        RogueliteCarUpgrades agility = configured(
                RogueliteCardId.GRAPHENE_CHASSIS,
                RogueliteCardId.AGILITY_FOCUS);
        update(agility, 0.1f, 1f, true, 0.02f, 0.65f, 0f, 0.20f, 0.2f, 0.2f, 0f, 0f);
        assertEquals(1f, agility.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.135f, agility.getGripMultiplier(0f), EPSILON);
        assertEquals(1f, agility.getDragMultiplier(), EPSILON);
        assertEquals(0.955f, agility.getMassMultiplier(), EPSILON);

        assertEquals(2f,
                traction.getActiveTimeRemainingSeconds(RogueliteCardId.TRACTION_FOCUS),
                EPSILON);
        assertEquals(2f,
                agility.getActiveTimeRemainingSeconds(RogueliteCardId.AGILITY_FOCUS),
                EPSILON);
    }

    @Test
    public void activeTechniqueAmplifiesPowerupBonuses() {
        RogueliteCarUpgrades upgrades = configured(
                RogueliteCardId.STRAIGHT_FOCUS,
                RogueliteCardId.NITRO_PULSE);
        for (int i = 0; i < 80 && upgrades.getActivePowerupCardId() == null; i++) {
            update(upgrades, 0.1f, 1f, true, 0.01f, 0.35f, 0f, 0f, 1f, 0f, 0f, 0f);
        }
        assertEquals(RogueliteCardId.NITRO_PULSE, upgrades.getActivePowerupCardId());
        assertEquals(1.30f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals((float) Math.cbrt(1.30f), upgrades.getMaxSpeedMultiplier(), EPSILON);
    }

    @Test
    public void activeTechniqueAmplifiesGripBonusesButNotPenaltiesOrWeather() {
        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.RALLY_FOCUS);
        update(upgrades, 0.1f, 1f, false, 0.02f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);

        assertEquals(0.90f, upgrades.getGripMultiplier(0f, 0.90f), EPSILON);
        assertEquals(0.58f, upgrades.getGripMultiplier(0f, 0.58f, 1f), EPSILON);
        assertEquals(0.522f, upgrades.getGripMultiplier(0f, 0.58f, 0.90f), EPSILON);
        assertEquals(0.85f, upgrades.getMassMultiplier(0.90f), EPSILON);
        assertEquals(1f, upgrades.getGripMultiplier(0f), EPSILON);
    }

    @Test
    public void activeTechniqueAppliesSurfaceGripAfterAmplifyingGripBonus() {
        RogueliteCarUpgrades upgrades = configured(
                RogueliteCardId.TRACK_WING,
                RogueliteCardId.CORNER_FOCUS);
        update(upgrades, 0.1f, 1f, true, 0.02f, 0.65f, 0f, 0.20f, 0.2f, 0.2f, 0f, 0f);

        assertEquals(1.09f, upgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(0.6322f, upgrades.getGripMultiplier(0f, 0.58f, 1f), EPSILON);
        assertEquals(0.55332f, upgrades.getGripMultiplier(0f, 0.58f, 0.90f), EPSILON);
    }

    @Test
    public void techniqueAmplificationIncreasesStrictlyByTier() {
        RogueliteCarUpgrades tierOne = configured(
                RogueliteCardId.AERO_TRIM,
                RogueliteCardId.STRAIGHT_FOCUS);
        RogueliteCarUpgrades tierTwo = configured(
                RogueliteCardId.AERO_TRIM,
                RogueliteCardId.STRAIGHT_EXPERT);
        RogueliteCarUpgrades tierThree = configured(
                RogueliteCardId.AERO_TRIM,
                RogueliteCardId.STRAIGHT_MASTER);

        update(tierOne, 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        update(tierTwo, 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        update(tierThree, 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);

        assertEquals(1.18f, tierOne.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.24f, tierTwo.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.36f, tierThree.getAccelerationMultiplier(), EPSILON);
        assertEquals(3f, tierOne.getActiveTimeRemainingSeconds(RogueliteCardId.STRAIGHT_FOCUS), EPSILON);
        assertEquals(4f, tierTwo.getActiveTimeRemainingSeconds(RogueliteCardId.STRAIGHT_EXPERT), EPSILON);
        assertEquals(5f, tierThree.getActiveTimeRemainingSeconds(RogueliteCardId.STRAIGHT_MASTER), EPSILON);
    }

    @Test
    public void positionTechniquesBoostPowerGripAndAeroWithoutChangingMassOrSteering() {
        assertPositionTechnique(RogueliteCardId.UNDERDOG_INSTINCT, 0.10f);
        assertPositionTechnique(RogueliteCardId.COMEBACK_DRIVE, 0.20f);
        assertPositionTechnique(RogueliteCardId.LAST_PLACE_FURY, 0.40f);
    }

    @Test
    public void nearbyRivalTechniquesBoostPowerGripAeroAndLowerMassWithoutChangingSteering() {
        assertNearbyRivalTechnique(RogueliteCardId.CLOSE_QUARTERS, 0.05f);
        assertNearbyRivalTechnique(RogueliteCardId.PACK_RACER, 0.10f);
        assertNearbyRivalTechnique(RogueliteCardId.TRAFFIC_DOMINANCE, 0.20f);
    }

    @Test
    public void techniqueCanUseItsWiderNearbyRivalSignalWithoutActivatingPowerup() {
        RogueliteCarUpgrades upgrades = configured(
                RogueliteCardId.CLOSE_QUARTERS,
                RogueliteCardId.GHOST_CLOAK);

        for (int i = 0; i < 20; i++) {
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
                    0f,
                    0f,
                    false,
                    0.5f);
        }

        assertEquals(1.05f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertTrue(upgrades.getActiveCardIds().contains(RogueliteCardId.CLOSE_QUARTERS));
        assertFalse(upgrades.getActiveCardIds().contains(RogueliteCardId.GHOST_CLOAK));
    }

    @Test
    public void randomCardsExposeTheirLoadedChildForTheCarPanel() {
        RogueliteCarUpgrades powerup = configured(RogueliteCardId.LUCKY_SPARK);
        RogueliteCardId loadedPowerup =
                powerup.getLoadedCardId(RogueliteSlotType.POWERUP);
        assertNotNull(loadedPowerup);
        assertEquals(
                RogueliteSlotType.POWERUP,
                RogueliteCardCatalog.get(loadedPowerup).getSlotType());
        assertEquals(1, RogueliteCardCatalog.get(loadedPowerup).getTier());
        assertFalse(RandomCardEffect.isRandomCard(loadedPowerup));
        assertNull(powerup.getLoadedCardId(RogueliteSlotType.REVENGE));

        RogueliteCarUpgrades revenge = configured(RogueliteCardId.CHAOS_RETORT);
        RogueliteCardId loadedRevenge =
                revenge.getLoadedCardId(RogueliteSlotType.REVENGE);
        assertNotNull(loadedRevenge);
        assertEquals(
                RogueliteSlotType.REVENGE,
                RogueliteCardCatalog.get(loadedRevenge).getSlotType());
        assertEquals(2, RogueliteCardCatalog.get(loadedRevenge).getTier());
        assertFalse(RandomCardEffect.isRandomCard(loadedRevenge));
    }

    @Test
    public void tuningCardsDisplayOneStatModifierPerLine() {
        List<RogueliteCardDefinition> cards = RogueliteCardCatalog.all();
        for (int i = 0; i < cards.size(); i++) {
            RogueliteCardDefinition card = cards.get(i);
            if (card.getSlotType() == RogueliteSlotType.TUNING) {
                if (isTechniqueAmplifierTuning(card.getId())) {
                    assertEquals(1, card.getEffectText().split("\\n").length);
                    continue;
                }
                int expectedLines = card.getTier() == 3 ? 2 : 3;
                assertEquals(card.getId().name(), expectedLines,
                        card.getEffectText().split("\\n").length);
            }
        }
    }

    @Test
    public void techniqueAndRevengeCardsLeadWithTheirActivationCondition() {
        List<RogueliteCardDefinition> cards = RogueliteCardCatalog.all();
        for (int i = 0; i < cards.size(); i++) {
            RogueliteCardDefinition card = cards.get(i);
            if (card.getSlotType() == RogueliteSlotType.TECHNIQUE) {
                String[] lines = card.getEffectText().split("\\n");
                assertTrue(card.getId().name(), lines[0].startsWith("Activation: "));
                assertTrue(card.getId().name(), lines.length >= 2);
                assertTrue(card.getId().name(), lines.length <= 5);
                assertFalse(card.getId().name(), card.getEffectText().toLowerCase().contains("bonus"));
                assertFalse(card.getId().name(), card.getEffectText().toLowerCase().contains("penalt"));
                if (isPowerupAmplifierTechnique(card.getId())) {
                    assertTrue(lines[1].startsWith("Powerup effects x"));
                    assertTrue(lines[2].startsWith("Cooldown recovery x"));
                    continue;
                }
                if (card.getId() == RogueliteCardId.LAP_DIVIDEND
                        || card.getId() == RogueliteCardId.LAP_BOOSTER
                        || card.getId() == RogueliteCardId.LAP_DOUBLER) {
                    assertTrue(lines[1].startsWith("Banked lap XP x"));
                    continue;
                }
                for (int line = 1; line < lines.length; line++) {
                    assertTrue(
                            card.getId().name() + ": " + lines[line],
                            lines[line].matches("^(Power|Grip|Aero|Mass) .+"));
                }
            } else if (card.getSlotType() == RogueliteSlotType.REVENGE) {
                String[] lines = card.getEffectText().split("\\n");
                assertEquals(card.getId().name(), "Activation: Rival hit", lines[0]);
                assertEquals(card.getId().name(), 2, lines.length);
            }
        }
    }

    @Test
    public void priorityHotlineContinuouslyRenewsWithConcurrentCooldown() {
        RogueliteCarUpgrades tierOne = activateAutomaticPowerup(
                RogueliteCardId.ACE_HOTLINE);
        RogueliteCarUpgrades tierTwo = configured(RogueliteCardId.PRIORITY_HOTLINE);
        update(tierTwo, 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);

        assertTrue(tierOne.isBestDriverActive());
        assertTrue(tierTwo.isBestDriverActive());
        assertEquals(10f, tierOne.getActiveTimeRemainingSeconds(
                RogueliteCardId.ACE_HOTLINE), EPSILON);
        assertEquals(9.9f, tierTwo.getActiveTimeRemainingSeconds(
                RogueliteCardId.PRIORITY_HOTLINE), EPSILON);
        assertEquals(20f, tierOne.getCooldownTimeRemainingSeconds(
                RogueliteCardId.ACE_HOTLINE), EPSILON);
        assertEquals(9.9f, tierTwo.getCooldownTimeRemainingSeconds(
                RogueliteCardId.PRIORITY_HOTLINE), EPSILON);
        assertEquals(1f, tierOne.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f, tierTwo.getAccelerationMultiplier(), EPSILON);

        update(tierOne, 10.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        update(tierTwo, 10.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertFalse(tierOne.isBestDriverActive());
        assertTrue(tierTwo.isBestDriverActive());
        assertEquals(10f, tierTwo.getActiveTimeRemainingSeconds(
                RogueliteCardId.PRIORITY_HOTLINE), EPSILON);
        assertEquals(10f, tierTwo.getCooldownTimeRemainingSeconds(
                RogueliteCardId.PRIORITY_HOTLINE), EPSILON);
        assertEquals(RogueliteCardId.PRIORITY_HOTLINE, tierTwo.getActivePowerupCardId());
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
        assertEquals((float) Math.cbrt(1.20f), nitro.getMaxSpeedMultiplier(), EPSILON);
        assertEquals((float) Math.cbrt(1.32f), rocket.getMaxSpeedMultiplier(), EPSILON);
        assertEquals((float) Math.cbrt(1.38f), hyperdrive.getMaxSpeedMultiplier(), EPSILON);
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
    public void powerupAmplifierChainMultipliesQuantumCopies() {
        RogueliteCarUpgrades directAmplifier = configured(
                RogueliteCardId.POWERUP_NEXUS,
                RogueliteCardId.OVERDRIVE_COIL);
        RogueliteCarUpgrades fullChain = configured(
                RogueliteCardId.TECHNIQUE_SINGULARITY,
                RogueliteCardId.POWERUP_NEXUS,
                RogueliteCardId.OVERDRIVE_COIL);

        assertEquals(7, directAmplifier.getMirrorTotalVehicleCount(
                RogueliteCardId.OVERDRIVE_COIL));
        assertEquals(13, fullChain.getMirrorTotalVehicleCount(
                RogueliteCardId.OVERDRIVE_COIL));
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
    public void collisionFieldTiersEnlargeOnlyWhileActiveAndIgnoreRecoil() {
        assertCollisionFieldPowerup(RogueliteCardId.BULK_FIELD, (float) Math.sqrt(2f));
        assertCollisionFieldPowerup(RogueliteCardId.TITAN_FIELD, (float) Math.sqrt(3f));
        assertCollisionFieldPowerup(RogueliteCardId.COLOSSUS_FIELD, 2f);
    }

    @Test
    public void collisionFieldRequiresNearbyOpponentButDoesNotRequireStraightRoad() {
        RogueliteCarUpgrades collisionField = configured(RogueliteCardId.BULK_FIELD);
        RogueliteCarUpgrades mirror = configured(RogueliteCardId.MIRROR_DUO);

        for (int i = 0; i < 100; i++) {
            update(collisionField, 0.1f, 1f, true, 0f, 0.5f, 0f, 0.5f, 0.1f, 0.8f, 0f, 0f);
        }
        assertTrue(collisionField.isPowerupReady());
        assertNull(collisionField.getActivePowerupCardId());

        update(collisionField, 0.1f, 1f, true, 0f, 0.5f, 0f, 0.5f, 0.1f, 0.8f, 0f, 0.5f);
        assertEquals(RogueliteCardId.BULK_FIELD, collisionField.getActivePowerupCardId());

        for (int i = 0; i < 100; i++) {
            update(mirror, 0.1f, 1f, true, 0f, 0.5f, 0f, 0.5f, 0.1f, 0.8f, 0f, 0.5f);
        }
        assertTrue(mirror.isPowerupReady());
        assertNull(mirror.getActivePowerupCardId());
    }

    @Test
    public void collisionFieldCooldownsMatchTheirConfiguredCadence() {
        assertEquals(20f,
                CollisionFieldPowerupSpec.cooldownSeconds(RogueliteCardId.BULK_FIELD),
                EPSILON);
        assertEquals(15f,
                CollisionFieldPowerupSpec.cooldownSeconds(RogueliteCardId.TITAN_FIELD),
                EPSILON);
        assertEquals(10f,
                CollisionFieldPowerupSpec.cooldownSeconds(RogueliteCardId.COLOSSUS_FIELD),
                EPSILON);

        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.COLOSSUS_FIELD);
        activateNearbyStraightPowerup(upgrades);
        boolean expired = false;
        boolean reactivated = false;
        int inactiveUpdates = 0;
        for (int i = 0; i < 120; i++) {
            updateStraightPowerupAtSpeed(upgrades, 0.72f, false, 0.5f);
            if (upgrades.getActivePowerupCardId() == null) {
                expired = true;
                inactiveUpdates++;
            } else if (expired) {
                reactivated = true;
                break;
            }
        }

        assertTrue(expired);
        assertTrue(reactivated);
        assertEquals(1, inactiveUpdates);
        assertEquals(10f,
                upgrades.getCooldownTimeRemainingSeconds(RogueliteCardId.COLOSSUS_FIELD),
                EPSILON);
    }

    @Test
    public void activeCollisionFieldBlocksSelectedRevengeCardsInEveryTier() {
        RogueliteCardId[] collisionFields = {
            RogueliteCardId.BULK_FIELD,
            RogueliteCardId.TITAN_FIELD,
            RogueliteCardId.COLOSSUS_FIELD
        };
        RogueliteCardId[] blockedRevengeCards = {
            RogueliteCardId.DRAFT_MAGNET,
            RogueliteCardId.RECOVERY_BEACON,
            RogueliteCardId.DRAFT_VENDETTA,
            RogueliteCardId.PAYBACK_SHIELD,
            RogueliteCardId.REPULSOR_WAVE,
            RogueliteCardId.REPULSOR_SURGE,
            RogueliteCardId.HUNTER_BARRAGE,
            RogueliteCardId.HUNTER_STORM,
            RogueliteCardId.TAR_TETHER,
            RogueliteCardId.EMP_SNARE,
            RogueliteCardId.VOID_ANCHOR,
            RogueliteCardId.SENSOR_JAMMER,
            RogueliteCardId.GRID_BLACKOUT,
            RogueliteCardId.TOTAL_BLACKOUT,
            RogueliteCardId.TRIAD_COUP,
            RogueliteCardId.CROWN_ENGINE
        };
        RogueliteCardId[] allowedRevengeCards = {
            RogueliteCardId.TELEMETRY_THEFT,
            RogueliteCardId.BUILD_HEIST,
            RogueliteCardId.APEX_PLUNDER,
            RogueliteCardId.FINAL_RECKONING
        };

        for (int i = 0; i < collisionFields.length; i++) {
            RogueliteCarUpgrades upgrades = configured(collisionFields[i]);
            assertTrue(
                    RogueliteCardCatalog.get(collisionFields[i])
                            .getEffectText()
                            .contains("Control-Revenge immune"));
            assertFalse(upgrades.isCollisionFieldActive());
            assertFalse(upgrades.blocksOpponentAwareness());
            activateNearbyStraightPowerup(upgrades);
            assertTrue(upgrades.isCollisionFieldActive());
            assertTrue(upgrades.blocksOpponentAwareness());
            for (int j = 0; j < blockedRevengeCards.length; j++) {
                assertTrue(upgrades.blocksRevengeCard(blockedRevengeCards[j]));
            }
            for (int j = 0; j < allowedRevengeCards.length; j++) {
                assertFalse(upgrades.blocksRevengeCard(allowedRevengeCards[j]));
            }
            assertFalse(upgrades.blocksRevengeCard(null));
        }
    }

    @Test
    public void collisionFieldDefersAnArmedControlRevengeWithoutConsumingIt() {
        RogueliteCarUpgrades tar = configured(RogueliteCardId.TAR_TETHER);
        RogueliteCarUpgrades collisionField = configured(RogueliteCardId.BULK_FIELD);
        tar.onHitBy(42, 12f);
        update(tar, 2.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        activateNearbyStraightPowerup(collisionField);

        assertTrue(tar.isRevengeReady());
        assertTrue(tar.isRevengeStrikeBlockedBy(collisionField));
        assertTrue(tar.isRevengeArmed());
        assertEquals(42, tar.getRevengeTargetVehicleId());

        for (int i = 0; i < 101; i++) {
            updateStraightPowerupAtSpeed(collisionField, 0.72f, false, 0f);
        }

        assertFalse(collisionField.isCollisionFieldActive());
        assertFalse(tar.isRevengeStrikeBlockedBy(collisionField));
        RogueliteRevengeStrike strike = tar.tryActivateOffenderStrike(42, 100f);
        assertNotNull(strike);
        assertEquals(RogueliteCardId.TAR_TETHER, strike.getCardId());
    }

    @Test
    public void powerupAmplifierMultipliesCollisionFieldAndMassBonus() {
        RogueliteCarUpgrades upgrades = configured(
                RogueliteCardId.POWERUP_NEXUS,
                RogueliteCardId.COLOSSUS_FIELD);

        activateNearbyStraightPowerup(upgrades);

        assertEquals((float) Math.sqrt(8f), upgrades.getCarCollisionScale(), EPSILON);
        assertEquals(1.4f, upgrades.getMassMultiplier(), EPSILON);
        assertEquals(1.1f, upgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(24f, upgrades.getCarCollisionMassMultiplier(), EPSILON);
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
        long initialActivationSequence = upgrades.getRevengeActivationSequence();

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
        assertEquals(
                initialActivationSequence + 1L,
                upgrades.getRevengeActivationSequence());
        assertEquals(
                2f,
                upgrades.getActiveTimeRemainingSeconds(RogueliteCardId.DRAFT_MAGNET),
                EPSILON);
        assertEquals(
                0f,
                upgrades.getCooldownTimeRemainingSeconds(RogueliteCardId.DRAFT_MAGNET),
                EPSILON);

        update(upgrades, 0.1f, 0f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0.80f);
        assertTrue(upgrades.isDraftMagnetActive());
        assertEquals(
                initialActivationSequence + 1L,
                upgrades.getRevengeActivationSequence());

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
    public void radialRevengeUsesPhysicalProximityWhenOpponentAwarenessIsBlind() {
        RogueliteCarUpgrades upgrades = configured(
                RogueliteCardId.DRAFT_MAGNET,
                RogueliteCardId.MIRROR_DUO);
        upgrades.onHitBy(42, 12f);

        for (int i = 0; i < 2; i++) {
            upgrades.update(
                    0.1f,
                    1f,
                    true,
                    false,
                    false,
                    0f,
                    0.65f,
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
                    0f,
                    0.8f);
        }

        assertEquals(RogueliteCardId.DRAFT_MAGNET, upgrades.getActiveAbilityCardId());
        assertTrue(upgrades.isDraftMagnetActive());
        assertNull(upgrades.getActivePowerupCardId());
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
        assertFalse(upgrades.isRevengeReady());
        assertTrue(upgrades.isRevengeArmed());
        assertEquals(0f, upgrades.getRevengeReadiness(), EPSILON);
        assertEquals(42, upgrades.getRevengeTargetVehicleId());
        assertEquals(
                30f,
                upgrades.getRevengeActiveTimeRemainingSeconds(),
                EPSILON);
        assertTrue(upgrades.getAccelerationMultiplier() > 1.50f);
        assertTrue(upgrades.getMaxSpeedMultiplier() > 1.15f);
        assertEquals(1f, upgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(1f, upgrades.getSteeringMultiplier(0f), EPSILON);
        assertNull(upgrades.tryActivateOffenderHit(42));
        assertNull(upgrades.tryActivateOffenderStrike(
                42,
                CrownBreakerRevengeEffect.RAM_TRIGGER_DISTANCE));

        update(upgrades, 2.9f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertFalse(upgrades.isRevengeReady());
        assertNull(upgrades.tryActivateOffenderHit(42));

        update(upgrades, 0.2f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertTrue(upgrades.isRevengeReady());
        assertEquals(1f, upgrades.getRevengeReadiness(), EPSILON);

        upgrades.onHitBy(7, 12f);
        assertEquals(7, upgrades.getRevengeTargetVehicleId());
        assertEquals(30f, upgrades.getRevengeActiveTimeRemainingSeconds(), EPSILON);
        assertFalse(upgrades.isRevengeReady());

        update(upgrades, CrownBreakerRevengeEffect.PREPARATION_SECONDS,
                1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertTrue(upgrades.isRevengeArmed());
        assertTrue(upgrades.isRevengeReady());
        assertNull(upgrades.tryActivateOffenderStrike(
                7,
                CrownBreakerRevengeEffect.RAM_TRIGGER_DISTANCE + 0.01f));
        assertNull(upgrades.tryActivateOffenderStrike(42, 1f));

        RogueliteRevengeStrike strike = upgrades.tryActivateOffenderStrike(
                7,
                CrownBreakerRevengeEffect.RAM_TRIGGER_DISTANCE);
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
                upgrades.getRevengeActiveTimeRemainingSeconds(),
                EPSILON);

        upgrades.onHitBy(7, 12f);
        upgrades.onHitBy(7, 12f);
        assertFalse(upgrades.isRevengeArmed());
        assertEquals(-1, upgrades.getRevengeTargetVehicleId());

        upgrades.onContactEnded(7);
        upgrades.onHitBy(42, 12f);
        assertTrue(upgrades.isRevengeArmed());
        assertEquals(42, upgrades.getRevengeTargetVehicleId());
    }

    @Test
    public void crownBreakerBuffAndTargetExpireAfterThirtySeconds() {
        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.CROWN_ENGINE);
        upgrades.onHitBy(42, 12f);

        update(upgrades, 29.9f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertTrue(upgrades.isRevengeArmed());
        assertTrue(upgrades.getAccelerationMultiplier() > 1f);
        assertTrue(upgrades.getMaxSpeedMultiplier() > 1f);

        update(upgrades, 0.2f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertFalse(upgrades.isRevengeArmed());
        assertFalse(upgrades.isRevengeReady());
        assertEquals(-1, upgrades.getRevengeTargetVehicleId());
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getMaxSpeedMultiplier(), EPSILON);
        assertNull(upgrades.tryActivateOffenderHit(42));
    }

    @Test
    public void activeTechniqueCanAmplifyCrownBreakerPowerWithoutAnUpperCap() {
        RogueliteCarUpgrades upgrades = configured(
                RogueliteCardId.TRACTION_MASTER,
                RogueliteCardId.CROWN_ENGINE);

        upgrades.onHitBy(42, 12f);
        update(upgrades, 0.1f, 1f, true, 0f, 0.65f, 0f, 0.2f, 0.2f, 0.2f, 0f, 0f);

        assertEquals(2.65f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertTrue(upgrades.getMaxSpeedMultiplier() > 1.35f);
    }

    @Test
    public void targetedRevengeCardsEmitDistinctActionsAgainstTheRecordedOffender() {
        RogueliteCarUpgrades positionSwap = configured(RogueliteCardId.RECOVERY_BEACON);
        long positionSwapActivationSequence =
                positionSwap.getRevengeActivationSequence();
        armRevenge(positionSwap);
        positionSwap.onHitBy(7, 12f);
        assertEquals(7, positionSwap.getRevengeTargetVehicleId());
        assertFalse(positionSwap.isRevengeReady());
        update(positionSwap, 1.9f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertNull(positionSwap.tryActivateOffenderStrike(7, 4f, true));
        update(positionSwap, 0.2f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertTrue(positionSwap.isRevengeReady());
        assertNull(positionSwap.tryActivateOffenderStrike(7, 2f, true));
        RogueliteRevengeStrike swapStrike =
                positionSwap.tryActivateOffenderStrike(7, 4f, true);
        assertNotNull(swapStrike);
        assertEquals(RogueliteRevengeStrike.Action.POSITION_SWAP, swapStrike.getAction());
        assertEquals(
                positionSwapActivationSequence + 1L,
                positionSwap.getRevengeActivationSequence());

        RogueliteCarUpgrades redline = configured(RogueliteCardId.DRAFT_VENDETTA);
        long redlineActivationSequence = redline.getRevengeActivationSequence();
        armRevenge(redline);
        assertNull(redline.tryActivateOffenderStrike(7, 4f));
        RogueliteRevengeStrike throttleStrike =
                redline.tryActivateOffenderStrike(42, 1f);
        assertNotNull(throttleStrike);
        assertEquals(RogueliteRevengeStrike.Action.FORCE_THROTTLE, throttleStrike.getAction());
        assertEquals(5f, throttleStrike.getDurationSeconds(), EPSILON);
        assertEquals(
                redlineActivationSequence + 1L,
                redline.getRevengeActivationSequence());

        RogueliteCarUpgrades hook = configured(RogueliteCardId.PAYBACK_SHIELD);
        armRevenge(hook);
        assertEquals(1, RogueliteCardCatalog.get(RogueliteCardId.PAYBACK_SHIELD).getTier());
        assertFalse(hook.isRevengeReady());
        update(hook, 1.9f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertNull(hook.tryActivateOffenderStrike(42, 6f));
        update(hook, 0.2f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertTrue(hook.isRevengeReady());
        assertTrue(hook.allowsOffRoadOffenderStrike());
        long hookActivationSequence = hook.getRevengeActivationSequence();
        assertNull(hook.tryActivateOffenderStrike(42, 30f, false));
        RogueliteRevengeStrike hookStrike =
                hook.tryActivateOffenderStrike(42, 30f, true);
        assertNotNull(hookStrike);
        assertEquals(RogueliteRevengeStrike.Action.HOOK, hookStrike.getAction());
        assertEquals(0f, hookStrike.getDurationSeconds(), EPSILON);
        assertEquals(0f, hookStrike.getAttackerLaunchSpeedRatio(), EPSILON);
        assertEquals(RogueliteCardId.PAYBACK_SHIELD, hook.getActiveAbilityCardId());
        assertEquals(
                hookActivationSequence + 1L,
                hook.getRevengeActivationSequence());
        update(hook, 20f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertEquals(RogueliteCardId.PAYBACK_SHIELD, hook.getActiveAbilityCardId());
        assertEquals(
                hookActivationSequence + 1L,
                hook.getRevengeActivationSequence());
        hook.completeOffenderStrike(RogueliteCardId.PAYBACK_SHIELD);
        assertNull(hook.getActiveAbilityCardId());

        RogueliteCarUpgrades repulsor = configured(RogueliteCardId.REPULSOR_SURGE);
        armRevenge(repulsor);
        update(repulsor, 0.1f, 0f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0.20f);
        assertEquals(RogueliteCardId.REPULSOR_SURGE, repulsor.getActiveAbilityCardId());
        assertTrue(repulsor.isDraftMagnetActive());
        assertEquals(
                2f,
                repulsor.getActiveTimeRemainingSeconds(RogueliteCardId.REPULSOR_SURGE),
                EPSILON);

        RogueliteCarUpgrades repulsorWave = configured(RogueliteCardId.REPULSOR_WAVE);
        armRevenge(repulsorWave);
        update(repulsorWave, 0.1f, 0f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0.20f);
        assertEquals(RogueliteCardId.REPULSOR_WAVE, repulsorWave.getActiveAbilityCardId());
        assertTrue(repulsorWave.isDraftMagnetActive());
        assertEquals(1.375f, repulsorWave.getDraftMagnetRangeMultiplier(), EPSILON);
        assertEquals(1.275f, repulsorWave.getDraftMagnetForceMultiplier(), EPSILON);

        RogueliteCarUpgrades triad = configured(RogueliteCardId.TRIAD_COUP);
        triad.onHitBy(42, 12f);
        triad.setRevengeSecondaryTargetVehicleId(7);
        assertEquals(42, triad.getRevengeTargetVehicleId());
        assertEquals(7, triad.getRevengeSecondaryTargetVehicleId());
        update(triad, 1.9f, 1f, false, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertNull(triad.tryActivateOffenderStrike(42, 100f, false));
        update(triad, 0.2f, 1f, false, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
        RogueliteRevengeStrike triadStrike =
                triad.tryActivateOffenderStrike(42, 100f, false);
        assertNotNull(triadStrike);
        assertEquals(RogueliteRevengeStrike.Action.POSITION_REORDER, triadStrike.getAction());
        assertEquals(7, triadStrike.getSecondaryTargetVehicleId());
        assertFalse(triad.isRevengeArmed());
        assertEquals(1.75f, repulsor.getDraftMagnetRangeMultiplier(), EPSILON);
        assertEquals(1.55f, repulsor.getDraftMagnetForceMultiplier(), EPSILON);
    }

    @Test
    public void finalReckoningImmediatelyArmsPersistentFieldHunt() {
        RogueliteCarUpgrades upgrades = configured(RogueliteCardId.FINAL_RECKONING);

        upgrades.onHitBy(42, 12f);

        assertFalse(upgrades.isCardEffectActive(RogueliteCardId.OVERDRIVE_COIL));
        assertFalse(upgrades.isCardEffectActive(RogueliteCardId.COLOSSUS_FIELD));
        assertNull(upgrades.getActivePowerupCardId());
        assertEquals(RogueliteCardId.FINAL_RECKONING, upgrades.getActiveAbilityCardId());
        assertTrue(upgrades.isFinalReckoningHuntActive());
        assertEquals(1.50f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(0f, upgrades.getFrontCollisionRecoilMultiplier(), EPSILON);
        assertEquals(2.50f, upgrades.getFrontCollisionPushMultiplier(), EPSILON);
        assertNotNull(upgrades.tryActivateFinalReckoningRam(3, 42, 4f));
        assertNull(upgrades.tryActivateFinalReckoningRam(3, 42, 4f));
        assertTrue(upgrades.isFinalReckoningHuntActive());
        assertEquals(42, upgrades.getRevengeTargetVehicleId());
    }

    @Test
    public void ironGiantSetReplacesItsOldStatBonusWithDebuffImmunity() {
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");

        upgrades.configure(
                loadout,
                0f,
                RogueliteSetCatalog.get(RogueliteSetId.IRON_GIANT));

        assertTrue(upgrades.blocksDebuffs());
        assertFalse(upgrades.blocksHostileEffects());
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);

        upgrades.configure(loadout, 0f, null);

        assertFalse(upgrades.blocksDebuffs());
    }

    @Test
    public void finalReckoningUsesTheFullRevengeAmplifierChain() {
        RogueliteCarUpgrades upgrades = configured(
                RogueliteCardId.TECHNIQUE_SINGULARITY,
                RogueliteCardId.POWERUP_NEXUS,
                RogueliteCardId.NEMESIS_ENGINE,
                RogueliteCardId.FINAL_RECKONING);

        upgrades.onHitBy(42, 12f);

        assertEquals(8f, upgrades.getRevengeEffectMultiplier(), EPSILON);
        assertEquals(120f, upgrades.getRevengeActiveTimeRemainingSeconds(), EPSILON);
        assertTrue(upgrades.isFinalReckoningHuntActive());
        assertEquals(5f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(0f, upgrades.getFrontCollisionRecoilMultiplier(), EPSILON);
        assertEquals(13f, upgrades.getFrontCollisionPushMultiplier(), EPSILON);
        RogueliteRevengeStrike strike =
                upgrades.tryActivateFinalReckoningRam(3, 42, 4f);
        assertNotNull(strike);
        assertEquals(3.84f, strike.getAttackerLaunchSpeedRatio(), EPSILON);
        assertEquals(5.76f, strike.getTargetPushSpeedRatio(), EPSILON);
    }

    @Test
    public void revengeAmplifierPowerupsExposeTheirTierMultipliers() {
        assertEquals(
                1.25f,
                configured(RogueliteCardId.GRUDGE_SPARK)
                        .getRevengeEffectMultiplier(),
                EPSILON);
        assertEquals(
                1.50f,
                configured(RogueliteCardId.VENGEANCE_CORE)
                        .getRevengeEffectMultiplier(),
                EPSILON);
        assertEquals(
                2f,
                configured(RogueliteCardId.NEMESIS_ENGINE)
                        .getRevengeEffectMultiplier(),
                EPSILON);
    }

    @Test
    public void revengeAmplifierDoesNotExtendDelayedRevengePreparation() {
        RogueliteCardId[] delayedCards = {
                RogueliteCardId.RECOVERY_BEACON,
                RogueliteCardId.PAYBACK_SHIELD,
                RogueliteCardId.TRIAD_COUP,
                RogueliteCardId.CROWN_ENGINE
        };
        for (int i = 0; i < delayedCards.length; i++) {
            RogueliteCarUpgrades upgrades = configured(
                    RogueliteCardId.NEMESIS_ENGINE,
                    delayedCards[i]);
            upgrades.onHitBy(42, 12f);
            upgrades.setRevengeSecondaryTargetVehicleId(7);

            float triggerDelay = delayedCards[i] == RogueliteCardId.CROWN_ENGINE
                    ? 3f
                    : 2f;
            update(upgrades, triggerDelay - 0.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
            float distance = delayedCards[i] == RogueliteCardId.CROWN_ENGINE
                    ? CrownBreakerRevengeEffect.RAM_TRIGGER_DISTANCE
                    : 100f;
            assertNull(upgrades.tryActivateOffenderStrike(42, distance, true));

            update(upgrades, 0.2f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
            assertNotNull(upgrades.tryActivateOffenderStrike(42, distance, true));
        }
    }

    @Test
    public void revengeAmplifierScalesTimedStrikesAndShowsItsPowerup() {
        RogueliteCarUpgrades upgrades = configured(
                RogueliteCardId.GRUDGE_SPARK,
                RogueliteCardId.DRAFT_VENDETTA);
        upgrades.onHitBy(42, 12f);

        RogueliteRevengeStrike strike =
                upgrades.tryActivateOffenderStrike(42, 1f);

        assertNotNull(strike);
        assertEquals(6.25f, strike.getDurationSeconds(), EPSILON);
        assertEquals(1.25f, strike.getEffectMultiplier(), EPSILON);
        assertEquals(
                RogueliteCardId.GRUDGE_SPARK,
                upgrades.getActivePowerupCardId());
        assertEquals(
                RogueliteCardId.DRAFT_VENDETTA,
                upgrades.getActiveCardId(RogueliteSlotType.REVENGE));
    }

    @Test
    public void revengeAmplifierScalesOutwardFieldDurationRangeAndForce() {
        RogueliteCarUpgrades upgrades = configured(
                RogueliteCardId.NEMESIS_ENGINE,
                RogueliteCardId.REPULSOR_WAVE);
        upgrades.onHitBy(42, 12f);

        update(upgrades, 0.1f, 0f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0.80f);

        assertEquals(
                4f,
                upgrades.getActiveTimeRemainingSeconds(RogueliteCardId.REPULSOR_WAVE),
                EPSILON);
        assertEquals(1.75f, upgrades.getDraftMagnetRangeMultiplier(), EPSILON);
        assertEquals(1.55f, upgrades.getDraftMagnetForceMultiplier(), EPSILON);
        assertEquals(
                RogueliteCardId.NEMESIS_ENGINE,
                upgrades.getActivePowerupCardId());
    }

    @Test
    public void revengeAmplifierAddsHunterBarrageShotsAndDuration() {
        RogueliteCarUpgrades upgrades = configured(
                RogueliteCardId.NEMESIS_ENGINE,
                RogueliteCardId.HUNTER_BARRAGE);
        upgrades.onHitBy(42, 12f);

        int shots = 0;
        for (int step = 0; step < 12; step++) {
            update(upgrades, 0.5f, 0.5f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);
            RogueliteRevengeStrike strike =
                    upgrades.tryActivateOffenderStrike(42, 1000f, false);
            if (strike != null) {
                shots++;
                assertEquals(2f, strike.getEffectMultiplier(), EPSILON);
            }
        }

        assertEquals(12, shots);
        assertFalse(upgrades.isRevengeArmed());
    }

    @Test
    public void positionHijackExpiresWithoutTriggeringWhenOffenderIsNotAheadAfterCharge() {
        RogueliteCarUpgrades positionSwap = configured(RogueliteCardId.RECOVERY_BEACON);
        long activationSequence = positionSwap.getRevengeActivationSequence();
        positionSwap.onHitBy(42, 12f);
        update(positionSwap, 2.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);

        assertTrue(positionSwap.expireOffenderStrikeIfConditionFailed(42, false));
        assertFalse(positionSwap.isRevengeArmed());
        assertFalse(positionSwap.isRevengeReady());
        assertEquals(-1, positionSwap.getRevengeTargetVehicleId());
        assertEquals(activationSequence, positionSwap.getRevengeActivationSequence());
        assertNull(positionSwap.getActiveAbilityCardId());
        assertFalse(positionSwap.expireOffenderStrikeIfConditionFailed(42, false));
    }

    @Test
    public void vendettaHookExpiresWithoutTriggeringWhenOffenderIsNotAheadAfterCharge() {
        RogueliteCarUpgrades hook = configured(RogueliteCardId.PAYBACK_SHIELD);
        long activationSequence = hook.getRevengeActivationSequence();
        hook.onHitBy(42, 12f);
        update(hook, 2.1f, 1f, true, 0f, 0.65f, 0f, 0f, 1f, 0f, 0f, 0f);

        assertNull(hook.tryActivateOffenderStrike(42, 30f, false));
        assertTrue(hook.expireOffenderStrikeIfConditionFailed(42, false));
        assertFalse(hook.isRevengeArmed());
        assertFalse(hook.isRevengeReady());
        assertEquals(-1, hook.getRevengeTargetVehicleId());
        assertEquals(activationSequence, hook.getRevengeActivationSequence());
        assertNull(hook.getActiveAbilityCardId());
        assertFalse(hook.expireOffenderStrikeIfConditionFailed(42, false));
    }

    @Test
    public void waitingTargetedRevengeRestartsAgainstTheLatestOffender() {
        RogueliteCardId[] targetedCards = {
                RogueliteCardId.RECOVERY_BEACON,
                RogueliteCardId.DRAFT_VENDETTA,
                RogueliteCardId.PAYBACK_SHIELD,
                RogueliteCardId.HUNTER_BARRAGE,
                RogueliteCardId.HUNTER_STORM,
                RogueliteCardId.TAR_TETHER,
                RogueliteCardId.EMP_SNARE,
                RogueliteCardId.VOID_ANCHOR,
                RogueliteCardId.TRIAD_COUP
        };
        for (int i = 0; i < targetedCards.length; i++) {
            RogueliteCarUpgrades upgrades = configured(targetedCards[i]);

            upgrades.onHitBy(42, 12f);
            upgrades.onHitBy(7, 18f);

            assertTrue(upgrades.isRevengeArmed());
            assertEquals(7, upgrades.getRevengeTargetVehicleId());
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
    public void offenderCurseTargetsOnlyTheCarThatLandedTheHit() {
        assertOffenderCurse(RogueliteCardId.SENSOR_JAMMER, 1.05f, 0.95f, 20f);
        assertOffenderCurse(RogueliteCardId.GRID_BLACKOUT, 1.10f, 0.90f, 30f);
        assertOffenderCurse(RogueliteCardId.TOTAL_BLACKOUT, 1.20f, 0.80f, 40f);
    }

    @Test
    public void tierThreeHexRemainsBoundedWithTierThreeRevengeAmplifier() {
        RogueliteCarUpgrades upgrades = configured(
                RogueliteCardId.NEMESIS_ENGINE,
                RogueliteCardId.TOTAL_BLACKOUT);
        upgrades.onHitBy(42, 12f);

        RogueliteRevengeStrike strike =
                upgrades.tryActivateOffenderStrike(42, 100f);

        assertNotNull(strike);
        assertEquals(1.40f, strike.getMassMultiplier(), EPSILON);
        assertEquals(0.60f, strike.getGripMultiplier(), EPSILON);
        assertEquals(80f, strike.getDurationSeconds(), EPSILON);
        assertEquals(2f, strike.getEffectMultiplier(), EPSILON);
    }

    @Test
    public void everyCrossSlotCardPairRemainsStableThroughASimulatedRace() {
        List<RogueliteCardDefinition> tuning = cardsForSlot(RogueliteSlotType.TUNING);
        List<RogueliteCardDefinition> techniques = cardsForSlot(RogueliteSlotType.TECHNIQUE);
        List<RogueliteCardDefinition> powerups = cardsForSlot(RogueliteSlotType.POWERUP);
        List<RogueliteCardDefinition> revenge = cardsForSlot(RogueliteSlotType.REVENGE);

        List<List<RogueliteCardDefinition>> slots =
                new ArrayList<List<RogueliteCardDefinition>>();
        slots.add(tuning);
        slots.add(techniques);
        slots.add(powerups);
        slots.add(revenge);

        // Cover every cross-slot pair while rotating the remaining two cards.
        int scenario = 0;
        for (int firstSlot = 0; firstSlot < slots.size(); firstSlot++) {
            for (int secondSlot = firstSlot + 1; secondSlot < slots.size(); secondSlot++) {
                List<RogueliteCardDefinition> firstCards = slots.get(firstSlot);
                List<RogueliteCardDefinition> secondCards = slots.get(secondSlot);
                for (int firstCard = 0; firstCard < firstCards.size(); firstCard++) {
                    for (int secondCard = 0; secondCard < secondCards.size(); secondCard++) {
                        int[] selected = new int[slots.size()];
                        for (int slot = 0; slot < slots.size(); slot++) {
                            selected[slot] =
                                    (scenario * 17 + firstCard * 7 + secondCard * 13 + slot)
                                            % slots.get(slot).size();
                        }
                        selected[firstSlot] = firstCard;
                        selected[secondSlot] = secondCard;

                        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
                        upgrades.configure(
                                loadout(
                                        slots.get(0).get(selected[0]).getId(),
                                        slots.get(1).get(selected[1]).getId(),
                                        slots.get(2).get(selected[2]).getId(),
                                        slots.get(3).get(selected[3]).getId()),
                                (scenario % 997) / 996f);
                        simulateRace(upgrades);
                        scenario++;
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

            assertFiniteAtLeast(upgrades.getAccelerationMultiplier(), 0.10f);
            assertFiniteAtLeast(upgrades.getMassMultiplier(), 0.10f);
            assertFiniteAtLeast(upgrades.getMaxSpeedMultiplier(), 0.65f);
            assertMultiplier(upgrades.getDragMultiplier(), 0.10f, 4f);
            assertFiniteAtLeast(upgrades.getGripMultiplier(slip), 0.10f);
            assertFiniteAtLeast(upgrades.getSteeringMultiplier(slip), 0.70f);
            assertMultiplier(upgrades.getSlipstreamRangeMultiplier(), 1f, 2f);
            assertMultiplier(upgrades.getSlipstreamStrengthMultiplier(), 1f, 2f);
            assertMultiplier(upgrades.getFrontCollisionRecoilMultiplier(), 0f, 1f);
            assertMultiplier(upgrades.getFrontCollisionPushMultiplier(), 1f, 7f);
            assertMultiplier(upgrades.adjustSurfaceGrip(0.58f), 0f, 1f);
            assertMultiplier(upgrades.getPowerupReadiness(), 0f, 1f);
            assertMultiplier(upgrades.getRevengeReadiness(), 0f, 1f);
        }
    }

    private static boolean isTechniqueAmplifierTuning(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.TECHNIQUE_COUPLER
                || cardId == RogueliteCardId.TECHNIQUE_MATRIX
                || cardId == RogueliteCardId.TECHNIQUE_SINGULARITY;
    }

    private static boolean isPowerupAmplifierTechnique(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.POWERUP_LINK
                || cardId == RogueliteCardId.POWERUP_MATRIX
                || cardId == RogueliteCardId.POWERUP_NEXUS;
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
        assertEquals(
                derivedTopSpeed(1f + midpointBonus, 1f + midpointBonus),
                upgrades.getMaxSpeedMultiplier(),
                EPSILON);
        assertEquals(1f / (1f + midpointBonus), upgrades.getDragMultiplier(), EPSILON);
        assertEquals(1f + midpointBonus, upgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(1f - midpointBonus, upgrades.getMassMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getSteeringMultiplier(0f), EPSILON);
        assertTrue(upgrades.getActiveCardIds().contains(cardId));

        updateRacePosition(upgrades, 1f);
        assertEquals(1f + maximumBonus, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(
                derivedTopSpeed(1f + maximumBonus, 1f + maximumBonus),
                upgrades.getMaxSpeedMultiplier(),
                EPSILON);
        assertEquals(1f / (1f + maximumBonus), upgrades.getDragMultiplier(), EPSILON);
        assertEquals(1f + maximumBonus, upgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(1f - maximumBonus, upgrades.getMassMultiplier(), EPSILON);
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
        assertEquals(
                derivedTopSpeed(1f + bonus, 1f + bonus),
                upgrades.getMaxSpeedMultiplier(),
                EPSILON);
        assertEquals(1f / (1f + bonus), upgrades.getDragMultiplier(), EPSILON);
        assertEquals(1f + bonus, upgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(1f - bonus, upgrades.getMassMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getSteeringMultiplier(0f), EPSILON);
        assertTrue(upgrades.getActiveCardIds().contains(cardId));

        update(upgrades, 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getDragMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(1f, upgrades.getMassMultiplier(), EPSILON);
        assertFalse(upgrades.getActiveCardIds().contains(cardId));
    }

    private static RogueliteCarUpgrades configured(RogueliteCardId cardId) {
        return configured(new RogueliteCardId[] {cardId});
    }

    private static String signedStatSignature(RogueliteCardDefinition card) {
        String[] lines = card.getEffectText().split("\\n");
        List<String> signature = new ArrayList<String>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("Activation: ")) {
                continue;
            }
            int separator = line.indexOf(' ');
            assertTrue(card.getId().name() + ": " + line, separator > 0);
            String operation;
            if (line.indexOf(" x", separator) >= 0) {
                operation = "x";
            } else if (line.indexOf('-', separator) >= 0) {
                operation = "-";
            } else {
                operation = "+";
            }
            signature.add(line.substring(0, separator) + operation);
        }
        Collections.sort(signature);
        return signature.toString();
    }

    private static String techniqueActivationSignature(RogueliteCardDefinition card) {
        String activation = card.getEffectText().split("\\n")[0];
        int durationSeparator = activation.indexOf('|');
        return durationSeparator < 0
                ? activation.trim()
                : activation.substring(0, durationSeparator).trim();
    }

    private static float derivedTopSpeed(float power, float aero) {
        return (float) Math.cbrt(power * aero);
    }

    private static void assertOffenderCurse(
            RogueliteCardId cardId,
            float massMultiplier,
            float gripMultiplier,
            float durationSeconds) {
        RogueliteCarUpgrades upgrades = configured(cardId);
        upgrades.onHitBy(42, 12f);

        assertTrue(upgrades.isRevengeArmed());
        assertEquals(42, upgrades.getRevengeTargetVehicleId());
        assertNull(upgrades.tryActivateOffenderStrike(7, 1f));
        RogueliteRevengeStrike strike = upgrades.tryActivateOffenderStrike(42, 100f);
        assertNotNull(strike);
        assertEquals(RogueliteRevengeStrike.Action.CURSE, strike.getAction());
        assertEquals(cardId, strike.getCardId());
        assertEquals(massMultiplier, strike.getMassMultiplier(), EPSILON);
        assertEquals(gripMultiplier, strike.getGripMultiplier(), EPSILON);
        assertEquals(durationSeconds, strike.getDurationSeconds(), EPSILON);
        assertFalse(upgrades.isRevengeArmed());
        assertEquals(-1, upgrades.getRevengeTargetVehicleId());
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

    private static RogueliteCarUpgrades activateAutomaticPowerup(RogueliteCardId cardId) {
        RogueliteCarUpgrades upgrades = configured(cardId);
        for (int i = 0; i < 100 && !upgrades.isBestDriverActive(); i++) {
            update(upgrades, 0.1f, 1f, true, 0f, 0.5f, 0f, 0f, 1f, 0f, 0f, 0f);
        }
        assertTrue(upgrades.isBestDriverActive());
        return upgrades;
    }

    private static void assertTechniqueWindow(
            RogueliteCardId cardId,
            float duration,
            RogueliteDrivingFrame activeFrame,
            RogueliteDrivingFrame inactiveFrame) {
        RaceTechniqueEffect effect = new RaceTechniqueEffect(cardId);

        effect.advance(0.1f, 0.1f, activeFrame);
        assertTrue(cardId.name(), effect.isActive());
        assertEquals(cardId.name(), duration, effect.activeTimeRemainingSeconds(), EPSILON);

        effect.advance(0.5f, 0.5f, inactiveFrame);
        assertTrue(cardId.name(), effect.isActive());
        assertEquals(cardId.name(), duration - 0.5f,
                effect.activeTimeRemainingSeconds(), EPSILON);

        effect.advance(0.1f, 0.1f, activeFrame);
        assertEquals(cardId.name(), duration - 0.6f,
                effect.activeTimeRemainingSeconds(), EPSILON);

        effect.advance(duration, duration, activeFrame);
        assertTrue(cardId.name(), effect.isActive());
        assertEquals(cardId.name(), duration, effect.activeTimeRemainingSeconds(), EPSILON);

        effect.advance(duration + 0.1f, duration + 0.1f, inactiveFrame);
        assertFalse(cardId.name(), effect.isActive());
        assertEquals(cardId.name(), 0f, effect.activeTimeRemainingSeconds(), EPSILON);
    }

    private static RogueliteDrivingFrame techniqueFrame(
            boolean onRoad,
            float slip,
            float speedRatio,
            float slipstreamBoost,
            boolean longStraight) {
        RogueliteDrivingFrame frame = new RogueliteDrivingFrame();
        frame.set(
                1f,
                onRoad,
                false,
                false,
                slip,
                speedRatio,
                slipstreamBoost,
                10f,
                100f,
                2f,
                0f,
                1f,
                0f,
                0f,
                0f,
                false,
                0f,
                0f,
                longStraight);
        return frame;
    }

    private static void assertTimeDilationPowerup(
            RogueliteCardId cardId,
            float expectedCooldown) {
        CooldownPowerupEffect effect = new CooldownPowerupEffect(cardId, 0f);
        RogueliteDrivingFrame frame = techniqueFrame(true, 0f, 0.5f, 0f, true);
        for (int step = 0; step < 1000 && !effect.isActive(); step++) {
            effect.advance(0.1f, 0.1f, frame);
        }

        assertTrue(cardId.name(), effect.isActive());
        assertTrue(cardId.name(), effect.acceleratesOwnDecisions());
        assertEquals(
                expectedCooldown,
                effect.cooldownTimeRemainingSeconds(),
                EPSILON);
        assertEquals(
                TimeDilationPowerupSpec.DURATION_SECONDS,
                effect.activeTimeRemainingSeconds(),
                EPSILON);
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

    private static void assertCollisionFieldPowerup(
            RogueliteCardId cardId,
            float collisionScale) {
        RogueliteCarUpgrades upgrades = configured(cardId);
        activateNearbyStraightPowerup(upgrades);

        assertEquals(cardId, upgrades.getActivePowerupCardId());
        assertEquals(collisionScale, upgrades.getCarCollisionScale(), EPSILON);
        assertEquals(1.2f, upgrades.getMassMultiplier(), EPSILON);
        assertEquals(1.05f, upgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(1f, upgrades.getFrontCollisionPushMultiplier(), EPSILON);
        assertEquals(CollisionFieldPowerupSpec.collisionMassMultiplier(cardId),
                upgrades.getCarCollisionMassMultiplier(), EPSILON);
        assertEquals(
                CollisionFieldPowerupSpec.DURATION_SECONDS,
                upgrades.getActiveTimeRemainingSeconds(cardId),
                EPSILON);
        assertEquals(
                CollisionFieldPowerupSpec.cooldownSeconds(cardId),
                upgrades.getCooldownTimeRemainingSeconds(cardId),
                EPSILON);
    }

    private static void activateNearbyStraightPowerup(RogueliteCarUpgrades upgrades) {
        for (int i = 0; i < 100 && upgrades.getActivePowerupCardId() == null; i++) {
            updateStraightPowerupAtSpeed(upgrades, 0.72f, false, 0.5f);
        }
        assertNotNull(upgrades.getActivePowerupCardId());
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
        assertTrue(
                "Multiplier " + value + " is below " + minimum,
                value >= minimum - EPSILON);
        assertTrue(
                "Multiplier " + value + " is above " + maximum,
                value <= maximum + EPSILON);
    }

    private static void assertFiniteAtLeast(float value, float minimum) {
        assertFalse(Float.isNaN(value));
        assertFalse(Float.isInfinite(value));
        assertTrue(
                "Multiplier " + value + " is below " + minimum,
                value >= minimum - EPSILON);
    }

    private static void assertOppositeBias(
            RogueliteCardId firstCard,
            RogueliteCardId secondCard,
            StatBias firstBias,
            StatBias secondBias) {
        RogueliteCarUpgrades first = configured(firstCard);
        RogueliteCarUpgrades second = configured(secondCard);
        assertTrue(statValue(first, firstBias) > statValue(second, firstBias));
        assertTrue(statValue(second, secondBias) > statValue(first, secondBias));
    }

    private static float statValue(RogueliteCarUpgrades upgrades, StatBias bias) {
        switch (bias) {
            case POWER:
                return upgrades.getAccelerationMultiplier();
            case GRIP:
                return upgrades.getGripMultiplier(0f);
            case AERO:
                return 1f / upgrades.getDragMultiplier();
            case MASS:
                return 1f / upgrades.getMassMultiplier();
            default:
                throw new IllegalArgumentException("Unsupported stat bias: " + bias);
        }
    }

    private enum StatBias {
        POWER,
        GRIP,
        AERO,
        MASS
    }

    private static RogueliteLoadout loadout(RogueliteCardId... cards) {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        for (int i = 0; i < cards.length; i++) {
            assertTrue(loadout.equip(cards[i]));
        }
        return loadout;
    }
}
