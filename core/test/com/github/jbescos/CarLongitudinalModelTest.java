package com.github.jbescos;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CarLongitudinalModelTest {
    private static final float CAR_AREA = 1.14f * 1.58f;
    private static final float FIXTURE_DENSITY = 1.30f;
    private static final float MASS_MULTIPLIER = 1300f / 1040f;
    private static final float MASS = CAR_AREA * FIXTURE_DENSITY * MASS_MULTIPLIER;
    private static final float HORSEPOWER_TO_FORCE = 0.70f;
    private static final float DRIVE_TRACTION_MULTIPLIER = 1.18f;
    private static final float LATERAL_GRIP_PER_SECOND = 15f;
    private static final float WHEEL_GRIP = 1.18f;
    private static final float ROLLING_RESISTANCE = 0.145f;
    private static final float LINEAR_DAMPING = 0.18f;

    @Test
    public void defaultGt3CanReachConfiguredTopSpeed() {
        float topSpeed =
                CarLongitudinalModel.DEFAULT_TOP_SPEED_KPH
                        / CarLongitudinalModel.KPH_PER_MPS;
        float engineForce =
                550f
                        * HORSEPOWER_TO_FORCE
                        * CarLongitudinalModel.engineForceRatio(1f);
        float tractionForce =
                MASS
                        * LATERAL_GRIP_PER_SECOND
                        * WHEEL_GRIP
                        * DRIVE_TRACTION_MULTIPLIER;
        float availableForce = Math.min(engineForce, tractionForce);
        float resistanceForce =
                CarLongitudinalModel.DEFAULT_AERO_DRAG * topSpeed * topSpeed
                        + ROLLING_RESISTANCE * MASS
                        + LINEAR_DAMPING * MASS * topSpeed;

        assertTrue(availableForce > resistanceForce);
        assertTrue(availableForce - resistanceForce < 2f);
    }

    @Test
    public void engineCurveKeepsLaunchForceAndHighSpeedDrive() {
        assertEquals(1f, CarLongitudinalModel.engineForceRatio(0f), 0.0001f);
        assertTrue(CarLongitudinalModel.engineForceRatio(1f) >= 0.18f);
    }
}
