package com.github.jbescos.gameplay.roguelite;

/** Effective passive car statistics for loadout presentation and previews. */
public final class RogueliteCarStatSnapshot {
    private final float accelerationMultiplier;
    private final float maxSpeedMultiplier;
    private final float gripMultiplier;
    private final float steeringMultiplier;
    private final float massMultiplier;
    private final float aerodynamicEfficiency;

    private RogueliteCarStatSnapshot(RogueliteCarUpgrades upgrades) {
        accelerationMultiplier = upgrades.getAccelerationMultiplier();
        maxSpeedMultiplier = upgrades.getMaxSpeedMultiplier();
        gripMultiplier = upgrades.getGripMultiplier(0f);
        steeringMultiplier = upgrades.getSteeringMultiplier(0f);
        massMultiplier = upgrades.getMassMultiplier();
        aerodynamicEfficiency = 1f / Math.max(0.01f, upgrades.getDragMultiplier());
    }

    public static RogueliteCarStatSnapshot from(
            RogueliteLoadout loadout,
            RogueliteCardId previewCard) {
        RogueliteLoadout previewLoadout = copy(loadout);
        if (previewCard != null) {
            previewLoadout.equip(previewCard);
        }
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(previewLoadout);
        return new RogueliteCarStatSnapshot(upgrades);
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
