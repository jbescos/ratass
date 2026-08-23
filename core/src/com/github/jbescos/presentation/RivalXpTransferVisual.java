package com.github.jbescos.presentation;

/** Rendering-agnostic state for an XP popup or a transfer between two cars. */
public final class RivalXpTransferVisual {
    private static final float DURATION_SECONDS = 2f;
    private static final float TRAVEL_FRACTION = 0.65f;
    private static final float FADE_START_FRACTION = 0.72f;

    private final int sourceVehicleId;
    private final int destinationVehicleId;
    private final int amount;
    private final boolean transfer;
    private float ageSeconds;

    public RivalXpTransferVisual(
            int sourceVehicleId,
            int destinationVehicleId,
            int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("XP transfer amount must be positive");
        }
        this.sourceVehicleId = sourceVehicleId;
        this.destinationVehicleId = destinationVehicleId;
        this.amount = amount;
        transfer = sourceVehicleId != destinationVehicleId;
    }

    public static RivalXpTransferVisual award(int vehicleId, int amount) {
        return new RivalXpTransferVisual(vehicleId, vehicleId, amount);
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
        return transfer;
    }
}
