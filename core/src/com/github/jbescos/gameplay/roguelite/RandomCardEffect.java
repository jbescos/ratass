package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Holds and delegates to one random card until that card finishes executing. */
final class RandomCardEffect extends RogueliteUpgradeEffect {
    private static final float MINIMUM_EXECUTION_SETTLE_SECONDS = 1.2f;

    private final float cycleOffset;
    private final RogueliteSlotType slotType;
    private final int tier;
    private final RogueliteRandom random;
    private final List<RogueliteCardId> candidates;

    private RogueliteUpgradeEffect delegate;
    private RogueliteCardId previousCardId;
    private boolean cycleExecuted;
    private float executionSettleTimer;

    RandomCardEffect(RogueliteCardId cardId, float cycleOffset) {
        this(
                cardId,
                cycleOffset,
                System.nanoTime()
                        ^ ((long) cardId.ordinal() << 32)
                        ^ Float.floatToIntBits(cycleOffset));
    }

    RandomCardEffect(RogueliteCardId cardId, float cycleOffset, long seed) {
        super(cardId);
        RogueliteCardDefinition definition = RogueliteCardCatalog.get(cardId);
        if (!isRandomCard(cardId)) {
            throw new IllegalArgumentException("Unsupported random card: " + cardId);
        }
        this.cycleOffset = cycleOffset;
        slotType = definition.getSlotType();
        tier = definition.getTier();
        random = new RogueliteRandom(seed);
        candidates = candidateCardIds(slotType, tier);
        if (candidates.isEmpty()) {
            throw new IllegalStateException(
                    "No cards available for " + slotType + " tier " + tier);
        }
        selectNextDelegate();
    }

    static boolean isRandomCard(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.LUCKY_SPARK
                || cardId == RogueliteCardId.CHAOS_RELAY
                || cardId == RogueliteCardId.WILDCARD_CORE
                || cardId == RogueliteCardId.LOADED_GRUDGE
                || cardId == RogueliteCardId.CHAOS_RETORT
                || cardId == RogueliteCardId.FATES_REVENGE;
    }

    static List<RogueliteCardId> candidateCardIds(
            RogueliteSlotType slotType,
            int tier) {
        List<RogueliteCardId> matches = new ArrayList<RogueliteCardId>();
        List<RogueliteCardDefinition> cards = RogueliteCardCatalog.all();
        for (int i = 0; i < cards.size(); i++) {
            RogueliteCardDefinition card = cards.get(i);
            if (card.getSlotType() == slotType
                    && card.getTier() == tier
                    && !isRandomCard(card.getId())) {
                matches.add(card.getId());
            }
        }
        return Collections.unmodifiableList(matches);
    }

    RogueliteCardId preparedCardId() {
        return delegate.behaviorCardId();
    }

    @Override
    RogueliteCardId behaviorCardId() {
        return delegate.behaviorCardId();
    }

    @Override
    RogueliteCardId activeDisplayCardId() {
        return delegate.activeDisplayCardId();
    }

    @Override
    RogueliteCardId loadedDisplayCardId() {
        return delegate.behaviorCardId();
    }

    @Override
    boolean isActive() {
        return delegate.isActive()
                || cycleExecuted && executionSettleTimer > 0f;
    }

    @Override
    boolean isReady() {
        return delegate.isReady();
    }

    @Override
    boolean isArmed() {
        return delegate.isArmed();
    }

    @Override
    boolean isOffenderCurse() {
        return delegate.isOffenderCurse();
    }

    @Override
    RevengeWorkflow revengeWorkflow() {
        return delegate.revengeWorkflow();
    }

    @Override
    float readiness() {
        return delegate.readiness();
    }

    @Override
    float activeTimeRemainingSeconds() {
        return delegate.activeTimeRemainingSeconds();
    }

    @Override
    float cooldownTimeRemainingSeconds() {
        return delegate.cooldownTimeRemainingSeconds();
    }

