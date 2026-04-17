package com.github.jbescos.ai.tuning;

import com.github.jbescos.ai.AiDrivingPersonality;
import java.util.Locale;
import java.util.Random;

final class AiPersonalityGenome {
    private final AiDrivingPersonality base;

    private float recoveryEdgeDistance;
    private float cautionEdgeDistance;
    private float maxTargetLeadTime;
    private float stagingDistance;
    private float stagingVelocityFactor;
    private float commitPushOffsetDistance;
    private float commitAlignmentThreshold;
    private float commitDistanceSq;
    private float centerAssistWeight;
    private float targetEdgeThreatWeight;
    private float targetApproachAlignmentWeight;
    private float targetDistancePenaltyWeight;
    private float attackTurnGain;
    private float recoveryTurnGain;
    private float cautiousAttackAngle;
    private float cautiousAttackThrottle;
    private float closeTargetDistanceSq;
    private float closeTargetThrottle;
    private float recoveryThrottle;
    private float stuckDuration;
    private float stuckReverseThrottle;
    private float targetLockBonus;
    private float pickupThreatWeight;
    private float empoweredThreatAvoidance;
    private float flankOffsetDistance;

    private AiPersonalityGenome(AiDrivingPersonality base) {
        this.base = base;
    }

    static AiPersonalityGenome from(AiDrivingPersonality personality) {
        AiPersonalityGenome genome = new AiPersonalityGenome(personality);
        genome.recoveryEdgeDistance = personality.recoveryEdgeDistance;
        genome.cautionEdgeDistance = personality.cautionEdgeDistance;
        genome.maxTargetLeadTime = personality.maxTargetLeadTime;
        genome.stagingDistance = personality.stagingDistance;
        genome.stagingVelocityFactor = personality.stagingVelocityFactor;
        genome.commitPushOffsetDistance = personality.commitPushOffsetDistance;
        genome.commitAlignmentThreshold = personality.commitAlignmentThreshold;
        genome.commitDistanceSq = personality.commitDistanceSq;
        genome.centerAssistWeight = personality.centerAssistWeight;
        genome.targetEdgeThreatWeight = personality.targetEdgeThreatWeight;
        genome.targetApproachAlignmentWeight = personality.targetApproachAlignmentWeight;
        genome.targetDistancePenaltyWeight = personality.targetDistancePenaltyWeight;
        genome.attackTurnGain = personality.attackTurnGain;
        genome.recoveryTurnGain = personality.recoveryTurnGain;
        genome.cautiousAttackAngle = personality.cautiousAttackAngle;
        genome.cautiousAttackThrottle = personality.cautiousAttackThrottle;
        genome.closeTargetDistanceSq = personality.closeTargetDistanceSq;
        genome.closeTargetThrottle = personality.closeTargetThrottle;
        genome.recoveryThrottle = personality.recoveryThrottle;
        genome.stuckDuration = personality.stuckDuration;
        genome.stuckReverseThrottle = personality.stuckReverseThrottle;
        genome.targetLockBonus = personality.targetLockBonus;
        genome.pickupThreatWeight = personality.pickupThreatWeight;
        genome.empoweredThreatAvoidance = personality.empoweredThreatAvoidance;
        genome.flankOffsetDistance = personality.flankOffsetDistance;
        return genome;
    }

    AiPersonalityGenome copy() {
        AiPersonalityGenome copy = new AiPersonalityGenome(base);
        copy.recoveryEdgeDistance = recoveryEdgeDistance;
        copy.cautionEdgeDistance = cautionEdgeDistance;
        copy.maxTargetLeadTime = maxTargetLeadTime;
        copy.stagingDistance = stagingDistance;
        copy.stagingVelocityFactor = stagingVelocityFactor;
        copy.commitPushOffsetDistance = commitPushOffsetDistance;
        copy.commitAlignmentThreshold = commitAlignmentThreshold;
        copy.commitDistanceSq = commitDistanceSq;
        copy.centerAssistWeight = centerAssistWeight;
        copy.targetEdgeThreatWeight = targetEdgeThreatWeight;
        copy.targetApproachAlignmentWeight = targetApproachAlignmentWeight;
        copy.targetDistancePenaltyWeight = targetDistancePenaltyWeight;
        copy.attackTurnGain = attackTurnGain;
        copy.recoveryTurnGain = recoveryTurnGain;
        copy.cautiousAttackAngle = cautiousAttackAngle;
        copy.cautiousAttackThrottle = cautiousAttackThrottle;
        copy.closeTargetDistanceSq = closeTargetDistanceSq;
        copy.closeTargetThrottle = closeTargetThrottle;
        copy.recoveryThrottle = recoveryThrottle;
        copy.stuckDuration = stuckDuration;
        copy.stuckReverseThrottle = stuckReverseThrottle;
        copy.targetLockBonus = targetLockBonus;
        copy.pickupThreatWeight = pickupThreatWeight;
        copy.empoweredThreatAvoidance = empoweredThreatAvoidance;
        copy.flankOffsetDistance = flankOffsetDistance;
        return copy;
    }

