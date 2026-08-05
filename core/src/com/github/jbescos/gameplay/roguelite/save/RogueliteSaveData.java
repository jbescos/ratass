package com.github.jbescos.gameplay.roguelite.save;

import com.github.jbescos.gameplay.roguelite.RogueliteCompetitionMode;
import com.github.jbescos.gameplay.roguelite.CustomGameRules;
import com.github.jbescos.gameplay.roguelite.RogueliteRun;
import java.util.ArrayList;
import java.util.List;

public final class RogueliteSaveData {
    public static final int CURRENT_VERSION = 3;
    public static final String PHASE_RACE = "race";
    public static final String PHASE_RESULT = "result";
    public static final String PHASE_REWARD = "reward";
    public static final String PHASE_END = "end";

    public int version = CURRENT_VERSION;
    public String phase = PHASE_RACE;
    public String mapId = "";
    public List<String> mapOrder = new ArrayList<String>();
    public int roundNumber = 1;
    public int playerWins;
    public String themeName = "gt3";
    public int carCount = 1;
    public int playerCarIndex;
    public int raceLaps = 1;
    public String competitionMode = RogueliteCompetitionMode.CHAMPIONSHIP.getId();
    public CustomGameRules.Snapshot customRules =
            new CustomGameRules.Snapshot();
    public boolean championshipTransitionPending;
    public List<Integer> pendingEliminatedVehicleIds =
            new ArrayList<Integer>();
    public boolean runEnded;
    public boolean playerWonRun;
    public RogueliteRun.Snapshot run = new RogueliteRun.Snapshot();
    public List<RosterEntry> roster = new ArrayList<RosterEntry>();
    public List<String> rewardCardIds = new ArrayList<String>();

    public boolean isStructurallyValid() {
        if (version != CURRENT_VERSION
                || !isKnownPhase(phase)
                || mapId == null
                || mapId.length() == 0
                || mapOrder == null
                || mapOrder.isEmpty()
                || !mapOrder.contains(mapId)
                || roundNumber < 1
                || playerWins < 0
                || themeName == null
                || themeName.length() == 0
                || carCount < 1
                || playerCarIndex < 0
                || raceLaps < 1
                || !RogueliteCompetitionMode.isKnownId(competitionMode)
                || (RogueliteCompetitionMode.CUSTOM.getId().equals(competitionMode)
                        && (customRules == null
                                || !customRules.isStructurallyValid()))
                || run == null
                || roster == null
                || pendingEliminatedVehicleIds == null
                || rewardCardIds == null) {
            return false;
        }
        for (int i = 0; i < pendingEliminatedVehicleIds.size(); i++) {
            Integer vehicleId = pendingEliminatedVehicleIds.get(i);
            if (vehicleId == null
                    || vehicleId.intValue() < 0
                    || pendingEliminatedVehicleIds.indexOf(vehicleId) != i) {
                return false;
            }
        }
        if (PHASE_REWARD.equals(phase) && rewardCardIds.isEmpty()) {
            return false;
        }
        for (int i = 0; i < mapOrder.size(); i++) {
            String map = mapOrder.get(i);
            if (map == null || map.length() == 0 || mapOrder.indexOf(map) != i) {
                return false;
            }
        }
        return true;
    }

    private static boolean isKnownPhase(String value) {
        return PHASE_RACE.equals(value)
                || PHASE_RESULT.equals(value)
                || PHASE_REWARD.equals(value)
                || PHASE_END.equals(value);
    }

    public static final class RosterEntry {
        public int vehicleId;
        public int totalPoints;
        public int nextGridPosition;
    }
}
