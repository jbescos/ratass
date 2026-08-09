package com.github.jbescos.gameplay.roguelite;

/** Temporary curse applied to an offender until its next qualified car collision. */
public final class OffenderCurseState {
    private boolean active;
    private float massMultiplier = 1f;
    private float gripMultiplier = 1f;

    public boolean apply(float nextMassMultiplier, float nextGripMultiplier) {
        float previousMassMultiplier = massMultiplier;
        active = true;
        massMultiplier = Math.max(
                massMultiplier,
                RogueliteEffectMath.clamp(nextMassMultiplier, 1f, 2f));
        gripMultiplier = Math.min(
                gripMultiplier,
                RogueliteEffectMath.clamp(nextGripMultiplier, 0f, 1f));
        return Math.abs(previousMassMultiplier - massMultiplier) > 0.0001f;
    }

    public boolean clearOnQualifiedCollision() {
        if (!active) {
            return false;
        }
        boolean massChanged = Math.abs(massMultiplier - 1f) > 0.0001f;
        reset();
        return massChanged;
    }

    public void reset() {
        active = false;
        massMultiplier = 1f;
        gripMultiplier = 1f;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isBlind() {
        return active;
    }

    public float getMassMultiplier() {
        return massMultiplier;
    }

    public float getGripMultiplier() {
        return gripMultiplier;
    }
}
