package com.github.jbescos.presentation;

import com.github.jbescos.TextFormat;
import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import com.github.jbescos.gameplay.roguelite.RogueliteCarUpgrades;
import com.github.jbescos.gameplay.roguelite.RogueliteLoadout;
import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;

public final class RogueliteCardRowDisplay {
    private RogueliteCardRowDisplay() {
    }

    public static RogueliteCardId resolveCardId(
            RogueliteLoadout loadout,
            RogueliteCarUpgrades upgrades,
            RogueliteSlotType slotType) {
        if (loadout == null || slotType == null || slotType.isDriver()) {
            return null;
        }

        RogueliteCardId equippedCardId = loadout.get(slotType);
        if (upgrades == null) {
            return equippedCardId;
        }

        RogueliteCardId loadedCardId = upgrades.getLoadedCardId(slotType);
        if (loadedCardId != null) {
            return loadedCardId;
        }

        RogueliteCardId activeCardId = upgrades.getActiveCardId(slotType);
        return activeCardId != null && activeCardId != equippedCardId
                ? activeCardId
                : equippedCardId;
    }

    public static String statusText(
            RogueliteSlotType slotType,
            float activeSeconds,
            float cooldownSeconds,
            boolean revengeArmed) {
        if (slotType == null || slotType.isDriver() || slotType == RogueliteSlotType.TUNING) {
            return "";
        }
        if (activeSeconds > 0.005f) {
            return TextFormat.fixed(activeSeconds, 1) + "s";
        }
        if (slotType == RogueliteSlotType.TECHNIQUE) {
            return "";
        }
        if (cooldownSeconds > 0.005f) {
            return "CD " + TextFormat.fixed(cooldownSeconds, 1) + "s";
        }
        if (slotType == RogueliteSlotType.REVENGE) {
            return revengeArmed ? "READY" : "WAIT";
        }
        return slotType == RogueliteSlotType.POWERUP ? "READY" : "";
    }

    public static String debuffStatusText(float remainingSeconds) {
        return remainingSeconds > 0.005f
                ? TextFormat.fixed(remainingSeconds, 1) + "s"
                : "";
    }
}
