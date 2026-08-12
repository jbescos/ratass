package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RogueliteCarUpgrades {
    private static final float MIN_TOP_SPEED_MULTIPLIER = 0.65f;
    private static final float MAX_TOP_SPEED_MULTIPLIER = 1.35f;

    private final List<RogueliteUpgradeEffect> effects =
            new ArrayList<RogueliteUpgradeEffect>();
    private final List<RogueliteCardId> activeCardIds =
            new ArrayList<RogueliteCardId>();
    private final List<RogueliteCardId> readOnlyActiveCardIds =
            Collections.unmodifiableList(activeCardIds);
    private final RogueliteDrivingFrame frame = new RogueliteDrivingFrame();
    private final RogueliteDrivingFrame techniqueObservationFrame =
            new RogueliteDrivingFrame();
    private float timedEffectDecay = 1f;
    private boolean overtakeInjectorEnabled;
    private RogueliteCardId activeRevengeCardId;
    private long revengeActivationSequence;
    private RogueliteUpgradeEffect amplifiedActiveRevengeEffect;

    public void configure(RogueliteLoadout loadout) {
        configure(loadout, 0f);
    }

    public void configure(RogueliteLoadout loadout, float powerupCycleOffset) {
        effects.clear();
        activeCardIds.clear();
        activeRevengeCardId = null;
        amplifiedActiveRevengeEffect = null;
        reconfigurePreservingCardState(loadout, powerupCycleOffset);
    }

    public void reconfigurePreservingCardState(
            RogueliteLoadout loadout,
            float powerupCycleOffset) {
        List<RogueliteUpgradeEffect> previousEffects =
                new ArrayList<RogueliteUpgradeEffect>(effects);
        effects.clear();
        frame.clear();
        techniqueObservationFrame.clear();
        timedEffectDecay = 1f;
        overtakeInjectorEnabled = false;

        if (loadout != null) {
            List<RogueliteCardId> cardIds = loadout.getModifications();
            for (int i = 0; i < cardIds.size(); i++) {
                RogueliteCardId cardId = cardIds.get(i);
                RogueliteUpgradeEffect effect =
                        takePreviousEffect(previousEffects, cardId);
                if (effect == null) {
                    effect = RogueliteEffectFactory.create(cardId, powerupCycleOffset);
                }
                effects.add(effect);
                timedEffectDecay = Math.min(timedEffectDecay, effect.timedEffectDecay());
                overtakeInjectorEnabled |= effect.tracksRacePosition();
            }
        }

        if (amplifiedActiveRevengeEffect != null
                && !effects.contains(amplifiedActiveRevengeEffect)) {
            amplifiedActiveRevengeEffect = null;
            notifyRevengeAmplifierFinished();
        }
        refreshActiveCards();
    }

    private static RogueliteUpgradeEffect takePreviousEffect(
            List<RogueliteUpgradeEffect> previousEffects,
            RogueliteCardId cardId) {
        for (int i = 0; i < previousEffects.size(); i++) {
            RogueliteUpgradeEffect effect = previousEffects.get(i);
            if (effect.getCardId() == cardId) {
                previousEffects.remove(i);
                return effect;
            }
        }
        return null;
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

    public RogueliteCardId getLoadedCardId(RogueliteSlotType slotType) {
        if (slotType == null) {
            return null;
        }
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (RogueliteCardCatalog.get(effect.getCardId()).getSlotType()
                    != slotType) {
                continue;
            }
            RogueliteCardId loadedCardId = effect.loadedDisplayCardId();
            if (loadedCardId != null) {
                return loadedCardId;
            }
        }
        return null;
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

    public boolean isBestDriverActive() {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).usesBestDriver()) {
                return true;
            }
        }
        return false;
    }

    public boolean isTimeDilationActive() {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).acceleratesOwnDecisions()) {
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

    public float getRevengeActiveTimeRemainingSeconds() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (RogueliteCardCatalog.get(effect.getCardId()).getSlotType()
                    == RogueliteSlotType.REVENGE) {
                return effect.activeTimeRemainingSeconds();
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
        boolean inferredLongStraight =
                onRoad
                        && throttle > 0.05f
                        && speedRatio >= 0.24f
                        && slip <= 0.10f
                        && cornerSeverity <= 0.055f
                        && (nextCornerSeverity <= 0.13f || nextCornerDistance >= 0.85f)
                        && !forwardLaneBlocked;
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
                revengeNearbyOpponentProximity,
                inferredLongStraight);
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
            float revengeNearbyOpponentProximity,
            boolean longStraight) {
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
                RogueliteEffectMath.clamp(revengeNearbyOpponentProximity, 0f, 1f),
                longStraight);
        float timerDelta = delta * timedEffectDecay;
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).advance(delta, timerDelta, frame);
        }
        refreshActiveCards();
    }

    /**
     * Lets another physical member of the same quantum family activate the shared
     * Technique card without advancing shared timers or other card workflows.
     */
    public void observeTechniqueConditions(
            boolean onRoad,
            float slip,
            float speedRatio,
            float slipstreamBoost,
            float cornerSeverity,
            boolean longStraight) {
        if (effects.isEmpty()) {
            return;
        }
        techniqueObservationFrame.set(
                0f,
                onRoad,
                false,
                false,
                slip,
                speedRatio,
                slipstreamBoost,
                0f,
                0f,
                0f,
                RogueliteEffectMath.clamp(cornerSeverity, 0f, 1f),
                1f,
                0f,
                0f,
                0f,
                false,
                0f,
                0f,
                longStraight);
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).observeTechniqueCondition(techniqueObservationFrame);
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
        return getAccelerationMultiplier(1f);
    }

    public float getAccelerationMultiplier(float externalMultiplier) {
        float bonus = 0f;
        for (int i = 0; i < effects.size(); i++) {
            bonus += effects.get(i).accelerationBonus();
        }
        float combined = (1f + bonus) * externalMultiplier;
        return RogueliteEffectMath.clamp(
                amplifyDeviation(combined, powerDeviationScale()),
                0f,
                1.85f);
    }

    public float getDriveForceLimitMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            multiplier *= effects.get(i).driveForceLimitMultiplier();
        }
        return RogueliteEffectMath.clamp(
                amplifyDeviation(multiplier, powerDeviationScale()),
                0.70f,
                2f);
    }

    public float getMaxSpeedMultiplier() {
        return getMaxSpeedMultiplier(1f, 1f);
    }

    public float getMaxSpeedMultiplier(
            float externalPowerMultiplier,
            float externalAerodynamicEfficiencyMultiplier) {
        return deriveMaxSpeedMultiplier(
                getAccelerationMultiplier(externalPowerMultiplier),
                getAerodynamicEfficiencyMultiplier(
                        externalAerodynamicEfficiencyMultiplier));
    }

    static float deriveMaxSpeedMultiplier(
            float powerMultiplier,
            float aerodynamicEfficiencyMultiplier) {
        float performanceProduct = Math.max(
                0f,
                powerMultiplier * aerodynamicEfficiencyMultiplier);
        return RogueliteEffectMath.clamp(
                (float) Math.cbrt(performanceProduct),
                MIN_TOP_SPEED_MULTIPLIER,
                MAX_TOP_SPEED_MULTIPLIER);
    }

    public float getDragMultiplier() {
        return getDragMultiplier(1f);
    }

    public float getDragMultiplier(float externalAerodynamicEfficiencyMultiplier) {
        return 1f
                / Math.max(
                        0.01f,
                        getAerodynamicEfficiencyMultiplier(
                                externalAerodynamicEfficiencyMultiplier));
    }

    public float getAerodynamicEfficiencyMultiplier(float externalMultiplier) {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            multiplier *= effects.get(i).dragMultiplier();
        }
        float combinedEfficiency = externalMultiplier / Math.max(0.01f, multiplier);
        return Math.max(
                0.01f,
                amplifyDeviation(combinedEfficiency, aeroDeviationScale()));
    }

    public float getMassMultiplier() {
        return getMassMultiplier(1f);
    }

    public float getMassMultiplier(float externalMultiplier) {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            multiplier *= effects.get(i).massMultiplier();
        }
        return RogueliteEffectMath.clamp(
                amplifyDeviation(multiplier * externalMultiplier, massDeviationScale()),
                0.10f,
                2f);
    }

    public float getGripMultiplier(float slip) {
        return getGripMultiplier(slip, 1f, 1f);
    }

    public float getGripMultiplier(float slip, float carEffectMultiplier) {
        return getGripMultiplier(slip, 1f, carEffectMultiplier);
    }

    public float getGripMultiplier(
            float slip,
            float surfaceMultiplier,
            float carEffectMultiplier) {
        float bonus = 0f;
        for (int i = 0; i < effects.size(); i++) {
            bonus += effects.get(i).gripBonus(slip);
        }
        float carGrip = (1f + bonus) * carEffectMultiplier;
        float techniqueGrip = RogueliteEffectMath.amplifyPositiveDeviation(
                carGrip,
                gripDeviationScale());
        // Weather and other surface loss remain independent from Technique effects.
        return RogueliteEffectMath.clamp(
                techniqueGrip * surfaceMultiplier,
                0f,
                2f);
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

    private float powerDeviationScale() {
        float scale = 1f;
        for (int i = 0; i < effects.size(); i++) {
            scale *= effects.get(i).powerDeviationScale();
        }
        return scale;
    }

    private float gripDeviationScale() {
        float scale = 1f;
        for (int i = 0; i < effects.size(); i++) {
            scale *= effects.get(i).gripDeviationScale();
        }
        return scale;
    }

    private float aeroDeviationScale() {
        float scale = 1f;
        for (int i = 0; i < effects.size(); i++) {
            scale *= effects.get(i).aeroDeviationScale();
        }
        return scale;
    }

    private float massDeviationScale() {
        float scale = 1f;
        for (int i = 0; i < effects.size(); i++) {
            scale *= effects.get(i).massDeviationScale();
        }
        return scale;
    }

    private static float amplifyDeviation(float multiplier, float scale) {
        return RogueliteEffectMath.amplifyDeviation(multiplier, scale);
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

    public boolean onHitBy(int vehicleId, float impactStrength) {
        return onHitBy(vehicleId, impactStrength, true);
    }

    public boolean onHitBy(int vehicleId, float impactStrength, boolean canArmRevenge) {
        if (!canArmRevenge) {
            return false;
        }
        boolean accepted = false;
        boolean resetAmplifier = false;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            boolean effectAccepted = effect.onHitBy(vehicleId, impactStrength);
            accepted |= effectAccepted;
            resetAmplifier |= effectAccepted && effect == amplifiedActiveRevengeEffect;
        }
        if (resetAmplifier) {
            amplifiedActiveRevengeEffect = null;
            notifyRevengeAmplifierFinished();
        }
        refreshActiveCards();
        return accepted;
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

    public void setRevengeSecondaryTargetVehicleId(int vehicleId) {
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).setRevengeSecondaryTargetVehicleId(vehicleId);
        }
    }

    public int getRevengeSecondaryTargetVehicleId() {
        for (int i = 0; i < effects.size(); i++) {
            int targetVehicleId = effects.get(i).revengeSecondaryTargetVehicleId();
            if (targetVehicleId >= 0) {
                return targetVehicleId;
            }
        }
        return -1;
    }

    public boolean cancelRevengeTarget(int vehicleId) {
        boolean cancelled = false;
        for (int i = 0; i < effects.size(); i++) {
            cancelled |= effects.get(i).cancelRevengeTarget(vehicleId);
        }
        if (cancelled) {
            refreshActiveCards();
        }
        return cancelled;
    }

    public boolean allowsOffRoadOffenderStrike() {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).allowsOffRoadOffenderStrike()) {
                return true;
            }
        }
        return false;
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

    public boolean expireOffenderStrikeIfConditionFailed(
            int targetVehicleId,
            boolean offenderAhead) {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).expireOffenderStrikeIfConditionFailed(
                    targetVehicleId,
                    offenderAhead)) {
                refreshActiveCards();
                return true;
            }
        }
        return false;
    }

    public RogueliteRevengeStrike tryActivateOffenderHit(int targetVehicleId) {
        long activationSequence = revengeActivationSequence;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteRevengeStrike strike =
                    effects.get(i).tryActivateOffenderHit(targetVehicleId);
            if (strike != null) {
                strike = amplifyRevengeStrike(effects.get(i), strike);
                refreshActiveCards();
                recordRevengeActivationIfMissing(true, activationSequence);
                return strike;
            }
        }
        return null;
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
                strike = amplifyRevengeStrike(effects.get(i), strike);
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
        synchronizeRevengeAmplifier();
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

    public float getRevengeEffectMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            multiplier = Math.max(multiplier, effects.get(i).revengeEffectMultiplier());
        }
        return multiplier;
    }

    private RogueliteRevengeStrike amplifyRevengeStrike(
            RogueliteUpgradeEffect revengeEffect,
            RogueliteRevengeStrike strike) {
        float multiplier = getRevengeEffectMultiplier();
        if (multiplier <= 1f) {
            return strike;
        }
        revengeEffect.amplifyActiveRevenge(multiplier);
        RogueliteRevengeStrike amplifiedStrike = strike.amplified(multiplier);
        notifyRevengeAmplifierActivated(
                Math.max(
                        revengeEffect.activeTimeRemainingSeconds(),
                        amplifiedStrike.getDurationSeconds()));
        if (revengeEffect.isActive()) {
            amplifiedActiveRevengeEffect = revengeEffect;
        } else {
            notifyRevengeAmplifierFinished();
        }
        return amplifiedStrike;
    }

    private void synchronizeRevengeAmplifier() {
        if (amplifiedActiveRevengeEffect != null
                && !amplifiedActiveRevengeEffect.isActive()) {
            amplifiedActiveRevengeEffect = null;
            notifyRevengeAmplifierFinished();
        }

        RogueliteUpgradeEffect activeRevengeEffect = null;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (RogueliteCardCatalog.get(effect.getCardId()).getSlotType()
                            == RogueliteSlotType.REVENGE
                    && effect.isActive()) {
                activeRevengeEffect = effect;
                break;
            }
        }
        if (activeRevengeEffect == null
                || activeRevengeEffect == amplifiedActiveRevengeEffect) {
            return;
        }

        float multiplier = getRevengeEffectMultiplier();
        if (multiplier <= 1f) {
            return;
        }
        activeRevengeEffect.amplifyActiveRevenge(multiplier);
        amplifiedActiveRevengeEffect = activeRevengeEffect;
        notifyRevengeAmplifierActivated(
                activeRevengeEffect.activeTimeRemainingSeconds());
    }

    private void notifyRevengeAmplifierActivated(float durationSeconds) {
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).onRevengeActivated(durationSeconds);
        }
    }

    private void notifyRevengeAmplifierFinished() {
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).onRevengeFinished();
        }
    }

    private void recordRevengeActivationIfMissing(
            boolean activated,
            long previousSequence) {
        if (activated && revengeActivationSequence == previousSequence) {
            revengeActivationSequence++;
        }
    }
}
