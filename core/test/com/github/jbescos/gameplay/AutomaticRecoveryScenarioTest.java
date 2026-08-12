package com.github.jbescos.gameplay;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AutomaticRecoveryScenarioTest {
    private static final float[] START_ANGLES = {
        0f,
        (float) (Math.PI * 0.5),
        (float) Math.PI,
        (float) (-Math.PI * 0.5)
    };

    @Test
    public void fixedTargetRecoveryCompletesFromEveryHeading() {
        for (float startAngle : START_ANGLES) {
            ScenarioResult result = simulate(startAngle, false);

            assertTrue("did not reach target from angle=" + startAngle, result.reachedTarget);
            assertTrue("did not align to route from angle=" + startAngle, result.aligned);
        }
    }

    @Test
    public void blockedRecoveryExplodesAndThenCompletes() {
        ScenarioResult result = simulate(0f, true);

        assertTrue("blocked recovery did not request explosion", result.exploded);
        assertTrue("blocked recovery did not reach target", result.reachedTarget);
        assertTrue("blocked recovery did not align", result.aligned);
    }

    private static ScenarioResult simulate(float startAngle, boolean initiallyBlocked) {
        AutomaticRecoveryManeuver maneuver = new AutomaticRecoveryManeuver();
        SimulatedCar car = new SimulatedCar(4f, 0f, startAngle);
        float targetX = 0f;
        float targetY = 8f;
        float tangentX = 0f;
        float tangentY = 1f;
        maneuver.begin(car.distanceTo(targetX, targetY));

        ScenarioResult result = new ScenarioResult();
        boolean blocked = initiallyBlocked;
        for (int step = 0; step < 500 && maneuver.isActive(); step++) {
            float distance = car.distanceTo(targetX, targetY);
            float targetAlignment = car.alignment(targetX - car.x, targetY - car.y);
            float routeAlignment = car.alignment(tangentX, tangentY);
            maneuver.update(
                    0.05f,
                    distance,
                    targetAlignment,
                    routeAlignment,
                    0.8f,
                    1.5f);

            if (maneuver.consumeExplosionRequest()) {
                result.exploded = true;
                blocked = false;
                maneuver.begin(distance);
            }

            if (maneuver.getPhase()
                    == AutomaticRecoveryManeuver.Phase.TURN_TO_TARGET) {
                car.rotateToward(targetX - car.x, targetY - car.y, 2.8f * 0.05f);
            } else if (maneuver.getPhase()
                    == AutomaticRecoveryManeuver.Phase.DRIVE_TO_TARGET) {
                if (!blocked) {
                    car.rotateToward(targetX - car.x, targetY - car.y, 1.2f * 0.05f);
                    car.moveForward(3f * 0.05f);
                }
            } else if (maneuver.getPhase()
                    == AutomaticRecoveryManeuver.Phase.ALIGN_TO_ROUTE) {
                result.reachedTarget = distance <= 0.9f;
                car.rotateToward(tangentX, tangentY, 2.8f * 0.05f);
            }
        }
        result.aligned = car.alignment(tangentX, tangentY) >= 0.98f;
        return result;
    }

    private static final class SimulatedCar {
        private float x;
        private float y;
        private float angle;

        private SimulatedCar(float x, float y, float angle) {
            this.x = x;
            this.y = y;
            this.angle = angle;
        }

        private float distanceTo(float targetX, float targetY) {
            float dx = targetX - x;
            float dy = targetY - y;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        private float alignment(float directionX, float directionY) {
            float length = (float) Math.sqrt(directionX * directionX + directionY * directionY);
            return length <= 0.0001f
                    ? 1f
                    : (forwardX() * directionX + forwardY() * directionY) / length;
        }

        private void rotateToward(float directionX, float directionY, float maximumStep) {
            float desiredAngle = (float) Math.atan2(-directionX, directionY);
            float delta = wrapAngle(desiredAngle - angle);
            angle += clamp(delta, -maximumStep, maximumStep);
        }

        private void moveForward(float distance) {
            x += forwardX() * distance;
            y += forwardY() * distance;
        }

        private float forwardX() {
            return -(float) Math.sin(angle);
        }

        private float forwardY() {
            return (float) Math.cos(angle);
        }

        private static float wrapAngle(float value) {
            return (float) Math.atan2(Math.sin(value), Math.cos(value));
        }

        private static float clamp(float value, float minimum, float maximum) {
            return Math.max(minimum, Math.min(maximum, value));
        }
    }

    private static final class ScenarioResult {
        private boolean exploded;
        private boolean reachedTarget;
        private boolean aligned;
    }
}
