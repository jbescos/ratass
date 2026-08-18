package com.github.jbescos.gameplay.roguelite;

/** Amplifies complete live car statistics when a driving condition is met. */
final class RaceTechniqueEffect extends RogueliteUpgradeEffect {
    private static final int POWER_STAT = 1;
    private static final int GRIP_STAT = 1 << 1;
    private static final int AERO_STAT = 1 << 2;
    private static final int MASS_STAT = 1 << 3;
    private static final float CORNER_SEVERITY_THRESHOLD = 0.10f;
    private static final float CORNER_EXIT_SEVERITY_THRESHOLD = 0.07f;
    private static final float DRIFT_SLIP_THRESHOLD = 0.18f;
    private static final float DRIFT_SPEED_THRESHOLD = 0.20f;
    private static final float SLIPSTREAM_THRESHOLD = 0.04f;

    private final Trigger trigger;
    private final float activeScale;
    private final float activeDuration;
    private float activeTimer;
    private boolean conditionActive;

    RaceTechniqueEffect(RogueliteCardId cardId) {
        super(cardId);
        trigger = triggerFor(cardId);
        int tier = RogueliteCardCatalog.get(cardId).getTier();
        activeScale = activeScaleFor(trigger, tier);
        activeDuration = activeDurationFor(trigger, tier);
    }

    @Override
    boolean isActive() {
        if (isPassiveCard()) {
            return passiveActivationBonus() > 0.001f;
        }
        return activeTimer > 0f;
    }

    @Override
    int activeDisplayPriority() {
        return isPassiveCard() ? 1 : 2;
    }

    @Override
    float activeTimeRemainingSeconds() {
        return activeTimer;
    }

    @Override
    boolean tracksRacePosition() {
        return trigger == Trigger.POSITION;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        activeTimer = Math.max(0f, activeTimer - timerDelta);
        conditionActive = conditionIsActive(frame);
        if (!isPassiveCard() && conditionActive && activeTimer <= 0f) {
            activeTimer = activeDuration;
        }
    }

    @Override
    void observeTechniqueCondition(RogueliteDrivingFrame frame) {
        if (isPassiveCard()) {
            return;
        }
        boolean observedConditionActive = conditionIsActive(frame);
        conditionActive |= observedConditionActive;
        if (observedConditionActive && activeTimer <= 0f) {
            activeTimer = activeDuration;
        }
    }

    @Override
    float accelerationBonus() {
        return isPassiveCard() ? passiveActivationBonus() : 0f;
    }

    @Override
    float gripBonus(float slip) {
        return isPassiveCard() ? passiveActivationBonus() : 0f;
    }

    @Override
    float dragMultiplier() {
        float bonus = (trigger == Trigger.POSITION || trigger == Trigger.NEARBY_RIVAL)
                ? passiveActivationBonus()
                : 0f;
        return bonus <= 0f ? 1f : 1f / (1f + bonus);
    }

    @Override
    float massMultiplier() {
        float bonus = (trigger == Trigger.POSITION || trigger == Trigger.NEARBY_RIVAL)
                ? passiveActivationBonus()
                : 0f;
        return bonus <= 0f ? 1f : 1f - bonus;
    }

    @Override
    float powerDeviationScale() {
        return isTimedActive() && amplifiesPower(getCardId()) ? activeScale : 1f;
    }

    @Override
    float gripDeviationScale() {
        return isTimedActive() && amplifiesGrip(getCardId()) ? activeScale : 1f;
    }

    @Override
    float aeroDeviationScale() {
        return isTimedActive() && amplifiesAero(getCardId()) ? activeScale : 1f;
    }

    @Override
    float massDeviationScale() {
        return isTimedActive() && amplifiesMass(getCardId()) ? activeScale : 1f;
    }

