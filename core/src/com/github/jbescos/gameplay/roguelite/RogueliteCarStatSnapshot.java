package com.github.jbescos.gameplay.roguelite;

/** Effective passive car statistics for loadout presentation and previews. */
public final class RogueliteCarStatSnapshot {
    private final float accelerationMultiplier;
    private final float maxSpeedMultiplier;
    private final float gripMultiplier;
    private final float steeringMultiplier;
    private final float massMultiplier;
    private final float aerodynamicEfficiency;
    private final float lapExperienceBankMultiplier;

    private RogueliteCarStatSnapshot(
            RogueliteCarUpgrades upgrades,
            float slip,
            float accelerationEffectMultiplier,
            float surfaceGripMultiplier,
            float gripEffectMultiplier,
            float steeringEffectMultiplier,
            float massEffectMultiplier,
            float aerodynamicEffectMultiplier) {
        accelerationMultiplier =
                upgrades.getAccelerationMultiplier(accelerationEffectMultiplier);
        aerodynamicEfficiency =
                upgrades.getAerodynamicEfficiencyMultiplier(
                        aerodynamicEffectMultiplier);
        maxSpeedMultiplier = RogueliteCarUpgrades.deriveMaxSpeedMultiplier(
                accelerationMultiplier,
                aerodynamicEfficiency);
        gripMultiplier = upgrades.getGripMultiplier(
                slip,
                surfaceGripMultiplier,
                gripEffectMultiplier);
        steeringMultiplier =
                upgrades.getSteeringMultiplier(slip) * steeringEffectMultiplier;
        massMultiplier = upgrades.getMassMultiplier(massEffectMultiplier);
        lapExperienceBankMultiplier = upgrades.getLapExperienceBankMultiplier();
    }

    public static RogueliteCarStatSnapshot from(
            RogueliteLoadout loadout,
            RogueliteCardId previewCard) {
        return from(loadout, previewCard, AntennaNetworkBonuses.NONE);
    }

    public static RogueliteCarStatSnapshot from(
            RogueliteLoadout loadout,
            RogueliteCardId previewCard,
            AntennaNetworkBonuses antennaNetwork) {
        RogueliteLoadout previewLoadout = copy(loadout);
        if (previewCard != null) {
            previewLoadout.equip(previewCard);
        }
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(previewLoadout);
        upgrades.setAntennaNetwork(antennaNetwork);
        return new RogueliteCarStatSnapshot(
                upgrades,
                0f,
                1f,
                1f,
                1f,
                1f,
                1f,
                1f);
    }

    public static RogueliteCarStatSnapshot fromLive(
            RogueliteCarUpgrades upgrades,
            float slip,
            float accelerationEffectMultiplier,
            float surfaceGripMultiplier,
            float gripEffectMultiplier,
            float steeringEffectMultiplier,
            float massEffectMultiplier,
            float aerodynamicEffectMultiplier) {
        RogueliteCarUpgrades liveUpgrades =
                upgrades == null ? new RogueliteCarUpgrades() : upgrades;
        return new RogueliteCarStatSnapshot(
                liveUpgrades,
                slip,
                accelerationEffectMultiplier,
                surfaceGripMultiplier,
                gripEffectMultiplier,
                steeringEffectMultiplier,
                massEffectMultiplier,
                aerodynamicEffectMultiplier);
    }

    public float getAccelerationMultiplier() {
        return accelerationMultiplier;
    }

    public float getMaxSpeedMultiplier() {
        return maxSpeedMultiplier;
    }

    public float getGripMultiplier() {
        return gripMultiplier;
    }

    public float getSteeringMultiplier() {
        return steeringMultiplier;
    }

    public float getMassMultiplier() {
        return massMultiplier;
    }

    public float getAerodynamicEfficiency() {
        return aerodynamicEfficiency;
    }

    public float getLapExperienceBankMultiplier() {
        return lapExperienceBankMultiplier;
    }

    private static RogueliteLoadout copy(RogueliteLoadout loadout) {
        String driverProfileId =
                loadout == null ? "profile00" : loadout.getDriverProfileId();
        RogueliteLoadout copy = new RogueliteLoadout(driverProfileId);
        if (loadout == null) {
            return copy;
        }
        for (RogueliteCardId cardId : loadout.getModifications()) {
            copy.equip(cardId);
        }
        return copy;
    }
}
