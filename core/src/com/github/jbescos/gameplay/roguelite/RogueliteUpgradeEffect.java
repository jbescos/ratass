package com.github.jbescos.gameplay.roguelite;

abstract class RogueliteUpgradeEffect {
    private final RogueliteCardId cardId;
    protected RogueliteDrivingFrame latestFrame;

    RogueliteUpgradeEffect(RogueliteCardId cardId) {
        this.cardId = cardId;
    }

    final void advance(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        latestFrame = frame;
        update(delta, timerDelta, frame);
    }

    final RogueliteCardId getCardId() {
        return cardId;
    }

    RogueliteCardId behaviorCardId() {
        return cardId;
    }

    RogueliteCardId activeDisplayCardId() {
        return cardId;
    }

    RogueliteCardId loadedDisplayCardId() {
        return null;
    }

    RogueliteCardId activePowerupCardId() {
        return null;
    }

    float nestedPowerupEffectMultiplier(RogueliteCardId candidateCardId) {
        return 1f;
    }

    boolean containsCardEffect(RogueliteCardId candidateCardId) {
        return behaviorCardId() == candidateCardId;
    }

    boolean isCardEffectActive(RogueliteCardId candidateCardId) {
        return containsCardEffect(candidateCardId) && isActive();
    }

    boolean isCardEffectArmed(RogueliteCardId candidateCardId) {
        return containsCardEffect(candidateCardId) && isArmed();
    }

    float cardEffectReadiness(RogueliteCardId candidateCardId) {
        return containsCardEffect(candidateCardId) ? readiness() : 0f;
    }

    float cardEffectActiveTimeRemainingSeconds(RogueliteCardId candidateCardId) {
        return containsCardEffect(candidateCardId)
                ? activeTimeRemainingSeconds()
                : 0f;
    }

    void onLoadedByRandomCard() {
    }

    void setAutomaticPowerupActivationAllowed(boolean allowed) {
    }

    boolean supportsManualPowerupActivation() {
        return false;
    }

    boolean requestManualPowerupActivation() {
        return false;
    }

    boolean isActive() {
        return false;
    }

    boolean isReady() {
        return false;
    }

    boolean isArmed() {
        return false;
    }

    boolean isOffenderCurse() {
        return false;
    }

    float readiness() {
        return 0f;
    }

    float activeTimeRemainingSeconds() {
        return 0f;
    }

    boolean suppressesOffenderBuildAndTransfersLapExperience() {
        return false;
    }

    float revengeTargetAgeSeconds() {
        return Float.POSITIVE_INFINITY;
    }

    float cooldownTimeRemainingSeconds() {
        return 0f;
    }

    int activeDisplayPriority() {
        return 0;
    }

    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
    }

    void observeTechniqueCondition(RogueliteDrivingFrame frame) {
    }

    float timedEffectDecay() {
        return 1f;
    }

    float techniqueEffectMultiplier() {
        return 1f;
    }

    float powerupEffectMultiplier() {
        return 1f;
    }

    float powerupCooldownRateMultiplier() {
        return 1f;
    }

    float lapExperienceBankMultiplier() {
        return 1f;
    }

    boolean tracksRacePosition() {
        return false;
    }

    float adjustSurfaceGrip(float baseGripMultiplier) {
        return baseGripMultiplier;
    }

    float accelerationBonus() {
        return 0f;
    }

    float powerDeviationScale() {
        return 1f;
    }

    float driveForceLimitMultiplier() {
        return 1f;
    }

    float dragMultiplier() {
        return 1f;
    }

    float aeroDeviationScale() {
        return 1f;
    }

    float massMultiplier() {
        return 1f;
    }

    float massDeviationScale() {
        return 1f;
    }

    float gripBonus(float slip) {
        return 0f;
    }

    float gripDeviationScale() {
        return 1f;
    }

    float steeringBonus(float slip) {
        return 0f;
    }

    float steeringTorqueMultiplier() {
        return 1f;
    }

    float slipstreamRangeMultiplier() {
        return 1f;
    }

    float slipstreamStrengthMultiplier() {
        return 1f;
    }

    float slipstreamReleaseLerp(float baseReleaseLerp) {
        return baseReleaseLerp;
    }

    float frontCollisionRecoilMultiplier() {
        return 1f;
    }

    float frontCollisionPushMultiplier() {
        return 1f;
    }

    float carCollisionAreaMultiplier() {
        return 1f;
    }

    float carCollisionMassMultiplier() {
        return 1f;
    }

    float consumeForwardLaunchSpeedRatio() {
        return 0f;
    }

    boolean isDraftMagnetActive() {
        return false;
    }

    float draftMagnetRangeMultiplier() {
        return 1f;
    }

    float draftMagnetForceMultiplier() {
        return 1f;
    }

    boolean isRamChargeActive() {
        return false;
    }

    void consumeRamCharge() {
    }

    float revengeEffectMultiplier() {
        return 1f;
    }

    void onRevengeActivated(float durationSeconds) {
    }

    void onRevengeFinished() {
    }

    void amplifyActiveRevenge(float multiplier) {
    }

    void onRacePositionImproved(int positionsGained, float slipstreamBoost) {
    }

    void onCollision(float impactStrength) {
    }

    RevengeWorkflow revengeWorkflow() {
        return null;
    }

    boolean onHitBy(int vehicleId, float impactStrength) {
        return false;
    }

    void onContactEnded(int vehicleId) {
    }

    boolean isInvisible() {
        return false;
    }

    boolean blocksHostileEffects() {
        return false;
    }

    boolean blocksDebuffs() {
        return false;
    }

    boolean usesBestDriver() {
        return false;
    }

    boolean acceleratesOwnDecisions() {
        return false;
    }

    void deferInvisibilityExpiration() {
    }

    int revengeTargetVehicleId() {
        return -1;
    }

    void setRevengeSecondaryTargetVehicleId(int vehicleId) {
    }

    int revengeSecondaryTargetVehicleId() {
        return -1;
    }

    boolean cancelRevengeTarget(int vehicleId) {
        return false;
    }

    boolean allowsOffRoadOffenderStrike() {
        return false;
    }

    boolean expireOffenderStrikeIfConditionFailed(
            int targetVehicleId,
            boolean offenderAhead) {
        return false;
    }

    RogueliteRevengeStrike tryActivateOffenderHit(int targetVehicleId) {
        return null;
    }

    RogueliteRevengeStrike tryActivateOffenderStrike(
            int targetVehicleId,
            float distance,
            boolean offenderAhead) {
        return null;
    }

    void completeOffenderStrike(RogueliteCardId cardId) {
    }
}
