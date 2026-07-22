package com.github.jbescos;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CarLongitudinalModelTest {
    private static final float CAR_AREA = 1.14f * 1.58f;
    private static final float FIXTURE_DENSITY = 1.30f;
    private static final float MASS_MULTIPLIER = 1300f / 1040f;
    private static final float MASS = CAR_AREA * FIXTURE_DENSITY * MASS_MULTIPLIER;
    private static final float LATERAL_GRIP_PER_SECOND = 15f;
    private static final float WHEEL_GRIP = 1.18f;
    private static final float ROLLING_RESISTANCE = 0.145f;

    @Test
    public void defaultGt3ForceBalanceReachesConfiguredTopSpeed() {
        float topSpeed =
                CarLongitudinalModel.DEFAULT_TOP_SPEED_KPH
                        / CarLongitudinalModel.KPH_PER_MPS;
        float availableForce = Math.min(
                CarLongitudinalModel.engineForce(550f)
                        * CarLongitudinalModel.engineForceRatio(1f),
                CarLongitudinalModel.driveTractionLimit(
                        MASS,
                        LATERAL_GRIP_PER_SECOND,
                        WHEEL_GRIP,
                        1f));
        float resistanceForce =
                CarLongitudinalModel.DEFAULT_AERO_DRAG * topSpeed * topSpeed
                        + ROLLING_RESISTANCE * MASS
                        + CarLongitudinalModel.DEFAULT_LINEAR_DAMPING * MASS * topSpeed;

        assertEquals(resistanceForce, availableForce, 0.25f);
    }

    @Test
    public void defaultGt3StillAcceleratesStronglyAt140Kph() {
        float speed = 140f / CarLongitudinalModel.KPH_PER_MPS;
        float speedRatio = speed
                / (CarLongitudinalModel.DEFAULT_TOP_SPEED_KPH
                        / CarLongitudinalModel.KPH_PER_MPS);
        float availableForce = Math.min(
                CarLongitudinalModel.engineForce(550f)
                        * CarLongitudinalModel.engineForceRatio(speedRatio),
                CarLongitudinalModel.driveTractionLimit(
                        MASS,
                        LATERAL_GRIP_PER_SECOND,
                        WHEEL_GRIP,
                        1f));
        float resistanceForce =
                CarLongitudinalModel.DEFAULT_AERO_DRAG * speed * speed
                        + ROLLING_RESISTANCE * MASS
                        + CarLongitudinalModel.DEFAULT_LINEAR_DAMPING * MASS * speed;

        assertTrue(availableForce - resistanceForce > 40f);
    }
}
