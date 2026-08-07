package com.github.jbescos.gameplay.roguelite;

/** Event-driven driving techniques. They reward useful race outcomes, not braking inputs. */
final class RaceTechniqueEffect extends RogueliteUpgradeEffect {
    private static final float DRIFT_START_SLIP = 0.18f;
    private static final float DRIFT_END_SLIP = 0.11f;
    private static final float CLEAN_MOMENTUM_AERO_BONUS = 0.03f;

    private float charge;
    private float boostTimer;
    private float boostStrength;
    private float offRoadStartProgress;
    private boolean loadingEvent;
    private boolean roadStateInitialized;
    private boolean wasOnRoad;

    RaceTechniqueEffect(RogueliteCardId cardId) {
        super(cardId);
    }

    @Override
    boolean isActive() {
        if (isConditionalStatCard()) {
            return conditionalStatBonus() > 0.001f;
        }
        if (getCardId() == RogueliteCardId.DRAFT_HUNTER) {
            return latestFrame != null && latestFrame.slipstreamBoost > 0.01f;
        }
        return loadingEvent || boostTimer > 0f || charge > 0.02f;
    }

    @Override
    int activeDisplayPriority() {
        return boostTimer > 0f ? 2 : 1;
    }

    @Override
    boolean tracksRacePosition() {
        return getCardId() == RogueliteCardId.OVERTAKE_SURGE
                || getCardId() == RogueliteCardId.RACECRAFT_MASTERY
                || isPositionCatchupCard();
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        boostTimer = Math.max(0f, boostTimer - timerDelta);
        if (boostTimer == 0f) {
            boostStrength = 0f;
        }
        switch (getCardId()) {
            case CORNER_EXIT:
                updateCornerExit(frame, 0.12f, 1.4f);
                break;
            case DRAFT_HUNTER:
                break;
            case CLEAN_MOMENTUM:
                updateCleanMomentum(delta, frame, 5.5f);
                break;
            case RECOVERY_LAUNCH:
                updateRecovery(frame);
                break;
            case DRIFT_SLINGSHOT:
                updateDrift(delta, frame);
                break;
            case SLIPSTREAM_SLINGSHOT:
                updateSlipstreamSlingshot(delta, frame);
                break;
            case OVERTAKE_SURGE:
                break;
            case APEX_SLINGSHOT:
                updateApex(delta, frame);
                break;
            case PERFECT_LAP:
                updateCleanMomentum(delta, frame, 5.0f);
                break;
            case RACECRAFT_MASTERY:
                updateRacecraft(delta, frame);
                break;
            case UNDERDOG_INSTINCT:
            case COMEBACK_DRIVE:
            case LAST_PLACE_FURY:
            case CLOSE_QUARTERS:
            case PACK_RACER:
            case TRAFFIC_DOMINANCE:
                break;
            default:
                throw new IllegalStateException("Unsupported technique card: " + getCardId());
        }
    }

    @Override
    void onRacePositionImproved(int positionsGained, float slipstreamBoost) {
        if (positionsGained <= 0) {
            return;
        }
        if (getCardId() == RogueliteCardId.OVERTAKE_SURGE) {
            boostStrength = 0.24f;
            boostTimer = Math.max(boostTimer, 2f * Math.min(2, positionsGained));
        } else if (getCardId() == RogueliteCardId.RACECRAFT_MASTERY) {
            boostStrength = 0.28f;
            boostTimer = Math.max(boostTimer, 2.15f * Math.min(2, positionsGained));
        }
    }

    @Override
    void onCollision(float impactStrength) {
        if (getCardId() == RogueliteCardId.CLEAN_MOMENTUM
                || getCardId() == RogueliteCardId.PERFECT_LAP) {
            charge = 0f;
        }
    }

    @Override
    float accelerationBonus() {
        if (isConditionalStatCard()) {
            return conditionalStatBonus();
        }
        if (boostTimer > 0f) {
            return boostStrength;
        }
        if (getCardId() == RogueliteCardId.PERFECT_LAP) {
            return charge * 0.15f;
        }
        if (getCardId() == RogueliteCardId.RACECRAFT_MASTERY
                && latestFrame != null
                && latestFrame.onRoad
                && latestFrame.slipstreamBoost > 0.04f) {
            return 0.13f;
        }
        return 0f;
    }

