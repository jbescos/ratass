package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ArenaMapManualSteeringRestrictionTest {
    @Test
    public void preservesAndScalesRestrictedIntersectionZones() {
        ArenaMap map =
                ArenaMap.builder("intersection", "Intersection")
                        .solid(ArenaShape.rectangle(0f, 0f, 20f, 20f))
                        .spawn(new SpawnPoint(0f, 0f, 0f))
                        .manualSteeringRestrictedZone(2f, 3f, 1.5f)
                        .scale(2f)
                        .build();

        assertEquals(1, map.getManualSteeringRestrictedZoneCount());
        assertTrue(map.isManualSteeringRestricted(4f, 6f));
        assertTrue(map.isManualSteeringRestricted(7f, 6f));
        assertFalse(map.isManualSteeringRestricted(7.01f, 6f));
    }
}
