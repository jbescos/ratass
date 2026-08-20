package com.github.jbescos.gameplay.roguelite;

/** Combines card-type amplifiers without turning a neutral card into an amplifier. */
public final class CardAmplifierChain {
    private CardAmplifierChain() {
    }

    public static float combine(float downstreamMultiplier, float upstreamMultiplier) {
        if (!Float.isFinite(downstreamMultiplier) || downstreamMultiplier <= 1f) {
            return 1f;
        }
        float safeUpstream = Float.isFinite(upstreamMultiplier)
                ? Math.max(1f, upstreamMultiplier)
                : 1f;
        return downstreamMultiplier * safeUpstream;
    }
}