    @Override
    float maxSpeedBonus() {
        if (isConditionalStatCard()) {
            return conditionalStatBonus();
        }
        if (getCardId() == RogueliteCardId.CLEAN_MOMENTUM) {
            return charge * 0.07f;
        }
        if (getCardId() == RogueliteCardId.PERFECT_LAP) {
            return charge * 0.10f;
        }
        if (boostTimer > 0f) {
            return boostStrength * 0.40f;
        }
        return 0f;
    }

    @Override
    float gripBonus(float slip) {
        if (isConditionalStatCard()) {
            return conditionalStatBonus();
        }
        if (getCardId() == RogueliteCardId.RECOVERY_LAUNCH && boostTimer > 0f) {
            return 0.18f;
        }
        if (getCardId() == RogueliteCardId.PERFECT_LAP) {
            return charge * 0.12f;
        }
        if (getCardId() == RogueliteCardId.RACECRAFT_MASTERY && boostTimer > 0f) {
            return 0.15f;
        }
        return 0f;
    }

    @Override
    float dragMultiplier() {
        if (getCardId() == RogueliteCardId.CLEAN_MOMENTUM) {
            return 1f / (1f + charge * CLEAN_MOMENTUM_AERO_BONUS);
        }
        float bonus = conditionalStatBonus();
        return bonus <= 0f ? 1f : 1f / (1f + bonus);
    }

    @Override
    float steeringBonus(float slip) {
        if (getCardId() == RogueliteCardId.APEX_SLINGSHOT && loadingEvent) {
            return 0.09f;
        }
        if (getCardId() == RogueliteCardId.PERFECT_LAP) {
            return charge * 0.06f;
        }
        if (getCardId() == RogueliteCardId.RACECRAFT_MASTERY && boostTimer > 0f) {
            return 0.09f;
        }
        return 0f;
    }

    @Override
    float slipstreamRangeMultiplier() {
        if (getCardId() == RogueliteCardId.DRAFT_HUNTER) {
            return 1.25f;
        }
        if (getCardId() == RogueliteCardId.SLIPSTREAM_SLINGSHOT) {
            return 1.15f;
        }
        if (getCardId() == RogueliteCardId.RACECRAFT_MASTERY) {
            return 1.28f;
        }
        return 1f;
    }

    @Override
    float slipstreamStrengthMultiplier() {
        if (getCardId() == RogueliteCardId.DRAFT_HUNTER) {
            return 1.25f;
        }
        if (getCardId() == RogueliteCardId.SLIPSTREAM_SLINGSHOT) {
            return 1.14f;
        }
        if (getCardId() == RogueliteCardId.RACECRAFT_MASTERY) {
            return 1.25f;
        }
        return 1f;
    }

    private void updateCornerExit(RogueliteDrivingFrame frame, float strength, float duration) {
        boolean cornering = frame.onRoad && frame.speedRatio >= 0.20f && frame.cornerSeverity >= 0.09f;
        if (cornering) {
            loadingEvent = true;
            return;
        }
        if (loadingEvent && frame.onRoad && frame.throttle > 0.15f && frame.cornerSeverity <= 0.06f) {
            boostStrength = strength;
            boostTimer = duration;
        }
        loadingEvent = false;
    }

    private void updateCleanMomentum(float delta, RogueliteDrivingFrame frame, float buildSeconds) {
        if (frame.onRoad && !frame.recentlyImpacted && frame.speedRatio >= 0.20f) {
            charge = Math.min(1f, charge + delta / buildSeconds);
        } else if (!frame.onRoad || frame.recentlyImpacted) {
            charge = 0f;
        }
    }

    private void updateRecovery(RogueliteDrivingFrame frame) {
        if (frame.routeLength <= 0f) {
            return;
        }
        if (!roadStateInitialized) {
            roadStateInitialized = true;
            wasOnRoad = frame.onRoad;
            return;
        }
        if (wasOnRoad && !frame.onRoad) {
            offRoadStartProgress = frame.routeProgress;
        } else if (!wasOnRoad && frame.onRoad) {
            float routeGain = RogueliteEffectMath.circularDelta(
                    offRoadStartProgress, frame.routeProgress, frame.routeLength);
            if (Math.abs(routeGain) <= frame.safeRecoveryRouteGain) {
                boostStrength = 0.14f;
                boostTimer = 1.45f;
            }
        }
        wasOnRoad = frame.onRoad;
    }

