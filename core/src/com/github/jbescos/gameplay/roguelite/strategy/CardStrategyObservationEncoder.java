package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.DriverProfileMetadata;
import com.github.jbescos.gameplay.roguelite.RogueliteCarStatSnapshot;
import com.github.jbescos.gameplay.roguelite.RogueliteCardCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import com.github.jbescos.gameplay.roguelite.RogueliteCardOffer;
import com.github.jbescos.gameplay.roguelite.RogueliteLoadout;
import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;
import java.util.List;

/** Converts one candidate and its strategic context into a stable normalized feature vector. */
public final class CardStrategyObservationEncoder {
    public static final int MAX_OPPONENTS = 9;
    private static final int CARD_COUNT = RogueliteCardId.values().length;
    private static final int DRIVER_FEATURE_COUNT = 9;
    private static final int GLOBAL_FEATURE_COUNT = 7;
    private static final int LOADOUT_SLOT_FEATURE_COUNT = 2 + CARD_COUNT;
    private static final int CANDIDATE_FEATURE_COUNT =
            2 + RogueliteSlotType.values().length + 1
                    + DRIVER_FEATURE_COUNT + CARD_COUNT + 5;
    private static final int OPPONENT_FEATURE_COUNT = 4 + DRIVER_FEATURE_COUNT;
    private static final int OBSERVATION_SIZE =
            GLOBAL_FEATURE_COUNT
                    + DRIVER_FEATURE_COUNT
                    + RogueliteLoadout.MODIFICATION_SLOT_COUNT * LOADOUT_SLOT_FEATURE_COUNT
                    + CANDIDATE_FEATURE_COUNT
                    + MAX_OPPONENTS * OPPONENT_FEATURE_COUNT;

    public int getObservationSize() {
        return OBSERVATION_SIZE;
    }

    public float[] encode(
            CardStrategyDecision decision,
            RogueliteCardOffer candidate) {
        float[] result = new float[OBSERVATION_SIZE];
        encode(decision, candidate, result);
        return result;
    }

    public void encode(
            CardStrategyDecision decision,
            RogueliteCardOffer candidate,
            float[] result) {
        if (decision == null || result == null || result.length < OBSERVATION_SIZE) {
            throw new IllegalArgumentException("Card strategy observation buffer is invalid.");
        }
        for (int i = 0; i < OBSERVATION_SIZE; i++) {
            result[i] = 0f;
        }
        Cursor cursor = new Cursor(result);
        CardStrategyContext context = decision.getContext();
        int fieldSize = Math.max(1, context.getOpponents().size() + 1);
        cursor.add(clamp01(decision.getProgress().getLevel() / 30f));
        cursor.add(ratio(
                decision.getProgress().getExperience(),
                decision.getProgress().getExperienceForNextLevel()));
        cursor.add(positionRatio(context.getCircuitIndex(), context.getCircuitCount()));
        cursor.add(positionRatio(context.getLap(), context.getLapCount()));
        cursor.add(positionRatio(context.getRacePosition(), fieldSize));
        cursor.add(positionRatio(context.getChampionshipPosition(), fieldSize));
        cursor.add(ratio(context.getRemainingCircuits(), context.getCircuitCount()));

        RogueliteLoadout loadout = decision.getProgress().getLoadout();
        writeDriver(cursor, decision.getDriverCatalog().get(loadout.getDriverProfileId()));
        for (RogueliteSlotType slot : RogueliteSlotType.modificationSlots()) {
            RogueliteCardId cardId = loadout.get(slot);
            cursor.add(cardId == null ? 0f : 1f);
            cursor.add(cardId == null
                    ? 0f
                    : RogueliteCardCatalog.get(cardId).getTier()
                            / (float) RogueliteCardCatalog.MAX_CARD_TIER);
            writeCardOneHot(cursor, cardId);
        }

        writeCandidate(cursor, decision, candidate);
        writeOpponents(cursor, context.getOpponents(), fieldSize);
        if (cursor.index != OBSERVATION_SIZE) {
            throw new IllegalStateException("Card strategy observation layout is inconsistent.");
        }
    }

