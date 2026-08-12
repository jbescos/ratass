package com.github.jbescos.gameplay.roguelite;

/** Describes how a prepared Revenge card reaches execution. */
enum RevengeWorkflow {
    PROXIMITY(false),
    TARGET_IMMEDIATE(true),
    TARGET_DELAYED(true),
    TARGET_RETURN_HIT(true),
    TARGET_SEQUENCE(true);

    private final boolean targeted;

    RevengeWorkflow(boolean targeted) {
        this.targeted = targeted;
    }

    boolean isTargeted() {
        return targeted;
    }
}
