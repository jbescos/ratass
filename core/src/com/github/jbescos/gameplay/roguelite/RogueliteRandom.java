package com.github.jbescos.gameplay.roguelite;

final class RogueliteRandom {
    private static final int NON_ZERO_FALLBACK_STATE = 0x6d2b79f5;
    private int state;

    RogueliteRandom() {
        this(System.currentTimeMillis());
    }

    RogueliteRandom(long seed) {
        setState((int) (seed ^ (seed >>> 32)));
    }

    int getState() {
        return state;
    }

    void setState(int newState) {
        state = newState == 0 ? NON_ZERO_FALLBACK_STATE : newState;
    }

    float nextFloat() {
        return (nextInt() >>> 8) * (1f / 16777216f);
    }

    int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("Bound must be positive.");
        }
        return (nextInt() & Integer.MAX_VALUE) % bound;
    }

    private int nextInt() {
        int value = state;
        value ^= value << 13;
        value ^= value >>> 17;
        value ^= value << 5;
        state = value;
        return value;
    }
}
