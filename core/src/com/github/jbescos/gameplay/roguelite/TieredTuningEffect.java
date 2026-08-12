package com.github.jbescos.gameplay.roguelite;

/** Permanent tuning limited to power, grip, aerodynamic efficiency, and mass. */
final class TieredTuningEffect extends RogueliteUpgradeEffect {
    private final float accelerationBonus;
    private final float dragMultiplier;
    private final float massMultiplier;
    private final float gripBonus;

    TieredTuningEffect(RogueliteCardId cardId) {
        super(cardId);
        TuningSetup setup = setup(cardId);
        accelerationBonus = setup.powerBonus;
        dragMultiplier = 1f / (1f + setup.aeroBonus);
        massMultiplier = setup.massMultiplier;
        gripBonus = setup.gripBonus;
    }

    @Override
    boolean isActive() {
        return true;
    }

    @Override
    float accelerationBonus() {
        return accelerationBonus;
    }

    @Override
    float driveForceLimitMultiplier() {
        return (1f + accelerationBonus) / massMultiplier;
    }

    @Override
    float dragMultiplier() {
        return dragMultiplier;
    }

    @Override
    float massMultiplier() {
        return massMultiplier;
    }

    @Override
    float gripBonus(float slip) {
        return gripBonus;
    }

    private static TuningSetup setup(RogueliteCardId cardId) {
        switch (cardId) {
            // Tier 1: two benefits and one drawback.
            case CLUB_TUNE:
                return tuning(0.07f, 0.03f, -0.08f, 1f);
            case SPORT_TUNE:
                return tuning(0.10f, 0.04f, 0f, 1.08f);
            case AERO_TRIM:
                return tuning(0.12f, -0.02f, 0.14f, 1f);
            case SHORT_GEARING:
                return tuning(0.13f, 0f, 0.13f, 1.05f);
            case CARBON_PANELS:
                return tuning(0.10f, -0.03f, 0f, 0.96f);
            case FEATHERWEIGHT_DRIVE:
                return tuning(0.07f, 0f, -0.08f, 0.97f);
            case TRACK_WING:
                return tuning(-0.03f, 0.06f, 0.15f, 1f);
            case GROUNDED_AERO:
                return tuning(0f, 0.06f, 0.18f, 1.02f);
            case LIGHT_COMPOUND:
                return tuning(-0.04f, 0.05f, 0f, 0.92f);
            case AGILE_CHASSIS:
                return tuning(0f, 0.05f, -0.07f, 0.96f);
            case STREAMLINED_CHASSIS:
                return tuning(-0.03f, 0f, 0.18f, 0.90f);
            case LOW_DRAG_FEATHERWEIGHT:
                return tuning(0f, -0.02f, 0.18f, 0.90f);

            // Tier 2: the same combinations with stronger benefits.
            case RACE_TUNE:
                return tuning(0.14f, 0.05f, -0.08f, 1f);
            case HEAVYWEIGHT_TUNE:
                return tuning(0.16f, 0.06f, 0f, 1.06f);
            case LOW_DRAG_BODY:
                return tuning(0.22f, -0.03f, 0.20f, 1f);
            case DRIFT_DIFFERENTIAL:
                return tuning(0.22f, 0f, 0.24f, 1.05f);
            case CARBON_MONOCOQUE:
                return tuning(0.15f, -0.03f, 0f, 0.92f);
            case TITANIUM_DRIVE:
                return tuning(0.14f, 0f, -0.05f, 0.96f);
            case DOWNFORCE_PACKAGE:
                return tuning(-0.03f, 0.12f, 0.30f, 1f);
            case GROUNDED_DOWNFORCE:
                return tuning(0f, 0.12f, 0.30f, 1.03f);
            case MAGNESIUM_SUSPENSION:
                return tuning(-0.05f, 0.08f, 0f, 0.90f);
            case AERO_AGILE_CHASSIS:
                return tuning(0f, 0.07f, -0.05f, 0.92f);
            case CARBON_LONGTAIL:
                return tuning(-0.04f, 0f, 0.32f, 0.82f);
            case VENTURI_MONOCOQUE:
                return tuning(0f, -0.02f, 0.32f, 0.82f);

            // Tier 3: two benefits and no drawback. Each attribute pair has two biases.
            case CHAMPIONSHIP_TUNE:
                return tuning(0.18f, 0f, 0.42f, 1f);
            case GROUND_EFFECT:
                return tuning(0f, 0.13f, 0.28f, 1f);
            case VELOCITY_SHELL:
                return tuning(0.14f, 0.04f, 0f, 1f);
            case TORQUE_VECTORING:
                return tuning(0.10f, 0.07f, 0f, 1f);
            case GRAPHENE_CHASSIS:
                return tuning(0f, 0.11f, 0f, 0.94f);
            case TITANIUM_SKELETON:
                return tuning(0.16f, 0f, 0f, 0.96f);
            case HYPERCAR_CORE:
                return tuning(0.22f, 0f, 0.24f, 1f);
            case ACTIVE_AERO_SHELL:
                return tuning(0f, 0f, 0.58f, 0.84f);
            case CARBON_PROTOTYPE:
                return tuning(0.07f, 0f, 0f, 0.90f);
            case TRACK_VACUUM:
                return tuning(0f, 0.12f, 0.44f, 1f);
            case WING_CAR:
                return tuning(0f, 0f, 0.24f, 0.82f);
            case FEATHERWEIGHT_GROUND_EFFECT:
                return tuning(0f, 0.07f, 0f, 0.90f);
            default:
                throw new IllegalArgumentException("Unsupported tuning card: " + cardId);
        }
    }

    private static TuningSetup tuning(
            float powerBonus,
            float gripBonus,
            float aeroBonus,
            float massMultiplier) {
        return new TuningSetup(powerBonus, gripBonus, aeroBonus, massMultiplier);
    }

    private static final class TuningSetup {
        private final float powerBonus;
        private final float gripBonus;
        private final float aeroBonus;
        private final float massMultiplier;

        private TuningSetup(
                float powerBonus,
                float gripBonus,
                float aeroBonus,
                float massMultiplier) {
            this.powerBonus = powerBonus;
            this.gripBonus = gripBonus;
            this.aeroBonus = aeroBonus;
            this.massMultiplier = massMultiplier;
        }
    }
}
