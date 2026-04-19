package com.github.jbescos.ai;

public final class AiControlDecision {
    public float throttle;
    public float turn;
    public boolean handbrake;

    public AiControlDecision set(float throttle, float turn) {
        return set(throttle, turn, false);
    }

    public AiControlDecision set(float throttle, float turn, boolean handbrake) {
        this.throttle = throttle;
        this.turn = turn;
        this.handbrake = handbrake;
        return this;
    }
}