    AiPersonalityGenome crossover(AiPersonalityGenome other, Random random) {
        AiPersonalityGenome child = copy();
        child.recoveryEdgeDistance = choose(recoveryEdgeDistance, other.recoveryEdgeDistance, random);
        child.cautionEdgeDistance = choose(cautionEdgeDistance, other.cautionEdgeDistance, random);
        child.maxTargetLeadTime = choose(maxTargetLeadTime, other.maxTargetLeadTime, random);
        child.stagingDistance = choose(stagingDistance, other.stagingDistance, random);
        child.stagingVelocityFactor = choose(stagingVelocityFactor, other.stagingVelocityFactor, random);
        child.commitPushOffsetDistance =
                choose(commitPushOffsetDistance, other.commitPushOffsetDistance, random);
        child.commitAlignmentThreshold =
                choose(commitAlignmentThreshold, other.commitAlignmentThreshold, random);
        child.commitDistanceSq = choose(commitDistanceSq, other.commitDistanceSq, random);
        child.centerAssistWeight = choose(centerAssistWeight, other.centerAssistWeight, random);
        child.targetEdgeThreatWeight =
                choose(targetEdgeThreatWeight, other.targetEdgeThreatWeight, random);
        child.targetApproachAlignmentWeight =
                choose(targetApproachAlignmentWeight, other.targetApproachAlignmentWeight, random);
        child.targetDistancePenaltyWeight =
                choose(targetDistancePenaltyWeight, other.targetDistancePenaltyWeight, random);
        child.attackTurnGain = choose(attackTurnGain, other.attackTurnGain, random);
        child.recoveryTurnGain = choose(recoveryTurnGain, other.recoveryTurnGain, random);
        child.cautiousAttackAngle = choose(cautiousAttackAngle, other.cautiousAttackAngle, random);
        child.cautiousAttackThrottle =
                choose(cautiousAttackThrottle, other.cautiousAttackThrottle, random);
        child.closeTargetDistanceSq = choose(closeTargetDistanceSq, other.closeTargetDistanceSq, random);
        child.closeTargetThrottle = choose(closeTargetThrottle, other.closeTargetThrottle, random);
        child.recoveryThrottle = choose(recoveryThrottle, other.recoveryThrottle, random);
        child.stuckDuration = choose(stuckDuration, other.stuckDuration, random);
        child.stuckReverseThrottle = choose(stuckReverseThrottle, other.stuckReverseThrottle, random);
        child.targetLockBonus = choose(targetLockBonus, other.targetLockBonus, random);
        child.pickupThreatWeight = choose(pickupThreatWeight, other.pickupThreatWeight, random);
        child.empoweredThreatAvoidance =
                choose(empoweredThreatAvoidance, other.empoweredThreatAvoidance, random);
        child.flankOffsetDistance = choose(flankOffsetDistance, other.flankOffsetDistance, random);
        return child;
    }

