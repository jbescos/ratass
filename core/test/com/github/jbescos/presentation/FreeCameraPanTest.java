package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

import com.badlogic.gdx.math.Vector2;
import org.junit.Test;

public class FreeCameraPanTest {
    @Test
    public void dragKeepsTheMapUnderThePointer() {
        Vector2 position = new Vector2(10f, 10f);

        FreeCameraPan.applyDrag(
                position,
                10f,
                20f,
                100f,
                50f,
                2f,
                1000f,
                500f);

        assertEquals(8f, position.x, 0.0001f);
        assertEquals(14f, position.y, 0.0001f);
    }

    @Test
    public void invalidViewportLeavesTheCameraUntouched() {
        Vector2 position = new Vector2(4f, 7f);

        FreeCameraPan.applyDrag(position, 20f, 20f, 100f, 50f, 1f, 0f, 500f);

        assertEquals(4f, position.x, 0.0001f);
        assertEquals(7f, position.y, 0.0001f);
    }
}
