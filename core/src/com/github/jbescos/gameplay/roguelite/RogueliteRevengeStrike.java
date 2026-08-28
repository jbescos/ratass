package com.github.jbescos.gameplay.roguelite;

/** A targeted retaliation emitted by an armed revenge card. */
public final class RogueliteRevengeStrike {
    public enum Action {
        DEBUFF,
        HARD_IMPACT,
        FORCE_BRAKE,
        FORCE_THROTTLE,
        POSITION_SWAP,
        POSITION_REORDER,
        HOOK,
        CURSE,
        PUSH_SHOT
    }

    private final RogueliteCardId cardId;
    private final Action action;
    private final float speedMultiplier;
    private final float gripMultiplier;
    private final float durationSeconds;
    private final float attackerLaunchSpeedRatio;
    private final float targetPushSpeedRatio;
    private final float massMultiplier;
    private final int secondaryTargetVehicleId;
    private final int strikeIndex;
    private final float effectMultiplier;

    RogueliteRevengeStrike(
            RogueliteCardId cardId,
            float speedMultiplier,
            float durationSeconds) {
        this(cardId, speedMultiplier, 1f, durationSeconds);
    }

    RogueliteRevengeStrike(
            RogueliteCardId cardId,
            float speedMultiplier,
            float gripMultiplier,
            float durationSeconds) {
        this(
                cardId,
                Action.DEBUFF,
                speedMultiplier,
                gripMultiplier,
                durationSeconds,
                0f,
                0f,
                1f,
                -1,
                1);
    }

    private RogueliteRevengeStrike(
            RogueliteCardId cardId,
            Action action,
            float speedMultiplier,
            float gripMultiplier,
            float durationSeconds,
            float attackerLaunchSpeedRatio,
            float targetPushSpeedRatio,
            float massMultiplier,
            int secondaryTargetVehicleId,
            int strikeIndex) {
        this(
                cardId,
                action,
                speedMultiplier,
                gripMultiplier,
                durationSeconds,
                attackerLaunchSpeedRatio,
                targetPushSpeedRatio,
                massMultiplier,
                secondaryTargetVehicleId,
                strikeIndex,
                1f);
    }

    private RogueliteRevengeStrike(
            RogueliteCardId cardId,
            Action action,
            float speedMultiplier,
            float gripMultiplier,
            float durationSeconds,
            float attackerLaunchSpeedRatio,
            float targetPushSpeedRatio,
            float massMultiplier,
            int secondaryTargetVehicleId,
            int strikeIndex,
            float effectMultiplier) {
        this.cardId = cardId;
        this.action = action;
        this.speedMultiplier = speedMultiplier;
        this.gripMultiplier = gripMultiplier;
        this.durationSeconds = durationSeconds;
        this.attackerLaunchSpeedRatio = attackerLaunchSpeedRatio;
        this.targetPushSpeedRatio = targetPushSpeedRatio;
        this.massMultiplier = massMultiplier;
        this.secondaryTargetVehicleId = secondaryTargetVehicleId;
        this.strikeIndex = strikeIndex;
        this.effectMultiplier = Math.max(1f, effectMultiplier);
    }

    static RogueliteRevengeStrike hardImpact(
            RogueliteCardId cardId,
            float attackerLaunchSpeedRatio,
            float targetPushSpeedRatio) {
        return new RogueliteRevengeStrike(
                cardId,
                Action.HARD_IMPACT,
                1f,
                1f,
                0f,
                attackerLaunchSpeedRatio,
                targetPushSpeedRatio,
                1f,
                -1,
                1);
    }

    static RogueliteRevengeStrike debuff(
            RogueliteCardId cardId,
            float speedMultiplier,
            float gripMultiplier,
            float durationSeconds) {
        return new RogueliteRevengeStrike(
                cardId,
                Action.DEBUFF,
                speedMultiplier,
                gripMultiplier,
                durationSeconds,
                0f,
                0f,
                1f,
                -1,
                1);
    }

    static RogueliteRevengeStrike forceThrottle(
            RogueliteCardId cardId,
            float durationSeconds) {
        return new RogueliteRevengeStrike(
                cardId,
                Action.FORCE_THROTTLE,
                1f,
                1f,
                durationSeconds,
                0f,
                0f,
                1f,
                -1,
                1);
    }