    AiPersonalityGenome mutate(Random random, float mutationChance, float mutationScale) {
        AiPersonalityGenome mutated = copy();
        mutated.recoveryEdgeDistance =
                maybeMutate(mutated.recoveryEdgeDistance, 1.2f, 2.8f, mutationChance, mutationScale, random);
        mutated.cautionEdgeDistance =
                maybeMutate(mutated.cautionEdgeDistance, 1.8f, 3.8f, mutationChance, mutationScale, random);
        mutated.maxTargetLeadTime =
                maybeMutate(mutated.maxTargetLeadTime, 0.20f, 0.72f, mutationChance, mutationScale, random);
        mutated.stagingDistance =
                maybeMutate(mutated.stagingDistance, 1.2f, 2.8f, mutationChance, mutationScale, random);
        mutated.stagingVelocityFactor =
                maybeMutate(mutated.stagingVelocityFactor, 0.05f, 0.35f, mutationChance, mutationScale, random);
        mutated.commitPushOffsetDistance =
                maybeMutate(
                        mutated.commitPushOffsetDistance,
                        0.80f,
                        1.55f,
                        mutationChance,
                        mutationScale,
                        random);
        mutated.commitAlignmentThreshold =
                maybeMutate(
                        mutated.commitAlignmentThreshold,
                        0.15f,
                        0.72f,
                        mutationChance,
                        mutationScale,
                        random);
        mutated.commitDistanceSq =
                maybeMutate(mutated.commitDistanceSq, 10f, 30f, mutationChance, mutationScale, random);
        mutated.centerAssistWeight =
                maybeMutate(mutated.centerAssistWeight, 0.10f, 1.45f, mutationChance, mutationScale, random);
        mutated.targetEdgeThreatWeight =
                maybeMutate(
                        mutated.targetEdgeThreatWeight,
                        1.20f,
                        3.50f,
                        mutationChance,
                        mutationScale,
                        random);
        mutated.targetApproachAlignmentWeight =
                maybeMutate(
                        mutated.targetApproachAlignmentWeight,
                        0.90f,
                        3.10f,
                        mutationChance,
                        mutationScale,
                        random);
        mutated.targetDistancePenaltyWeight =
                maybeMutate(
                        mutated.targetDistancePenaltyWeight,
                        0.10f,
                        0.40f,
                        mutationChance,
                        mutationScale,
                        random);
        mutated.attackTurnGain =
                maybeMutate(mutated.attackTurnGain, 2.4f, 4.2f, mutationChance, mutationScale, random);
        mutated.recoveryTurnGain =
                maybeMutate(mutated.recoveryTurnGain, 3.0f, 4.6f, mutationChance, mutationScale, random);
        mutated.cautiousAttackAngle =
                maybeMutate(
                        mutated.cautiousAttackAngle,
                        0.55f,
                        1.12f,
                        mutationChance,
                        mutationScale,
                        random);
        mutated.cautiousAttackThrottle =
                maybeMutate(
                        mutated.cautiousAttackThrottle,
                        0.35f,
                        0.86f,
                        mutationChance,
                        mutationScale,
                        random);
        mutated.closeTargetDistanceSq =
                maybeMutate(mutated.closeTargetDistanceSq, 1.0f, 3.8f, mutationChance, mutationScale, random);
        mutated.closeTargetThrottle =
                maybeMutate(mutated.closeTargetThrottle, 0.55f, 1.0f, mutationChance, mutationScale, random);
        mutated.recoveryThrottle =
                maybeMutate(mutated.recoveryThrottle, 0.65f, 1.0f, mutationChance, mutationScale, random);
        mutated.stuckDuration =
                maybeMutate(mutated.stuckDuration, 0.40f, 0.95f, mutationChance, mutationScale, random);
        mutated.stuckReverseThrottle =
                maybeMutate(
                        mutated.stuckReverseThrottle,
                        -0.95f,
                        -0.50f,
                        mutationChance,
                        mutationScale,
                        random);
        mutated.targetLockBonus =
                maybeMutate(mutated.targetLockBonus, 0f, 1.80f, mutationChance, mutationScale, random);
        mutated.pickupThreatWeight =
                maybeMutate(mutated.pickupThreatWeight, 0f, 2.20f, mutationChance, mutationScale, random);
        mutated.empoweredThreatAvoidance =
                maybeMutate(
                        mutated.empoweredThreatAvoidance,
                        0f,
                        1.50f,
                        mutationChance,
                        mutationScale,
                        random);
        mutated.flankOffsetDistance =
                maybeMutate(mutated.flankOffsetDistance, 0f, 1.25f, mutationChance, mutationScale, random);
        return mutated;
    }

