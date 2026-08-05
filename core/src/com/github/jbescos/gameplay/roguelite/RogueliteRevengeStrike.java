package com.github.jbescos.gameplay.roguelite;

/** A targeted retaliation emitted by an armed revenge card. */
public final class RogueliteRevengeStrike {
    public enum Action {
        DEBUFF,
        HARD_IMPACT,
        FORCE_THROTTLE,
        POSITION_SWAP,
        HOOK
    }

    private final RogueliteCardId cardId;
    private final Action action;
    private final float speedMultiplier;
    private final float gripMultiplier;
    private final float durationSeconds;
    private final float attackerLaunchSpeedRatio;
    private final float targetPushSpeedRatio;

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
        this(cardId, Action.DEBUFF, speedMultiplier, gripMultiplier, durationSeconds, 0f, 0f);
    }

    private RogueliteRevengeStrike(
            RogueliteCardId cardId,
            Action action,
            float speedMultiplier,
            float gripMultiplier,
            float durationSeconds,
            float attackerLaunchSpeedRatio,
            float targetPushSpeedRatio) {
        this.cardId = cardId;
        this.action = action;
        this.speedMultiplier = speedMultiplier;
        this.gripMultiplier = gripMultiplier;
        this.durationSeconds = durationSeconds;
        this.attackerLaunchSpeedRatio = attackerLaunchSpeedRatio;
        this.targetPushSpeedRatio = targetPushSpeedRatio;
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
                targetPushSpeedRatio);
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
                0f);
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
                0f);
    }

    static RogueliteRevengeStrike positionSwap(RogueliteCardId cardId) {
        return new RogueliteRevengeStrike(
                cardId,
                Action.POSITION_SWAP,
                1f,
                1f,
                0f,
                0f,
                0f);
    }

    static RogueliteRevengeStrike hook(
            RogueliteCardId cardId,
            float attackerLaunchSpeedRatio) {
        return new RogueliteRevengeStrike(
                cardId,
                Action.HOOK,
                1f,
                1f,
                0f,
                attackerLaunchSpeedRatio,
                0f);
    }

    public RogueliteCardId getCardId() {
        return cardId;
    }

    public Action getAction() {
        return action;
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
}
