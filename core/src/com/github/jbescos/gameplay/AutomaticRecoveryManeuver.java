package com.github.jbescos.gameplay;

/** Stateful steering helper for cars that cannot make useful progress. */
public final class AutomaticRecoveryManeuver {
    private static final float REVERSE_ENTER_ALIGNMENT = -0.55f;
    private static final float REVERSE_EXIT_ALIGNMENT = 0.15f;
    private static final float USEFUL_DISTANCE_GAIN = 0.10f;
    private static final float USEFUL_ALIGNMENT_GAIN = 0.08f;
    private static final float STALL_SECONDS = 0.85f;
    private static final float ESCAPE_GEAR_LOCK_SECONDS = 0.70f;
    private static final float MAX_CONTINUOUS_REVERSE_SECONDS = 2.20f;
    private static final float TURNAROUND_GEAR_LOCK_SECONDS = 2.0f;
    private static final float TURNAROUND_STEERING_SECONDS = 1.4f;
    private static final float MIN_THROTTLE_FACTOR = 0.32f;
    private static final float APPROACH_BRAKE_SPEED_EPSILON = 0.15f;
    private static final float MIN_APPROACH_THROTTLE_FACTOR = 0.20f;
    private static final float STEERING_DIRECTION_SPEED_EPSILON = 0.08f;

    private boolean active;
    private boolean reversing;
    private boolean replanRequested;
    private float gearLockTimer;
    private float stallTimer;
    private float continuousReverseTimer;
    private float turnaroundSteeringTimer;
    private float turnaroundDirection;
    private float bestDistance;
    private float bestDriveAlignment;

    public AutomaticRecoveryManeuver() {
    }

    public static boolean requiresContactRecovery(
            int contactCount, boolean contactCarMovingForward) {
        return contactCount > 0 && !contactCarMovingForward;
    }

    public static boolean requiresOffRoadRecovery(
            boolean offRoad, boolean safeToReturnControl) {
        return offRoad || !safeToReturnControl;
    }

    public static float calculateShortestForwardTurn(
            float forwardAlignment, float sideAlignment) {
        if (!Float.isFinite(forwardAlignment) || !Float.isFinite(sideAlignment)) {
            return 0f;
        }
        float signedAngle = (float) Math.atan2(sideAlignment, forwardAlignment);
        return Math.max(-1f, Math.min(1f, -signedAngle / ((float) Math.PI * 0.5f)));
    }

    public static boolean isSafeDirectionalHandoff(
            boolean onRoad,
            float roadMargin,
            float requiredRoadMargin,
            float totalSpeed,
            float maximumSpeed,
            float routeAlignment,
            float minimumRouteAlignment,
            float routeForwardSpeed,
            float minimumRouteForwardSpeed) {
        return onRoad
                && roadMargin >= Math.max(0f, requiredRoadMargin)
                && totalSpeed <= Math.max(0f, maximumSpeed)
                && routeAlignment >= minimumRouteAlignment
                && routeForwardSpeed >= Math.max(0f, minimumRouteForwardSpeed);
    }

    public void begin(float targetDistance, float forwardAlignment) {
        active = true;
        reversing = forwardAlignment <= REVERSE_ENTER_ALIGNMENT;
        replanRequested = false;
        gearLockTimer = 0f;
        stallTimer = 0f;
        continuousReverseTimer = 0f;
        turnaroundSteeringTimer = 0f;
        turnaroundDirection = 0f;
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
        gearLockTimer = 0f;
        stallTimer = 0f;
        continuousReverseTimer = 0f;
        turnaroundSteeringTimer = 0f;
        turnaroundDirection = 0f;
        bestDistance = 0f;
        bestDriveAlignment = 0f;
    }

