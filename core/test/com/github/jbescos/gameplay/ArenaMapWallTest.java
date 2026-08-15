package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;

import com.badlogic.gdx.math.Vector2;
import org.junit.Test;

public class ArenaMapWallTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void preservesAndScalesWallSegments() {
        ArenaMap map =
                ArenaMap.builder("walls", "Walls")
                        .solid(ArenaShape.rectangle(0f, 0f, 10f, 10f))
                        .spawn(new SpawnPoint(0f, 0f, 0f))
                        .wall(-2f, 1f, 3f, 4f)
                        .wall(5f, -3f, 7f, -3f)
                        .scale(2f)
                        .build();

        assertEquals(2, map.getWallCount());
        assertWall(map, 0, -4f, 2f, 6f, 8f);
        assertWall(map, 1, 10f, -6f, 14f, -6f);
    }

    private static void assertWall(
            ArenaMap map,
            int index,
            float startX,
            float startY,
            float endX,
            float endY) {
        Vector2 start = new Vector2();
        Vector2 end = new Vector2();
        map.getWallStart(index, start);
        map.getWallEnd(index, end);
        assertEquals(startX, start.x, EPSILON);
        assertEquals(startY, start.y, EPSILON);
        assertEquals(endX, end.x, EPSILON);
        assertEquals(endY, end.y, EPSILON);
    }
}
