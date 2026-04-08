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
    private static final float STUCK_SPEED_SQ = 1f;
    private static final float STUCK_RESET_DURATION = 1.1f;
    private static final float POWER_PICKUP_EDGE_MARGIN = 1.05f;
    private static final float BOOSTED_TARGET_BONUS = 1.85f;

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

    private float stuckTimer;

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
            Vector2 growthPickupPosition) {
        if (self == null || !self.isActive()) {
            stuckTimer = 0f;
            return decision.set(0f, 0f);
        }

        Body body = self.getBody();
        if (body == null) {
            stuckTimer = 0f;
            return decision.set(0f, 0f);
        }

        arenaMap.getFocusPoint(arenaFocus);
        boolean chaseGrowthPickup =
                growthPickupActive
                        && growthPickupPosition != null
                        && !self.hasGrowthBoost()
                        && arenaMap.distanceToHazard(growthPickupPosition) >= POWER_PICKUP_EDGE_MARGIN;

        AiVehicleView target = null;
        if (!chaseGrowthPickup) {
            target = findTarget(self, body, vehicles, arenaMap, arenaFocus);
        }

        if (!chaseGrowthPickup && (target == null || target.getBody() == null)) {
            stuckTimer = 0f;
            return decision.set(0f, 0f);
        }

        Vector2 position = body.getPosition();
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

        if (recovering) {
            arenaMap.findRecoveryPoint(position, desiredVector);
        } else if (chaseGrowthPickup) {
            desiredVector.set(growthPickupPosition);
            arenaMap.clampToPlayable(desiredVector, INTERCEPT_CLAMP_MARGIN);
        } else {
            Body targetBody = target.getBody();
            Vector2 targetPosition = targetBody.getPosition();
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
                    targetEdge < personality.recoveryEdgeDistance
                            || (pushAlignment > personality.commitAlignmentThreshold
                            && position.dst2(targetPosition) < personality.commitDistanceSq);

            if (commitPush) {
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

        float turn = MathUtils.clamp(
                angleError * (recovering ? personality.recoveryTurnGain : personality.attackTurnGain),
                -1f,
                1f);

        float throttle;
        if (recovering) {
            if (Math.abs(angleError) > personality.recoveryReverseAngle) {
                throttle = RECOVERY_REVERSE_THROTTLE;
            } else if (nearestHazard < EMERGENCY_RECOVERY_EDGE_DISTANCE) {
                throttle = EMERGENCY_RECOVERY_THROTTLE;
            } else {
                throttle = personality.recoveryThrottle;
            }
        } else if (Math.abs(angleError) > personality.reverseAttackAngle) {
            throttle = ATTACK_REVERSE_THROTTLE;
        } else if (Math.abs(angleError) > personality.cautiousAttackAngle) {
            throttle = personality.cautiousAttackThrottle;
        } else if (desiredVector.len2() < personality.closeTargetDistanceSq) {
            throttle = personality.closeTargetThrottle;
        } else {
            throttle = FULL_ATTACK_THROTTLE;
        }

        if (body.getLinearVelocity().len2() < STUCK_SPEED_SQ) {
            stuckTimer += delta;
        } else {
            stuckTimer = 0f;
        }

        if (stuckTimer > personality.stuckDuration) {
            throttle = personality.stuckReverseThrottle;
            turn = turn >= 0f ? 1f : -1f;
            if (stuckTimer > STUCK_RESET_DURATION) {
                stuckTimer = 0f;
            }
        }

        return decision.set(throttle, turn);
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
            float edgeThreat = 4.8f - arenaMap.distanceToHazard(candidatePosition);

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
                            + centerDistanceScore
                            + approachAlignment * personality.targetApproachAlignmentWeight
                            - distance * personality.targetDistancePenaltyWeight;

            if (candidate.isPlayerControlled()) {
                score += personality.targetPlayerBias;
            }
            if (candidate.hasGrowthBoost()) {
                score += BOOSTED_TARGET_BONUS;
            }

            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best;
    }
}