    public void update(float delta, float targetDistance, float forwardAlignment) {
        if (!active) {
            begin(targetDistance, forwardAlignment);
            return;
        }

        float safeDelta = Math.max(0f, delta);
        gearLockTimer = Math.max(0f, gearLockTimer - safeDelta);
        turnaroundSteeringTimer = Math.max(0f, turnaroundSteeringTimer - safeDelta);
        continuousReverseTimer = reversing
                ? continuousReverseTimer + safeDelta
                : 0f;
        if (reversing
                && continuousReverseTimer >= MAX_CONTINUOUS_REVERSE_SECONDS) {
            reversing = false;
            continuousReverseTimer = 0f;
            gearLockTimer = TURNAROUND_GEAR_LOCK_SECONDS;
            turnaroundSteeringTimer = TURNAROUND_STEERING_SECONDS;
            turnaroundDirection = 0f;
            rememberTargetState(targetDistance, forwardAlignment);
        }
        if (gearLockTimer <= 0f) {
            if (reversing && forwardAlignment >= REVERSE_EXIT_ALIGNMENT) {
                reversing = false;
                continuousReverseTimer = 0f;
                rememberTargetState(targetDistance, forwardAlignment);
            } else if (!reversing && forwardAlignment <= REVERSE_ENTER_ALIGNMENT) {
                reversing = true;
                continuousReverseTimer = 0f;
                turnaroundSteeringTimer = 0f;
                turnaroundDirection = 0f;
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

        reversing = !reversing;
        continuousReverseTimer = 0f;
        turnaroundSteeringTimer = 0f;
        turnaroundDirection = 0f;
        gearLockTimer = ESCAPE_GEAR_LOCK_SECONDS;
        stallTimer = 0f;
        replanRequested = true;
        rememberTargetState(targetDistance, forwardAlignment);
    }

    public boolean consumeReplanRequest() {
        boolean requested = replanRequested;
        replanRequested = false;
        return requested;
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
        return calculateTurn(
                sideAlignment,
                reversing ? -STEERING_DIRECTION_SPEED_EPSILON : STEERING_DIRECTION_SPEED_EPSILON);
    }

    public float calculateTurn(float sideAlignment, float signedForwardSpeed) {
        boolean movingInReverse =
                signedForwardSpeed < -STEERING_DIRECTION_SPEED_EPSILON
                        || (Math.abs(signedForwardSpeed)
                                        <= STEERING_DIRECTION_SPEED_EPSILON
                                && reversing);
        if (!reversing && turnaroundSteeringTimer > 0f) {
            if (turnaroundDirection == 0f) {
                turnaroundDirection = sideAlignment >= 0f ? -1f : 1f;
            }
            return movingInReverse ? -turnaroundDirection : turnaroundDirection;
        }
        float turn = (movingInReverse ? sideAlignment : -sideAlignment) * 1.35f;
        return Math.max(-1f, Math.min(1f, turn));
    }

    public float limitApproachThrottle(
            float requestedThrottle,
            float signedForwardSpeed,
            float totalSpeed,
            float targetDistance,
            float brakingDistance,
            float targetRadius,
            float slowDistance,
            float maximumApproachSpeed) {
        float safeSpeed = Math.max(0f, totalSpeed);
        float availableDistance =
                Math.max(0f, targetDistance - Math.max(0f, targetRadius));
        boolean shouldBrake =
                safeSpeed > Math.max(0f, maximumApproachSpeed)
                        || (safeSpeed > APPROACH_BRAKE_SPEED_EPSILON
                                && Math.max(0f, brakingDistance) >= availableDistance);
        if (shouldBrake) {
            if (Math.abs(signedForwardSpeed) <= APPROACH_BRAKE_SPEED_EPSILON) {
                return 0f;
            }
            return signedForwardSpeed > 0f ? -1f : 1f;
        }

        float approachFactor =
                MIN_APPROACH_THROTTLE_FACTOR
                        + (1f - MIN_APPROACH_THROTTLE_FACTOR)
                                * Math.min(
                                        1f,
                                        availableDistance
                                                / Math.max(0.001f, slowDistance));
        return requestedThrottle * approachFactor;
    }

    private void rememberTargetState(float targetDistance, float forwardAlignment) {
        bestDistance = Math.max(0f, targetDistance);
        bestDriveAlignment = getDriveAlignment(forwardAlignment);
    }

    private float getDriveAlignment(float forwardAlignment) {
        return reversing ? -forwardAlignment : forwardAlignment;
    }
}
