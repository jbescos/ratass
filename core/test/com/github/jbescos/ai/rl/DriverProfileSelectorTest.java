package com.github.jbescos.ai.rl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.Test;

public class DriverProfileSelectorTest {
    @Test
    public void reservesPlayerProfileAndAssignsEveryOpponentUniquely() {
        DriverProfileSelector selector =
                new DriverProfileSelector(new Random(17L));

        List<String> assignments =
                selector.selectUnique(createProfileIds(), "profile03", 9);

        assertEquals(9, assignments.size());
        assertEquals(9, new HashSet<String>(assignments).size());
        assertFalse(assignments.contains("profile03"));
    }

    @Test
    public void reshufflesProfilesForEachRace() {
        DriverProfileSelector selector =
                new DriverProfileSelector(new Random(31L));

        List<String> firstRace =
                selector.selectUnique(createProfileIds(), null, 9);
        List<String> secondRace =
                selector.selectUnique(createProfileIds(), null, 9);

        assertNotEquals(firstRace, secondRace);
    }

    private static List<String> createProfileIds() {
        List<String> profileIds = new ArrayList<String>();
        for (int i = 0; i < 10; i++) {
            profileIds.add(String.format("profile%02d", Integer.valueOf(i)));
        }
        return profileIds;
    }
}
