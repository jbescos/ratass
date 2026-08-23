package com.github.jbescos.gameplay.roguelite;

/** Activates three Tier 3 Powerups together when the equipped Revenge is hit. */
final class FinalReckoningEffect extends RevengeUpgradeEffect {
    private static final RogueliteCardId MIRROR_CARD_ID = RogueliteCardId.OVERDRIVE_COIL;
    private static final RogueliteCardId COLLISION_CARD_ID = RogueliteCardId.COLOSSUS_FIELD;
    private static final RogueliteCardId TIME_CARD_ID = RogueliteCardId.TEMPORAL_DOMINION;

    private float mirrorTimer;
    private float collisionTimer;
    private float timeTimer;
    private float effectMultiplier = 1f;
    private boolean amplificationApplied;

    FinalReckoningEffect() {
        super(RogueliteCardId.FINAL_RECKONING, RevengeWorkflow.TARGET_IMMEDIATE);
    }

    @Override
    boolean containsCardEffect(RogueliteCardId candidateCardId) {
        return candidateCardId == getCardId()
                || candidateCardId == MIRROR_CARD_ID
                || candidateCardId == COLLISION_CARD_ID
                || candidateCardId == TIME_CARD_ID;
    }

    @Override
    boolean isCardEffectActive(RogueliteCardId candidateCardId) {
        if (candidateCardId == getCardId()) {
            return isActive();
        }
        if (candidateCardId == MIRROR_CARD_ID) {
            return mirrorTimer > 0f;
        }
        if (candidateCardId == COLLISION_CARD_ID) {
            return collisionTimer > 0f;
        }
        if (candidateCardId == TIME_CARD_ID) {
            return timeTimer > 0f;
        }
        return false;
    }

    @Override
    float cardEffectReadiness(RogueliteCardId candidateCardId) {
        return containsCardEffect(candidateCardId) ? 1f : 0f;
    }

    @Override
    float cardEffectActiveTimeRemainingSeconds(RogueliteCardId candidateCardId) {
        if (candidateCardId == MIRROR_CARD_ID) {
            return mirrorTimer;
        }
        if (candidateCardId == COLLISION_CARD_ID) {
            return collisionTimer;
        }
        if (candidateCardId == TIME_CARD_ID) {
            return timeTimer;
        }
        return candidateCardId == getCardId() ? activeTimeRemainingSeconds() : 0f;
    }

    @Override
    RogueliteCardId activePowerupCardId() {
        return mirrorTimer > 0f ? MIRROR_CARD_ID : null;
    }

    @Override
    float nestedPowerupEffectMultiplier(RogueliteCardId candidateCardId) {
        return candidateCardId == MIRROR_CARD_ID && mirrorTimer > 0f
                ? effectMultiplier
                : 1f;
    }

    @Override
    boolean isActive() {
        return mirrorTimer > 0f || collisionTimer > 0f || timeTimer > 0f;
    }

    @Override
    float readiness() {
        return 1f;
    }

    @Override
    float activeTimeRemainingSeconds() {
        return Math.max(mirrorTimer, Math.max(collisionTimer, timeTimer));
    }

    @Override
    int activeDisplayPriority() {
        return 9;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        float elapsed = Math.max(0f, timerDelta);
        mirrorTimer = Math.max(0f, mirrorTimer - elapsed);
        collisionTimer = Math.max(0f, collisionTimer - elapsed);
        timeTimer = Math.max(0f, timeTimer - elapsed);
        if (!isActive()) {
            clearTarget();
            effectMultiplier = 1f;
            amplificationApplied = false;
        }
    }

    @Override
    protected void prepareFromHit(int vehicleId, float impactStrength) {
        mirrorTimer = MirrorPowerupSpec.durationSeconds(MIRROR_CARD_ID);
        collisionTimer = CollisionFieldPowerupSpec.DURATION_SECONDS;
        timeTimer = TimeDilationPowerupSpec.durationSeconds(TIME_CARD_ID);
        effectMultiplier = 1f;
        amplificationApplied = false;
    }

    @Override
    protected boolean isExecutionInProgress() {
        return isActive();
    }

    @Override
    protected void onTargetCancelled() {
        mirrorTimer = 0f;
        collisionTimer = 0f;
        timeTimer = 0f;
        effectMultiplier = 1f;
        amplificationApplied = false;
    }

    @Override
    void amplifyActiveRevenge(float multiplier) {
        if (!isActive() || amplificationApplied) {
            return;
        }
        float safeMultiplier = Float.isFinite(multiplier)
                ? Math.max(1f, multiplier)
                : 1f;
        mirrorTimer *= safeMultiplier;
        collisionTimer *= safeMultiplier;
        timeTimer *= safeMultiplier;
        effectMultiplier = safeMultiplier;
        amplificationApplied = true;
    }

    @Override
    boolean acceleratesOwnDecisions() {
        return timeTimer > 0f;
    }

    @Override
    float massMultiplier() {
        return collisionTimer > 0f
                ? 1f + (CollisionFieldPowerupSpec.MASS_MULTIPLIER - 1f) * effectMultiplier
                : 1f;
    }

    @Override
    float gripBonus(float slip) {
        return collisionTimer > 0f
                ? CollisionFieldPowerupSpec.GRIP_BONUS * effectMultiplier
                : 0f;
    }

    @Override
    float carCollisionAreaMultiplier() {
        return collisionTimer > 0f
                ? CollisionFieldPowerupSpec.collisionAreaMultiplier(COLLISION_CARD_ID)
                        * effectMultiplier
                : 1f;
    }

    @Override
    float carCollisionMassMultiplier() {
        return collisionTimer > 0f
                ? CollisionFieldPowerupSpec.collisionMassMultiplier(COLLISION_CARD_ID)
                        * effectMultiplier
                : 1f;
    }
}
