package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AntennaNetworkBonusesTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void linkedCarsBroadcastBestTuningValuesAcrossDifferentAntennaTiers() {
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
        assertEquals(1.05f, tuningUpgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(1.14f, tuningUpgrades.getAerodynamicEfficiencyMultiplier(1f), EPSILON);
        assertEquals(0.92f, tuningUpgrades.getMassMultiplier(), EPSILON);

        // Tier 2 broadcasts tuning to Tier 1, but does not consume tuning itself.
        assertEquals(0.96f, techniqueUpgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.05f, techniqueUpgrades.getGripMultiplier(0f), EPSILON);
        assertEquals(0.92f, techniqueUpgrades.getMassMultiplier(), EPSILON);
    }

    @Test
    public void explicitlySpecifiedMassIsSharedEvenWhenItIsADrawback() {
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

        RogueliteCarUpgrades receiver = upgrades(massSource, network);
        assertEquals(1.13f, receiver.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.03f, receiver.getGripMultiplier(0f), EPSILON);
        assertEquals(1.13f, receiver.getAerodynamicEfficiencyMultiplier(1f), EPSILON);
        assertEquals(1.05f, receiver.getMassMultiplier(), EPSILON);
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
                RogueliteCardId.TECHNIQUE_LINK);
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
        assertTrue(receiver.getActiveCardIds().contains(RogueliteCardId.TECHNIQUE_LINK));
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
