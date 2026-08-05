package com.github.jbescos.gameplay.roguelite;

public final class RogueliteCompetitorProgress {
    private static final int BASE_LEVEL_XP = 80;
    private static final int LEVEL_XP_INCREMENT = 40;
    private static final int LAST_PLACE_XP = 30;
    private static final int FIRST_PLACE_XP = 100;

    private final RogueliteLoadout loadout;
    private int level = 1;
    private int experience;
    private int pendingRewards;
    private int rewardDeferredUntilLevel;

    RogueliteCompetitorProgress(String defaultDriverProfileId) {
        loadout = new RogueliteLoadout(defaultDriverProfileId);
    }

    public RogueliteLoadout getLoadout() {
        return loadout;
    }

    public int getLevel() {
        return level;
    }

    public int getExperience() {
        return experience;
    }

    public int getExperienceForNextLevel() {
        return experienceForLevel(level);
    }

    public int getPendingRewards() {
        return pendingRewards;
    }

    public boolean hasPendingReward() {
        return pendingRewards > 0;
    }

    public boolean hasOfferableReward() {
        return pendingRewards > 0 && rewardDeferredUntilLevel == 0;
    }

    int getRewardDeferredUntilLevel() {
        return rewardDeferredUntilLevel;
    }

    boolean isPristine() {
        return level == 1
                && experience == 0
                && pendingRewards == 0
                && rewardDeferredUntilLevel == 0
                && loadout.getModifications().isEmpty();
    }

    public int awardRacePosition(int position, int fieldSize) {
        int gained = experienceForPosition(position, fieldSize);
        experience += gained;
        while (experience >= getExperienceForNextLevel()) {
            experience -= getExperienceForNextLevel();
            level++;
            pendingRewards++;
            if (rewardDeferredUntilLevel > 0
                    && level >= rewardDeferredUntilLevel) {
                rewardDeferredUntilLevel = 0;
            }
        }
        return gained;
    }

    boolean deferRewardsUntilNextLevel() {
        if (!hasOfferableReward()) {
            return false;
        }
        rewardDeferredUntilLevel = level + 1;
        return true;
    }

    boolean consumePendingReward() {
        if (pendingRewards <= 0) {
            return false;
        }
        pendingRewards--;
        if (pendingRewards == 0) {
            rewardDeferredUntilLevel = 0;
        }
        return true;
    }

    void restore(
            int restoredLevel,
            int restoredExperience,
            int restoredPendingRewards,
            int restoredRewardDeferredUntilLevel) {
        if (restoredLevel < 1
                || restoredExperience < 0
                || restoredExperience >= experienceForLevel(restoredLevel)
                || restoredPendingRewards < 0
                || restoredRewardDeferredUntilLevel < 0
                || (restoredRewardDeferredUntilLevel > 0
                        && (restoredPendingRewards == 0
                                || restoredRewardDeferredUntilLevel
                                        <= restoredLevel))) {
            throw new IllegalArgumentException("Invalid roguelite competitor progress.");
        }
        level = restoredLevel;
        experience = restoredExperience;
        pendingRewards = restoredPendingRewards;
        rewardDeferredUntilLevel = restoredRewardDeferredUntilLevel;
    }

    static int experienceForPosition(int position, int fieldSize) {
        if (fieldSize <= 1) {
            return FIRST_PLACE_XP;
        }
        int clampedPosition = Math.max(1, Math.min(fieldSize, position));
        float finishRatio =
                (fieldSize - clampedPosition) / (float) (fieldSize - 1);
        return Math.round(
                LAST_PLACE_XP
                        + (FIRST_PLACE_XP - LAST_PLACE_XP) * finishRatio);
    }

    private static int experienceForLevel(int currentLevel) {
        return BASE_LEVEL_XP + Math.max(0, currentLevel - 1) * LEVEL_XP_INCREMENT;
    }
}
