package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Emits one overtake event after a changed race order remains stable. */
public final class RacePositionExperience {
    public static final float DEFAULT_CONFIRMATION_SECONDS = 0.75f;

    private final float confirmationSeconds;
    private final List<Integer> confirmedOrder = new ArrayList<Integer>();
    private final List<Integer> candidateOrder = new ArrayList<Integer>();
    private final List<Overtake> overtakes = new ArrayList<Overtake>();
    private final List<Overtake> overtakesView = Collections.unmodifiableList(overtakes);
    private float candidateAge;

    public RacePositionExperience() {
        this(DEFAULT_CONFIRMATION_SECONDS);
    }

    RacePositionExperience(float confirmationSeconds) {
        this.confirmationSeconds = Math.max(0f, confirmationSeconds);
    }

    public void reset(List<Integer> orderedVehicleIds) {
        replaceOrder(confirmedOrder, orderedVehicleIds);
        candidateOrder.clear();
        overtakes.clear();
        candidateAge = 0f;
    }

    public List<Overtake> update(
            List<Integer> orderedVehicleIds,
            float deltaSeconds) {
        overtakes.clear();
        if (!isValidOrder(orderedVehicleIds)) {
            return overtakesView;
        }
        if (confirmedOrder.isEmpty()
                || confirmedOrder.size() != orderedVehicleIds.size()) {
            reset(orderedVehicleIds);
            return overtakesView;
        }
        if (sameOrder(confirmedOrder, orderedVehicleIds)) {
            candidateOrder.clear();
            candidateAge = 0f;
            return overtakesView;
        }
        if (!sameOrder(candidateOrder, orderedVehicleIds)) {
            replaceOrder(candidateOrder, orderedVehicleIds);
            candidateAge = 0f;
        }
        candidateAge += sanitizeDelta(deltaSeconds);
        if (candidateAge < confirmationSeconds) {
            return overtakesView;
        }

        emitOvertakes();
        replaceOrder(confirmedOrder, candidateOrder);
        candidateOrder.clear();
        candidateAge = 0f;
        return overtakesView;
    }

    /** Returns whether the candidate is currently listed ahead of the reference vehicle. */
    public static boolean isListedAhead(
            List<Integer> orderedVehicleIds,
            int candidateVehicleId,
            int referenceVehicleId) {
        if (orderedVehicleIds == null
                || candidateVehicleId < 0
                || referenceVehicleId < 0
                || candidateVehicleId == referenceVehicleId) {
            return false;
        }
        int candidateIndex = orderedVehicleIds.indexOf(Integer.valueOf(candidateVehicleId));
        int referenceIndex = orderedVehicleIds.indexOf(Integer.valueOf(referenceVehicleId));
        return candidateIndex >= 0
                && referenceIndex >= 0
                && candidateIndex < referenceIndex;
    }

    private void emitOvertakes() {
        for (int currentIndex = 0; currentIndex < candidateOrder.size(); currentIndex++) {
            int vehicleId = candidateOrder.get(currentIndex).intValue();
            int previousIndex = confirmedOrder.indexOf(Integer.valueOf(vehicleId));
            if (previousIndex <= currentIndex) {
                continue;
            }
            int rivalsPassed = 0;
            for (int rivalIndex = 0; rivalIndex < confirmedOrder.size(); rivalIndex++) {
                int rivalId = confirmedOrder.get(rivalIndex).intValue();
                if (rivalIndex < previousIndex
                        && candidateOrder.indexOf(Integer.valueOf(rivalId)) > currentIndex) {
                    rivalsPassed++;
                }
            }
            if (rivalsPassed > 0) {
                overtakes.add(new Overtake(vehicleId, rivalsPassed));
            }
        }
    }

    private static boolean isValidOrder(List<Integer> order) {
        if (order == null || order.isEmpty()) {
            return false;
        }
        for (int i = 0; i < order.size(); i++) {
            Integer vehicleId = order.get(i);
            if (vehicleId == null || order.indexOf(vehicleId) != i) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameOrder(List<Integer> left, List<Integer> right) {
        if (left == null || right == null || left.size() != right.size()) {
            return false;
        }
        for (int i = 0; i < left.size(); i++) {
            if (!left.get(i).equals(right.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static void replaceOrder(List<Integer> target, List<Integer> source) {
        target.clear();
        if (source != null) {
            target.addAll(source);
        }
    }

    private static float sanitizeDelta(float deltaSeconds) {
        return Float.isNaN(deltaSeconds)
                        || Float.isInfinite(deltaSeconds)
                        || deltaSeconds <= 0f
                ? 0f
                : deltaSeconds;
    }

    public static final class Overtake {
        private final int vehicleId;
        private final int rivalsPassed;

        private Overtake(int vehicleId, int rivalsPassed) {
            this.vehicleId = vehicleId;
            this.rivalsPassed = rivalsPassed;
        }

        public int getVehicleId() {
            return vehicleId;
        }

        public int getRivalsPassed() {
            return rivalsPassed;
        }
    }
}
