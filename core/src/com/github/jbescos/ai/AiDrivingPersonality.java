package com.github.jbescos.ai;

public final class AiDrivingPersonality {
    public final String id;
    public final String displayName;
    public final float recoveryEdgeDistance;
    public final float cautionEdgeDistance;
    public final float outwardVelocityThreshold;
    public final float maxTargetLeadTime;
    public final float stagingDistance;
    public final float stagingVelocityFactor;
    public final float commitPushOffsetDistance;
    public final float commitAlignmentThreshold;
    public final float commitDistanceSq;
    public final float centerAssistWeight;
    public final float targetEdgeThreatWeight;
    public final float targetCenterDistanceWeight;
    public final float targetApproachAlignmentWeight;
    public final float targetDistancePenaltyWeight;
    public final float targetPlayerBias;
    public final float attackTurnGain;
    public final float recoveryTurnGain;
    public final float reverseAttackAngle;
    public final float cautiousAttackAngle;
    public final float cautiousAttackThrottle;
    public final float closeTargetDistanceSq;
    public final float closeTargetThrottle;
    public final float recoveryReverseAngle;
    public final float recoveryThrottle;
    public final float stuckDuration;
    public final float stuckReverseThrottle;

    private AiDrivingPersonality(Builder builder) {
        if (builder.id == null || builder.id.length() == 0) {
            throw new IllegalArgumentException("Ai personality id is required.");
        }
        if (builder.displayName == null || builder.displayName.length() == 0) {
            throw new IllegalArgumentException("Ai personality displayName is required.");
        }

        id = builder.id;
        displayName = builder.displayName;
        recoveryEdgeDistance = builder.recoveryEdgeDistance;
        cautionEdgeDistance = builder.cautionEdgeDistance;
        outwardVelocityThreshold = builder.outwardVelocityThreshold;
        maxTargetLeadTime = builder.maxTargetLeadTime;
        stagingDistance = builder.stagingDistance;
        stagingVelocityFactor = builder.stagingVelocityFactor;
        commitPushOffsetDistance = builder.commitPushOffsetDistance;
        commitAlignmentThreshold = builder.commitAlignmentThreshold;
        commitDistanceSq = builder.commitDistanceSq;
        centerAssistWeight = builder.centerAssistWeight;
        targetEdgeThreatWeight = builder.targetEdgeThreatWeight;
        targetCenterDistanceWeight = builder.targetCenterDistanceWeight;
        targetApproachAlignmentWeight = builder.targetApproachAlignmentWeight;
        targetDistancePenaltyWeight = builder.targetDistancePenaltyWeight;
        targetPlayerBias = builder.targetPlayerBias;
        attackTurnGain = builder.attackTurnGain;
        recoveryTurnGain = builder.recoveryTurnGain;
        reverseAttackAngle = builder.reverseAttackAngle;
        cautiousAttackAngle = builder.cautiousAttackAngle;
        cautiousAttackThrottle = builder.cautiousAttackThrottle;
        closeTargetDistanceSq = builder.closeTargetDistanceSq;
        closeTargetThrottle = builder.closeTargetThrottle;
        recoveryReverseAngle = builder.recoveryReverseAngle;
        recoveryThrottle = builder.recoveryThrottle;
        stuckDuration = builder.stuckDuration;
        stuckReverseThrottle = builder.stuckReverseThrottle;
    }

    public static Builder builder(String id, String displayName) {
        return new Builder(id, displayName);
    }

    public static final class Builder {
        private final String id;
        private final String displayName;