    private boolean conditionIsActive(RogueliteDrivingFrame frame) {
        switch (trigger) {
            case CORNER:
                return frame.onRoad
                        && frame.speedRatio >= 0.18f
                        && frame.cornerSeverity
                                >= (conditionActive
                                        ? CORNER_EXIT_SEVERITY_THRESHOLD
                                        : CORNER_SEVERITY_THRESHOLD);
            case SLIPSTREAM:
                return frame.onRoad && frame.slipstreamBoost >= SLIPSTREAM_THRESHOLD;
            case LONG_STRAIGHT:
                return frame.longStraight;
            case DRIFT:
                return frame.onRoad
                        && frame.speedRatio >= DRIFT_SPEED_THRESHOLD
                        && frame.slip >= DRIFT_SLIP_THRESHOLD;
            case OFF_ROAD:
                return !frame.onRoad;
            default:
                return false;
        }
    }

    private boolean isTimedActive() {
        return !isPassiveCard() && isActive();
    }

    private boolean isPassiveCard() {
        return trigger == Trigger.POSITION || trigger == Trigger.NEARBY_RIVAL;
    }

    private float passiveActivationBonus() {
        if (trigger == Trigger.POSITION) {
            return positionBonus();
        }
        if (trigger == Trigger.NEARBY_RIVAL) {
            return nearbyRivalBonus();
        }
        return 0f;
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
                maximum = 0.25f;
                break;
            case LAST_PLACE_FURY:
                maximum = 0.50f;
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
                return 0.20f;
            default:
                return 0f;
        }
    }

    static float tuningBaselineScore(RogueliteCardId tuningCardId) {
        if (tuningCardId == null
                || RogueliteCardCatalog.get(tuningCardId).getSlotType()
                        != RogueliteSlotType.TUNING) {
            return Float.NaN;
        }
        if (isTechniqueAmplifierTuning(tuningCardId)) {
            return 0f;
        }
        TieredTuningEffect tuning = new TieredTuningEffect(tuningCardId);
        return performanceScore(
                1f + tuning.accelerationBonus(),
                1f + tuning.gripBonus(0f),
                1f / tuning.dragMultiplier(),
                tuning.massMultiplier());
    }

    static float tuningTechniqueScore(
            RogueliteCardId tuningCardId,
            RogueliteCardId techniqueCardId) {
        if (techniqueCardId == null
                || RogueliteCardCatalog.get(techniqueCardId).getSlotType()
                        != RogueliteSlotType.TECHNIQUE) {
            return Float.NaN;
        }
        if (isPowerupAmplifierTechnique(techniqueCardId)) {
            float baseline = tuningBaselineScore(tuningCardId);
            if (Float.isNaN(baseline)) {
                return Float.NaN;
            }
            float tuningMultiplier = isTechniqueAmplifierTuning(tuningCardId)
                    ? techniqueAmplifier(tuningCardId)
                    : 1f;
            float powerupMultiplier = powerupAmplifier(techniqueCardId);
            float effectiveMultiplier = RogueliteEffectMath.amplifyDeviation(
                    powerupMultiplier,
                    tuningMultiplier);
            // Effect strength and cooldown recovery are separate benefits.
            return baseline + (effectiveMultiplier - 1f) * 2f;
        }
        Trigger techniqueTrigger = triggerFor(techniqueCardId);
        if (techniqueTrigger == Trigger.POSITION
                || techniqueTrigger == Trigger.NEARBY_RIVAL) {
            float baseline = tuningBaselineScore(tuningCardId);
            if (Float.isNaN(baseline)) {
                return Float.NaN;
            }
            float multiplier = isTechniqueAmplifierTuning(tuningCardId)
                    ? techniqueAmplifier(tuningCardId)
                    : 1f;
            return baseline + passiveTechniqueScore(techniqueCardId) * multiplier;
        }

        float baseline = tuningBaselineScore(tuningCardId);
        if (Float.isNaN(baseline)) {
            return Float.NaN;
        }
        if (isTechniqueAmplifierTuning(tuningCardId)) {
            return baseline;
        }
        TieredTuningEffect tuning = new TieredTuningEffect(tuningCardId);
        float power = 1f + tuning.accelerationBonus();
        float grip = 1f + tuning.gripBonus(0f);
        float aero = 1f / tuning.dragMultiplier();
        float mass = tuning.massMultiplier();
        float scale = activeScaleFor(
                techniqueTrigger,
                RogueliteCardCatalog.get(techniqueCardId).getTier());

        if (amplifiesPower(techniqueCardId)) {
            power = RogueliteEffectMath.amplifyDeviation(power, scale);
        }
        if (amplifiesGrip(techniqueCardId)) {
            grip = RogueliteEffectMath.amplifyPositiveDeviation(grip, scale);
        }
        if (amplifiesAero(techniqueCardId)) {
            aero = RogueliteEffectMath.amplifyDeviation(aero, scale);
        }
        if (amplifiesMass(techniqueCardId)) {
            mass = RogueliteEffectMath.amplifyDeviation(mass, scale);
        }
        return performanceScore(power, grip, aero, mass);
    }

    private static float passiveTechniqueScore(RogueliteCardId cardId) {
        float bonus;
        switch (cardId) {
            case UNDERDOG_INSTINCT:
                bonus = 0.10f;
                break;
            case COMEBACK_DRIVE:
                bonus = 0.25f;
                break;
            case LAST_PLACE_FURY:
                bonus = 0.50f;
                break;
            case CLOSE_QUARTERS:
                bonus = 0.05f;
                break;
            case PACK_RACER:
                bonus = 0.10f;
                break;
            case TRAFFIC_DOMINANCE:
                bonus = 0.20f;
                break;
            default:
                return 0f;
        }
        // These passive cards improve power, grip, aero, and mass together.
        return bonus * 4f;
    }

    private static float activeScaleFor(Trigger trigger, int tier) {
        if (tier <= 0) {
            return 1f;
        }
        if (trigger == Trigger.SLIPSTREAM) {
            return tier == 1 ? 2f : tier == 2 ? 3f : 4f;
        }
        if (trigger == Trigger.OFF_ROAD) {
            return tier == 1 ? 2f : tier == 2 ? 3f : 4f;
        }
        return tier == 1 ? 1.5f : tier == 2 ? 2f : 3f;
    }

    private static float activeDurationFor(Trigger trigger, int tier) {
        if (trigger == Trigger.SLIPSTREAM || trigger == Trigger.OFF_ROAD) {
            return 10f;
        }
        if (trigger == Trigger.CORNER) {
            return 1f + Math.max(0, tier);
        }
        return 2f + Math.max(0, tier);
    }

    private static boolean amplifiesPower(RogueliteCardId cardId) {
        return (amplifiedStatMask(cardId) & POWER_STAT) != 0;
    }

    private static boolean amplifiesGrip(RogueliteCardId cardId) {
        return (amplifiedStatMask(cardId) & GRIP_STAT) != 0;
    }

    private static boolean amplifiesAero(RogueliteCardId cardId) {
        return (amplifiedStatMask(cardId) & AERO_STAT) != 0;
    }

    private static boolean amplifiesMass(RogueliteCardId cardId) {
        return (amplifiedStatMask(cardId) & MASS_STAT) != 0;
    }

    static int amplifiedStatMask(RogueliteCardId cardId) {
        switch (cardId) {
            case DRAFT_FOCUS:
            case DRAFT_EXPERT:
            case DRAFT_MASTER:
            case STRAIGHT_FOCUS:
            case STRAIGHT_EXPERT:
            case STRAIGHT_MASTER:
                return POWER_STAT | AERO_STAT;
            case DRIFT_FOCUS:
            case DRIFT_EXPERT:
            case DRIFT_MASTER:
            case SPRINT_FOCUS:
            case SPRINT_EXPERT:
            case SPRINT_MASTER:
                return POWER_STAT | MASS_STAT;
            case CORNER_FOCUS:
            case CORNER_EXPERT:
            case CORNER_MASTER:
                return GRIP_STAT | AERO_STAT;
            case TRACTION_FOCUS:
            case TRACTION_EXPERT:
            case TRACTION_MASTER:
                return POWER_STAT | GRIP_STAT;
            case AGILITY_FOCUS:
            case AGILITY_EXPERT:
            case AGILITY_MASTER:
                return GRIP_STAT | MASS_STAT;
            case RALLY_FOCUS:
            case RALLY_EXPERT:
            case RALLY_MASTER:
                return POWER_STAT | GRIP_STAT | AERO_STAT | MASS_STAT;
            case APEX_FOCUS:
            case APEX_EXPERT:
            case APEX_MASTER:
            case SLIDE_FOCUS:
            case SLIDE_EXPERT:
            case SLIDE_MASTER:
                return AERO_STAT | MASS_STAT;
            default:
                return 0;
        }
    }

    private static boolean isTechniqueAmplifierTuning(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.TECHNIQUE_COUPLER
                || cardId == RogueliteCardId.TECHNIQUE_MATRIX
                || cardId == RogueliteCardId.TECHNIQUE_SINGULARITY;
    }

    private static boolean isPowerupAmplifierTechnique(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.POWERUP_LINK
                || cardId == RogueliteCardId.POWERUP_MATRIX
                || cardId == RogueliteCardId.POWERUP_NEXUS;
    }

    private static float techniqueAmplifier(RogueliteCardId cardId) {
        return new TechniqueAmplifierTuningEffect(cardId).techniqueEffectMultiplier();
    }

    private static float powerupAmplifier(RogueliteCardId cardId) {
        return new PowerupAmplifierTechniqueEffect(cardId).powerupEffectMultiplier();
    }

    private static float performanceScore(
            float power,
            float grip,
            float aero,
            float mass) {
        return (power - 1f)
                + (grip - 1f)
                + (aero - 1f)
                + (1f - mass);
    }

    private static Trigger triggerFor(RogueliteCardId cardId) {
        switch (cardId) {
            case CORNER_FOCUS:
            case CORNER_EXPERT:
            case CORNER_MASTER:
            case APEX_FOCUS:
            case APEX_EXPERT:
            case APEX_MASTER:
            case TRACTION_FOCUS:
            case TRACTION_EXPERT:
            case TRACTION_MASTER:
            case AGILITY_FOCUS:
            case AGILITY_EXPERT:
            case AGILITY_MASTER:
                return Trigger.CORNER;
            case DRAFT_FOCUS:
            case DRAFT_EXPERT:
            case DRAFT_MASTER:
                return Trigger.SLIPSTREAM;
            case STRAIGHT_FOCUS:
            case STRAIGHT_EXPERT:
            case STRAIGHT_MASTER:
            case SPRINT_FOCUS:
            case SPRINT_EXPERT:
            case SPRINT_MASTER:
                return Trigger.LONG_STRAIGHT;
            case DRIFT_FOCUS:
            case DRIFT_EXPERT:
            case DRIFT_MASTER:
            case SLIDE_FOCUS:
            case SLIDE_EXPERT:
            case SLIDE_MASTER:
                return Trigger.DRIFT;
            case RALLY_FOCUS:
            case RALLY_EXPERT:
            case RALLY_MASTER:
                return Trigger.OFF_ROAD;
            case UNDERDOG_INSTINCT:
            case COMEBACK_DRIVE:
            case LAST_PLACE_FURY:
                return Trigger.POSITION;
            case CLOSE_QUARTERS:
            case PACK_RACER:
            case TRAFFIC_DOMINANCE:
                return Trigger.NEARBY_RIVAL;
            default:
                throw new IllegalArgumentException("Unsupported technique card: " + cardId);
        }
    }

    private enum Trigger {
        CORNER,
        SLIPSTREAM,
        LONG_STRAIGHT,
        DRIFT,
        OFF_ROAD,
        POSITION,
        NEARBY_RIVAL
    }
}
