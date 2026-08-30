package com.github.jbescos.gameplay.roguelite;

public final class DriverProfileMetadata {
    public static final int SCHEMA_VERSION = 3;
    public static final int DEFAULT_ACTION_REPEAT = 4;
    private static final float REFERENCE_TOP_SPEED_KPH = 300f;

    private final String profileId;
    private final String policySha256;
    private final String benchmarkVersion;
    private final float paceRating;
    private final float controlRating;
    private final float consistencyRating;
    private final float finishRate;
    private final float averageFastestLapSeconds;
    private final float averageLapSeconds;
    private final float averageOffRoadActions;
    private final float averageOffRoadPercent;
    private final float averageDriftPercent;
    private final float maximumSpeedKph;
    private final int tier;
    private final int actionRepeat;

    public DriverProfileMetadata(
            String profileId,
            String policySha256,
            String benchmarkVersion,
            float paceRating,
            float controlRating,
            float consistencyRating,
            float finishRate,
            float averageFastestLapSeconds,
            float averageLapSeconds,
            float averageOffRoadActions) {
        this(
                profileId,
                policySha256,
                benchmarkVersion,
                paceRating,
                controlRating,
                consistencyRating,
                finishRate,
                averageFastestLapSeconds,
                averageLapSeconds,
                averageOffRoadActions,
                0f,
                0f,
                0f,
                0,
                DEFAULT_ACTION_REPEAT);
    }

    public DriverProfileMetadata(
            String profileId,
            String policySha256,
            String benchmarkVersion,
            float paceRating,
            float controlRating,
            float consistencyRating,
            float finishRate,
            float averageFastestLapSeconds,
            float averageLapSeconds,
            float averageOffRoadActions,
            float averageDriftPercent) {
        this(
                profileId,
                policySha256,
                benchmarkVersion,
                paceRating,
                controlRating,
                consistencyRating,
                finishRate,
                averageFastestLapSeconds,
                averageLapSeconds,
                averageOffRoadActions,
                0f,
                averageDriftPercent,
                0f,
                0,
                DEFAULT_ACTION_REPEAT);
    }

    public DriverProfileMetadata(
            String profileId,
            String policySha256,
            String benchmarkVersion,
            float paceRating,
            float controlRating,
            float consistencyRating,
            float finishRate,
            float averageFastestLapSeconds,
            float averageLapSeconds,
            float averageOffRoadActions,
            float averageOffRoadPercent,
            float averageDriftPercent,
            float maximumSpeedKph) {
        this(
                profileId,
                policySha256,
                benchmarkVersion,
                paceRating,
                controlRating,
                consistencyRating,
                finishRate,
                averageFastestLapSeconds,
                averageLapSeconds,
                averageOffRoadActions,
                averageOffRoadPercent,
                averageDriftPercent,
                maximumSpeedKph,
                0,
                DEFAULT_ACTION_REPEAT);
    }

    public DriverProfileMetadata(
            String profileId,
            String policySha256,
            String benchmarkVersion,
            float paceRating,
            float controlRating,
            float consistencyRating,
            float finishRate,
            float averageFastestLapSeconds,
            float averageLapSeconds,
            float averageOffRoadActions,
            float averageOffRoadPercent,
            float averageDriftPercent,
            float maximumSpeedKph,
            int tier,
            int actionRepeat) {
        if (profileId == null || profileId.trim().length() == 0) {
            throw new IllegalArgumentException("Driver profile ID is required.");
        }
        this.profileId = profileId.trim();
        this.policySha256 = policySha256 == null ? "" : policySha256;
        this.benchmarkVersion = benchmarkVersion == null ? "" : benchmarkVersion;
        this.paceRating = clampRating(paceRating);
        this.controlRating = clampRating(controlRating);
        this.consistencyRating = clampRating(consistencyRating);
        this.finishRate = clamp01(finishRate);
        this.averageFastestLapSeconds = Math.max(0f, averageFastestLapSeconds);
        this.averageLapSeconds = Math.max(0f, averageLapSeconds);
        this.averageOffRoadActions = Math.max(0f, averageOffRoadActions);
        this.averageOffRoadPercent = clampRating(averageOffRoadPercent);
        this.averageDriftPercent = clampRating(averageDriftPercent);
        this.maximumSpeedKph = Math.max(0f, maximumSpeedKph);
        this.tier = Math.max(0, tier);
        this.actionRepeat = actionRepeat > 0 ? actionRepeat : DEFAULT_ACTION_REPEAT;
    }

    public String getProfileId() {
        return profileId;
    }

    public String getPolicySha256() {
        return policySha256;
    }

    public String getBenchmarkVersion() {
        return benchmarkVersion;
    }

    public float getPaceRating() {
        return paceRating;
    }

    public float getControlRating() {
        return controlRating;
    }

    public float getConsistencyRating() {
        return consistencyRating;
    }

    public float getFinishRate() {
        return finishRate;
    }

    public float getAverageFastestLapSeconds() {
        return averageFastestLapSeconds;
    }

    public float getAverageLapSeconds() {
        return averageLapSeconds;
    }

    public float getAverageOffRoadActions() {
        return averageOffRoadActions;
    }

    public float getAverageDriftPercent() {
        return averageDriftPercent;
    }

    public float getAverageOffRoadPercent() {
        return averageOffRoadPercent;
    }

    public float getMaximumSpeedKph() {
        return maximumSpeedKph;
    }

    public int getTier() {
        return tier;
    }

    public int getActionRepeat() {
        return actionRepeat;
    }

    public float getOffRoadRating() {
        return clampRating(100f - averageOffRoadPercent * 10f);
    }

    public float getDriftRating() {
        return clampRating(averageDriftPercent * 5f);
    }

    public float getMaximumSpeedRating() {
        return clampRating(maximumSpeedKph / REFERENCE_TOP_SPEED_KPH * 100f);
    }

    public static DriverProfileMetadata fromData(Data data, String expectedProfileId) {
        if (data == null
                || data.schemaVersion != SCHEMA_VERSION
                || data.profileId == null
                || !data.profileId.equals(expectedProfileId)) {
            return null;
        }
        return new DriverProfileMetadata(
                data.profileId,
                data.policySha256,
                data.benchmarkVersion,
                data.paceRating,
                data.controlRating,
                data.consistencyRating,
                data.finishRate,
                data.averageFastestLapSeconds,
                data.averageLapSeconds,
                data.averageOffRoadActions,
                data.averageOffRoadPercent,
                data.averageDriftPercent,
                data.maximumSpeedKph,
                data.tier,
                data.actionRepeat);
    }

    static DriverProfileMetadata fallback(String profileId, int order) {
        float rating = Math.max(1f, Math.min(99f, 5f + order * 8f));
        return new DriverProfileMetadata(
                profileId,
                "",
                "unrated",
                rating,
                rating,
                rating,
                0f,
                0f,
                0f,
                0f);
    }

    private static float clampRating(float value) {
        return Math.max(0f, Math.min(100f, value));
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    public static final class Data {
        public int schemaVersion;
        public String profileId = "";
        public String policySha256 = "";
        public String benchmarkVersion = "";
        public float paceRating;
        public float controlRating;
        public float consistencyRating;
        public float finishRate;
        public float averageFastestLapSeconds;
        public float averageLapSeconds;
        public float averageOffRoadActions;
        public float averageOffRoadPercent;
        public float averageDriftPercent;
        public float maximumSpeedKph;
        public int tier;
        public int actionRepeat;
    }
}