        private float recoveryEdgeDistance = 1.8f;
        private float cautionEdgeDistance = 2.7f;
        private float outwardVelocityThreshold = 1.15f;
        private float maxTargetLeadTime = 0.45f;
        private float stagingDistance = 1.85f;
        private float stagingVelocityFactor = 0.18f;
        private float commitPushOffsetDistance = 1.15f;
        private float commitAlignmentThreshold = 0.42f;
        private float commitDistanceSq = 20f;
        private float centerAssistWeight = 0.75f;
        private float targetEdgeThreatWeight = 2.3f;
        private float targetCenterDistanceWeight = 0.28f;
        private float targetApproachAlignmentWeight = 1.8f;
        private float targetDistancePenaltyWeight = 0.24f;
        private float targetPlayerBias = 0.45f;
        private float attackTurnGain = 3.1f;
        private float recoveryTurnGain = 3.8f;
        private float reverseAttackAngle = 1.55f;
        private float cautiousAttackAngle = 0.85f;
        private float cautiousAttackThrottle = 0.55f;
        private float closeTargetDistanceSq = 2f;
        private float closeTargetThrottle = 0.78f;
        private float recoveryReverseAngle = 1.35f;
        private float recoveryThrottle = 0.85f;
        private float stuckDuration = 0.65f;
        private float stuckReverseThrottle = -0.72f;

        private Builder(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public Builder recoveryEdgeDistance(float value) {
            recoveryEdgeDistance = value;
            return this;
        }

        public Builder cautionEdgeDistance(float value) {
            cautionEdgeDistance = value;
            return this;
        }

        public Builder outwardVelocityThreshold(float value) {
            outwardVelocityThreshold = value;
            return this;
        }

        public Builder maxTargetLeadTime(float value) {
            maxTargetLeadTime = value;
            return this;
        }

        public Builder stagingDistance(float value) {
            stagingDistance = value;
            return this;
        }

        public Builder stagingVelocityFactor(float value) {
            stagingVelocityFactor = value;
            return this;
        }

        public Builder commitPushOffsetDistance(float value) {
            commitPushOffsetDistance = value;
            return this;
        }

        public Builder commitAlignmentThreshold(float value) {
            commitAlignmentThreshold = value;
            return this;
        }

        public Builder commitDistanceSq(float value) {
            commitDistanceSq = value;
            return this;
        }

        public Builder centerAssistWeight(float value) {
            centerAssistWeight = value;
            return this;
        }

        public Builder targetEdgeThreatWeight(float value) {
            targetEdgeThreatWeight = value;
            return this;
        }

        public Builder targetCenterDistanceWeight(float value) {
            targetCenterDistanceWeight = value;
            return this;
        }

        public Builder targetApproachAlignmentWeight(float value) {
            targetApproachAlignmentWeight = value;
            return this;
        }

        public Builder targetDistancePenaltyWeight(float value) {
            targetDistancePenaltyWeight = value;
            return this;
        }

        public Builder targetPlayerBias(float value) {
            targetPlayerBias = value;
            return this;
        }

        public Builder attackTurnGain(float value) {
            attackTurnGain = value;
            return this;
        }

        public Builder recoveryTurnGain(float value) {
            recoveryTurnGain = value;
            return this;
        }

        public Builder reverseAttackAngle(float value) {
            reverseAttackAngle = value;
            return this;
        }

        public Builder cautiousAttackAngle(float value) {
            cautiousAttackAngle = value;
            return this;
        }

        public Builder cautiousAttackThrottle(float value) {
            cautiousAttackThrottle = value;
            return this;
        }

        public Builder closeTargetDistanceSq(float value) {
            closeTargetDistanceSq = value;
            return this;
        }

        public Builder closeTargetThrottle(float value) {
            closeTargetThrottle = value;
            return this;
        }

        public Builder recoveryReverseAngle(float value) {
            recoveryReverseAngle = value;
            return this;
        }

        public Builder recoveryThrottle(float value) {
            recoveryThrottle = value;
            return this;
        }

        public Builder stuckDuration(float value) {
            stuckDuration = value;
            return this;
        }

        public Builder stuckReverseThrottle(float value) {
            stuckReverseThrottle = value;
            return this;
        }

        public AiDrivingPersonality build() {
            return new AiDrivingPersonality(this);
        }
    }
}