    AiDrivingPersonality toPersonality(String id, String displayName) {
        return base.copyBuilder(id, displayName)
                .recoveryEdgeDistance(recoveryEdgeDistance)
                .cautionEdgeDistance(cautionEdgeDistance)
                .maxTargetLeadTime(maxTargetLeadTime)
                .stagingDistance(stagingDistance)
                .stagingVelocityFactor(stagingVelocityFactor)
                .commitPushOffsetDistance(commitPushOffsetDistance)
                .commitAlignmentThreshold(commitAlignmentThreshold)
                .commitDistanceSq(commitDistanceSq)
                .centerAssistWeight(centerAssistWeight)
                .targetEdgeThreatWeight(targetEdgeThreatWeight)
                .targetApproachAlignmentWeight(targetApproachAlignmentWeight)
                .targetDistancePenaltyWeight(targetDistancePenaltyWeight)
                .attackTurnGain(attackTurnGain)
                .recoveryTurnGain(recoveryTurnGain)
                .cautiousAttackAngle(cautiousAttackAngle)
                .cautiousAttackThrottle(cautiousAttackThrottle)
                .closeTargetDistanceSq(closeTargetDistanceSq)
                .closeTargetThrottle(closeTargetThrottle)
                .recoveryThrottle(recoveryThrottle)
                .stuckDuration(stuckDuration)
                .stuckReverseThrottle(stuckReverseThrottle)
                .targetLockBonus(targetLockBonus)
                .pickupThreatWeight(pickupThreatWeight)
                .empoweredThreatAvoidance(empoweredThreatAvoidance)
                .flankOffsetDistance(flankOffsetDistance)
                .build();
    }

    String toBuilderSnippet(String id, String displayName) {
        StringBuilder builder = new StringBuilder();
        builder.append("AiDrivingPersonality.builder(\"")
                .append(id)
                .append("\", \"")
                .append(displayName)
                .append("\")\n");
        append(builder, "recoveryEdgeDistance", recoveryEdgeDistance);
        append(builder, "cautionEdgeDistance", cautionEdgeDistance);
        append(builder, "outwardVelocityThreshold", base.outwardVelocityThreshold);
        append(builder, "maxTargetLeadTime", maxTargetLeadTime);
        append(builder, "stagingDistance", stagingDistance);
        append(builder, "stagingVelocityFactor", stagingVelocityFactor);
        append(builder, "commitPushOffsetDistance", commitPushOffsetDistance);
        append(builder, "commitAlignmentThreshold", commitAlignmentThreshold);
        append(builder, "commitDistanceSq", commitDistanceSq);
        append(builder, "centerAssistWeight", centerAssistWeight);
        append(builder, "targetEdgeThreatWeight", targetEdgeThreatWeight);
        append(builder, "targetCenterDistanceWeight", base.targetCenterDistanceWeight);
        append(builder, "targetApproachAlignmentWeight", targetApproachAlignmentWeight);
        append(builder, "targetDistancePenaltyWeight", targetDistancePenaltyWeight);
        append(builder, "targetPlayerBias", base.targetPlayerBias);
        append(builder, "attackTurnGain", attackTurnGain);
        append(builder, "recoveryTurnGain", recoveryTurnGain);
        append(builder, "reverseAttackAngle", base.reverseAttackAngle);
        append(builder, "cautiousAttackAngle", cautiousAttackAngle);
        append(builder, "cautiousAttackThrottle", cautiousAttackThrottle);
        append(builder, "closeTargetDistanceSq", closeTargetDistanceSq);
        append(builder, "closeTargetThrottle", closeTargetThrottle);
        append(builder, "recoveryReverseAngle", base.recoveryReverseAngle);
        append(builder, "recoveryThrottle", recoveryThrottle);
        append(builder, "stuckDuration", stuckDuration);
        append(builder, "stuckReverseThrottle", stuckReverseThrottle);
        append(builder, "targetLockBonus", targetLockBonus);
        append(builder, "pickupThreatWeight", pickupThreatWeight);
        append(builder, "empoweredThreatAvoidance", empoweredThreatAvoidance);
        append(builder, "flankOffsetDistance", flankOffsetDistance);
        builder.append("        .build();");
        return builder.toString();
    }

    private static void append(StringBuilder builder, String methodName, float value) {
        builder.append("        .")
                .append(methodName)
                .append("(")
                .append(String.format(Locale.US, "%.3ff", value))
                .append(")\n");
    }

    private static float choose(float left, float right, Random random) {
        if (random.nextFloat() < 0.25f) {
            return (left + right) * 0.5f;
        }
        return random.nextBoolean() ? left : right;
    }

    private static float maybeMutate(
            float value,
            float min,
            float max,
            float mutationChance,
            float mutationScale,
            Random random) {
        if (random.nextFloat() >= mutationChance) {
            return value;
        }

        float span = max - min;
        float delta = (float) (random.nextGaussian() * span * mutationScale * 0.35f);
        return clamp(value + delta, min, max);
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
