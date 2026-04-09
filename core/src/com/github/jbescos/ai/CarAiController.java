package com.github.jbescos.ai;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;
import com.github.jbescos.gameplay.ArenaMap;

public final class CarAiController {
    private static final float EPSILON = 0.001f;
    private static final float TARGET_LEAD_DISTANCE_FACTOR = 10f;
    private static final float MIN_TARGET_LEAD_TIME = 0.14f;
    private static final float INTERCEPT_CLAMP_MARGIN = 0.7f;
    private static final float MIN_STAGING_DISTANCE = 1.45f;
    private static final float MAX_STAGING_DISTANCE = 2.55f;
    private static final float CENTER_ASSIST_TARGET_EDGE_THRESHOLD = 3.4f;
    private static final float CENTER_ASSIST_SELF_EDGE_THRESHOLD = 3f;
    private static final float EMERGENCY_RECOVERY_EDGE_DISTANCE = 1.1f;
    private static final float EMERGENCY_RECOVERY_THROTTLE = 1f;
    private static final float RECOVERY_REVERSE_THROTTLE = -0.58f;
    private static final float ATTACK_REVERSE_THROTTLE = -0.42f;
    private static final float FULL_ATTACK_THROTTLE = 1f;
    private static final float CHARGE_ATTACK_THROTTLE = 0.82f;
    private static final float LOW_SPEED_PIVOT_SPEED_SQ = 1.1f;
    private static final float LOW_SPEED_PIVOT_THROTTLE = 0.18f;
    private static final float STUCK_PROGRESS_SPEED_SQ = 4.2f;
    private static final float STUCK_PROGRESS_DISTANCE_SQ = 0.48f;
    private static final float STUCK_PROGRESS_RESET_DISTANCE_SQ = 1.44f;
    private static final float STUCK_MIN_COMMAND_THROTTLE = 0.45f;
    private static final float STUCK_DISENGAGE_DURATION = 0.62f;
    private static final float STUCK_CHARGE_DURATION = 0.95f;
    private static final float DISENGAGE_REVERSE_THROTTLE = -1f;
    private static final float DISENGAGE_TURN_GAIN_MULTIPLIER = 1.18f;
    private static final float CHARGE_TURN_GAIN_MULTIPLIER = 1.20f;
    private static final float STUCK_RESET_DURATION = 1.1f;
    private static final float OSCILLATION_LOW_SPEED_SQ = 2.2f;
    private static final float OSCILLATION_DIRECTION_THROTTLE = LOW_SPEED_PIVOT_THROTTLE * 0.8f;
    private static final float OSCILLATION_TRIGGER_WINDOW = 1.2f;
    private static final float OSCILLATION_TRIGGER_DISTANCE_SQ = 1.05f;
    private static final int OSCILLATION_TRIGGER_FLIPS = 3;
    private static final float OSCILLATION_ESCAPE_DURATION = 0.95f;
    private static final float OSCILLATION_ESCAPE_THROTTLE = 0.88f;
    private static final float NAVIGATION_STALL_SPEED_SQ = 2.4f;
    private static final float NAVIGATION_STALL_DISTANCE_SQ = 1.25f;
    private static final float NAVIGATION_SWITCH_DISTANCE_SQ = 5.4f;
    private static final float NAVIGATION_SWITCH_WINDOW = 1.3f;
    private static final int NAVIGATION_SWITCH_TRIGGER_COUNT = 2;
    private static final float NAVIGATION_COMMIT_DURATION = 1.05f;
    private static final float NAVIGATION_COMMIT_REACHED_DISTANCE_SQ = 0.40f;
    private static final float NAVIGATION_MARGIN = 0.52f;
    private static final float POWER_PICKUP_EDGE_MARGIN = 1.05f;
    private static final float POINT_PICKUP_TARGET_EDGE_OVERRIDE_DISTANCE = 2.35f;
    private static final float POINT_PICKUP_ATTACK_OVERRIDE_DISTANCE_SQ = 12f;
    private static final float BOOSTED_TARGET_BONUS = 1.85f;
    private static final float RAM_CHARGE_TARGET_BONUS = 1.35f;
    private static final float LEADER_TARGET_BONUS = 1.75f;
    private static final float GRUDGE_TARGET_BONUS = 2.65f;
    private static final float DOGPILE_TARGET_BONUS = 1.85f;
    private static final float FINISH_HIM_BONUS = 1.10f;
    private static final float EDGE_ATTACK_COMMIT_DISTANCE = 2.55f;
    private static final float EDGE_VULNERABILITY_DISTANCE = 2.9f;
    private static final float EDGE_VULNERABILITY_BONUS = 2.4f;
    private static final float EDGE_OUTWARD_SPEED_BONUS = 0.9f;

