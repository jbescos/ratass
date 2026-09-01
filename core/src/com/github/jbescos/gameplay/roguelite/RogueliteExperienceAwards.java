package com.github.jbescos.gameplay.roguelite;

public final class RogueliteExperienceAwards {
    public static final int PASS_RIVAL = 6;
    public static final int FASTEST_LAP = 6;
    public static final int LAP_COMPLETE = 5;
    public static final int REVENGE = 4;
    public static final int PUSH_RIVAL_OFF_ROAD = 6;
    public static final int DRIFT_SECOND = 1;
    public static final int MAX_RACECRAFT_XP_PER_LAP = 40;

    public static final float DRIFT_AWARD_INTERVAL_SECONDS = 1f;
    public static final float DRIFT_MIN_SPEED_RATIO = 0.18f;
    public static final float DRIFT_START_LATERAL_SLIP = 0.32f;
    public static final float DRIFT_STOP_LATERAL_SLIP = 0.25f;

    private RogueliteExperienceAwards() {
    }

    public static int forPositionsGained(int positionsGained) {
        return forPositionsGained(positionsGained, PASS_RIVAL);
    }

    public static int forPositionsGained(int positionsGained, int experiencePerPosition) {
        return Math.max(0, positionsGained) * Math.max(0, experiencePerPosition);
    }

    public static int completedDriftSeconds(float accumulatedDriftSeconds) {
        if (Float.isNaN(accumulatedDriftSeconds)
                || Float.isInfinite(accumulatedDriftSeconds)) {
            return 0;
        }
        return Math.max(
                0,
                (int) Math.floor(
                        Math.max(0f, accumulatedDriftSeconds)
                                / DRIFT_AWARD_INTERVAL_SECONDS));
    }

    public static float remainingDriftSeconds(float accumulatedDriftSeconds) {
        if (Float.isNaN(accumulatedDriftSeconds)
                || Float.isInfinite(accumulatedDriftSeconds)) {
            return 0f;
        }
        int completedSeconds = completedDriftSeconds(accumulatedDriftSeconds);
        return Math.max(
                0f,
                accumulatedDriftSeconds
                        - completedSeconds * DRIFT_AWARD_INTERVAL_SECONDS);
    }

    public static boolean isDrifting(
            boolean wasDrifting,
            boolean onRoad,
            float speedRatio,
            float lateralSlip) {
        if (!onRoad
                || Float.isNaN(speedRatio)
                || Float.isInfinite(speedRatio)
                || Float.isNaN(lateralSlip)
                || Float.isInfinite(lateralSlip)
                || speedRatio < DRIFT_MIN_SPEED_RATIO) {
            return false;
        }
        float slipThreshold =
                wasDrifting
                        ? DRIFT_STOP_LATERAL_SLIP
                        : DRIFT_START_LATERAL_SLIP;
        return lateralSlip >= slipThreshold;
    }

    public static String formatNotice(Reason reason, int amount) {
        if (reason == null || amount <= 0) {
            return "";
        }
        return reason.getDisplayName() + " +" + amount + " XP";
    }

    public enum Reason {
        OVERTAKE("Overtake", PASS_RIVAL, true),
        FASTEST_LAP("Fastest lap", RogueliteExperienceAwards.FASTEST_LAP, true),
        REVENGE("Revenge", RogueliteExperienceAwards.REVENGE, true),
        PUSH_OFF_ROAD("Push off-road", PUSH_RIVAL_OFF_ROAD, true),
        DRIFT("Drift", DRIFT_SECOND, true),
        LAP_COMPLETE("Lap complete", RogueliteExperienceAwards.LAP_COMPLETE, true),
        FINISH("Finish", 0, false);

        private final String displayName;
        private final int defaultExperience;
        private final boolean customizable;

        Reason(String displayName, int defaultExperience, boolean customizable) {
            this.displayName = displayName;
            this.defaultExperience = defaultExperience;
            this.customizable = customizable;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getDefaultExperience() {
            return defaultExperience;
        }

        public boolean isCustomizable() {
            return customizable;
        }
    }
}