    @Override
    int activeDisplayPriority() {
        return delegate.activeDisplayPriority();
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        boolean wasActive = delegate.isActive();
        delegate.advance(delta, timerDelta, frame);
        observeActivation(wasActive);
        advanceReselection(delta);
    }

    @Override
    float timedEffectDecay() {
        return delegate.timedEffectDecay();
    }

    @Override
    boolean tracksRacePosition() {
        return delegate.tracksRacePosition();
    }

    @Override
    float adjustSurfaceGrip(float baseGripMultiplier) {
        return delegate.adjustSurfaceGrip(baseGripMultiplier);
    }

    @Override
    float accelerationBonus() {
        return delegate.accelerationBonus();
    }

    @Override
    float powerDeviationScale() {
        return delegate.powerDeviationScale();
    }

    @Override
    float dragMultiplier() {
        return delegate.dragMultiplier();
    }

    @Override
    float aeroDeviationScale() {
        return delegate.aeroDeviationScale();
    }

    @Override
    float massMultiplier() {
        return delegate.massMultiplier();
    }

    @Override
    float massDeviationScale() {
        return delegate.massDeviationScale();
    }

    @Override
    float gripBonus(float slip) {
        return delegate.gripBonus(slip);
    }

    @Override
    float gripDeviationScale() {
        return delegate.gripDeviationScale();
    }

    @Override
    float steeringBonus(float slip) {
        return delegate.steeringBonus(slip);
    }

    @Override
    float slipstreamRangeMultiplier() {
        return delegate.slipstreamRangeMultiplier();
    }

    @Override
    float slipstreamStrengthMultiplier() {
        return delegate.slipstreamStrengthMultiplier();
    }

    @Override
    float slipstreamReleaseLerp(float baseReleaseLerp) {
        return delegate.slipstreamReleaseLerp(baseReleaseLerp);
    }

    @Override
    float frontCollisionRecoilMultiplier() {
        return delegate.frontCollisionRecoilMultiplier();
    }

    @Override
    float frontCollisionPushMultiplier() {
        return delegate.frontCollisionPushMultiplier();
    }

    @Override
    float consumeForwardLaunchSpeedRatio() {
        return delegate.consumeForwardLaunchSpeedRatio();
    }

    @Override
    boolean isDraftMagnetActive() {
        return delegate.isDraftMagnetActive();
    }

    @Override
    float draftMagnetRangeMultiplier() {
        return delegate.draftMagnetRangeMultiplier();
    }

    @Override
    float draftMagnetForceMultiplier() {
        return delegate.draftMagnetForceMultiplier();
    }

    @Override
    boolean isRamChargeActive() {
        return delegate.isRamChargeActive();
    }

    @Override
    void consumeRamCharge() {
        boolean wasActive = delegate.isActive();
        delegate.consumeRamCharge();
        observeActivation(wasActive);
    }

    @Override
    float revengeEffectMultiplier() {
        return delegate.revengeEffectMultiplier();
    }

    @Override
    void onRevengeActivated(float durationSeconds) {
        boolean wasActive = delegate.isActive();
        delegate.onRevengeActivated(durationSeconds);
        observeActivation(wasActive);
    }

    @Override
    void onRevengeFinished() {
        delegate.onRevengeFinished();
    }

    @Override
    void amplifyActiveRevenge(float multiplier) {
        delegate.amplifyActiveRevenge(multiplier);
    }

    @Override
    void onRacePositionImproved(int positionsGained, float slipstreamBoost) {
        boolean wasActive = delegate.isActive();
        delegate.onRacePositionImproved(positionsGained, slipstreamBoost);
        observeActivation(wasActive);
    }

    @Override
    void onCollision(float impactStrength) {
        boolean wasActive = delegate.isActive();
        delegate.onCollision(impactStrength);
        observeActivation(wasActive);
    }

    @Override
    boolean onHitBy(int vehicleId, float impactStrength) {
        if (cycleExecuted) {
            return false;
        }
        boolean wasActive = delegate.isActive();
        boolean accepted = delegate.onHitBy(vehicleId, impactStrength);
        observeActivation(wasActive);
        return accepted;
    }