    private final AiDrivingPersonality personality;
    private final AiControlDecision decision = new AiControlDecision();
    private final Vector2 forwardAxis = new Vector2();
    private final Vector2 desiredVector = new Vector2();
    private final Vector2 centerBias = new Vector2();
    private final Vector2 targetLead = new Vector2();
    private final Vector2 attackVector = new Vector2();
    private final Vector2 interceptPoint = new Vector2();
    private final Vector2 arenaFocus = new Vector2();
    private final Vector2 working = new Vector2();
    private final Vector2 stuckAnchor = new Vector2();
    private final Vector2 oscillationAnchor = new Vector2();
    private final Vector2 previousNavigationTarget = new Vector2();
    private final Vector2 committedNavigationTarget = new Vector2();
    private final Vector2 navigationAnchor = new Vector2();

    private float stuckTimer;
    private float disengageTimer;
    private float chargeTimer;
    private float navigationSwitchTimer;
    private float navigationCommitTimer;
    private float oscillationFlipTimer;
    private float oscillationEscapeTimer;
    private float stuckTurnDirection = 1f;
    private int lastThrottleSign;
    private int navigationSwitchCount;
    private int oscillationFlipCount;
    private boolean stuckAnchorInitialized;
    private boolean previousNavigationTargetInitialized;
    private boolean navigationAnchorInitialized;
    private boolean committedNavigationTargetInitialized;
    private boolean oscillationAnchorInitialized;

    public CarAiController(AiDrivingPersonality personality) {
        this.personality = personality == null ? AiDrivingPersonalities.BALANCED : personality;
    }

    public AiDrivingPersonality getPersonality() {
        return personality;
    }

