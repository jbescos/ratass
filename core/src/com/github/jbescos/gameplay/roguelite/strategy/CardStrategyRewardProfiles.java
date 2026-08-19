package com.github.jbescos.gameplay.roguelite.strategy;

/** Default reward personalities. Properties files can override every weight during training. */
public final class CardStrategyRewardProfiles {
    private CardStrategyRewardProfiles() {
    }

    public static CardStrategyRewardConfig forProfile(String profileId) {
        if ("strategy01".equals(profileId)) {
            return config(100f, 30f, 3f, 1.25f, 0.15f, 0f,
                    0.5f, 0f, 0f, 0f, 0f, 0f);
        }
        if ("strategy02".equals(profileId)) {
            return config(100f, 30f, 3f, 1f, 0.10f, 0f,
                    0.5f, 0f, 1f, 1f, 0f, 0f);
        }
        if ("strategy03".equals(profileId)) {
            return config(100f, 30f, 3f, 1f, 0.10f, 0f,
                    0.5f, 0f, 0f, 0f, 2f, 0f);
        }
        if ("strategy04".equals(profileId)) {
            return config(100f, 30f, 3f, 1f, 0.10f, 0f,
                    0.5f, 0f, 0f, 0f, 0f, 2f);
        }
        if ("strategy05".equals(profileId)) {
            return config(100f, 30f, 3f, 1f, 0.10f, 0f,
                    0.5f, 2f, 0f, 0f, 0f, 0f);
        }
        if ("strategy06".equals(profileId)) {
            return config(100f, 35f, 4f, 1f, 0.10f, 0f,
                    0.5f, 0f, 0f, 0f, 0f, 0f);
        }
        if ("strategy07".equals(profileId)) {
            return config(100f, 30f, 3f, 1f, 0.10f, 2f,
                    0.5f, 0f, 0f, 0f, 0f, 0f);
        }
        if ("strategy08".equals(profileId)) {
            return new CardStrategyRewardConfig(
                    100f, 30f, 3f, 1f, 0.10f, 0f,
                    0.5f, 0f, 0f, 0f, 0f, 0f,
                    20f, 4f, 4f, 6f, 0.5f, 0.5f);
        }
        if ("strategy09".equals(profileId)) {
            return config(110f, 35f, 4f, 1f, 0.10f, 0f,
                    0.5f, 0.20f, 0.30f, 0.30f, 0f, 0f);
        }
        if ("strategy10".equals(profileId)) {
            return config(100f, 30f, 3f, 1f, 0.10f, 0f,
                    0.5f, 0f, 0f, 0f, 0.50f, 2f);
        }
        if ("strategy11".equals(profileId)) {
            return config(100f, 35f, 4f, 1f, 0.10f, 0f,
                    0.5f, 0f, 0f, 0.75f, 0f, 1.50f);
        }
        if ("strategy12".equals(profileId)) {
            return config(100f, 30f, 3f, 1f, 0.10f, 1.50f,
                    0.5f, 0f, 0f, 0f, 0.75f, 0.75f);
        }
        return config(100f, 30f, 3f, 1f, 0.10f, 0f,
                0.5f, 0f, 0f, 0f, 0f, 0f);
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
            float revenge) {
        return new CardStrategyRewardConfig(
                win, finalPosition, racePosition, level, experience,
                novelty, skip,
                driver, tuning, technique, powerup, revenge);
    }
}