    private static void writeCandidate(
            Cursor cursor,
            CardStrategyDecision decision,
            RogueliteCardOffer candidate) {
        cursor.add(candidate == null ? 1f : 0f);
        cursor.add(candidate != null && candidate.isDriver() ? 1f : 0f);
        for (RogueliteSlotType slot : RogueliteSlotType.values()) {
            cursor.add(candidate != null && candidate.getSlotType() == slot ? 1f : 0f);
        }
        cursor.add(candidate == null
                ? 0f
                : candidate.getTier() / (float) RogueliteCardCatalog.MAX_CARD_TIER);
        writeDriver(cursor, candidate != null && candidate.isDriver()
                ? candidate.getDriver() : null);
        RogueliteCardId candidateId = candidate == null || candidate.isDriver()
                ? null : candidate.getCard().getId();
        writeCardOneHot(cursor, candidateId);

        RogueliteLoadout loadout = decision.getProgress().getLoadout();
        RogueliteCarStatSnapshot before = RogueliteCarStatSnapshot.from(loadout, null);
        RogueliteCarStatSnapshot after = RogueliteCarStatSnapshot.from(loadout, candidateId);
        cursor.add(clampSigned(after.getAccelerationMultiplier() - before.getAccelerationMultiplier()));
        cursor.add(clampSigned(after.getMaxSpeedMultiplier() - before.getMaxSpeedMultiplier()));
        cursor.add(clampSigned(after.getGripMultiplier() - before.getGripMultiplier()));
        cursor.add(clampSigned(before.getMassMultiplier() - after.getMassMultiplier()));
        cursor.add(clampSigned(after.getAerodynamicEfficiency() - before.getAerodynamicEfficiency()));

    }

    private static void writeOpponents(
            Cursor cursor,
            List<CardStrategyContext.Opponent> opponents,
            int fieldSize) {
        for (int i = 0; i < MAX_OPPONENTS; i++) {
            CardStrategyContext.Opponent opponent = i < opponents.size()
                    ? opponents.get(i) : null;
            cursor.add(opponent == null ? 0f : 1f);
            cursor.add(opponent == null ? 0f : clamp01(opponent.getLevel() / 30f));
            cursor.add(opponent == null ? 0f : positionRatio(opponent.getRacePosition(), fieldSize));
            cursor.add(opponent == null ? 0f : positionRatio(
                    opponent.getChampionshipPosition(), fieldSize));
            writeDriver(cursor, opponent == null ? null : opponent.getDriver());
        }
    }

    private static void writeDriver(Cursor cursor, DriverProfileMetadata driver) {
        cursor.add(driver == null ? 0f : driver.getPaceRating() / 100f);
        cursor.add(driver == null ? 0f : driver.getControlRating() / 100f);
        cursor.add(driver == null ? 0f : driver.getConsistencyRating() / 100f);
        cursor.add(driver == null ? 0f : driver.getFinishRate());
        cursor.add(driver == null ? 0f : clamp01(driver.getAverageFastestLapSeconds() / 120f));
        cursor.add(driver == null ? 0f : clamp01(driver.getAverageLapSeconds() / 120f));
        cursor.add(driver == null ? 0f : clamp01(driver.getAverageOffRoadPercent() / 100f));
        cursor.add(driver == null ? 0f : clamp01(driver.getAverageDriftPercent() / 100f));
        cursor.add(driver == null ? 0f : clamp01(driver.getMaximumSpeedKph() / 400f));
    }

    private static void writeCardOneHot(Cursor cursor, RogueliteCardId cardId) {
        for (int i = 0; i < CARD_COUNT; i++) {
            cursor.add(cardId != null && cardId.ordinal() == i ? 1f : 0f);
        }
    }

    private static float ratio(int numerator, int denominator) {
        return denominator <= 0 ? 0f : clamp01(numerator / (float) denominator);
    }

    private static float positionRatio(int position, int count) {
        if (position <= 0 || count <= 1) {
            return 0f;
        }
        return clamp01((position - 1f) / (count - 1f));
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static float clampSigned(float value) {
        return Math.max(-1f, Math.min(1f, value));
    }

    private static final class Cursor {
        private final float[] values;
        private int index;

        private Cursor(float[] values) {
            this.values = values;
        }

        private void add(float value) {
            values[index++] = value;
        }
    }
}