    public AiControlDecision plan(
            float delta,
            AiVehicleView self,
            ArenaMap arenaMap,
            Array<? extends AiVehicleView> vehicles,
            boolean growthPickupActive,
            Vector2 growthPickupPosition,
            boolean pointPickupActive,
            Vector2 pointPickupPosition) {
        if (self == null || !self.isActive()) {
            clearAttackResetState();
            return decision.set(0f, 0f);
        }

        Body body = self.getBody();
        if (body == null) {
            clearAttackResetState();
            return decision.set(0f, 0f);
        }

        advanceAttackResetState(delta);

        arenaMap.getFocusPoint(arenaFocus);
        boolean chaseGrowthPickup =
                growthPickupActive
                        && growthPickupPosition != null
                        && !self.hasGrowthBoost()
                        && arenaMap.distanceToHazard(growthPickupPosition) >= POWER_PICKUP_EDGE_MARGIN;

        AiVehicleView target = null;
        Body targetBody = null;
        if (!chaseGrowthPickup) {
            target = findTarget(self, body, vehicles, arenaMap, arenaFocus);
            if (target != null) {
                targetBody = target.getBody();
            }
        }

        Vector2 position = body.getPosition();
        if (!stuckAnchorInitialized) {
            stuckAnchor.set(position);
            stuckAnchorInitialized = true;
        }
        if (!navigationAnchorInitialized) {
            navigationAnchor.set(position);
            navigationAnchorInitialized = true;
        }
        float nearestHazard = arenaMap.distanceToHazard(position);
        float awayFromSafetyVelocity = 0f;

        arenaMap.findRecoveryPoint(position, centerBias);
        centerBias.sub(position);
        if (!centerBias.isZero(EPSILON)) {
            centerBias.nor();
            awayFromSafetyVelocity = -centerBias.dot(body.getLinearVelocity());
        }

        boolean recovering =
                nearestHazard < personality.recoveryEdgeDistance
                        || (nearestHazard < personality.cautionEdgeDistance
                        && awayFromSafetyVelocity > personality.outwardVelocityThreshold);

        boolean chasePointPickup =
                !chaseGrowthPickup
                        && pointPickupActive
                        && pointPickupPosition != null
                        && !self.hasRamCharge()
                        && arenaMap.distanceToHazard(pointPickupPosition) >= POWER_PICKUP_EDGE_MARGIN
                        && nearestHazard >= personality.recoveryEdgeDistance + 0.15f;

        float targetDistanceSq = Float.MAX_VALUE;
        if (chasePointPickup) {
            if (targetBody != null) {
                targetDistanceSq = position.dst2(targetBody.getPosition());
                float targetEdgeDistance = arenaMap.distanceToHazard(targetBody.getPosition());
                boolean targetPrime =
                        target.hasGrowthBoost()
                                || targetEdgeDistance < POINT_PICKUP_TARGET_EDGE_OVERRIDE_DISTANCE;
                if (targetPrime && targetDistanceSq <= POINT_PICKUP_ATTACK_OVERRIDE_DISTANCE_SQ) {
                    chasePointPickup = false;
                }
            }
        }

        if (recovering || chaseGrowthPickup || chasePointPickup) {
            clearAttackResetState();
        }

        if (!recovering && !chaseGrowthPickup && !chasePointPickup && targetBody == null) {
            clearAttackResetState();
            return decision.set(0f, 0f);
        }

        boolean disengaging = false;
        boolean charging = false;
        boolean escapingOscillation = oscillationEscapeTimer > 0f;

        if (recovering) {
            arenaMap.findRecoveryPoint(position, desiredVector);
        } else if (chaseGrowthPickup) {
            desiredVector.set(growthPickupPosition);
            arenaMap.clampToPlayable(desiredVector, INTERCEPT_CLAMP_MARGIN);
        } else if (chasePointPickup) {
            desiredVector.set(pointPickupPosition);
            arenaMap.clampToPlayable(desiredVector, INTERCEPT_CLAMP_MARGIN);
        } else {
            Vector2 targetPosition = targetBody.getPosition();
            targetDistanceSq = position.dst2(targetPosition);
            float leadTime = MathUtils.clamp(
                    position.dst(targetPosition) / TARGET_LEAD_DISTANCE_FACTOR,
                    MIN_TARGET_LEAD_TIME,
                    personality.maxTargetLeadTime);
            interceptPoint.set(targetPosition).mulAdd(targetBody.getLinearVelocity(), leadTime);
            arenaMap.clampToPlayable(interceptPoint, INTERCEPT_CLAMP_MARGIN);

            arenaMap.findRecoveryPoint(targetPosition, centerBias);
            attackVector.set(targetPosition).sub(centerBias);
            if (attackVector.isZero(EPSILON)) {
                attackVector.set(targetPosition).sub(arenaFocus);
            }
            if (attackVector.isZero(EPSILON)) {
                attackVector.set(interceptPoint).sub(position);
            }
            if (attackVector.isZero(EPSILON)) {
                attackVector.set(0f, 1f);
            } else {
                attackVector.nor();
            }

            targetLead.set(interceptPoint).sub(position);
            float pushAlignment = 0f;
            if (!targetLead.isZero(EPSILON)) {
                targetLead.nor();
                pushAlignment = targetLead.dot(attackVector);
            }

            float targetEdge = arenaMap.distanceToHazard(targetPosition);
            boolean commitPush =
                    targetEdge < EDGE_ATTACK_COMMIT_DISTANCE
                            || (pushAlignment > personality.commitAlignmentThreshold
                            && position.dst2(targetPosition) < personality.commitDistanceSq);

            disengaging = disengageTimer > 0f;
            charging = !disengaging && chargeTimer > 0f;

            if (disengaging) {
                desiredVector.set(targetPosition);
            } else if (commitPush) {
                desiredVector.set(interceptPoint)
                        .mulAdd(attackVector, personality.commitPushOffsetDistance);
            } else {
                float stagingDistance = MathUtils.clamp(
                        personality.stagingDistance
                                + targetBody.getLinearVelocity().len()
                                * personality.stagingVelocityFactor,
                        MIN_STAGING_DISTANCE,
                        MAX_STAGING_DISTANCE);
                desiredVector.set(interceptPoint)
                        .mulAdd(attackVector, -stagingDistance);

                if (targetEdge > CENTER_ASSIST_TARGET_EDGE_THRESHOLD
                        && nearestHazard > CENTER_ASSIST_SELF_EDGE_THRESHOLD) {
                    arenaMap.findRecoveryPoint(interceptPoint, centerBias);
                    centerBias.sub(interceptPoint);
                    if (!centerBias.isZero(EPSILON)) {
                        centerBias.nor().scl(personality.centerAssistWeight);
                        desiredVector.add(centerBias);
                    }
                }
            }

            arenaMap.clampToPlayable(desiredVector, INTERCEPT_CLAMP_MARGIN);
        }

        arenaMap.findDriveTarget(position, desiredVector, NAVIGATION_MARGIN, desiredVector);
        applyNavigationCommit(position, arenaMap, desiredVector, body.getLinearVelocity().len2());
        desiredVector.sub(position);
        if (desiredVector.isZero(EPSILON)) {
            desiredVector.set(arenaFocus).sub(position);
            if (desiredVector.isZero(EPSILON)) {
                desiredVector.set(0f, 1f);
            }
        }

        float currentHeading = body.getAngle() + MathUtils.HALF_PI;
        float desiredHeading = desiredVector.angleRad();
        float angleError = desiredHeading - currentHeading;
        angleError = MathUtils.atan2(MathUtils.sin(angleError), MathUtils.cos(angleError));

        float turnGain = recovering ? personality.recoveryTurnGain : personality.attackTurnGain;
        if (disengaging) {
            turnGain *= DISENGAGE_TURN_GAIN_MULTIPLIER;
        } else if (charging) {
            turnGain *= CHARGE_TURN_GAIN_MULTIPLIER;
        }
        float turn = MathUtils.clamp(angleError * turnGain, -1f, 1f);
        float speedSq = body.getLinearVelocity().len2();

        float throttle;
        if (recovering) {
            if (Math.abs(angleError) > personality.recoveryReverseAngle) {
                throttle = speedSq <= LOW_SPEED_PIVOT_SPEED_SQ
                        ? LOW_SPEED_PIVOT_THROTTLE
                        : RECOVERY_REVERSE_THROTTLE;
            } else if (nearestHazard < EMERGENCY_RECOVERY_EDGE_DISTANCE) {
                throttle = EMERGENCY_RECOVERY_THROTTLE;
            } else {
                throttle = personality.recoveryThrottle;
            }
        } else if (disengaging) {
            throttle = DISENGAGE_REVERSE_THROTTLE;
        } else if (charging) {
            if (Math.abs(angleError) > personality.reverseAttackAngle) {
                throttle = CHARGE_ATTACK_THROTTLE;
            } else if (Math.abs(angleError) > personality.cautiousAttackAngle) {
                throttle = Math.max(personality.cautiousAttackThrottle, CHARGE_ATTACK_THROTTLE);
            } else {
                throttle = FULL_ATTACK_THROTTLE;
            }
        } else if (Math.abs(angleError) > personality.reverseAttackAngle) {
            throttle = speedSq <= LOW_SPEED_PIVOT_SPEED_SQ
                    ? LOW_SPEED_PIVOT_THROTTLE
                    : ATTACK_REVERSE_THROTTLE;
        } else if (Math.abs(angleError) > personality.cautiousAttackAngle) {
            throttle = personality.cautiousAttackThrottle;
        } else if (desiredVector.len2() < personality.closeTargetDistanceSq) {
            throttle = personality.closeTargetThrottle;
        } else {
            throttle = FULL_ATTACK_THROTTLE;
        }

        boolean stuckEligible =
                !chaseGrowthPickup
                        && !chasePointPickup
                        && (recovering || target != null)
                        && disengageTimer <= 0f
                        && chargeTimer <= 0f
                        && Math.abs(throttle) >= STUCK_MIN_COMMAND_THROTTLE
                        && body.getLinearVelocity().len2() < STUCK_PROGRESS_SPEED_SQ;
        if (stuckEligible) {
            float stuckDisplacementSq = position.dst2(stuckAnchor);
            if (stuckDisplacementSq >= STUCK_PROGRESS_RESET_DISTANCE_SQ) {
                resetStuckProgress(position);
            } else if (stuckDisplacementSq <= STUCK_PROGRESS_DISTANCE_SQ) {
                stuckTimer += delta;
            } else {
                stuckTimer = Math.max(0f, stuckTimer - delta * 0.5f);
            }
        } else {
            resetStuckProgress(position);
        }

        if (stuckEligible && stuckTimer > personality.stuckDuration && chargeTimer <= 0f) {
            engageStuckRecovery(turn, position);
            disengaging = true;
            charging = false;
        } else if (stuckTimer > STUCK_RESET_DURATION) {
            resetStuckProgress(position);
        }

        if (disengaging) {
            throttle = personality.stuckReverseThrottle;
            turn = stuckTurnDirection;
        } else if (charging) {
            throttle = FULL_ATTACK_THROTTLE;
            turn = stuckTurnDirection;
        }

        updateOscillationState(
                delta,
                position,
                body.getLinearVelocity().len2(),
                throttle,
                recovering,
                chaseGrowthPickup,
                chasePointPickup,
                disengaging,
                charging);

        if (oscillationEscapeTimer > 0f) {
            throttle = OSCILLATION_ESCAPE_THROTTLE;
            turn = stuckTurnDirection;
            escapingOscillation = true;
        }

        if (escapingOscillation) {
            resetStuckProgress(position);
        }

        return decision.set(throttle, turn);
    }

