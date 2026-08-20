package com.github.jbescos.gameplay.roguelite.strategy;

/** Default reward personalities. Properties files can override every weight during training. */
public final class CardStrategyRewardProfiles {
    private CardStrategyRewardProfiles() {
    }

    public static CardStrategyRewardConfig forProfile(String profileId) {
        if ("strategy01".equals(profileId)) {
            return config(100f, 30f, 3f, 1f, 0.10f, 0f,
                    4f, 0f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 2.5f);
        }
        if ("strategy02".equals(profileId)) {
            return config(100f, 30f, 3f, 1f, 0.10f, 0f,
                    0.5f, 0f, 0f, 0f, 0f, 0f,
                    0f, 0f, 30f, 0f);
        }
        if ("strategy08".equals(profileId)) {
            return new CardStrategyRewardConfig(
                    100f, 30f, 3f, 1f, 0.10f, 0f,
                    0.5f, 0f, 0f, 0f, 0f, 0f,
                    10f, 10f, 10f, 20f, 0f, 0f,
                    "", 0f, "", 0f,
                    0f, 0f, 0f, 0f);
        }
        return config(200f, 40f, 2f, 0.25f, 0.02f, 0f,
                0.5f, 0f, 0f, 0f, 0f, 0f,
                3f, 0f, 0f, 0f);
    }

    private static CardStrategyRewardConfig config(
            float win,
            float finalPosition,
            float racePosition,
            float level,
            float experience,
            float novelty,
            float skip,
            float driver,
            float tuning,
            float technique,
            float powerup,
            float revenge,
            float lapWin,
            float cardSelection,
            float tuningTechniqueSynergy,
            float cardTypeRotation) {
        return new CardStrategyRewardConfig(
                win, finalPosition, racePosition, level, experience,
                novelty, skip,
                driver, tuning, technique, powerup, revenge,
                0f, 0f, 0f, 0f, 0f, 0f,
                "", 0f, "", 0f,
                lapWin, cardSelection, tuningTechniqueSynergy, cardTypeRotation);
    }
}
