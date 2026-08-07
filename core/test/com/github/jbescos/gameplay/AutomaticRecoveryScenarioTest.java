package com.github.jbescos.gameplay;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AutomaticRecoveryScenarioTest {
    private static final float ROAD_HALF_WIDTH = 3f;
    private static final float SAFE_ROAD_MARGIN = 0.7f;
    private static final float MAX_HANDOFF_SPEED = 6f;
    private static final float MIN_HANDOFF_ALIGNMENT = 0.86f;
    private static final float MIN_HANDOFF_FORWARD_SPEED = 0.5f;
    private static final float[] START_ANGLES = {
        0f,
        (float) (Math.PI * 0.5),
        (float) Math.PI,
        (float) (-Math.PI * 0.5)
    };
    private static final float[][] BLOCKER_OFFSETS = {
        {0f, 1.2f},
        {0f, -1.2f},
        {1.2f, 0f},
        {-1.2f, 0f}
    };

    @Test
    public void offRoadCarsRecoverFromDifferentHeadingsWithoutCrossingTheRoad() {
        for (float angle : START_ANGLES) {
            ScenarioResult result = simulate(4f, 0f, angle, 8f, null);

            assertTrue("failed heading=" + angle + " " + result, result.handoffReached);
            assertTrue("crossed road heading=" + angle, result.minimumX > -ROAD_HALF_WIDTH);
        }
    }

    @Test
    public void onRoadStuckCarsAlignAndMoveForwardBeforeHandoff() {
        for (float angle : START_ANGLES) {
            ScenarioResult result = simulate(1.4f, 0f, angle, 0f, null);

            assertTrue("failed heading=" + angle + " " + result, result.handoffReached);
            assertTrue("insufficient route gain heading=" + angle, result.routeGain >= 0.8f);
        }
    }

    @Test
    public void nearbyCarsAreClearedBeforeOffRoadRecoveryForEveryRelativePosition() {
        for (float[] offset : BLOCKER_OFFSETS) {
            ScenarioResult result = simulate(4f, 0f, 0f, 0f, offset);

            assertTrue("did not separate offset=" + offset[0] + "," + offset[1],
                    result.maximumBlockerDistance > 1.8f);
            assertTrue("did not recover offset=" + offset[0] + "," + offset[1],
                    result.handoffReached);
        }
    }

    @Test
    public void nearbyCarsAreClearedBeforeOnRoadStuckRecovery() {
        for (float[] offset : BLOCKER_OFFSETS) {
            ScenarioResult result = simulate(1.4f, 0f, 0f, 0f, offset);

            assertTrue("did not separate offset=" + offset[0] + "," + offset[1],
                    result.maximumBlockerDistance > 1.8f);
            assertTrue("did not recover offset=" + offset[0] + "," + offset[1],
                    result.handoffReached);
        }
    }

    private static ScenarioResult simulate(
            float startX,
            float startY,
            float startAngle,
            float startSpeed,
            float[] blockerOffset) {
        SimulatedCar car = new SimulatedCar(startX, startY, startAngle, startSpeed);
        float blockerX = blockerOffset == null ? 0f : startX + blockerOffset[0];
        float blockerY = blockerOffset == null ? 0f : startY + blockerOffset[1];
        RecoverySeparationPlan separation = new RecoverySeparationPlan();
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        boolean separating = blockerOffset != null;
        float targetX;
        float targetY;
        if (separating) {
            separation.begin(
                    car.x,
                    car.y,
                    blockerX,
                    blockerY,
                    car.forwardX(),
                    car.forwardY(),
                    1.58f);
            targetX = separation.getTargetX();
            targetY = separation.getTargetY();
        } else {
            targetX = 0f;
            targetY = startY + 4f;
        }
        maneuver.begin(car.distanceTo(targetX, targetY), car.forwardAlignment(targetX, targetY));

        ScenarioResult result = new ScenarioResult();
        for (int step = 0; step < 800; step++) {
            if (separating) {
                float blockerDistance = car.distanceTo(blockerX, blockerY);
                result.maximumBlockerDistance =
                        Math.max(result.maximumBlockerDistance, blockerDistance);
                if (separation.hasMoved(car.x, car.y, 1.58f)
                        || separation.hasClearance(
                                car.x,
                                car.y,
                                blockerX,
                                blockerY,
                                1.8f)) {
                    separating = false;
                    targetX = 0f;
                    targetY = car.y + 4f;
                    maneuver.begin(
                            car.distanceTo(targetX, targetY),
                            car.forwardAlignment(targetX, targetY));
                }
            } else if (car.distanceTo(targetX, targetY) <= 0.87f) {
                targetX = 0f;
                targetY += 4f;
                maneuver.retarget(
                        car.distanceTo(targetX, targetY),
                        car.forwardAlignment(targetX, targetY));
            }

            float distance = car.distanceTo(targetX, targetY);
            float forwardAlignment = car.forwardAlignment(targetX, targetY);
            float sideAlignment = car.sideAlignment(targetX, targetY);
            maneuver.update(0.05f, distance, forwardAlignment);
            float throttle = maneuver.calculateThrottle(forwardAlignment, 0.86f, 0.62f);
            throttle = maneuver.limitApproachThrottle(
                    throttle,
                    car.speed,
                    Math.abs(car.speed),
                    distance,
                    car.brakingDistance(),
                    0.87f,
                    6.32f,
                    MAX_HANDOFF_SPEED);
            car.step(throttle, maneuver.calculateTurn(sideAlignment, car.speed), 0.05f);
            result.minimumX = Math.min(result.minimumX, car.x);
            result.routeGain = car.y - startY;

            float routeAlignment = car.forwardY();
            float routeForwardSpeed = car.forwardY() * car.speed;
            float roadMargin = ROAD_HALF_WIDTH - Math.abs(car.x);
            boolean safe = AutomaticRecoveryManeuver.isSafeDirectionalHandoff(
                    Math.abs(car.x) <= ROAD_HALF_WIDTH,
                    roadMargin,
                    SAFE_ROAD_MARGIN,
                    Math.abs(car.speed),
                    MAX_HANDOFF_SPEED,
                    routeAlignment,
                    MIN_HANDOFF_ALIGNMENT,
                    routeForwardSpeed,
                    MIN_HANDOFF_FORWARD_SPEED);
            if (!separating && safe && result.routeGain >= 0.8f) {
                result.handoffReached = true;
                break;
            }
        }
        result.finalX = car.x;
        result.finalY = car.y;
        result.finalAngle = car.angle;
        result.finalSpeed = car.speed;
        return result;
    }

    private static final class SimulatedCar {
        private float x;
        private float y;
        private float angle;
        private float speed;

        private SimulatedCar(float x, float y, float angle, float speed) {
            this.x = x;
            this.y = y;
            this.angle = angle;
            this.speed = speed;
        }

        private void step(float throttle, float turn, float delta) {
            if (Math.abs(speed) > 0.05f
                    && Math.signum(speed) != Math.signum(throttle)
                    && Math.abs(throttle) > 0.01f) {
                float removed = Math.min(Math.abs(speed), 14f * Math.abs(throttle) * delta);
                speed -= Math.signum(speed) * removed;
            } else {
                speed += throttle * 7f * delta;
            }
            speed *= Math.max(0f, 1f - 0.12f * delta);
            speed = clamp(speed, -8f, 12f);

            float yawRate =
                    speed / 1.2f * (float) Math.tan(turn * 0.55f);
            yawRate = clamp(yawRate, -3f, 3f);
            angle += yawRate * delta;
            x += forwardX() * speed * delta;
            y += forwardY() * speed * delta;
        }

        private float brakingDistance() {
            return speed * speed / (2f * 14f);
        }

        private float distanceTo(float targetX, float targetY) {
            float dx = targetX - x;
            float dy = targetY - y;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        private float forwardAlignment(float targetX, float targetY) {
            float distance = distanceTo(targetX, targetY);
            return distance <= 0.0001f
                    ? 1f
                    : ((targetX - x) * forwardX() + (targetY - y) * forwardY()) / distance;
        }

        private float sideAlignment(float targetX, float targetY) {
            float distance = distanceTo(targetX, targetY);
            return distance <= 0.0001f
                    ? 0f
                    : ((targetX - x) * sideX() + (targetY - y) * sideY()) / distance;
        }

        private float forwardX() {
            return -(float) Math.sin(angle);
        }

        private float forwardY() {
            return (float) Math.cos(angle);
        }

        private float sideX() {
            return (float) Math.cos(angle);
        }

        private float sideY() {
            return (float) Math.sin(angle);
        }

        private static float clamp(float value, float minimum, float maximum) {
            return Math.max(minimum, Math.min(maximum, value));
        }
    }

    private static final class ScenarioResult {
        private boolean handoffReached;
        private float minimumX = Float.MAX_VALUE;
        private float maximumBlockerDistance;
        private float routeGain;
        private float finalX;
        private float finalY;
        private float finalAngle;
        private float finalSpeed;

        @Override
        public String toString() {
            return "x=" + finalX
                    + " y=" + finalY
                    + " angle=" + finalAngle
                    + " speed=" + finalSpeed
                    + " routeGain=" + routeGain;
        }
    }
}
