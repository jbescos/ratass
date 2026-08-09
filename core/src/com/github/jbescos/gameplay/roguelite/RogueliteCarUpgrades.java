package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RogueliteCarUpgrades {
    private final List<RogueliteUpgradeEffect> effects =
            new ArrayList<RogueliteUpgradeEffect>();
    private final List<RogueliteCardId> activeCardIds =
            new ArrayList<RogueliteCardId>();
    private final List<RogueliteCardId> readOnlyActiveCardIds =
            Collections.unmodifiableList(activeCardIds);
    private final RogueliteDrivingFrame frame = new RogueliteDrivingFrame();
    private float timedEffectDecay = 1f;
    private boolean overtakeInjectorEnabled;
    private RogueliteCardId activeRevengeCardId;
    private long revengeActivationSequence;

    public void configure(RogueliteLoadout loadout) {
        configure(loadout, 0f);
    }

    public void configure(RogueliteLoadout loadout, float powerupCycleOffset) {
        effects.clear();
        activeCardIds.clear();
        activeRevengeCardId = null;
        frame.clear();
        timedEffectDecay = 1f;
        overtakeInjectorEnabled = false;
        if (loadout == null || loadout.getModifications().isEmpty()) {
            return;
        }

        List<RogueliteCardId> cardIds = loadout.getModifications();
        for (int i = 0; i < cardIds.size(); i++) {
            RogueliteUpgradeEffect effect =
                    RogueliteEffectFactory.create(
                            cardIds.get(i),
                            powerupCycleOffset);
            effects.add(effect);
            timedEffectDecay = Math.min(timedEffectDecay, effect.timedEffectDecay());
            overtakeInjectorEnabled |= effect.tracksRacePosition();
        }
    }

    public boolean isEnabled() {
        return !effects.isEmpty();
    }

    public boolean hasOvertakeInjector() {
        return overtakeInjectorEnabled;
    }

    public List<RogueliteCardId> getActiveCardIds() {
        return readOnlyActiveCardIds;
    }

    public RogueliteCardId getActiveCardId(RogueliteSlotType slotType) {
        for (int i = 0; i < activeCardIds.size(); i++) {
            RogueliteCardId cardId = activeCardIds.get(i);
            if (RogueliteCardCatalog.get(cardId).getSlotType() == slotType) {
                return cardId;
            }
        }
        return null;
    }

    public RogueliteCardId getActivePowerupCardId() {
        return getActiveCardId(RogueliteSlotType.POWERUP);
    }

    public RogueliteCardId getActiveTechniqueCardId() {
        return getActiveCardId(RogueliteSlotType.TECHNIQUE);
    }

    public long getRevengeActivationSequence() {
        return revengeActivationSequence;
    }

    public RogueliteCardId getActiveAbilityCardId() {
        for (int i = 0; i < activeCardIds.size(); i++) {
            RogueliteCardId cardId = activeCardIds.get(i);
            RogueliteSlotType slotType =
                    RogueliteCardCatalog.get(cardId).getSlotType();
            if (slotType == RogueliteSlotType.POWERUP
                    || slotType == RogueliteSlotType.REVENGE) {
                return cardId;
            }
        }
        return null;
    }

    public boolean isPowerupReady() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (RogueliteCardCatalog.get(effect.getCardId()).getSlotType()
                            == RogueliteSlotType.POWERUP
                    && effect.isReady()) {
                return true;
            }
        }
        return false;
    }

    public float getPowerupReadiness() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (RogueliteCardCatalog.get(effect.getCardId()).getSlotType()
                    == RogueliteSlotType.POWERUP) {
                return effect.readiness();
            }
        }
        return 0f;
    }

    public boolean isRevengeArmed() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (RogueliteCardCatalog.get(effect.getCardId()).getSlotType()
                            == RogueliteSlotType.REVENGE
                    && effect.isArmed()) {
                return true;
            }
        }
        return false;
    }

    public boolean isRevengeReady() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (RogueliteCardCatalog.get(effect.getCardId()).getSlotType()
                            == RogueliteSlotType.REVENGE
                    && effect.isReady()) {
                return true;
            }
        }
        return false;
    }

    public float getRevengeReadiness() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (RogueliteCardCatalog.get(effect.getCardId()).getSlotType()
                    == RogueliteSlotType.REVENGE) {
                return effect.readiness();
            }
        }
        return 0f;
    }

    public RogueliteCardId getRevengeCardId() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteCardId cardId = effects.get(i).behaviorCardId();
            if (RogueliteCardCatalog.get(cardId).getSlotType()
                    == RogueliteSlotType.REVENGE) {
                return cardId;
            }
        }
        return null;
    }

    public float getActiveTimeRemainingSeconds(RogueliteCardId cardId) {
        RogueliteUpgradeEffect effect = findEffect(cardId);
        return effect == null ? 0f : effect.activeTimeRemainingSeconds();
    }

    public float getCooldownTimeRemainingSeconds(RogueliteCardId cardId) {
        RogueliteUpgradeEffect effect = findEffect(cardId);
        return effect == null ? 0f : effect.cooldownTimeRemainingSeconds();
    }

    private RogueliteUpgradeEffect findEffect(RogueliteCardId cardId) {
        if (cardId == null) {
            return null;
        }
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (effect.getCardId() == cardId) {
                return effect;
            }
        }
        return null;
    }

    public void update(
            float delta,
            float throttle,
            boolean onRoad,
            boolean adverseWeather,
            boolean recentlyImpacted,
            float slip,
            float speedRatio,
            float routeProgress,
            float routeLength,
            float safeRecoveryRouteGain) {
        update(
                delta,
                throttle,
                onRoad,
                adverseWeather,
                recentlyImpacted,
                slip,
                speedRatio,
                0f,
                routeProgress,
                routeLength,
                safeRecoveryRouteGain);
    }

    public void update(
            float delta,
            float throttle,
            boolean onRoad,
            boolean adverseWeather,
            boolean recentlyImpacted,
            float slip,
            float speedRatio,
            float slipstreamBoost,
            float routeProgress,
            float routeLength,
            float safeRecoveryRouteGain) {
        update(
                delta,
                throttle,
                onRoad,
                adverseWeather,
                recentlyImpacted,
                slip,
                speedRatio,
                slipstreamBoost,
                routeProgress,
                routeLength,
                safeRecoveryRouteGain,
                0f,
                1f,
                0f,
                0f,
                0f);
    }

    public void update(
            float delta,
            float throttle,
            boolean onRoad,
            boolean adverseWeather,
            boolean recentlyImpacted,
            float slip,
            float speedRatio,
            float slipstreamBoost,
            float routeProgress,
            float routeLength,
            float safeRecoveryRouteGain,
            float cornerSeverity,
            float nextCornerDistance,
            float nextCornerSeverity) {
        update(
                delta,
                throttle,
                onRoad,
                adverseWeather,
                recentlyImpacted,
                slip,
                speedRatio,
                slipstreamBoost,
                routeProgress,
                routeLength,
                safeRecoveryRouteGain,
                cornerSeverity,
                nextCornerDistance,
                nextCornerSeverity,
                0f,
                0f);
    }

    public void update(
            float delta,
            float throttle,
            boolean onRoad,
            boolean adverseWeather,
            boolean recentlyImpacted,
            float slip,
            float speedRatio,
            float slipstreamBoost,
            float routeProgress,
            float routeLength,
            float safeRecoveryRouteGain,
            float cornerSeverity,
            float nextCornerDistance,
            float nextCornerSeverity,
            float opponentAheadProximity,
            float nearbyOpponentProximity) {
        update(
                delta,
                throttle,
                onRoad,
                adverseWeather,
                recentlyImpacted,
                slip,
                speedRatio,
                slipstreamBoost,
                routeProgress,
                routeLength,
                safeRecoveryRouteGain,
                cornerSeverity,
                nextCornerDistance,
                nextCornerSeverity,
                opponentAheadProximity,
                nearbyOpponentProximity,
                false);
    }

    public void update(
            float delta,
            float throttle,
            boolean onRoad,
            boolean adverseWeather,
            boolean recentlyImpacted,
            float slip,
            float speedRatio,
            float slipstreamBoost,
            float routeProgress,
            float routeLength,
            float safeRecoveryRouteGain,
            float cornerSeverity,
            float nextCornerDistance,
            float nextCornerSeverity,
            float opponentAheadProximity,
            float nearbyOpponentProximity,
            boolean forwardLaneBlocked) {
        update(
                delta,
                throttle,
                onRoad,
                adverseWeather,
                recentlyImpacted,
                slip,
                speedRatio,
                slipstreamBoost,
                routeProgress,
                routeLength,
                safeRecoveryRouteGain,
                cornerSeverity,
                nextCornerDistance,
                nextCornerSeverity,
                opponentAheadProximity,
                nearbyOpponentProximity,
                forwardLaneBlocked,
                0f);
    }

    public void update(
            float delta,
            float throttle,
            boolean onRoad,
            boolean adverseWeather,
            boolean recentlyImpacted,
            float slip,
            float speedRatio,
            float slipstreamBoost,
            float routeProgress,
            float routeLength,
            float safeRecoveryRouteGain,
            float cornerSeverity,
            float nextCornerDistance,
            float nextCornerSeverity,
            float opponentAheadProximity,
            float nearbyOpponentProximity,
            boolean forwardLaneBlocked,
            float racePositionFactor) {
        update(
                delta,
                throttle,
                onRoad,
                adverseWeather,
                recentlyImpacted,
                slip,
                speedRatio,
                slipstreamBoost,
                routeProgress,
                routeLength,
                safeRecoveryRouteGain,
                cornerSeverity,
                nextCornerDistance,
                nextCornerSeverity,
                opponentAheadProximity,
                nearbyOpponentProximity,
                forwardLaneBlocked,
                racePositionFactor,
                nearbyOpponentProximity);
    }

    public void update(
            float delta,
            float throttle,
            boolean onRoad,
            boolean adverseWeather,
            boolean recentlyImpacted,
            float slip,
            float speedRatio,
            float slipstreamBoost,
            float routeProgress,
            float routeLength,
            float safeRecoveryRouteGain,
            float cornerSeverity,
            float nextCornerDistance,
            float nextCornerSeverity,
            float opponentAheadProximity,
            float nearbyOpponentProximity,
            boolean forwardLaneBlocked,
            float racePositionFactor,
            float revengeNearbyOpponentProximity) {
        if (effects.isEmpty()) {
            return;
        }
        frame.set(
                throttle,
                onRoad,
                adverseWeather,
                recentlyImpacted,
                slip,
                speedRatio,
                slipstreamBoost,
                routeProgress,
                routeLength,
                safeRecoveryRouteGain,
                RogueliteEffectMath.clamp(cornerSeverity, 0f, 1f),
                RogueliteEffectMath.clamp(nextCornerDistance, 0f, 1f),
                RogueliteEffectMath.clamp(nextCornerSeverity, 0f, 1f),
                RogueliteEffectMath.clamp(opponentAheadProximity, 0f, 1f),
                RogueliteEffectMath.clamp(nearbyOpponentProximity, 0f, 1f),
                forwardLaneBlocked,
                RogueliteEffectMath.clamp(racePositionFactor, 0f, 1f),
                RogueliteEffectMath.clamp(revengeNearbyOpponentProximity, 0f, 1f));
        float timerDelta = delta * timedEffectDecay;
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).advance(delta, timerDelta, frame);
        }
        refreshActiveCards();
    }

    public float adjustSurfaceGrip(float baseGripMultiplier) {
        float adjusted = baseGripMultiplier;
        for (int i = 0; i < effects.size(); i++) {
            adjusted = effects.get(i).adjustSurfaceGrip(adjusted);
        }
        return RogueliteEffectMath.clamp(adjusted, 0f, 1f);
    }

    public float getAccelerationMultiplier() {
        float bonus = 0f;
        for (int i = 0; i < effects.size(); i++) {
            bonus += effects.get(i).accelerationBonus();
        }
        return RogueliteEffectMath.clamp(1f + bonus, 0.80f, 1.85f);
    }

    public float getMaxSpeedMultiplier() {
        float bonus = 0f;
        for (int i = 0; i < effects.size(); i++) {
            bonus += effects.get(i).maxSpeedBonus();
        }
        return RogueliteEffectMath.clamp(1f + bonus, 0.80f, 1.35f);
    }

    public float getDragMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            multiplier *= effects.get(i).dragMultiplier();
        }
        return multiplier;
    }

    public float getMassMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            multiplier *= effects.get(i).massMultiplier();
        }
        return RogueliteEffectMath.clamp(multiplier, 0.75f, 1.35f);
    }

    public float getGripMultiplier(float slip) {
        float bonus = 0f;
        for (int i = 0; i < effects.size(); i++) {
            bonus += effects.get(i).gripBonus(slip);
        }
        return RogueliteEffectMath.clamp(1f + bonus, 0.65f, 2f);
    }

    public float getSteeringMultiplier(float slip) {
        float bonus = 0f;
        for (int i = 0; i < effects.size(); i++) {
            bonus += effects.get(i).steeringBonus(slip);
        }
        return RogueliteEffectMath.clamp(1f + bonus, 0.70f, 2f);
    }

    public float getSlipstreamRangeMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            multiplier *= effects.get(i).slipstreamRangeMultiplier();
        }
        return multiplier;
    }

    public float getSlipstreamStrengthMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            multiplier *= effects.get(i).slipstreamStrengthMultiplier();
        }
        return multiplier;
    }

    public float getSlipstreamReleaseLerp(float baseReleaseLerp) {
        float releaseLerp = baseReleaseLerp;
        for (int i = 0; i < effects.size(); i++) {
            releaseLerp = effects.get(i).slipstreamReleaseLerp(releaseLerp);
        }
        return releaseLerp;
    }

    public float getFrontCollisionRecoilMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            multiplier *= effects.get(i).frontCollisionRecoilMultiplier();
        }
        return multiplier;
    }

    public float getFrontCollisionPushMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            multiplier *= effects.get(i).frontCollisionPushMultiplier();
        }
        return multiplier;
    }

    public float consumeForwardLaunchSpeedRatio() {
        float speedRatio = 0f;
        for (int i = 0; i < effects.size(); i++) {
            speedRatio += effects.get(i).consumeForwardLaunchSpeedRatio();
        }
        return RogueliteEffectMath.clamp(speedRatio, 0f, 0.50f);
    }

    public boolean isDraftMagnetActive() {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).isDraftMagnetActive()) {
                return true;
            }
        }
        return false;
    }

    public float getDraftMagnetRangeMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            multiplier = Math.max(multiplier, effects.get(i).draftMagnetRangeMultiplier());
        }
        return multiplier;
    }

    public float getDraftMagnetForceMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            multiplier = Math.max(multiplier, effects.get(i).draftMagnetForceMultiplier());
        }
        return multiplier;
    }

    public boolean isRamChargeActive() {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).isRamChargeActive()) {
                return true;
            }
        }
        return false;
    }

    public void consumeRamCharge() {
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).consumeRamCharge();
        }
        refreshActiveCards();
    }

    public boolean isImpactCounterReady() {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).isImpactCounterReady()) {
                return true;
            }
        }
        return false;
    }

    public void consumeImpactCounter() {
        boolean wasReady = isImpactCounterReady();
        long activationSequence = revengeActivationSequence;
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).consumeImpactCounter();
        }
        refreshActiveCards();
        recordRevengeActivationIfMissing(wasReady, activationSequence);
    }

    public void onRacePositionImproved(int positionsGained, float slipstreamBoost) {
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).onRacePositionImproved(positionsGained, slipstreamBoost);
        }
        refreshActiveCards();
    }

    public void onCollision(float impactStrength) {
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).onCollision(impactStrength);
        }
        refreshActiveCards();
    }

    public void onHitBy(int vehicleId, float impactStrength) {
        onHitBy(vehicleId, impactStrength, true);
    }

    public void onHitBy(int vehicleId, float impactStrength, boolean canArmRevenge) {
        if (!canArmRevenge) {
            return;
        }
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).onHitBy(vehicleId, impactStrength);
        }
        refreshActiveCards();
    }

    public void onContactEnded(int vehicleId) {
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).onContactEnded(vehicleId);
        }
        refreshActiveCards();
    }

    public boolean isInvisible() {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).isInvisible()) {
                return true;
            }
        }
        return false;
    }

    public boolean blocksHostileEffects() {
        return isInvisible();
    }

    public void deferInvisibilityExpiration() {
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).deferInvisibilityExpiration();
        }
    }

    public int getRevengeTargetVehicleId() {
        for (int i = 0; i < effects.size(); i++) {
            int targetVehicleId = effects.get(i).revengeTargetVehicleId();
            if (targetVehicleId >= 0) {
                return targetVehicleId;
            }
        }
        return -1;
    }

    public boolean isOffenderCurseArmed() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (effect.isOffenderCurse() && effect.revengeTargetVehicleId() >= 0) {
                return true;
            }
        }
        return false;
    }

    public RogueliteRevengeStrike tryActivateOffenderStrike(
            int targetVehicleId,
            float distance) {
        return tryActivateOffenderStrike(targetVehicleId, distance, true);
    }

    public RogueliteRevengeStrike tryActivateOffenderStrike(
            int targetVehicleId,
            float distance,
            boolean offenderAhead) {
        long activationSequence = revengeActivationSequence;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteRevengeStrike strike =
                    effects.get(i).tryActivateOffenderStrike(
                            targetVehicleId,
                            distance,
                            offenderAhead);
            if (strike != null) {
                refreshActiveCards();
                recordRevengeActivationIfMissing(true, activationSequence);
                return strike;
            }
        }
        return null;
    }

    public void completeOffenderStrike(RogueliteCardId cardId) {
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).completeOffenderStrike(cardId);
        }
        refreshActiveCards();
    }

    private void refreshActiveCards() {
        activeCardIds.clear();
        int highestPriority = 0;
        for (int i = 0; i < effects.size(); i++) {
            highestPriority = Math.max(highestPriority, effects.get(i).activeDisplayPriority());
        }
        for (int priority = highestPriority; priority >= 0; priority--) {
            for (int i = 0; i < effects.size(); i++) {
                RogueliteUpgradeEffect effect = effects.get(i);
                if (effect.activeDisplayPriority() == priority && effect.isActive()) {
                    RogueliteCardId displayCardId = effect.activeDisplayCardId();
                    activeCardIds.add(displayCardId);
                    if (displayCardId != effect.getCardId()) {
                        activeCardIds.add(effect.getCardId());
                    }
                }
            }
        }
        RogueliteCardId currentRevengeCardId = getActiveCardId(RogueliteSlotType.REVENGE);
        if (currentRevengeCardId != null
                && currentRevengeCardId != activeRevengeCardId) {
            revengeActivationSequence++;
        }
        activeRevengeCardId = currentRevengeCardId;
    }

    private void recordRevengeActivationIfMissing(
            boolean activated,
            long previousSequence) {
        if (activated && revengeActivationSequence == previousSequence) {
            revengeActivationSequence++;
        }
    }
}