    private void clearAttackResetState() {
        stuckTimer = 0f;
        disengageTimer = 0f;
        chargeTimer = 0f;
        navigationSwitchTimer = 0f;
        navigationCommitTimer = 0f;
        oscillationFlipTimer = 0f;
        oscillationEscapeTimer = 0f;
        stuckTurnDirection = 1f;
        lastThrottleSign = 0;
        navigationSwitchCount = 0;
        oscillationFlipCount = 0;
        stuckAnchorInitialized = false;
        previousNavigationTargetInitialized = false;
        navigationAnchorInitialized = false;
        committedNavigationTargetInitialized = false;
        oscillationAnchorInitialized = false;
    }

    private void advanceAttackResetState(float delta) {
        boolean wasDisengaging = disengageTimer > 0f;
        if (disengageTimer > 0f) {
            disengageTimer = Math.max(0f, disengageTimer - delta);
        }
        if (wasDisengaging && disengageTimer == 0f) {
            chargeTimer = Math.max(chargeTimer, STUCK_CHARGE_DURATION);
        }
        if (chargeTimer > 0f) {
            chargeTimer = Math.max(0f, chargeTimer - delta);
        }
        if (navigationSwitchTimer > 0f) {
            navigationSwitchTimer = Math.max(0f, navigationSwitchTimer - delta);
            if (navigationSwitchTimer == 0f) {
                navigationSwitchCount = 0;
            }
        }
        if (navigationCommitTimer > 0f) {
            navigationCommitTimer = Math.max(0f, navigationCommitTimer - delta);
            if (navigationCommitTimer == 0f) {
                committedNavigationTargetInitialized = false;
            }
        }
        if (oscillationFlipTimer > 0f) {
            oscillationFlipTimer = Math.max(0f, oscillationFlipTimer - delta);
            if (oscillationFlipTimer == 0f) {
                oscillationFlipCount = 0;
            }
        }
        if (oscillationEscapeTimer > 0f) {
            oscillationEscapeTimer = Math.max(0f, oscillationEscapeTimer - delta);
        }
    }

