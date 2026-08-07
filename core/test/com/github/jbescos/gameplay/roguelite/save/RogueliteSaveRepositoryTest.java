package com.github.jbescos.gameplay.roguelite.save;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.github.jbescos.gameplay.roguelite.RogueliteCompetitionMode;
import com.github.jbescos.gameplay.roguelite.CustomGameRules;
import com.github.jbescos.gameplay.roguelite.RogueliteRun;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class RogueliteSaveRepositoryTest {
    @Test
    public void savesAndLoadsNewestValidGeneration() {
        MemoryStore store = new MemoryStore();
        RogueliteSaveRepository repository = new RogueliteSaveRepository(store);

        RogueliteSaveData first = saveData(2);
        assertTrue(repository.save(first));
        RogueliteSaveData second = saveData(3);
        second.playerWins = 2;
        assertTrue(repository.save(second));

        RogueliteSaveData loaded = repository.load();
        assertNotNull(loaded);
        assertEquals(3, loaded.roundNumber);
        assertEquals(2, loaded.playerWins);
    }

    @Test
    public void fallsBackWhenNewestSlotIsCorrupt() {
        MemoryStore store = new MemoryStore();
        RogueliteSaveRepository repository = new RogueliteSaveRepository(store);
        assertTrue(repository.save(saveData(4)));
        assertTrue(repository.save(saveData(5)));

        store.values.put("slot.b", "{broken");

        RogueliteSaveData loaded = repository.load();
        assertNotNull(loaded);
        assertEquals(4, loaded.roundNumber);
    }

    @Test
    public void committedSlotSurvivesFailureBeforeActivePointerFlush() {
        MemoryStore store = new MemoryStore();
        store.failOnFlush = 2;
        RogueliteSaveRepository repository = new RogueliteSaveRepository(store);

        assertFalse(repository.save(saveData(6)));
        RogueliteSaveData loaded = repository.load();
        assertNotNull(loaded);
        assertEquals(6, loaded.roundNumber);
    }

    @Test
    public void deleteRemovesEveryJournalSlot() {
        MemoryStore store = new MemoryStore();
        RogueliteSaveRepository repository = new RogueliteSaveRepository(store);
        assertTrue(repository.save(saveData(7)));

        repository.delete();

        assertNull(repository.load());
    }

    @Test
    public void championshipTransitionSurvivesSaveAndLoad() {
        MemoryStore store = new MemoryStore();
        RogueliteSaveRepository repository = new RogueliteSaveRepository(store);
        RogueliteSaveData data = saveData(18);
        data.championshipTransitionPending = true;

        assertTrue(repository.save(data));

        RogueliteSaveData loaded = repository.load();
        assertNotNull(loaded);
        assertTrue(loaded.championshipTransitionPending);
    }

    @Test
    public void liveRewardResumeContextSurvivesSaveAndLoad() {
        MemoryStore store = new MemoryStore();
        RogueliteSaveRepository repository = new RogueliteSaveRepository(store);
        RogueliteSaveData data = saveData(181);
        data.phase = RogueliteSaveData.PHASE_REWARD;
        data.rewardResumesRace = true;
        data.rewardCardIds.add("card:CLUB_TUNE");

        assertTrue(repository.save(data));

        RogueliteSaveData loaded = repository.load();
        assertNotNull(loaded);
        assertTrue(loaded.rewardResumesRace);
    }

    @Test
    public void removedInfiniteCompetitionModeIsRejected() {
        MemoryStore store = new MemoryStore();
        RogueliteSaveRepository repository = new RogueliteSaveRepository(store);
        RogueliteSaveData data = saveData(19);
        data.competitionMode = "infinite";

        assertFalse(repository.save(data));
        assertNull(repository.load());
    }

    @Test
    public void customRulesSurviveSaveAndLoad() {
        MemoryStore store = new MemoryStore();
        RogueliteSaveRepository repository = new RogueliteSaveRepository(store);
        RogueliteSaveData data = saveData(20);
        CustomGameRules rules = new CustomGameRules();
        rules.resetMaps(Arrays.asList("map001"));
        rules.setLaps(6);
        rules.setLevelXpIncrement(70);
        rules.setRacecraftXpPerLapCap(55);
        data.competitionMode = RogueliteCompetitionMode.CUSTOM.getId();
        data.customRules = rules.snapshot();

        assertTrue(repository.save(data));

        RogueliteSaveData loaded = repository.load();
        assertNotNull(loaded);
        assertEquals(RogueliteCompetitionMode.CUSTOM.getId(), loaded.competitionMode);
        assertEquals(6, loaded.customRules.laps);
        assertEquals(70, loaded.customRules.levelXpIncrement);
        assertEquals(55, loaded.customRules.racecraftXpPerLapCap);
    }

    private static RogueliteSaveData saveData(int round) {
        RogueliteSaveData data = new RogueliteSaveData();
        data.roundNumber = round;
        data.mapId = "map001";
        data.mapOrder.add("map000");
        data.mapOrder.add("map001");
        data.themeName = "gt3";
        data.carCount = 10;
        data.raceLaps = 5;
        data.run = new RogueliteRun(17L).snapshot();
        return data;
    }

    private static final class MemoryStore implements RogueliteSaveStore {
        private final Map<String, String> values = new LinkedHashMap<String, String>();
        private int flushes;
        private int failOnFlush = -1;

        @Override
        public String get(String key) {
            String value = values.get(key);
            return value == null ? "" : value;
        }

        @Override
        public void put(String key, String value) {
            values.put(key, value);
        }

        @Override
        public void remove(String key) {
            values.remove(key);
        }

        @Override
        public void flush() {
            flushes++;
            if (flushes == failOnFlush) {
                throw new RuntimeException("simulated interruption");
            }
        }
    }
}
