package com.github.jbescos.gameplay;

/** Stateful steering helper for cars that cannot make useful progress. */
public final class AutomaticRecoveryManeuver {
    private static final float REVERSE_ENTER_ALIGNMENT = -0.55f;
    private static final float REVERSE_EXIT_ALIGNMENT = 0.15f;
    private static final float USEFUL_DISTANCE_GAIN = 0.10f;
    private static final float USEFUL_ALIGNMENT_GAIN = 0.08f;
    private static final float STALL_SECONDS = 0.85f;
    private static final float ESCAPE_GEAR_LOCK_SECONDS = 0.70f;
    private static final float MIN_THROTTLE_FACTOR = 0.32f;

    private boolean active;
    private boolean reversing;
    private boolean replanRequested;
    private boolean relocationRequested;
    private float gearLockTimer;
    private float stallTimer;
    private float bestDistance;
    private float bestDriveAlignment;
    private int failedManeuvers;

    public void begin(float targetDistance, float forwardAlignment) {
        active = true;
        reversing = forwardAlignment <= REVERSE_ENTER_ALIGNMENT;
        replanRequested = false;
        relocationRequested = false;
        gearLockTimer = 0f;
        stallTimer = 0f;
        failedManeuvers = 0;
        rememberTargetState(targetDistance, forwardAlignment);
    }

    public void retarget(float targetDistance, float forwardAlignment) {
        if (!active) {
            begin(targetDistance, forwardAlignment);
            return;
        }
        stallTimer = 0f;
        rememberTargetState(targetDistance, forwardAlignment);
    }

    public void reset() {
        active = false;
        reversing = false;
        replanRequested = false;
        relocationRequested = false;
        gearLockTimer = 0f;
        stallTimer = 0f;
        bestDistance = 0f;
        bestDriveAlignment = 0f;
        failedManeuvers = 0;
    }

    public void update(float delta, float targetDistance, float forwardAlignment) {
        if (!active) {
            begin(targetDistance, forwardAlignment);
            return;
        }

        float safeDelta = Math.max(0f, delta);
        gearLockTimer = Math.max(0f, gearLockTimer - safeDelta);
        if (gearLockTimer <= 0f) {
            if (reversing && forwardAlignment >= REVERSE_EXIT_ALIGNMENT) {
                reversing = false;
                rememberTargetState(targetDistance, forwardAlignment);
            } else if (!reversing && forwardAlignment <= REVERSE_ENTER_ALIGNMENT) {
                reversing = true;
                rememberTargetState(targetDistance, forwardAlignment);
            }
        }

        float driveAlignment = getDriveAlignment(forwardAlignment);
        boolean distanceImproved = targetDistance <= bestDistance - USEFUL_DISTANCE_GAIN;
        boolean alignmentImproved = driveAlignment >= bestDriveAlignment + USEFUL_ALIGNMENT_GAIN;
        if (distanceImproved || alignmentImproved) {
            bestDistance = Math.min(bestDistance, targetDistance);
            bestDriveAlignment = Math.max(bestDriveAlignment, driveAlignment);
            stallTimer = 0f;
            return;
        }

        stallTimer += safeDelta;
        if (stallTimer < STALL_SECONDS) {
            return;
        }

        failedManeuvers++;
        reversing = !reversing;
        gearLockTimer = ESCAPE_GEAR_LOCK_SECONDS;
        stallTimer = 0f;
        replanRequested = true;
        relocationRequested = failedManeuvers >= 2;
        rememberTargetState(targetDistance, forwardAlignment);
    }

    public boolean consumeReplanRequest() {
        boolean requested = replanRequested;
        replanRequested = false;
        return requested;
    }

    public boolean isRelocationRequested() {
        return relocationRequested;
    }

    public boolean isReversing() {
        return reversing;
    }

    public float calculateThrottle(
            float forwardAlignment,
            float maximumForwardThrottle,
            float maximumReverseThrottle) {
        float driveAlignment = Math.max(0f, getDriveAlignment(forwardAlignment));
        float throttleFactor = MIN_THROTTLE_FACTOR
                + (1f - MIN_THROTTLE_FACTOR) * Math.min(1f, driveAlignment);
        float magnitude = (reversing ? maximumReverseThrottle : maximumForwardThrottle)
                * throttleFactor;
        return reversing ? -magnitude : magnitude;
    }

    public float calculateTurn(float sideAlignment) {
        float turn = (reversing ? sideAlignment : -sideAlignment) * 1.35f;
        return Math.max(-1f, Math.min(1f, turn));
    }

    private void rememberTargetState(float targetDistance, float forwardAlignment) {
        bestDistance = Math.max(0f, targetDistance);
        bestDriveAlignment = getDriveAlignment(forwardAlignment);
    }

    private float getDriveAlignment(float forwardAlignment) {
        return reversing ? -forwardAlignment : forwardAlignment;
    }
}