    private void updateDrift(float delta, RogueliteDrivingFrame frame) {
        boolean drifting = frame.onRoad && frame.speedRatio >= 0.24f && frame.slip >= DRIFT_START_SLIP;
        if (drifting) {
            loadingEvent = true;
            charge = Math.min(2.5f, charge + delta);
            return;
        }
        if (loadingEvent && frame.onRoad && frame.slip <= DRIFT_END_SLIP && charge >= 0.25f) {
            float normalized = RogueliteEffectMath.clamp(charge / 2.5f, 0f, 1f);
            boostStrength = normalized * 0.18f;
            boostTimer = 0.65f + normalized * 1.30f;
        }
        if (!frame.onRoad || frame.slip <= DRIFT_END_SLIP) {
            loadingEvent = false;
            charge = 0f;
        }
    }

    private void updateSlipstreamSlingshot(float delta, RogueliteDrivingFrame frame) {
        if (frame.onRoad && frame.slipstreamBoost >= 0.07f) {
            loadingEvent = true;
            charge = Math.min(1f, charge + delta / 1.55f);
            return;
        }
        if (loadingEvent && frame.onRoad && frame.slipstreamBoost <= 0.025f && charge >= 0.15f) {
            boostStrength = charge * 0.20f;
            boostTimer = 0.8f + charge * 1.15f;
        }
        if (!frame.onRoad || frame.slipstreamBoost <= 0.025f) {
            loadingEvent = false;
            charge = 0f;
        }
    }

    private void updateApex(float delta, RogueliteDrivingFrame frame) {
        boolean fastCorner = frame.onRoad && frame.speedRatio >= 0.32f && frame.cornerSeverity >= 0.10f;
        if (fastCorner) {
            loadingEvent = true;
            charge = Math.min(2.4f, charge + delta * (0.65f + frame.cornerSeverity));
            return;
        }
        if (loadingEvent && frame.onRoad && frame.throttle > 0.15f && charge >= 0.25f) {
            float normalized = RogueliteEffectMath.clamp(charge / 2.4f, 0f, 1f);
            boostStrength = normalized * 0.23f;
            boostTimer = 0.75f + normalized * 1.3f;
        }
        loadingEvent = false;
        charge = 0f;
    }

    private void updateRacecraft(float delta, RogueliteDrivingFrame frame) {
        if (!frame.onRoad) {
            loadingEvent = false;
            charge = 0f;
            return;
        }
        if (frame.cornerSeverity >= 0.10f && frame.speedRatio >= 0.25f) {
            loadingEvent = true;
            charge = Math.min(1f, charge + delta / 1.5f);
        } else if (loadingEvent && frame.cornerSeverity <= 0.06f && frame.throttle > 0.15f) {
            boostStrength = 0.20f + charge * 0.08f;
            boostTimer = 1.9f;
            loadingEvent = false;
            charge = 0f;
        }
    }

    private boolean isPositionCatchupCard() {
        return getCardId() == RogueliteCardId.UNDERDOG_INSTINCT
                || getCardId() == RogueliteCardId.COMEBACK_DRIVE
                || getCardId() == RogueliteCardId.LAST_PLACE_FURY;
    }

    private boolean isNearbyRivalCard() {
        return getCardId() == RogueliteCardId.CLOSE_QUARTERS
                || getCardId() == RogueliteCardId.PACK_RACER
                || getCardId() == RogueliteCardId.TRAFFIC_DOMINANCE;
    }

    private boolean isConditionalStatCard() {
        return isPositionCatchupCard() || isNearbyRivalCard();
    }

    private float conditionalStatBonus() {
        if (isPositionCatchupCard()) {
            return positionBonus();
        }
        return nearbyRivalBonus();
    }

    private float positionBonus() {
        if (latestFrame == null) {
            return 0f;
        }
        float maximum;
        switch (getCardId()) {
            case UNDERDOG_INSTINCT:
                maximum = 0.10f;
                break;
            case COMEBACK_DRIVE:
                maximum = 0.15f;
                break;
            case LAST_PLACE_FURY:
                maximum = 0.20f;
                break;
            default:
                return 0f;
        }
        return maximum * latestFrame.racePositionFactor;
    }

    private float nearbyRivalBonus() {
        if (latestFrame == null || latestFrame.nearbyOpponentProximity <= 0.01f) {
            return 0f;
        }
        switch (getCardId()) {
            case CLOSE_QUARTERS:
                return 0.05f;
            case PACK_RACER:
                return 0.10f;
            case TRAFFIC_DOMINANCE:
                return 0.15f;
            default:
                return 0f;
        }
    }
}