    private void engageStuckRecovery(float suggestedTurn, Vector2 position) {
        disengageTimer = STUCK_DISENGAGE_DURATION;
        chargeTimer = 0f;
        if (Math.abs(suggestedTurn) >= 0.2f) {
            stuckTurnDirection = Math.signum(suggestedTurn);
        } else {
            stuckTurnDirection = stuckTurnDirection > 0f ? -1f : 1f;
        }
        resetStuckProgress(position);
    }

    private void resetStuckProgress(Vector2 position) {
        stuckTimer = 0f;
        if (position == null) {
            stuckAnchorInitialized = false;
            return;
        }
        stuckAnchor.set(position);
        stuckAnchorInitialized = true;
    }

    private void updateOscillationState(
            float delta,
            Vector2 position,
            float speedSq,
            float throttle,
            boolean recovering,
            boolean chaseGrowthPickup,
            boolean chasePointPickup,
            boolean disengaging,
            boolean charging) {
        if (position == null) {
            oscillationAnchorInitialized = false;
            lastThrottleSign = 0;
            return;
        }

        if (!oscillationAnchorInitialized) {
            oscillationAnchor.set(position);
            oscillationAnchorInitialized = true;
        }

        boolean eligible =
                !recovering
                        && !chaseGrowthPickup
                        && !chasePointPickup
                        && !disengaging
                        && !charging
                        && oscillationEscapeTimer <= 0f
                        && speedSq <= OSCILLATION_LOW_SPEED_SQ
                        && position.dst2(oscillationAnchor) <= OSCILLATION_TRIGGER_DISTANCE_SQ;

        if (!eligible) {
            if (position.dst2(oscillationAnchor) > OSCILLATION_TRIGGER_DISTANCE_SQ) {
                oscillationAnchor.set(position);
            }
            lastThrottleSign = toOscillationThrottleSign(throttle);
            if (oscillationFlipTimer == 0f) {
                oscillationFlipCount = 0;
            }
            return;
        }

        int throttleSign = toOscillationThrottleSign(throttle);
        if (throttleSign == 0) {
            return;
        }

        if (lastThrottleSign != 0 && throttleSign != lastThrottleSign) {
            oscillationFlipCount++;
            oscillationFlipTimer = OSCILLATION_TRIGGER_WINDOW;
            if (oscillationFlipCount >= OSCILLATION_TRIGGER_FLIPS) {
                oscillationEscapeTimer = OSCILLATION_ESCAPE_DURATION;
                oscillationFlipCount = 0;
                oscillationFlipTimer = 0f;
                stuckTurnDirection = stuckTurnDirection > 0f ? -1f : 1f;
                oscillationAnchor.set(position);
                lastThrottleSign = 0;
                return;
            }
        }

        if (position.dst2(oscillationAnchor) > OSCILLATION_TRIGGER_DISTANCE_SQ * 0.55f) {
            oscillationAnchor.set(position);
            oscillationFlipCount = Math.max(0, oscillationFlipCount - 1);
        }

        lastThrottleSign = throttleSign;
    }

