package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AntennaNetworkBonusesTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void antennaTiersImportOnlyTheirAllowedNumberOfMissingAttributes() {
        RogueliteLoadout tuningReceiver = loadout(
                RogueliteCardId.AERO_TRIM,
                RogueliteCardId.DRIFT_FOCUS,
                RogueliteCardId.TUNE_LINK);
        RogueliteLoadout techniqueReceiver = loadout(
                RogueliteCardId.LIGHT_COMPOUND,
                RogueliteCardId.CORNER_EXPERT,
                RogueliteCardId.TECHNIQUE_LINK);
        AntennaNetworkBonuses network = AntennaNetworkBonuses.builder()
                .include(tuningReceiver)
                .include(techniqueReceiver)
                .build();

        RogueliteCarUpgrades tuningUpgrades = upgrades(tuningReceiver, network);
        RogueliteCarUpgrades techniqueUpgrades = upgrades(techniqueReceiver, network);

        assertEquals(1.12f, tuningUpgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(0.98f, tuningUpgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(1.14f, tuningUpgrades.getAerodynamicEfficiencyMultiplier(1f), EPSILON);
        assertEquals(0.92f, tuningUpgrades.getMassMultiplier(), EPSILON);

        // Tier 2 keeps its own grip and mass, then imports aero and power.
        assertEquals(1.12f, techniqueUpgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.05f, techniqueUpgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(
                1.14f,
                techniqueUpgrades.getAerodynamicEfficiencyMultiplier(1f),
                EPSILON);
        assertEquals(0.92f, techniqueUpgrades.getMassMultiplier(), EPSILON);
    }

    @Test
    public void doesNotImportAStatThatWouldMakeTheReceiverWorse() {
        RogueliteLoadout massSource = loadout(
                RogueliteCardId.SHORT_GEARING,
                RogueliteCardId.DRIFT_FOCUS,
                RogueliteCardId.TUNE_LINK);
        RogueliteLoadout gripSource = loadout(
                RogueliteCardId.CLUB_TUNE,
                RogueliteCardId.CORNER_EXPERT,
                RogueliteCardId.TECHNIQUE_LINK);
        AntennaNetworkBonuses network = AntennaNetworkBonuses.builder()
                .include(massSource)
                .include(gripSource)
                .build();

        assertEquals(0.13f, network.getPowerBonus(), EPSILON);
        assertEquals(0.03f, network.getGripBonus(), EPSILON);
        assertEquals(1.13f, network.getAerodynamicEfficiency(), EPSILON);
        assertEquals(1.05f, network.getMassMultiplier(), EPSILON);

        RogueliteCarUpgrades receiver = upgrades(gripSource, network);
        assertEquals(1.13f, receiver.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.03f, receiver.getGripMultiplier(0f), EPSILON);
        assertEquals(1.13f, receiver.getAerodynamicEfficiencyMultiplier(1f), EPSILON);
        assertEquals(1f, receiver.getMassMultiplier(), EPSILON);
    }

    @Test
    public void sharedTechniqueMultipliersUseRecipientsOwnActivationCondition() {
        RogueliteLoadout driftingSource = loadout(
                RogueliteCardId.CARBON_PROTOTYPE,
                RogueliteCardId.DRIFT_FOCUS,
                RogueliteCardId.TUNE_LINK);
        RogueliteLoadout cornerReceiver = loadout(
                RogueliteCardId.VELOCITY_SHELL,
                RogueliteCardId.CORNER_EXPERT,
                RogueliteCardId.GRID_LINK);
        AntennaNetworkBonuses network = AntennaNetworkBonuses.builder()
                .include(driftingSource)
                .include(cornerReceiver)
                .build();
        RogueliteCarUpgrades receiver = upgrades(cornerReceiver, network);

        assertEquals(1.14f, receiver.getAccelerationMultiplier(), EPSILON);
        updateInCorner(receiver);

        // The receiver's corner trigger applies the x1.5 power multiplier shared
        // from the other car's drifting Technique.
        assertEquals(1.21f, receiver.getAccelerationMultiplier(), EPSILON);
        assertTrue(receiver.getActiveCardIds().contains(RogueliteCardId.GRID_LINK));
    }

    @Test
    public void gridLinkImportsOneWholeTechniqueInsteadOfBestStatsFromSeveral() {
        RogueliteLoadout receiver = loadout(
                RogueliteCardId.SHORT_GEARING,
                RogueliteCardId.CORNER_FOCUS,
                RogueliteCardId.GRID_LINK);
        RogueliteLoadout draftSource = loadout(
                RogueliteCardId.AERO_TRIM,
                RogueliteCardId.DRAFT_MASTER,
                RogueliteCardId.TUNE_LINK);
        RogueliteLoadout driftSource = loadout(
                RogueliteCardId.CLUB_TUNE,
                RogueliteCardId.DRIFT_MASTER,
                RogueliteCardId.TECHNIQUE_LINK);
        AntennaNetworkBonuses network = AntennaNetworkBonuses.builder()
                .include(receiver)
                .include(draftSource)
                .include(driftSource)
                .build();
        RogueliteCarUpgrades upgrades = upgrades(receiver, network);

        updateInCorner(upgrades);

        // Draft Master is the strongest missing card and adds power/aero x4.
        // Drift Master's mass multiplier must not be merged into that selection.
        assertEquals(1.52f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.05f, upgrades.getMassMultiplier(), EPSILON);
    }

    @Test
    public void gridLinkConsumesBothNetworkChannels() {
        RogueliteLoadout first = loadout(
                RogueliteCardId.AERO_TRIM,
                RogueliteCardId.DRIFT_FOCUS,
                RogueliteCardId.TUNE_LINK);
        RogueliteLoadout grid = loadout(
                RogueliteCardId.LIGHT_COMPOUND,
                RogueliteCardId.CORNER_MASTER,
                RogueliteCardId.GRID_LINK);
        AntennaNetworkBonuses network = AntennaNetworkBonuses.builder()
                .include(first)
                .include(grid)
                .build();
        RogueliteCarUpgrades upgrades = upgrades(grid, network);

        assertEquals(1.12f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(0.92f, upgrades.getMassMultiplier(), EPSILON);
        updateInCorner(upgrades);
        assertEquals(1.18f, upgrades.getAccelerationMultiplier(), EPSILON);
    }

    @Test
    public void techniqueAntennasShareTheBestLapExperienceMultiplier() {
        RogueliteLoadout source = loadout(
                RogueliteCardId.CLUB_TUNE,
                RogueliteCardId.LAP_DIVIDEND,
                RogueliteCardId.TUNE_LINK);
        RogueliteLoadout receiver = loadout(
                RogueliteCardId.AERO_TRIM,
                RogueliteCardId.LAP_DOUBLER,
                RogueliteCardId.TECHNIQUE_LINK);
        AntennaNetworkBonuses network = AntennaNetworkBonuses.builder()
                .include(source)
                .include(receiver)
                .build();

        assertEquals(2f, network.getLapExperienceBankMultiplier(), EPSILON);
        assertEquals(
                1.25f,
                upgrades(source, network).getLapExperienceBankMultiplier(),
                EPSILON);
        assertEquals(
                2f,
                upgrades(receiver, network).getLapExperienceBankMultiplier(),
                EPSILON);
    }

    @Test
    public void suppressedParticipantDoesNotBroadcastItsBuildCards() {
        RogueliteLoadout source = loadout(
                RogueliteCardId.AERO_TRIM,
                RogueliteCardId.CORNER_EXPERT,
                RogueliteCardId.TUNE_LINK);
        AntennaNetworkBonuses network = AntennaNetworkBonuses.builder()
                .includeActive(source, RogueliteCardId.TUNE_LINK, false)
                .build();

        assertEquals(0f, network.getPowerBonus(), EPSILON);
        assertEquals(0f, network.getGripBonus(), EPSILON);
        assertEquals(1f, network.getAerodynamicEfficiency(), EPSILON);
        assertEquals(1f, network.getMassMultiplier(), EPSILON);
        assertEquals(1f, network.getPowerTechniqueScale(), EPSILON);
    }

    private static RogueliteCarUpgrades upgrades(
            RogueliteLoadout loadout,
            AntennaNetworkBonuses network) {
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(loadout);
        upgrades.setAntennaNetwork(network);
        return upgrades;
    }

    private static void updateInCorner(RogueliteCarUpgrades upgrades) {
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
                0.2f,
                0.2f,
                0.2f,
                0f,
                0f,
                false,
                0f,
                0f,
                false);
    }

    private static RogueliteLoadout loadout(RogueliteCardId... cardIds) {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        for (int i = 0; i < cardIds.length; i++) {
            assertTrue(loadout.equip(cardIds[i]));
        }
        return loadout;
    }
}
