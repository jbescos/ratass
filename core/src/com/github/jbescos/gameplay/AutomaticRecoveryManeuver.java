package com.github.jbescos.gameplay;

/** Three-stage route recovery for cars that cannot make useful progress. */
public final class AutomaticRecoveryManeuver {
    public enum Phase {
        IDLE,
        TURN_TO_TARGET,
        DRIVE_TO_TARGET,
        ALIGN_TO_ROUTE,
        MODEL_HANDOFF
    }

    public enum ModelHandoffResult {
        YIELDING,
        COMPLETED,
        RESUME_ASSISTANCE
    }

    public static final float TARGET_ROUTE_FRACTION = 0.025f;
    private static final float TARGET_ALIGNMENT = 0.985f;
    private static final float ROUTE_ALIGNMENT = 0.985f;
    private static final float MODEL_HANDOFF_MIN_ROUTE_ALIGNMENT =
            (float) Math.cos(Math.toRadians(70f));
    private static final float USEFUL_DISTANCE_GAIN = 0.10f;
    private static final float USEFUL_ALIGNMENT_GAIN = 0.02f;

    private Phase phase = Phase.IDLE;
    private float bestTargetDistance;
    private float bestAlignment;
    private float blockedSeconds;
    private float assistanceSeconds;
    private float modelHandoffSeconds;
    private float policyAttemptSeconds;
    private float policyNoProgressSeconds;
    private float policyBestTargetDistance;
    private float policyBestTargetAlignment;
    private float scriptedAssistSeconds;
    private float scriptedStartTargetDistance;
    private float scriptedStartTargetAlignment;
    private boolean explosionRequested;

    public void begin(float targetDistance) {
        phase = Phase.TURN_TO_TARGET;
        bestTargetDistance = Math.max(0f, targetDistance);
        bestAlignment = -1f;
        blockedSeconds = 0f;
        assistanceSeconds = 0f;
        modelHandoffSeconds = 0f;
        resetPolicyAttempt();
        explosionRequested = false;
    }

    public void reset() {
        phase = Phase.IDLE;
        bestTargetDistance = 0f;
        bestAlignment = -1f;
        blockedSeconds = 0f;
        assistanceSeconds = 0f;
        modelHandoffSeconds = 0f;
        resetPolicyAttempt();
        explosionRequested = false;
    }

    public void beginPolicyAttempt(float targetDistance, float targetAlignment) {
        policyAttemptSeconds = 0f;
        policyNoProgressSeconds = 0f;
        policyBestTargetDistance = Math.max(0f, targetDistance);
        policyBestTargetAlignment = finiteOr(targetAlignment, -1f);
    }

    public boolean shouldFallbackFromPolicy(
            float delta,
            float targetDistance,
            float targetAlignment,
            float usefulDistanceGain,
            float usefulAlignmentGain,
            float noProgressTimeoutSeconds,
            float attemptTimeoutSeconds) {
        float elapsed = Math.max(0f, delta);
        policyAttemptSeconds += elapsed;

        float distance = Math.max(0f, finiteOr(targetDistance, policyBestTargetDistance));
        float alignment = finiteOr(targetAlignment, policyBestTargetAlignment);
        boolean improved =
                distance <= policyBestTargetDistance - Math.max(0f, usefulDistanceGain)
                        || alignment
                                >= policyBestTargetAlignment
                                        + Math.max(0f, usefulAlignmentGain);
        if (improved) {
            policyBestTargetDistance = Math.min(policyBestTargetDistance, distance);
            policyBestTargetAlignment = Math.max(policyBestTargetAlignment, alignment);
            policyNoProgressSeconds = 0f;
        } else {
            policyNoProgressSeconds += elapsed;
        }

        return policyNoProgressSeconds >= Math.max(0.01f, noProgressTimeoutSeconds)
                || policyAttemptSeconds >= Math.max(0.01f, attemptTimeoutSeconds);
    }

    public void beginScriptedPolicyRetry(float targetDistance, float targetAlignment) {
        scriptedAssistSeconds = 0f;
        scriptedStartTargetDistance = Math.max(0f, targetDistance);
        scriptedStartTargetAlignment = finiteOr(targetAlignment, -1f);
    }

    public boolean shouldRetryPolicyAfterScriptedAssist(
            float delta,
            float targetDistance,
            float targetAlignment,
            float minimumAssistSeconds,
            float usefulDistanceGain,
            float usefulAlignmentGain) {
        scriptedAssistSeconds += Math.max(0f, delta);
        if (scriptedAssistSeconds < Math.max(0f, minimumAssistSeconds)) {
            return false;
        }
        float distance = Math.max(0f, finiteOr(targetDistance, scriptedStartTargetDistance));
        float alignment = finiteOr(targetAlignment, scriptedStartTargetAlignment);
        return distance
                        <= scriptedStartTargetDistance
                                - Math.max(0f, usefulDistanceGain)
                || alignment
                        >= scriptedStartTargetAlignment
                                + Math.max(0f, usefulAlignmentGain);
    }