    static RogueliteRevengeStrike forceBrake(
            RogueliteCardId cardId,
            float durationSeconds) {
        return new RogueliteRevengeStrike(
                cardId,
                Action.FORCE_BRAKE,
                1f,
                1f,
                durationSeconds,
                0f,
                0f,
                1f,
                -1,
                1);
    }

    static RogueliteRevengeStrike positionSwap(RogueliteCardId cardId) {
        return new RogueliteRevengeStrike(
                cardId,
                Action.POSITION_SWAP,
                1f,
                1f,
                0f,
                0f,
                0f,
                1f,
                -1,
                1);
    }

    static RogueliteRevengeStrike positionReorder(
            RogueliteCardId cardId,
            int secondaryTargetVehicleId) {
        return new RogueliteRevengeStrike(
                cardId,
                Action.POSITION_REORDER,
                1f,
                1f,
                0f,
                0f,
                0f,
                1f,
                secondaryTargetVehicleId,
                1);
    }

    static RogueliteRevengeStrike hook(RogueliteCardId cardId) {
        return new RogueliteRevengeStrike(
                cardId,
                Action.HOOK,
                1f,
                1f,
                0f,
                0f,
                0f,
                1f,
                -1,
                1);
    }

    static RogueliteRevengeStrike curse(
            RogueliteCardId cardId,
            float massMultiplier,
            float performanceMultiplier,
            float durationSeconds) {
        return new RogueliteRevengeStrike(
                cardId,
                Action.CURSE,
                1f,
                performanceMultiplier,
                durationSeconds,
                0f,
                0f,
                massMultiplier,
                -1,
                1);
    }

    static RogueliteRevengeStrike pushShot(
            RogueliteCardId cardId,
            int strikeIndex) {
        return new RogueliteRevengeStrike(
                cardId,
                Action.PUSH_SHOT,
                1f,
                1f,
                0f,
                0f,
                0f,
                1f,
                -1,
                Math.max(1, strikeIndex));
    }

    public RogueliteCardId getCardId() {
        return cardId;
    }

    public Action getAction() {
        return action;
    }

    public boolean appliesDebuff() {
        return action == Action.DEBUFF
                || action == Action.FORCE_BRAKE
                || action == Action.FORCE_THROTTLE
                || action == Action.HOOK
                || action == Action.CURSE;
    }

    public float getSpeedMultiplier() {
        return speedMultiplier;
    }

    public float getGripMultiplier() {
        return gripMultiplier;
    }

    public float getDurationSeconds() {
        return durationSeconds;
    }

    public boolean isHardImpact() {
        return action == Action.HARD_IMPACT;
    }

    public float getAttackerLaunchSpeedRatio() {
        return attackerLaunchSpeedRatio;
    }

    public float getTargetPushSpeedRatio() {
        return targetPushSpeedRatio;
    }

    public float getMassMultiplier() {
        return massMultiplier;
    }

    public int getSecondaryTargetVehicleId() {
        return secondaryTargetVehicleId;
    }

    public int getStrikeIndex() {
        return strikeIndex;
    }

    public float getEffectMultiplier() {
        return effectMultiplier;
    }

    RogueliteRevengeStrike amplified(float multiplier) {
        float safeMultiplier = Float.isFinite(multiplier)
                ? Math.max(1f, multiplier)
                : 1f;
        if (safeMultiplier <= 1f) {
            return this;
        }
        return new RogueliteRevengeStrike(
                cardId,
                action,
                amplifyDeviation(speedMultiplier, safeMultiplier),
                amplifyDeviation(gripMultiplier, safeMultiplier),
                durationSeconds * safeMultiplier,
                attackerLaunchSpeedRatio * safeMultiplier,
                targetPushSpeedRatio * safeMultiplier,
                amplifyDeviation(massMultiplier, safeMultiplier),
                secondaryTargetVehicleId,
                strikeIndex,
                effectMultiplier * safeMultiplier);
    }

    private static float amplifyDeviation(float value, float multiplier) {
        return Math.max(0f, 1f + (value - 1f) * multiplier);
    }

    public boolean isOpeningStrike() {
        return strikeIndex == 1;
    }
}
