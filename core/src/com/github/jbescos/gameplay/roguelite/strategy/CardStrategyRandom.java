package com.github.jbescos.gameplay.roguelite.strategy;

/** Deterministic random source supplied by the owning roguelite run. */
public interface CardStrategyRandom {
    int nextInt(int bound);
}
