package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Resolves the identities occupying the selected race positions after a Triad Coup. */
public final class TriadCoupOrder {
    private TriadCoupOrder() {
    }

    public static List<Integer> resolve(
            List<Integer> raceOrder,
            int sourceVehicleId,
            int offenderVehicleId,
            int secondaryVehicleId) {
        if (raceOrder == null
                || sourceVehicleId < 0
                || offenderVehicleId < 0
                || sourceVehicleId == offenderVehicleId
                || !raceOrder.contains(Integer.valueOf(sourceVehicleId))
                || !raceOrder.contains(Integer.valueOf(offenderVehicleId))) {
            return Collections.emptyList();
        }

        List<Integer> rivals = new ArrayList<Integer>(2);
        rivals.add(Integer.valueOf(offenderVehicleId));
        if (secondaryVehicleId >= 0
                && secondaryVehicleId != sourceVehicleId
                && secondaryVehicleId != offenderVehicleId
                && raceOrder.contains(Integer.valueOf(secondaryVehicleId))) {
            rivals.add(Integer.valueOf(secondaryVehicleId));
        }
        Collections.sort(rivals, (left, right) ->
                Integer.compare(raceOrder.indexOf(right), raceOrder.indexOf(left)));

        List<Integer> resolved = new ArrayList<Integer>(1 + rivals.size());
        resolved.add(Integer.valueOf(sourceVehicleId));
        resolved.addAll(rivals);
        return Collections.unmodifiableList(resolved);
    }

    public static List<Integer> selectedPositions(
            List<Integer> raceOrder,
            List<Integer> resolvedOrder) {
        if (raceOrder == null || resolvedOrder == null || resolvedOrder.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> selected = new ArrayList<Integer>(resolvedOrder);
        Collections.sort(selected, (left, right) ->
                Integer.compare(raceOrder.indexOf(left), raceOrder.indexOf(right)));
        return Collections.unmodifiableList(selected);
    }
}