    private int toOscillationThrottleSign(float throttle) {
        return Math.abs(throttle) >= OSCILLATION_DIRECTION_THROTTLE
                ? (int) Math.signum(throttle)
                : 0;
    }

    private void applyNavigationCommit(
            Vector2 position,
            ArenaMap arenaMap,
            Vector2 proposedTarget,
            float speedSq) {
        if (position == null || proposedTarget == null) {
            previousNavigationTargetInitialized = false;
            committedNavigationTargetInitialized = false;
            navigationAnchorInitialized = false;
            return;
        }

        if (!navigationAnchorInitialized) {
            navigationAnchor.set(position);
            navigationAnchorInitialized = true;
        }

        if (!previousNavigationTargetInitialized) {
            previousNavigationTarget.set(proposedTarget);
            previousNavigationTargetInitialized = true;
            navigationAnchor.set(position);
            return;
        }

        if (navigationCommitTimer > 0f && committedNavigationTargetInitialized) {
            if (position.dst2(committedNavigationTarget) <= NAVIGATION_COMMIT_REACHED_DISTANCE_SQ) {
                navigationCommitTimer = 0f;
                committedNavigationTargetInitialized = false;
            } else {
                arenaMap.findDriveTarget(position, committedNavigationTarget, NAVIGATION_MARGIN, proposedTarget);
                previousNavigationTarget.set(proposedTarget);
                return;
            }
        }

        boolean stalled =
                speedSq <= NAVIGATION_STALL_SPEED_SQ
                        && position.dst2(navigationAnchor) <= NAVIGATION_STALL_DISTANCE_SQ;
        if (stalled && proposedTarget.dst2(previousNavigationTarget) >= NAVIGATION_SWITCH_DISTANCE_SQ) {
            navigationSwitchCount++;
            navigationSwitchTimer = NAVIGATION_SWITCH_WINDOW;
            if (navigationSwitchCount >= NAVIGATION_SWITCH_TRIGGER_COUNT) {
                committedNavigationTarget.set(proposedTarget);
                committedNavigationTargetInitialized = true;
                navigationCommitTimer = NAVIGATION_COMMIT_DURATION;
                navigationSwitchCount = 0;
                navigationSwitchTimer = 0f;
                navigationAnchor.set(position);
            }
        } else if (position.dst2(navigationAnchor) > NAVIGATION_STALL_DISTANCE_SQ) {
            navigationAnchor.set(position);
            navigationSwitchCount = 0;
            navigationSwitchTimer = 0f;
        }

        previousNavigationTarget.set(proposedTarget);
    }

