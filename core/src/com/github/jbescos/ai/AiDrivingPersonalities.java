package com.github.jbescos.ai;

import com.badlogic.gdx.utils.Array;

public final class AiDrivingPersonalities {
    public static final AiDrivingPersonality BALANCED =
            AiDrivingPersonality.builder("balanced", "Balanced").build();

    public static final AiDrivingPersonality BRAWLER =
            AiDrivingPersonality.builder("brawler", "Brawler")
                    .recoveryEdgeDistance(1.55f)
                    .cautionEdgeDistance(2.25f)
                    .outwardVelocityThreshold(1.30f)
                    .maxTargetLeadTime(0.52f)
                    .stagingDistance(1.45f)
                    .stagingVelocityFactor(0.12f)
                    .commitPushOffsetDistance(1.35f)
                    .commitAlignmentThreshold(0.26f)
                    .commitDistanceSq(26f)
                    .centerAssistWeight(0.35f)
                    .targetEdgeThreatWeight(2.75f)
                    .targetCenterDistanceWeight(0.34f)
                    .targetApproachAlignmentWeight(2.05f)
                    .targetDistancePenaltyWeight(0.18f)
                    .targetPlayerBias(0.70f)
                    .attackTurnGain(3.45f)
                    .recoveryTurnGain(3.55f)
                    .reverseAttackAngle(1.72f)
                    .cautiousAttackAngle(0.98f)
                    .cautiousAttackThrottle(0.72f)
                    .closeTargetDistanceSq(3.20f)
                    .closeTargetThrottle(0.94f)
                    .recoveryReverseAngle(1.42f)
                    .recoveryThrottle(0.92f)
                    .stuckDuration(0.75f)
                    .stuckReverseThrottle(-0.78f)
                    .targetLockBonus(1.05f)
                    .pickupThreatWeight(0.58f)
                    .empoweredThreatAvoidance(0.34f)
                    .flankOffsetDistance(0.30f)
                    .build();

    public static final AiDrivingPersonality INTERCEPTOR =
            AiDrivingPersonality.builder("interceptor", "Interceptor")
                    .recoveryEdgeDistance(1.70f)
                    .cautionEdgeDistance(2.60f)
                    .outwardVelocityThreshold(1.08f)
                    .maxTargetLeadTime(0.62f)
                    .stagingDistance(1.95f)
                    .stagingVelocityFactor(0.24f)
                    .commitPushOffsetDistance(1.18f)
                    .commitAlignmentThreshold(0.36f)
                    .commitDistanceSq(22f)
                    .centerAssistWeight(0.68f)
                    .targetEdgeThreatWeight(2.15f)
                    .targetCenterDistanceWeight(0.24f)
                    .targetApproachAlignmentWeight(2.55f)
                    .targetDistancePenaltyWeight(0.20f)
                    .targetPlayerBias(0.42f)
                    .attackTurnGain(3.25f)
                    .recoveryTurnGain(3.95f)
                    .reverseAttackAngle(1.58f)
                    .cautiousAttackAngle(0.82f)
                    .cautiousAttackThrottle(0.60f)
                    .closeTargetDistanceSq(1.80f)
                    .closeTargetThrottle(0.82f)
                    .recoveryReverseAngle(1.30f)
                    .recoveryThrottle(0.84f)
                    .stuckDuration(0.62f)
                    .stuckReverseThrottle(-0.70f)
                    .targetLockBonus(0.84f)
                    .pickupThreatWeight(1.30f)
                    .empoweredThreatAvoidance(0.56f)
                    .flankOffsetDistance(0.80f)
                    .build();

    public static final AiDrivingPersonality SURVIVOR =
            AiDrivingPersonality.builder("survivor", "Survivor")
                    .recoveryEdgeDistance(2.15f)
                    .cautionEdgeDistance(3.25f)
                    .outwardVelocityThreshold(0.92f)
                    .maxTargetLeadTime(0.34f)
                    .stagingDistance(2.30f)
                    .stagingVelocityFactor(0.22f)
                    .commitPushOffsetDistance(0.96f)
                    .commitAlignmentThreshold(0.58f)
                    .commitDistanceSq(15f)
                    .centerAssistWeight(1.10f)
                    .targetEdgeThreatWeight(1.75f)
                    .targetCenterDistanceWeight(0.16f)
                    .targetApproachAlignmentWeight(1.35f)
                    .targetDistancePenaltyWeight(0.30f)
                    .targetPlayerBias(0.20f)
                    .attackTurnGain(2.85f)
                    .recoveryTurnGain(4.20f)
                    .reverseAttackAngle(1.40f)
                    .cautiousAttackAngle(0.72f)
                    .cautiousAttackThrottle(0.44f)
                    .closeTargetDistanceSq(1.20f)
                    .closeTargetThrottle(0.66f)
                    .recoveryReverseAngle(1.18f)
                    .recoveryThrottle(0.76f)
                    .stuckDuration(0.52f)
                    .stuckReverseThrottle(-0.68f)
                    .targetLockBonus(0.44f)
                    .pickupThreatWeight(0.40f)
                    .empoweredThreatAvoidance(1.18f)
                    .flankOffsetDistance(0.64f)
                    .build();

    public static Array<AiDrivingPersonality> createPresetList() {
        Array<AiDrivingPersonality> presets = new Array<AiDrivingPersonality>();
        presets.add(BALANCED);
        presets.add(BRAWLER);
        presets.add(INTERCEPTOR);
        presets.add(SURVIVOR);
        return presets;
    }

    public static AiDrivingPersonality byId(String id) {
        if (BALANCED.id.equals(id)) {
            return BALANCED;
        }
        if (BRAWLER.id.equals(id)) {
            return BRAWLER;
        }
        if (INTERCEPTOR.id.equals(id)) {
            return INTERCEPTOR;
        }
        if (SURVIVOR.id.equals(id)) {
            return SURVIVOR;
        }
        return null;
    }

    private AiDrivingPersonalities() {
    }
}
