package com.github.jbescos.gameplay.roguelite;

public final class DriverProfileMetadata {
    public static final int SCHEMA_VERSION = 1;

    private final String profileId;
    private final String policySha256;
    private final String benchmarkVersion;
    private final float overallRating;
    private final float paceRating;
    private final float controlRating;
    private final float consistencyRating;
    private final float finishRate;
    private final float averageFastestLapSeconds;
    private final float averageLapSeconds;
    private final float averageOffRoadActions;

    public DriverProfileMetadata(
            String profileId,
            String policySha256,
            String benchmarkVersion,
            float overallRating,
            float paceRating,
            float controlRating,
            float consistencyRating,
            float finishRate,
            float averageFastestLapSeconds,
            float averageLapSeconds,
            float averageOffRoadActions) {
        if (profileId == null || profileId.trim().length() == 0) {
            throw new IllegalArgumentException("Driver profile ID is required.");
        }
        this.profileId = profileId.trim();
        this.policySha256 = policySha256 == null ? "" : policySha256;
        this.benchmarkVersion = benchmarkVersion == null ? "" : benchmarkVersion;
        this.overallRating = clampRating(overallRating);
        this.paceRating = clampRating(paceRating);
        this.controlRating = clampRating(controlRating);
        this.consistencyRating = clampRating(consistencyRating);
        this.finishRate = clamp01(finishRate);
        this.averageFastestLapSeconds = Math.max(0f, averageFastestLapSeconds);
        this.averageLapSeconds = Math.max(0f, averageLapSeconds);
        this.averageOffRoadActions = Math.max(0f, averageOffRoadActions);
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

    public float getOverallRating() {
        return overallRating;
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
                data.overallRating,
                data.paceRating,
                data.controlRating,
                data.consistencyRating,
                data.finishRate,
                data.averageFastestLapSeconds,
                data.averageLapSeconds,
                data.averageOffRoadActions);
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
        public float overallRating;
        public float paceRating;
        public float controlRating;
        public float consistencyRating;
        public float finishRate;
        public float averageFastestLapSeconds;
        public float averageLapSeconds;
        public float averageOffRoadActions;
    }
}