    private float computeOutwardVelocity(ArenaMap arenaMap, Vector2 position, Body body) {
        arenaMap.findRecoveryPoint(position, centerBias);
        centerBias.sub(position);
        if (centerBias.isZero(EPSILON)) {
            return 0f;
        }
        centerBias.nor();
        return Math.max(0f, -centerBias.dot(body.getLinearVelocity()));
    }

    private AiVehicleView findTarget(
            AiVehicleView self,
            Body body,
            Array<? extends AiVehicleView> vehicles,
            ArenaMap arenaMap,
            Vector2 arenaCenter) {
        AiVehicleView best = null;
        float bestScore = -Float.MAX_VALUE;
        Vector2 myPosition = body.getPosition();
        forwardAxis.set(body.getWorldVector(working.set(0f, 1f)));
        int topScore = Integer.MIN_VALUE;

        for (int i = 0; i < vehicles.size; i++) {
            AiVehicleView candidate = vehicles.get(i);
            if (candidate != null && candidate.isActive()) {
                topScore = Math.max(topScore, candidate.getScore());
            }
        }

        for (int i = 0; i < vehicles.size; i++) {
            AiVehicleView candidate = vehicles.get(i);
            if (candidate == self || !candidate.isActive()) {
                continue;
            }

            Body candidateBody = candidate.getBody();
            if (candidateBody == null) {
                continue;
            }

            Vector2 candidatePosition = candidateBody.getPosition();
            float distance = myPosition.dst(candidatePosition);
            float candidateEdgeDistance = arenaMap.distanceToHazard(candidatePosition);
            float edgeThreat = 4.8f - candidateEdgeDistance;
            float edgeVulnerability = Math.max(0f, EDGE_VULNERABILITY_DISTANCE - candidateEdgeDistance);
            float outwardVelocity = computeOutwardVelocity(arenaMap, candidatePosition, candidateBody);

            targetLead.set(candidatePosition).sub(arenaCenter);
            float centerDistanceScore = targetLead.len() * personality.targetCenterDistanceWeight;

            targetLead.set(candidatePosition).sub(myPosition);
            float approachAlignment = 0f;
            if (!targetLead.isZero(EPSILON)) {
                targetLead.nor();
                approachAlignment = Math.max(0f, forwardAxis.dot(targetLead));
            }

            // Blend ring-out chances with current approach so presets can lean aggressive or safe.
            float score =
                    edgeThreat * personality.targetEdgeThreatWeight
                            + edgeVulnerability * EDGE_VULNERABILITY_BONUS
                            + outwardVelocity * EDGE_OUTWARD_SPEED_BONUS
                            + centerDistanceScore
                            + approachAlignment * personality.targetApproachAlignmentWeight
                            - distance * personality.targetDistancePenaltyWeight;

            if (candidate.isPlayerControlled()) {
                score += personality.targetPlayerBias;
            }
            if (candidate.hasGrowthBoost()) {
                score += BOOSTED_TARGET_BONUS;
            }
            if (candidate.hasRamCharge()) {
                score += RAM_CHARGE_TARGET_BONUS;
            }
            if (candidate.getScore() == topScore && candidate.getScore() > self.getScore()) {
                score += LEADER_TARGET_BONUS;
            }
            if (candidate.getVehicleId() == self.getLastAttackerId()
                    && self.getRecentImpactTime() > 0f) {
                score += GRUDGE_TARGET_BONUS;
            }
            if (candidate.getRecentImpactTime() > 0f) {
                score += DOGPILE_TARGET_BONUS;
                if (candidateEdgeDistance < EDGE_ATTACK_COMMIT_DISTANCE) {
                    score += FINISH_HIM_BONUS;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best;
    }
}
