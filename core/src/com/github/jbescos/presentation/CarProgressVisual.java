package com.github.jbescos.presentation;

/** Rendering-agnostic state for an XP transfer, XP award, or level-up popup. */
public final class CarProgressVisual {
    public enum Kind {
        EXPERIENCE,
        LEVEL_UP
    }

    private static final float DURATION_SECONDS = 2f;
    private static final float TRAVEL_FRACTION = 0.65f;
    private static final float FADE_START_FRACTION = 0.72f;

    private final Kind kind;
    private final int sourceVehicleId;
    private final int destinationVehicleId;
    private final int amount;
    private float ageSeconds;

    private CarProgressVisual(
            Kind kind,
            int sourceVehicleId,
            int destinationVehicleId,
            int amount) {
        this.kind = kind;
        this.sourceVehicleId = sourceVehicleId;
        this.destinationVehicleId = destinationVehicleId;
        this.amount = amount;
    }

    public static CarProgressVisual experienceTransfer(
            int sourceVehicleId,
            int destinationVehicleId,
            int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("XP transfer amount must be positive");
        }
        return new CarProgressVisual(
                Kind.EXPERIENCE,
                sourceVehicleId,
                destinationVehicleId,
                amount);
    }

    public static CarProgressVisual experienceAward(int vehicleId, int amount) {
        return experienceTransfer(vehicleId, vehicleId, amount);
    }

    public static CarProgressVisual levelUp(int vehicleId) {
        return new CarProgressVisual(Kind.LEVEL_UP, vehicleId, vehicleId, 0);
    }

    public void update(float deltaSeconds) {
        if (deltaSeconds > 0f && Float.isFinite(deltaSeconds)) {
            ageSeconds += Math.min(deltaSeconds, 1f);
        }
    }

    public boolean isActive() {
        return ageSeconds < DURATION_SECONDS;
    }

    public float getProgress() {
        float linear = Math.max(
                0f,
                Math.min(1f, ageSeconds / (DURATION_SECONDS * TRAVEL_FRACTION)));
        return linear * linear * (3f - 2f * linear);
    }

    public float getAlpha() {
        float lifetime = Math.max(0f, Math.min(1f, ageSeconds / DURATION_SECONDS));
        if (lifetime <= FADE_START_FRACTION) {
            return 1f;
        }
        return Math.max(
                0f,
                1f - (lifetime - FADE_START_FRACTION)
                        / (1f - FADE_START_FRACTION));
    }

    public Kind getKind() {
        return kind;
    }

    public int getSourceVehicleId() {
        return sourceVehicleId;
    }

    public int getDestinationVehicleId() {
        return destinationVehicleId;
    }

    public int getAmount() {
        return amount;
    }

    public boolean isTransfer() {
        return sourceVehicleId != destinationVehicleId;
    }
}