    public void update(
            float delta,
            float targetDistance,
            float targetForwardAlignment,
            float routeForwardAlignment,
            float targetReachedDistance,
            float blockedTimeoutSeconds) {
        if (phase == Phase.IDLE) {
            return;
        }
        if (phase == Phase.MODEL_HANDOFF) {
            return;
        }

        if (phase == Phase.TURN_TO_TARGET) {
            if (targetForwardAlignment >= TARGET_ALIGNMENT) {
                phase = Phase.DRIVE_TO_TARGET;
                bestTargetDistance = Math.max(0f, targetDistance);
                bestAlignment = -1f;
                blockedSeconds = 0f;
            } else {
                updateBlockedAlignment(
                        delta,
                        targetForwardAlignment,
                        blockedTimeoutSeconds);
            }
            return;
        }

        if (phase == Phase.DRIVE_TO_TARGET) {
            if (targetDistance <= Math.max(0f, targetReachedDistance)) {
                phase = Phase.ALIGN_TO_ROUTE;
                bestAlignment = routeForwardAlignment;
                blockedSeconds = 0f;
                return;
            }

            if (targetDistance <= bestTargetDistance - USEFUL_DISTANCE_GAIN) {
                bestTargetDistance = targetDistance;
                blockedSeconds = 0f;
                return;
            }

            blockedSeconds += Math.max(0f, delta);
            if (blockedSeconds >= Math.max(0.01f, blockedTimeoutSeconds)) {
                explosionRequested = true;
                blockedSeconds = 0f;
                bestTargetDistance = Math.max(0f, targetDistance);
            }
            return;
        }

        if (routeForwardAlignment >= ROUTE_ALIGNMENT) {
            phase = Phase.IDLE;
            return;
        }
        updateBlockedAlignment(delta, routeForwardAlignment, blockedTimeoutSeconds);
    }

    public boolean consumeExplosionRequest() {
        boolean requested = explosionRequested;
        explosionRequested = false;
        return requested;
    }

    public boolean isActive() {
        return phase != Phase.IDLE;
    }

    public Phase getPhase() {
        return phase;
    }

    public boolean isModelHandoff() {
        return phase == Phase.MODEL_HANDOFF;
    }

    public boolean beginModelHandoffIfReady(
            float delta,
            boolean safelyOnRoad,
            float routeForwardAlignment,
            float minimumAssistanceSeconds) {
        if (phase == Phase.IDLE || phase == Phase.MODEL_HANDOFF) {
            return false;
        }
        assistanceSeconds += Math.max(0f, delta);
        if (!safelyOnRoad
                || !isRouteAlignedForModelHandoff(routeForwardAlignment)
                || assistanceSeconds < Math.max(0f, minimumAssistanceSeconds)) {
            return false;
        }
        phase = Phase.MODEL_HANDOFF;
        modelHandoffSeconds = 0f;
        blockedSeconds = 0f;
        return true;
    }

    public static boolean isRouteAlignedForModelHandoff(float routeForwardAlignment) {
        return Float.isFinite(routeForwardAlignment)
                && routeForwardAlignment >= MODEL_HANDOFF_MIN_ROUTE_ALIGNMENT;
    }

    public static boolean isControlAllowed(
            boolean requested,
            boolean stoppedByDebuff) {
        return requested && !stoppedByDebuff;
    }

    public ModelHandoffResult updateModelHandoff(
            float delta,
            boolean madeUsefulProgress,
            boolean remainsOnRoad,
            float timeoutSeconds) {
        if (phase != Phase.MODEL_HANDOFF) {
            return ModelHandoffResult.RESUME_ASSISTANCE;
        }
        if (madeUsefulProgress) {
            reset();
            return ModelHandoffResult.COMPLETED;
        }
        modelHandoffSeconds += Math.max(0f, delta);
        if (!remainsOnRoad
                || modelHandoffSeconds >= Math.max(0.01f, timeoutSeconds)) {
            phase = Phase.IDLE;
            assistanceSeconds = 0f;
            modelHandoffSeconds = 0f;
            return ModelHandoffResult.RESUME_ASSISTANCE;
        }
        return ModelHandoffResult.YIELDING;
    }

    public static float targetRouteProgress(float currentProgress, float routeLength) {
        return currentProgress + Math.max(0f, routeLength) * TARGET_ROUTE_FRACTION;
    }

    private void updateBlockedAlignment(
            float delta,
            float alignment,
            float blockedTimeoutSeconds) {
        if (alignment >= bestAlignment + USEFUL_ALIGNMENT_GAIN) {
            bestAlignment = alignment;
            blockedSeconds = 0f;
            return;
        }
        blockedSeconds += Math.max(0f, delta);
        if (blockedSeconds >= Math.max(0.01f, blockedTimeoutSeconds)) {
            explosionRequested = true;
            blockedSeconds = 0f;
            bestAlignment = alignment;
        }
    }

    private void resetPolicyAttempt() {
        policyAttemptSeconds = 0f;
        policyNoProgressSeconds = 0f;
        policyBestTargetDistance = 0f;
        policyBestTargetAlignment = -1f;
        scriptedAssistSeconds = 0f;
        scriptedStartTargetDistance = 0f;
        scriptedStartTargetAlignment = -1f;
    }

    private static float finiteOr(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    public static float steeringToward(
            float forwardAlignment,
            float sideAlignment) {
        if (!Float.isFinite(forwardAlignment) || !Float.isFinite(sideAlignment)) {
            return 0f;
        }
        float headingError = (float) Math.atan2(sideAlignment, forwardAlignment);
        return clamp(-headingError / ((float) Math.PI * 0.5f), -1f, 1f);
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
