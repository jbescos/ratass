package com.github.jbescos.ai;

public final class AiControlDecision {
    public float throttle;
    public float turn;

    public AiControlDecision set(float throttle, float turn) {
        this.throttle = throttle;
        this.turn = turn;
        return this;
    }
}
