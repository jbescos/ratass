package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ArenaMapRouteLookaheadTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void findsCornersBeyondTheObservationLookahead() {
        ArenaMap map = createMapWithCornerAtSixtyMeters();

        assertEquals(60f, map.findRouteNextCornerDistanceWorld(0f, 70f), EPSILON);
        assertEquals(40f, map.findRouteNextCornerDistanceWorld(0f, 40f), EPSILON);
        assertEquals(30f, map.findRouteNextCornerDistanceWorld(30f, 70f), EPSILON);
    }

    private static ArenaMap createMapWithCornerAtSixtyMeters() {
        float[] sampleX = {0f, 10f, 20f, 20f, 20f, 10f, 0f, 0f};
        float[] sampleY = {0f, 0f, 0f, 10f, 20f, 20f, 20f, 10f};
        float[] tangentX = {1f, 1f, 0f, 0f, -1f, -1f, 0f, 0f};
        float[] tangentY = {0f, 0f, 1f, 1f, 0f, 0f, -1f, -1f};
        float[] curvature = {0f, 0f, 0f, 0f, 0f, 0f, 0.2f, 0f};
        float[] zeroes = new float[8];
        float[] clearances = {3f, 3f, 3f, 3f, 3f, 3f, 3f, 3f};
        ArenaMap.RouteMetadata metadata =
                new ArenaMap.RouteMetadata(
                        10f,
                        80f,
                        sampleX,
                        sampleY,
                        tangentX,
                        tangentY,
                        curvature,
                        zeroes,
                        zeroes,
                        zeroes,
                        clearances,
                        clearances,
                        clearances,
                        0,
                        0,
                        0f,
                        0f,
                        0f,
                        0f,
                        null);
        return ArenaMap.builder("lookahead", "Lookahead")
                .solid(ArenaShape.rectangle(10f, 10f, 30f, 30f))
                .spawn(new SpawnPoint(0f, 0f, 0f))
                .routePoint(0f, 0f)
                .routePoint(20f, 0f)
                .routePoint(20f, 20f)
                .routePoint(0f, 20f)
                .routeMetadata(metadata)
                .build();
    }
}
