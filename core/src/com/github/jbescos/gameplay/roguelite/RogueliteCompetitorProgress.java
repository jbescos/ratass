package com.github.jbescos.gameplay.roguelite;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class RogueliteCompetitorProgress {
    private static final int BASE_LEVEL_XP = 80;
    private static final int LAST_PLACE_XP = 30;
    private static final int FIRST_PLACE_XP = 100;

    private final RogueliteLoadout loadout;
    private final int levelXpIncrement;
    private final Set<String> acquiredDriverProfileIds =
            new LinkedHashSet<String>();
    private final Set<RogueliteCardId> acquiredModificationCardIds =
            new LinkedHashSet<RogueliteCardId>();
    private final Set<String> readOnlyAcquiredDriverProfileIds =
            Collections.unmodifiableSet(acquiredDriverProfileIds);
    private final Set<RogueliteCardId> readOnlyAcquiredModificationCardIds =
            Collections.unmodifiableSet(acquiredModificationCardIds);
    private int level = 1;
    private int experience;
    private int lapExperience;
    private int raceExperience;
    private RogueliteExperienceAwards.Reason lastExperienceReason;
    private int lastExperienceAmount;
    private int pendingRewards;
    private boolean tierFourUnlocked;

    RogueliteCompetitorProgress(String defaultDriverProfileId) {
        this(defaultDriverProfileId, CustomGameRules.DEFAULT_LEVEL_XP_INCREMENT);
    }

    RogueliteCompetitorProgress(
            String defaultDriverProfileId,
            int configuredLevelXpIncrement) {
        loadout = new RogueliteLoadout(defaultDriverProfileId);
        acquiredDriverProfileIds.add(loadout.getDriverProfileId());
        levelXpIncrement = Math.max(0, configuredLevelXpIncrement);
    }

    public RogueliteLoadout getLoadout() {
        return loadout;
    }

    Set<String> getAcquiredDriverProfileIds() {
        return readOnlyAcquiredDriverProfileIds;
    }

    Set<RogueliteCardId> getAcquiredModificationCardIds() {
        return readOnlyAcquiredModificationCardIds;
    }

    boolean hasAcquiredDriver(String profileId) {
        return profileId != null && acquiredDriverProfileIds.contains(profileId);
    }

    boolean hasAcquiredModification(RogueliteCardId cardId) {
        return cardId != null && acquiredModificationCardIds.contains(cardId);
    }

    void recordAcquiredDriver(String profileId) {
        if (profileId == null || profileId.trim().length() == 0) {
            throw new IllegalArgumentException("Driver profile ID is required.");
        }
        acquiredDriverProfileIds.add(profileId.trim());
    }

    void recordAcquiredModification(RogueliteCardId cardId) {
        if (cardId == null) {
            throw new IllegalArgumentException("Card ID is required.");
        }
        acquiredModificationCardIds.add(cardId);
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

    public int getLapExperience() {
        return lapExperience;
    }

    public int getRaceExperience() {
        return raceExperience;
    }

    public RogueliteExperienceAwards.Reason getLastExperienceReason() {
        return lastExperienceReason;
    }

    public int getLastExperienceAmount() {
        return lastExperienceAmount;
    }

    public int getPendingRewards() {
        return pendingRewards;
    }

    public boolean isTierFourUnlocked() {
        return tierFourUnlocked;
    }

    boolean unlockTierFour() {
        if (tierFourUnlocked) {
            return false;
        }
        tierFourUnlocked = true;
        return true;
    }

    public boolean hasPendingReward() {
        return pendingRewards > 0;
    }

    public boolean hasOfferableReward() {
        return pendingRewards > 0;
    }

    boolean isPristine() {
        return level == 1
                && experience == 0
                && lapExperience == 0
                && pendingRewards == 0
                && loadout.getModifications().isEmpty();
    }

    public int awardRacePosition(int position, int fieldSize) {
        int gained = awardExperience(experienceForPosition(position, fieldSize));
        recordRaceAward(RogueliteExperienceAwards.Reason.FINISH, gained);
        return gained;
    }

    public int awardExperience(int amount) {
        if (hasPendingReward() || amount <= 0) {
            return 0;
        }
        int gained = amount;
        experience += gained;
        if (experience >= getExperienceForNextLevel()) {
            experience -= getExperienceForNextLevel();
            level++;
            pendingRewards = 1;
            experience = Math.min(experience, getExperienceForNextLevel() - 1);
        }
        return gained;
    }

    int awardRacecraftExperience(int amount, int lapExperienceCap) {
        return awardRacecraftExperience(null, amount, lapExperienceCap);
    }

    int awardRacecraftExperience(
            RogueliteExperienceAwards.Reason reason,
            int amount,
            int lapExperienceCap) {
        int remaining = Math.max(0, lapExperienceCap - lapExperience);
        int gained = awardExperience(Math.min(amount, remaining));
        lapExperience += gained;
        recordRaceAward(reason, gained);
        return gained;
    }

    void resetLapExperience() {
        lapExperience = 0;
    }

    void resetRaceExperience() {
        lapExperience = 0;
        raceExperience = 0;
        lastExperienceReason = null;
        lastExperienceAmount = 0;
    }

    void restoreLapExperience(int restoredLapExperience, int lapExperienceCap) {
        if (restoredLapExperience < 0
                || restoredLapExperience > lapExperienceCap) {
            throw new IllegalArgumentException("Invalid lap experience.");
        }
        lapExperience = restoredLapExperience;
    }

    boolean consumePendingReward() {
        if (pendingRewards <= 0) {
            return false;
        }
        pendingRewards = 0;
        return true;
    }

    void restore(
            int restoredLevel,
            int restoredExperience,
            int restoredPendingRewards,
            boolean restoredTierFourUnlocked) {
        if (restoredLevel < 1
                || restoredExperience < 0
                || restoredExperience >= experienceForLevel(restoredLevel)
                || restoredPendingRewards < 0) {
            throw new IllegalArgumentException("Invalid roguelite competitor progress.");
        }
        level = restoredLevel;
        experience = restoredExperience;
        pendingRewards = restoredPendingRewards > 0 ? 1 : 0;
        tierFourUnlocked = restoredTierFourUnlocked;
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

    private void recordRaceAward(
            RogueliteExperienceAwards.Reason reason,
            int gained) {
        if (gained <= 0) {
            return;
        }
        raceExperience += gained;
        if (reason != null) {
            lastExperienceReason = reason;
            lastExperienceAmount = gained;
        }
    }

    private int experienceForLevel(int currentLevel) {
        return BASE_LEVEL_XP
                + Math.max(0, currentLevel - 1) * levelXpIncrement;
    }
}
