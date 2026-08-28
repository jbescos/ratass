package com.github.jbescos.gameplay.roguelite;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Marks one offender and lets the whole field repeatedly hunt it for lap XP. */
final class FinalReckoningEffect extends RevengeUpgradeEffect {
    static final float PREPARATION_SECONDS = 0f;
    static final float HUNT_DURATION_SECONDS = 15f;
    static final float RAM_TRIGGER_DISTANCE = 5f;
    static final float RAM_COOLDOWN_SECONDS = 1.5f;

    private static final float POWER_BONUS = 0.50f;
    private static final float RECOIL_REDUCTION = 1.50f;
    private static final float PUSH_BONUS = 1.50f;
    private static final float ATTACKER_LAUNCH_SPEED_RATIO = 0.48f;
    private static final float TARGET_PUSH_SPEED_RATIO = 0.72f;

    private final Map<Integer, Float> ramCooldownByVehicleId =
            new HashMap<Integer, Float>();
    private float huntTimeRemaining;
    private float effectMultiplier = 1f;
    private boolean amplificationApplied;

    FinalReckoningEffect() {
        super(RogueliteCardId.FINAL_RECKONING, RevengeWorkflow.TARGET_IMMEDIATE);
    }

    @Override
    boolean isActive() {
        return hasTarget();
    }

    @Override
    boolean isReady() {
        return hasTarget()
                && targetAgeSeconds() >= PREPARATION_SECONDS
                && huntTimeRemaining > 0f;
    }

    @Override
    boolean isArmed() {
        return hasTarget();
    }

    @Override
    float readiness() {
        return hasTarget() ? 1f : 0f;
    }

    @Override
    float activeTimeRemainingSeconds() {
        return huntTimeRemaining;
    }

    @Override
    int activeDisplayPriority() {
        return 9;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        float elapsed = sanitizeElapsed(delta);
        updateRamCooldowns(elapsed);
        if (!hasTarget()) {
            return;
        }

        float previousAge = targetAgeSeconds();
        advanceTargetAge(elapsed);
        float activeElapsed = Math.max(0f, targetAgeSeconds() - PREPARATION_SECONDS)
                - Math.max(0f, previousAge - PREPARATION_SECONDS);
        huntTimeRemaining = Math.max(0f, huntTimeRemaining - activeElapsed);
        if (huntTimeRemaining <= 0f) {
            reset();
        }
    }

    @Override
    float accelerationBonus() {
        return isReady() ? POWER_BONUS * effectMultiplier : 0f;
    }

    @Override
    float frontCollisionRecoilMultiplier() {
        return isReady()
                ? Math.max(0f, 1f - RECOIL_REDUCTION * effectMultiplier)
                : 1f;
    }

    @Override
    float frontCollisionPushMultiplier() {
        return isReady() ? 1f + PUSH_BONUS * effectMultiplier : 1f;
    }

    @Override
    protected void prepareFromHit(int vehicleId, float impactStrength) {
        huntTimeRemaining = HUNT_DURATION_SECONDS;
        effectMultiplier = 1f;
        amplificationApplied = false;
        ramCooldownByVehicleId.clear();
    }

    @Override
    protected boolean isExecutionInProgress() {
        return hasTarget();
    }

    @Override
    protected void onTargetCancelled() {
        resetState();
    }

    @Override
    void amplifyActiveRevenge(float multiplier) {
        if (!hasTarget() || amplificationApplied) {
            return;
        }
        float safeMultiplier = Float.isFinite(multiplier)
                ? Math.max(1f, multiplier)
                : 1f;
        huntTimeRemaining *= safeMultiplier;
        effectMultiplier = safeMultiplier;
        amplificationApplied = true;
    }

    RogueliteRevengeStrike tryActivateHuntRam(
            int rammerVehicleId,
            int targetVehicleId,
            float distance) {
        if (!isReady()
                || rammerVehicleId < 0
                || rammerVehicleId == targetVehicleId
                || !targets(targetVehicleId)
                || !Float.isFinite(distance)
                || distance > RAM_TRIGGER_DISTANCE
                || ramCooldownByVehicleId.containsKey(Integer.valueOf(rammerVehicleId))) {
            return null;
        }
        ramCooldownByVehicleId.put(
                Integer.valueOf(rammerVehicleId),
                Float.valueOf(RAM_COOLDOWN_SECONDS));
        return RogueliteRevengeStrike.hardImpact(
                getCardId(),
                ATTACKER_LAUNCH_SPEED_RATIO * effectMultiplier,
                TARGET_PUSH_SPEED_RATIO * effectMultiplier);
    }

    private void updateRamCooldowns(float elapsed) {
        if (elapsed <= 0f || ramCooldownByVehicleId.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Integer, Float>> iterator =
                ramCooldownByVehicleId.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Float> entry = iterator.next();
            float remaining = entry.getValue().floatValue() - elapsed;
            if (remaining <= 0f) {
                iterator.remove();
            } else {
                entry.setValue(Float.valueOf(remaining));
            }
        }
    }

    private void reset() {
        clearTarget();
        resetState();
    }

    private void resetState() {
        huntTimeRemaining = 0f;
        effectMultiplier = 1f;
        amplificationApplied = false;
        ramCooldownByVehicleId.clear();
    }

    private static float sanitizeElapsed(float delta) {
        return Float.isFinite(delta) ? Math.max(0f, delta) : 0f;
    }
}