    @Override
    void onContactEnded(int vehicleId) {
        delegate.onContactEnded(vehicleId);
    }

    @Override
    boolean isInvisible() {
        return delegate.isInvisible();
    }

    @Override
    boolean usesBestDriver() {
        return delegate.usesBestDriver();
    }

    @Override
    boolean acceleratesOwnDecisions() {
        return delegate.acceleratesOwnDecisions();
    }

    @Override
    void deferInvisibilityExpiration() {
        delegate.deferInvisibilityExpiration();
    }

    @Override
    int revengeTargetVehicleId() {
        return delegate.revengeTargetVehicleId();
    }

    @Override
    void setRevengeSecondaryTargetVehicleId(int vehicleId) {
        delegate.setRevengeSecondaryTargetVehicleId(vehicleId);
    }

    @Override
    int revengeSecondaryTargetVehicleId() {
        return delegate.revengeSecondaryTargetVehicleId();
    }

    @Override
    boolean cancelRevengeTarget(int vehicleId) {
        if (!delegate.cancelRevengeTarget(vehicleId)) {
            return false;
        }
        selectNextDelegate();
        return true;
    }

    @Override
    boolean allowsOffRoadOffenderStrike() {
        return delegate.allowsOffRoadOffenderStrike();
    }

    @Override
    boolean expireOffenderStrikeIfConditionFailed(
            int targetVehicleId,
            boolean offenderAhead) {
        if (!delegate.expireOffenderStrikeIfConditionFailed(
                targetVehicleId,
                offenderAhead)) {
            return false;
        }
        selectNextDelegate();
        return true;
    }

    @Override
    RogueliteRevengeStrike tryActivateOffenderHit(int targetVehicleId) {
        RogueliteRevengeStrike strike = delegate.tryActivateOffenderHit(targetVehicleId);
        if (strike != null) {
            markCycleExecuted();
        }
        return strike;
    }

    @Override
    RogueliteRevengeStrike tryActivateOffenderStrike(
            int targetVehicleId,
            float distance,
            boolean offenderAhead) {
        RogueliteRevengeStrike strike =
                delegate.tryActivateOffenderStrike(
                        targetVehicleId,
                        distance,
                        offenderAhead);
        if (strike != null) {
            markCycleExecuted();
        }
        return strike;
    }

    @Override
    void completeOffenderStrike(RogueliteCardId cardId) {
        delegate.completeOffenderStrike(cardId);
    }

    private void observeActivation(boolean wasActive) {
        if (!cycleExecuted
                && !wasActive
                && delegate.isActive()
                && !delegate.isArmed()) {
            markCycleExecuted();
        }
    }

    private void markCycleExecuted() {
        cycleExecuted = true;
        executionSettleTimer = MINIMUM_EXECUTION_SETTLE_SECONDS;
    }

    private void advanceReselection(float delta) {
        if (!cycleExecuted) {
            return;
        }
        executionSettleTimer = Math.max(0f, executionSettleTimer - Math.max(0f, delta));
        if (executionSettleTimer <= 0f
                && !delegate.isActive()
                && !delegate.isArmed()) {
            selectNextDelegate();
        }
    }

    private void selectNextDelegate() {
        int candidateCount = candidates.size();
        int selectedIndex = random.nextInt(
                previousCardId == null || candidateCount == 1
                        ? candidateCount
                        : candidateCount - 1);
        RogueliteCardId selected = candidates.get(selectedIndex);
        if (previousCardId != null && candidateCount > 1) {
            int previousIndex = candidates.indexOf(previousCardId);
            if (selectedIndex >= previousIndex) {
                selected = candidates.get(selectedIndex + 1);
            }
        }
        previousCardId = selected;
        delegate = RogueliteEffectFactory.create(selected, cycleOffset);
        delegate.onLoadedByRandomCard();
        cycleExecuted = false;
        executionSettleTimer = 0f;
    }
}
