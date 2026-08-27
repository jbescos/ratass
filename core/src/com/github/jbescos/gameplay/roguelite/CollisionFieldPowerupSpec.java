package com.github.jbescos.gameplay.roguelite;

/** Shared behavior for Powerups that enlarge only car-to-car collision space. */
public final class CollisionFieldPowerupSpec {
    public static final float BULK_COOLDOWN_SECONDS = 20f;
    public static final float TITAN_COOLDOWN_SECONDS = 15f;
    public static final float COLOSSUS_COOLDOWN_SECONDS = 10f;
    public static final float DURATION_SECONDS = 10f;
    public static final float MASS_MULTIPLIER = 1.20f;
    public static final float GRIP_BONUS = 0.05f;
    public static final float COLOSSUS_COLLISION_MASS_MULTIPLIER = 12f;

    private CollisionFieldPowerupSpec() {
    }

    public static boolean isCollisionFieldCard(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.BULK_FIELD
                || cardId == RogueliteCardId.TITAN_FIELD
                || cardId == RogueliteCardId.COLOSSUS_FIELD;
    }

    public static float collisionAreaMultiplier(RogueliteCardId cardId) {
        if (cardId == RogueliteCardId.BULK_FIELD) {
            return 2f;
        }
        if (cardId == RogueliteCardId.TITAN_FIELD) {
            return 3f;
        }
        if (cardId == RogueliteCardId.COLOSSUS_FIELD) {
            return 4f;
        }
        return 1f;
    }

    public static float cooldownSeconds(RogueliteCardId cardId) {
        if (cardId == RogueliteCardId.BULK_FIELD) {
            return BULK_COOLDOWN_SECONDS;
        }
        if (cardId == RogueliteCardId.TITAN_FIELD) {
            return TITAN_COOLDOWN_SECONDS;
        }
        if (cardId == RogueliteCardId.COLOSSUS_FIELD) {
            return COLOSSUS_COOLDOWN_SECONDS;
        }
        return 0f;
    }

    public static float collisionMassMultiplier(RogueliteCardId cardId) {
        if (cardId == RogueliteCardId.COLOSSUS_FIELD) {
            return COLOSSUS_COLLISION_MASS_MULTIPLIER;
        }
        return collisionAreaMultiplier(cardId);
    }

    public static boolean blocksRevengeCard(RogueliteCardId cardId) {
        if (cardId == null) {
            return false;
        }
        switch (cardId) {
            case DRAFT_MAGNET:
            case RECOVERY_BEACON:
            case DRAFT_VENDETTA:
            case PAYBACK_SHIELD:
            case REPULSOR_WAVE:
            case REPULSOR_SURGE:
            case HUNTER_BARRAGE:
            case HUNTER_STORM:
            case TAR_TETHER:
            case EMP_SNARE:
            case VOID_ANCHOR:
            case SENSOR_JAMMER:
            case GRID_BLACKOUT:
            case TOTAL_BLACKOUT:
            case TRIAD_COUP:
            case CROWN_ENGINE:
                return true;
            default:
                return false;
        }
    }
}
